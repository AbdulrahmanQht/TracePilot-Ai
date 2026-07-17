package com.tracepilot.api.Integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tracepilot.api.DTO.Response.AuditResponse;
import com.tracepilot.api.DTO.Response.AuthResponse;

class AuditLifecycleIntegrationTest extends IntegrationTestBase {

        private final ObjectMapper objectMapper = new ObjectMapper();

        private String registerAndGetAccessToken(String email) {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                String body = """
                                {"email":"%s","password":"SuperSecret123!","displayName":"Audit Tester"}
                                """.formatted(email);
                ResponseEntity<AuthResponse> response = restTemplate.postForEntity(
                                "/api/v1/auth/register", new HttpEntity<>(body, headers), AuthResponse.class);
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                return response.getBody().accessToken();
        }

        private HttpHeaders authJsonHeaders(String accessToken) {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.setBearerAuth(accessToken);
                return headers;
        }

        @Test
        void submittingAnAudit_persistsItPendingAndPublishesARealJobMessage() {
                String accessToken = registerAndGetAccessToken("audit-" + UUID.randomUUID() + "@example.com");

                String requestBody = """
                                {"rawTrace":"$ npm test\\nsession started 2026-07-17T10:00:00\\nagent ran tests\\ntests passed with exit code 0",
                                 "title":"Integration test audit","repoName":"tracepilot-it",
                                 "agentTool":"GENERIC","inputSource":"PASTED_TEXT"}
                                """;
                ResponseEntity<AuditResponse> response = restTemplate.exchange(
                                "/api/v1/audits", HttpMethod.POST,
                                new HttpEntity<>(requestBody, authJsonHeaders(accessToken)), AuditResponse.class);

                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
                AuditResponse audit = response.getBody();
                assertThat(audit).isNotNull();
                assertThat(audit.status().name()).isEqualTo("PENDING");
                assertThat(audit.title()).isEqualTo("Integration test audit");

                Message jobMessage = rabbitTemplate.receive("audit.jobs", 5000);
                assertThat(jobMessage).as("a real job message should have been published").isNotNull();

                JsonNode jobJson = readJson(jobMessage);
                assertThat(jobJson.path("auditId").asText()).isEqualTo(audit.id().toString());
                assertThat(jobJson.path("rawTrace").asText()).contains("agent ran tests");
                assertThat(jobJson.path("agentTool").asText()).isEqualTo("GENERIC");
        }

        @Test
        void aSimulatedWorkerCompletion_isPersistedByTheRealResultListener() {
                String accessToken = registerAndGetAccessToken("audit-complete-" + UUID.randomUUID() + "@example.com");

                String requestBody = """
                                {"rawTrace":"$ npm test\\nsession started 2026-07-17T10:00:00\\nagent ran tests\\ntests passed with exit code 0",
                                 "title":"Completion test audit","agentTool":"CLAUDE_CODE","inputSource":"PASTED_TEXT"}
                                """;
                ResponseEntity<AuditResponse> submitResponse = restTemplate.exchange(
                                "/api/v1/audits", HttpMethod.POST,
                                new HttpEntity<>(requestBody, authJsonHeaders(accessToken)), AuditResponse.class);
                UUID auditId = submitResponse.getBody().id();

                rabbitTemplate.receive("audit.jobs", 5000);
                publishSimulatedWorkerResult(auditId, 82, 74);

                AuditResponse audit = pollUntilTerminal(auditId, accessToken, Duration.ofSeconds(10));
                assertThat(audit.status().name()).isEqualTo("COMPLETE");
                assertThat(audit.overallScore()).isEqualTo(82);
                assertThat(audit.reports()).hasSize(3);
                assertThat(audit.completedAt()).isNotNull();
        }

