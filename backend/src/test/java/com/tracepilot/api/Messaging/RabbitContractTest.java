package com.tracepilot.api.Messaging;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tracepilot.api.Config.RabbitConfig;
import com.tracepilot.api.DTO.Messages.AuditJobMessage;
import com.tracepilot.api.DTO.Messages.AuditProgressMessage;
import com.tracepilot.api.Enums.AuditProgressStatus;
import com.tracepilot.api.Enums.TypesOfAgent;

@Testcontainers
class RabbitContractTest {

    @Container
    static final RabbitMQContainer RABBITMQ = new RabbitMQContainer("rabbitmq:4.3-management");

    private final ObjectMapper objectMapper = new ObjectMapper();

    private CachingConnectionFactory newConnectionFactory() {
        CachingConnectionFactory cf = new CachingConnectionFactory(RABBITMQ.getHost());
        cf.setPort(RABBITMQ.getAmqpPort());
        cf.setUsername(RABBITMQ.getAdminUsername());
        cf.setPassword(RABBITMQ.getAdminPassword());
        return cf;
    }

    private void declareTopology(CachingConnectionFactory cf) {
        RabbitAdmin admin = new RabbitAdmin(cf);
        RabbitConfig config = new RabbitConfig();
        admin.declareExchange(config.tracepilotExchange());
        admin.declareQueue(config.auditJobsDlq());
        admin.declareQueue(config.auditJobsQueue());
        admin.declareQueue(config.auditProgressQueue());
        admin.declareQueue(config.auditResultsDlq());
        admin.declareQueue(config.auditResultsQueue());
        admin.declareBinding(config.auditJobsBinding(config.auditJobsQueue(), config.tracepilotExchange()));
        admin.declareBinding(config.auditJobsDlqBinding(config.auditJobsDlq(), config.tracepilotExchange()));
        admin.declareBinding(config.auditProgressBinding(config.auditProgressQueue(), config.tracepilotExchange()));
        admin.declareBinding(config.auditResultsBinding(config.auditResultsQueue(), config.tracepilotExchange()));
        admin.declareBinding(config.auditResultsDlqBinding(config.auditResultsDlq(), config.tracepilotExchange()));
    }

    @Test
    void auditJobMessagePublishedByBackendIsShapedForPythonConsumer() throws Exception {
        CachingConnectionFactory cf = newConnectionFactory();
        declareTopology(cf);
        RabbitTemplate template = new RabbitTemplate(cf);

        AuditJobMessage job = new AuditJobMessage(
                UUID.randomUUID(), UUID.randomUUID(), "Sample trace", "raw trace body",
                "acme/repo", "generic", "PASTED_TEXT", false,
                List.of(new AuditJobMessage.PriorReliability(80, "stable", "2026-07-01T00:00:00Z")));

        String json = objectMapper.writeValueAsString(job);
        template.convertAndSend(RabbitConfig.EXCHANGE_NAME, "audit.job",
                new Message(json.getBytes(), new MessageProperties()));

        BlockingQueue<String> received = new ArrayBlockingQueue<>(1);
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer(cf);
        container.setQueueNames("audit.jobs");
        container.setMessageListener(message -> received.add(new String(message.getBody())));
        container.start();

        String body = received.poll(10, java.util.concurrent.TimeUnit.SECONDS);
        container.stop();

        assertThat(body).isNotNull();
        JsonNode node = objectMapper.readTree(body);

        // These are exactly the field names ai-worker's AuditJobMessage(CamelModel)
        // expects via to_camel aliasing -- audit_id -> auditId, raw_trace -> rawTrace, etc.
        assertThat(node.has("auditId")).isTrue();
        assertThat(node.has("userId")).isTrue();
        assertThat(node.has("rawTrace")).isTrue();
        assertThat(node.has("repoName")).isTrue();
        assertThat(node.has("agentTool")).isTrue();
        assertThat(node.has("inputSource")).isTrue();
        assertThat(node.has("suspiciousContent")).isTrue();
        assertThat(node.has("priorHistory")).isTrue();
        assertThat(node.get("priorHistory").get(0).has("reliabilityScore")).isTrue();
        assertThat(node.get("priorHistory").get(0).has("signalSummary")).isTrue();
        assertThat(node.get("priorHistory").get(0).has("recordedAt")).isTrue();
    }

