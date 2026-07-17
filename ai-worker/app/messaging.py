from __future__ import annotations

import asyncio
import json
import os
import ssl

import aio_pika
from aio_pika.abc import (
    AbstractIncomingMessage,
    AbstractRobustChannel,
    AbstractRobustConnection,
    AbstractQueue,
)

from opentelemetry import context, trace
from opentelemetry.trace import Status, StatusCode
from opentelemetry.propagate import extract

from app.messaging_schemas import (
    AuditJobMessage,
    AuditResultMessage,
    AuditProgressMessage,
)
from utils.logger import Logger

from app.agents import run_audit

logger = Logger()

tracer = trace.get_tracer("tracepilot-worker")

RABBITMQ_HOST = os.getenv("RABBITMQ_HOST", "localhost")
RABBITMQ_PORT = int(os.getenv("RABBITMQ_PORT", "5671"))
RABBITMQ_USERNAME = os.getenv("RABBITMQ_USERNAME", "guest")
RABBITMQ_PASSWORD = os.getenv("RABBITMQ_PASSWORD", "guest")
RABBITMQ_VHOST = os.getenv("RABBITMQ_VHOST", "/")
JOBS_QUEUE = os.getenv("AUDIT_JOBS_QUEUE", "audit.jobs")
DLQ_QUEUE = os.getenv("AUDIT_JOBS_DLQ", "audit.jobs.dlq")
PROGRESS_QUEUE = os.getenv("AUDIT_PROGRESS_QUEUE", "audit.progress")
RESULTS_QUEUE = os.getenv("AUDIT_RESULTS_QUEUE", "audit.results")
PREFETCH_COUNT = int(os.getenv("AUDIT_WORKER_PREFETCH", "4"))
RABBITMQ_SSL = os.getenv("RABBITMQ_SSL", "true").lower() == "true"
ssl_context = ssl.create_default_context() if RABBITMQ_SSL else None


class AuditConsumer:
    def __init__(self) -> None:
        self._connection: AbstractRobustConnection | None = None
        self._channel: AbstractRobustChannel | None = None
        self._jobs_queue: AbstractQueue | None = None
        self._consumer_tag: str | None = None

    async def start(self) -> None:
        self._connection = await aio_pika.connect_robust(
            host=RABBITMQ_HOST,
            port=RABBITMQ_PORT,
            login=RABBITMQ_USERNAME,
            password=RABBITMQ_PASSWORD,
            virtualhost=RABBITMQ_VHOST,
            ssl=RABBITMQ_SSL,
            ssl_context=ssl_context,
        )
        self._channel = await self._connection.channel()
        await self._channel.set_qos(prefetch_count=PREFETCH_COUNT)

        # DLQ must exist before audit.jobs references it as a dead-letter target.
        await self._channel.declare_queue(DLQ_QUEUE, durable=True)

        self._jobs_queue = await self._channel.get_queue(JOBS_QUEUE)

        await self._channel.get_queue(PROGRESS_QUEUE)

        # Declared here so the first publish_result() call never races a missing queue.
        await self._channel.get_queue(RESULTS_QUEUE)

        self._consumer_tag = await self._jobs_queue.consume(self._on_message)
        logger.info(
            f"audit_consumer_started queue={JOBS_QUEUE} prefetch={PREFETCH_COUNT}"
        )

    async def stop(self) -> None:
        if self._jobs_queue and self._consumer_tag:
            await self._jobs_queue.cancel(self._consumer_tag)
        if self._connection:
            await self._connection.close()
        logger.info("audit_consumer_stopped")

    async def _on_message(self, message: AbstractIncomingMessage) -> None:
        headers = message.headers if message.headers is not None else {}
        ctx = extract(headers)
        token = context.attach(ctx)

        try:
            # Start the trace span for the worker execution
            with tracer.start_as_current_span("crew_execute_audit") as span:
                try:
                    payload = json.loads(message.body.decode("utf-8"))
                    job = AuditJobMessage.model_validate(payload)

                    audit_id = job.audit_id
                    span.set_attribute("audit.id", str(audit_id))
                    span.set_attribute("agent.tool", "generic")
                    span.set_attribute("llm.provider", "gemini")
                    span.set_attribute("trace.length", len(job.raw_trace))
                    span.set_attribute("messaging.system", "rabbitmq")
                    span.set_attribute("messaging.destination", JOBS_QUEUE)
                    span.set_attribute("messaging.operation", "process")
                except Exception as exc:
                    logger.error(f"audit_job_malformed error={str(exc)}")
                    span.record_exception(exc)
                    span.set_status(Status(StatusCode.ERROR, str(exc)))
                    await message.reject(requeue=False)
                    return

                audit_id = job.audit_id
                logger.info(f"audit_job_received audit_id={audit_id}")

                try:
                    history_prompt = "\n".join(
                        f"- {h.recorded_at}: score={h.reliability_score}, summary={h.signal_summary}"
                        for h in job.prior_history
                    )

                    loop = asyncio.get_running_loop()

                    def on_progress(agent_type: str, status: str) -> None:
                        msg = AuditProgressMessage(
                            audit_id=audit_id,
                            agent_type=agent_type,
                            status=status,
                        )

                        asyncio.run_coroutine_threadsafe(
                            self._publish_progress(msg),
                            loop,
                        )

                    report = await asyncio.wait_for(
                        asyncio.to_thread(
                            run_audit,
                            job.raw_trace,
                            history_prompt,
                            job.suspicious_content,
                            on_progress,
                        ),
                        timeout=300,
                    )
                    result = AuditResultMessage(
                        audit_id=audit_id, status="COMPLETE", report=report
                    )
                    logger.info(f"audit_job_completed audit_id={audit_id}")
                    span.set_attribute("audit.status", result.status)
                except Exception as exc:
                    logger.error(
                        f"audit_job_failed audit_id={audit_id} error={str(exc)}"
                    )
                    result = AuditResultMessage(
                        audit_id=audit_id, status="FAILED", error=str(exc)
                    )

                try:
                    await self._publish_result(result)
                except Exception as exc:
                    logger.error(
                        f"audit_result_publish_failed audit_id={audit_id} error={str(exc)}"
                    )
                    await message.reject(requeue=False)
                    return

                await message.ack()
                span.set_status(Status(StatusCode.OK))
        finally:
            context.detach(token)

    async def _publish_result(self, result: AuditResultMessage) -> None:
        if self._channel is None:
            raise RuntimeError("channel not initialized")
        await self._channel.default_exchange.publish(
            aio_pika.Message(
                body=result.model_dump_json(by_alias=True).encode("utf-8"),
                content_type="application/json",
                delivery_mode=aio_pika.DeliveryMode.PERSISTENT,
            ),
            routing_key=RESULTS_QUEUE,
        )

    async def _publish_progress(self, progress: AuditProgressMessage) -> None:
        if self._channel is None:
            return
        try:
            await self._channel.default_exchange.publish(
                aio_pika.Message(
                    body=progress.model_dump_json(by_alias=True).encode("utf-8"),
                    content_type="application/json",
                    delivery_mode=aio_pika.DeliveryMode.PERSISTENT,
                ),
                routing_key=PROGRESS_QUEUE,
            )
        except Exception as exc:
            logger.error(f"audit_progress_publish_failed error={str(exc)}")


audit_consumer = AuditConsumer()
