from opentelemetry import trace
from opentelemetry.sdk.resources import Resource
from opentelemetry.sdk.trace import TracerProvider
from opentelemetry.sdk.trace.export import BatchSpanProcessor
from opentelemetry.exporter.otlp.proto.grpc.trace_exporter import OTLPSpanExporter
from opentelemetry.instrumentation.fastapi import FastAPIInstrumentor


def setup_tracing(app):
    provider = trace.get_tracer_provider()

    # CrewAI already installed an SDK provider
    if isinstance(provider, TracerProvider):
        provider.add_span_processor(
            BatchSpanProcessor(
                OTLPSpanExporter(
                    endpoint="http://localhost:4317",
                    insecure=True,
                )
            )
        )
        if provider.resource.attributes.get("service.name") == "unknown_service":
            provider._resource = Resource.create({"service.name": "tracepilot-worker"})

    else:
        provider = TracerProvider(
            resource=Resource.create({"service.name": "tracepilot-worker"})
        )

        provider.add_span_processor(
            BatchSpanProcessor(
                OTLPSpanExporter(
                    endpoint="http://localhost:4317",
                    insecure=True,
                )
            )
        )

        trace.set_tracer_provider(provider)

    FastAPIInstrumentor.instrument_app(app)

    return trace.get_tracer("tracepilot-worker")