    @Test
    void progressMessageShapedLikePythonWorkerIsDeserializedByBackendListener() throws Exception {
        CachingConnectionFactory cf = newConnectionFactory();
        declareTopology(cf);
        RabbitTemplate template = new RabbitTemplate(cf);

        UUID auditId = UUID.randomUUID();
        String workerJson = """
                {"auditId":"%s","agentType":"TRACE_LOOP_EFFICIENCY","status":"STARTED"}
                """.formatted(auditId).strip();

        template.convertAndSend(RabbitConfig.EXCHANGE_NAME, "audit.progress",
                new Message(workerJson.getBytes(), new MessageProperties()));

        BlockingQueue<AuditProgressMessage> received = new ArrayBlockingQueue<>(1);
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer(cf);
        container.setQueueNames("audit.progress");
        container.setMessageListener(message -> {
            try {
                JsonNode node = objectMapper.readTree(message.getBody());
                AuditProgressMessage parsed = new AuditProgressMessage(
                        UUID.fromString(node.get("auditId").asText()),
                        TypesOfAgent.valueOf(node.get("agentType").asText()),
                        node.has("step") ? node.get("step").asText(null) : null,
                        AuditProgressStatus.valueOf(node.get("status").asText()),
                        node.has("message") ? node.get("message").asText(null) : null);
                received.add(parsed);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        container.start();

        AuditProgressMessage parsed = received.poll(10, java.util.concurrent.TimeUnit.SECONDS);
        container.stop();

        assertThat(parsed).isNotNull();
        assertThat(parsed.auditId()).isEqualTo(auditId);
        assertThat(parsed.agentType()).isEqualTo(TypesOfAgent.TRACE_LOOP_EFFICIENCY);
        assertThat(parsed.status()).isEqualTo(AuditProgressStatus.STARTED);
    }

    @Test
    void resultMessageShapedLikePythonWorkerHasFieldsBackendListenerReadsManually() throws Exception {
        CachingConnectionFactory cf = newConnectionFactory();
        declareTopology(cf);
        RabbitTemplate template = new RabbitTemplate(cf);

        UUID auditId = UUID.randomUUID();
        String workerJson = """
                {"auditId":"%s","status":"FAILED","report":null,"error":"LLM timeout"}
                """.formatted(auditId).strip();

        template.convertAndSend(RabbitConfig.EXCHANGE_NAME, "audit.result",
                new Message(workerJson.getBytes(), new MessageProperties()));

        BlockingQueue<JsonNode> received = new ArrayBlockingQueue<>(1);
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer(cf);
        container.setQueueNames("audit.results");
        container.setMessageListener(message -> {
            try {
                received.add(objectMapper.readTree(message.getBody()));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        container.start();

        JsonNode node = received.poll(10, java.util.concurrent.TimeUnit.SECONDS);
        container.stop();

        assertThat(node).isNotNull();
        assertThat(node.path("auditId").asText()).isEqualTo(auditId.toString());
        assertThat(node.path("status").asText()).isEqualTo("FAILED");
        assertThat(node.path("error").asText()).isEqualTo("LLM timeout");
        assertThat(node.has("report")).isTrue();
    }

    @Test
    void auditJobsQueueDeadLettersToDlqOnReject() throws Exception {
        CachingConnectionFactory cf = newConnectionFactory();
        declareTopology(cf);
        RabbitTemplate template = new RabbitTemplate(cf);

        template.convertAndSend(RabbitConfig.EXCHANGE_NAME, "audit.job",
                new Message("{\"malformed\":true}".getBytes(), new MessageProperties()));

        BlockingQueue<byte[]> received = new ArrayBlockingQueue<>(1);
        SimpleMessageListenerContainer rejecting = new SimpleMessageListenerContainer(cf);
        rejecting.setQueueNames("audit.jobs");
        rejecting.setDefaultRequeueRejected(false);
        rejecting.setMessageListener(message -> {
            throw new org.springframework.amqp.AmqpRejectAndDontRequeueException("simulated bad message");
        });
        rejecting.start();

        SimpleMessageListenerContainer dlqListener = new SimpleMessageListenerContainer(cf);
        dlqListener.setQueueNames("audit.jobs.dlq");
        dlqListener.setMessageListener(message -> received.add(message.getBody()));
        dlqListener.start();

        byte[] dlqBody = received.poll(10, java.util.concurrent.TimeUnit.SECONDS);
        rejecting.stop();
        dlqListener.stop();

        assertThat(dlqBody).isNotNull();
        assertThat(new String(dlqBody)).contains("malformed");
    }
}