        private AuditResponse pollUntilTerminal(UUID auditId, String accessToken, Duration timeout) {
                Instant deadline = Instant.now().plus(timeout);
                AuditResponse last = null;
                while (Instant.now().isBefore(deadline)) {
                        ResponseEntity<AuditResponse> polled = restTemplate.exchange(
                                        "/api/v1/audits/" + auditId, HttpMethod.GET,
                                        new HttpEntity<>(authJsonHeaders(accessToken)), AuditResponse.class);

                        if (!polled.getStatusCode().is2xxSuccessful()) {
                                throw new AssertionError("GET /api/v1/audits/" + auditId
                                                + " returned unexpected status " + polled.getStatusCode()
                                                + " while polling");
                        }

                        last = polled.getBody();
                        if (last != null && ("COMPLETE".equals(last.status().name())
                                        || "FAILED".equals(last.status().name()))) {
                                return last;
                        }
                        try {
                                Thread.sleep(200);
                        } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                                throw new RuntimeException(e);
                        }
                }
                throw new AssertionError("Audit " + auditId + " did not reach a terminal state within " + timeout
                                + "; last seen status=" + (last == null ? "null" : last.status()));
        }

        @Test
        void sharingAnAudit_makesItPubliclyReadable_andRevokingBlocksItAgain() {
                String accessToken = registerAndGetAccessToken("audit-share-" + UUID.randomUUID() + "@example.com");

                String requestBody = """
                                {"rawTrace":"$ npm test\\nsession started 2026-07-17T10:00:00\\nagent ran tests\\ntests passed with exit code 0",
                                 "title":"Share test audit",
                                 "agentTool":"GENERIC","inputSource":"PASTED_TEXT"}
                                """;
                ResponseEntity<AuditResponse> submitResponse = restTemplate.exchange(
                                "/api/v1/audits", HttpMethod.POST,
                                new HttpEntity<>(requestBody, authJsonHeaders(accessToken)), AuditResponse.class);
                UUID auditId = submitResponse.getBody().id();
                rabbitTemplate.receive("audit.jobs", 5000);

                ResponseEntity<AuditResponse> shareResponse = restTemplate.exchange(
                                "/api/v1/audits/" + auditId + "/share", HttpMethod.POST,
                                new HttpEntity<>(authJsonHeaders(accessToken)), AuditResponse.class);
                assertThat(shareResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(shareResponse.getBody().isPublic()).isTrue();
                String shareToken = shareResponse.getBody().shareToken();
                assertThat(shareToken).isNotBlank();

                ResponseEntity<AuditResponse> publicView = restTemplate.getForEntity(
                                "/api/v1/shared/" + shareToken, AuditResponse.class);
                assertThat(publicView.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(publicView.getBody().id()).isEqualTo(auditId);

                ResponseEntity<Void> revokeResponse = restTemplate.exchange(
                                "/api/v1/audits/" + auditId + "/share", HttpMethod.DELETE,
                                new HttpEntity<>(authJsonHeaders(accessToken)), Void.class);
                assertThat(revokeResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

                ResponseEntity<String> revokedView = restTemplate.getForEntity(
                                "/api/v1/shared/" + shareToken, String.class);
                assertThat(revokedView.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }

        @Test
        void aUserCannotAccessOrShareAnotherUsersAudit() {
                String ownerToken = registerAndGetAccessToken("audit-owner-" + UUID.randomUUID() + "@example.com");
                String otherToken = registerAndGetAccessToken("audit-other-" + UUID.randomUUID() + "@example.com");

                String requestBody = """
                                {"rawTrace":"$ npm test\\nsession started 2026-07-17T10:00:00\\nagent ran tests\\ntests passed with exit code 0",
                                 "title":"Private audit",
                                 "agentTool":"GENERIC","inputSource":"PASTED_TEXT"}
                                """;
                ResponseEntity<AuditResponse> submitResponse = restTemplate.exchange(
                                "/api/v1/audits", HttpMethod.POST,
                                new HttpEntity<>(requestBody, authJsonHeaders(ownerToken)), AuditResponse.class);
                UUID auditId = submitResponse.getBody().id();
                rabbitTemplate.receive("audit.jobs", 5000);

                ResponseEntity<String> otherUsersGet = restTemplate.exchange(
                                "/api/v1/audits/" + auditId, HttpMethod.GET,
                                new HttpEntity<>(authJsonHeaders(otherToken)), String.class);
                assertThat(otherUsersGet.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

                ResponseEntity<String> otherUsersShare = restTemplate.exchange(
                                "/api/v1/audits/" + auditId + "/share", HttpMethod.POST,
                                new HttpEntity<>(authJsonHeaders(otherToken)), String.class);
                assertThat(otherUsersShare.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }

        private JsonNode readJson(Message message) {
                try {
                        return objectMapper.readTree(message.getBody());
                } catch (Exception e) {
                        throw new RuntimeException("Failed to parse message body as JSON", e);
                }
        }

        private void publishSimulatedWorkerResult(UUID auditId, int overallScore, int reliabilityScore) {
                String json = """
                                {
                                  "auditId": "%s",
                                  "status": "COMPLETE",
                                  "report": {
                                    "loopEfficiencyReport": {"severity_score": 20, "summary": "Efficient loop, no wasted retries."},
                                    "blindOutcomeReport": {"severity_score": 15, "outcome_verdict": "LIKELY_COMPLETE"},
                                    "reliabilityTrendReport": {"severity_score": 10, "current_reliability_score": %d},
                                    "overallScore": %d,
                                    "extractedEvidence": ["tests passed"],
                                    "withheldClaims": [],
                                    "processingTimeMs": {"loop_efficiency": 120, "blind_outcome": 140, "reliability_trend": 95}
                                  }
                                }
                                """
                                .formatted(auditId, reliabilityScore, overallScore);

                MessageProperties props = new MessageProperties();
                props.setContentType("application/json");
                Message message = new Message(json.getBytes(StandardCharsets.UTF_8), props);

                rabbitTemplate.send("", "audit.results", message);
        }
}
