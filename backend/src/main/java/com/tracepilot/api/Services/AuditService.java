package com.tracepilot.api.Services;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tracepilot.api.Config.RabbitConfig;
import com.tracepilot.api.DTO.Messages.AuditJobMessage;
import com.tracepilot.api.DTO.Request.AuditRequest;
import com.tracepilot.api.DTO.Response.AuditResponse;
import com.tracepilot.api.DTO.Response.ReliabilityResponse;
import com.tracepilot.api.Entities.TraceAudit;
import com.tracepilot.api.Entities.User;
import com.tracepilot.api.Enums.AuditInputSource;
import com.tracepilot.api.Exceptions.ApiException;
import com.tracepilot.api.Repositories.ReliabilityHistoryRepository;
import com.tracepilot.api.Repositories.TraceAuditRepository;
import com.tracepilot.api.Repositories.UserRepository;
import com.tracepilot.api.Security.AuthenticatedUser;
import com.tracepilot.api.Util.TraceInputGuard;
import com.tracepilot.api.Util.TraceSanitizer;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapSetter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class AuditService {
        private final TraceAuditRepository auditRepository;
        private final UserRepository userRepository;
        private final ReliabilityHistoryRepository historyRepository;
        private final RabbitTemplate rabbitTemplate;

        public AuditService(TraceAuditRepository auditRepository,
                        UserRepository userRepository,
                        ReliabilityHistoryRepository historyRepository,
                        RabbitTemplate rabbitTemplate) {
                this.auditRepository = auditRepository;
                this.userRepository = userRepository;
                this.historyRepository = historyRepository;
                this.rabbitTemplate = rabbitTemplate;
        }

        @Transactional
        public AuditResponse initiateAudit(AuditRequest request, AuthenticatedUser principal) {
                log.info("=== Initiating audit ===");
                log.info("User ID: {}", principal.id());
                log.info("Title: {}", request.title());
                log.info("Repo: {}", request.repoName());
                log.info("Agent: {}", request.agentTool());
                log.info("Input source: {}", request.inputSource());

                TraceInputGuard.validate(request.rawTrace());
                log.info("Trace validation passed.");

                String safeTrace = TraceSanitizer.redactSecrets(request.rawTrace());
                boolean suspicious = TraceSanitizer.detectInjectionAttempt(safeTrace);
                String traceHash = TraceInputGuard.computeNormalizedHash(safeTrace);

                log.info("Trace sanitized.");
                log.info("Suspicious: {}", suspicious);
                log.info("Trace hash: {}", traceHash);

                log.info("Checking for cached audit...");
                Optional<TraceAudit> cachedAudit = auditRepository.findByUserIdAndTraceHash(principal.id(), traceHash);

                if (cachedAudit.isPresent()) {
                        log.info("Cache hit. Audit ID: {}", cachedAudit.get().getId());
                        return AuditResponse.from(cachedAudit.get());
                }

                log.info("No cached audit found.");

                User user = userRepository.findById(principal.id())
                                .orElseThrow(() -> new IllegalStateException("User not found"));

                log.info("Loaded user: {}", user.getId());

                var newAudit = new TraceAudit();
                newAudit.setUser(user);
                newAudit.setTitle(request.title());
                newAudit.setRepoName(request.repoName());
                newAudit.setAgentTool(request.agentTool() != null ? request.agentTool() : "GENERIC");
                newAudit.setInputSource(
                                request.inputSource() != null
                                                ? request.inputSource()
                                                : AuditInputSource.PASTED_TEXT);
                newAudit.setRawTrace(safeTrace);
                newAudit.setTraceHash(traceHash);
                newAudit.setSuspiciousContent(suspicious);

                log.info("About to save audit...");
                log.info("userId={}", user.getId());
                log.info("traceHash={}", traceHash);

                TraceAudit savedAudit;
                try {
                        savedAudit = auditRepository.saveAndFlush(newAudit);
                        log.info("Save successful. Audit ID: {}", savedAudit.getId());
                } catch (Exception e) {
                        log.error("saveAndFlush() failed!", e);

                        if (e instanceof org.springframework.dao.DataIntegrityViolationException dive) {
                                log.error("Most specific cause: {}", dive.getMostSpecificCause().getMessage());
                        }

                        throw e;
                }

                log.info("Reloading audit...");
                savedAudit = auditRepository.findById(savedAudit.getId())
                                .orElseThrow();

                log.info("Reload successful.");
                log.info("Created at: {}", savedAudit.getCreatedAt());

                Pageable topFive = PageRequest.of(0, 5);

                log.info("Loading prior reliability history...");
                List<AuditJobMessage.PriorReliability> priorHistory = historyRepository
                                .findByUserIdAndRepoNameAndAgentToolOrderByRecordedAtDesc(
                                                user.getId(),
                                                request.repoName(),
                                                request.agentTool(),
                                                topFive)
                                .stream()
                                .map(history -> new AuditJobMessage.PriorReliability(
                                                history.getReliabilityScore(),
                                                history.getSignalSummary().toString(),
                                                history.getRecordedAt().toString()))
                                .toList();

                log.info("Prior history count: {}", priorHistory.size());

                log.info("Incrementing user's audit count...");
                userRepository.incrementAuditCount(user.getId());

                AuditJobMessage jobMessage = new AuditJobMessage(
                                savedAudit.getId(),
                                user.getId(),
                                savedAudit.getTitle(),
                                safeTrace,
                                savedAudit.getRepoName(),
                                savedAudit.getAgentTool(),
                                savedAudit.getInputSource().name(),
                                suspicious,
                                priorHistory);

                log.info("Publishing audit {} to RabbitMQ...", savedAudit.getId());

                rabbitTemplate.convertAndSend(
                                RabbitConfig.EXCHANGE_NAME,
                                "audit.job",
                                jobMessage,
                                message -> {
                                        MessageProperties props = message.getMessageProperties();

                                        TextMapSetter<MessageProperties> setter = (carrier, key, value) -> carrier
                                                        .setHeader(key, value);

                                        GlobalOpenTelemetry.getPropagators()
                                                        .getTextMapPropagator()
                                                        .inject(Context.current(), props, setter);

                                        return message;
                                });

                log.info("RabbitMQ publish complete.");
                log.info("=== Audit initiation complete ===");

                return AuditResponse.from(savedAudit);
        }

        @Transactional(readOnly = true)
        public AuditResponse getExistingByHash(AuditRequest request, AuthenticatedUser principal) {
                String safeTrace = TraceSanitizer.redactSecrets(request.rawTrace());
                String traceHash = TraceInputGuard.computeNormalizedHash(safeTrace);

                log.info("Looking up existing audit");
                log.info("User ID: {}", principal.id());
                log.info("Trace hash: {}", traceHash);

                Optional<TraceAudit> existing = auditRepository.findByUserIdAndTraceHash(principal.id(), traceHash);

                if (existing.isPresent()) {
                        log.info("Existing audit found. Audit ID: {}", existing.get().getId());
                        return AuditResponse.from(existing.get());
                }

                log.error("No audit found for userId={} and traceHash={}",
                                principal.id(), traceHash);

                throw new IllegalStateException(
                                "Unique violation but no matching audit found");
        }

        @Transactional
        public void deleteAudit(UUID auditId, AuthenticatedUser principal) {
                log.info("Delete audit request received for audit ID {} by user {}", auditId, principal.id());

                TraceAudit audit = auditRepository.findById(auditId)
                                .filter(a -> a.getUser().getId().equals(principal.id()))
                                .orElseThrow(() -> {
                                        log.warn("Audit {} not found for user {}", auditId, principal.id());
                                        return new ApiException("Audit not found", HttpStatus.NOT_FOUND);
                                });

                auditRepository.delete(audit);

                log.info("Audit {} deleted successfully by user {}", auditId, principal.id());
        }

        @Transactional(readOnly = true)
        public AuditResponse getAuditById(UUID auditId, AuthenticatedUser principal) {
                TraceAudit audit = auditRepository.findById(auditId)
                                .filter(a -> a.getUser().getId().equals(principal.id()))
                                .orElseThrow(() -> new ApiException("Audit not found", HttpStatus.NOT_FOUND));
                return AuditResponse.from(audit);
        }

        @Transactional(readOnly = true)
        public Page<AuditResponse> listAudits(AuthenticatedUser principal, Pageable pageable) {
                return auditRepository.findByUserId(principal.id(), pageable)
                                .map(AuditResponse::from);
        }

        @Transactional(readOnly = true)
        public List<ReliabilityResponse> getReliabilityTrend(
                        AuthenticatedUser principal, String repoName, String agentTool, int limit) {
                return historyRepository
                                .findByUserIdAndRepoNameAndAgentToolOrderByRecordedAtDesc(
                                                principal.id(), repoName, agentTool, PageRequest.of(0, limit))
                                .stream()
                                .map(ReliabilityResponse::from)
                                .toList();
        }

        @Transactional
        public AuditResponse createShareLink(UUID auditId, AuthenticatedUser principal) {
                TraceAudit audit = auditRepository.findById(auditId)
                                .filter(a -> a.getUser().getId().equals(principal.id()))
                                .orElseThrow(() -> new ApiException("Audit not found", HttpStatus.NOT_FOUND));

                if (Boolean.TRUE.equals(audit.getIsPublic()) && audit.getShareToken() != null) {
                        return AuditResponse.from(audit); // already shared — return existing token, don't regenerate
                }

                audit.setIsPublic(true);
                audit.setShareToken(java.util.UUID.randomUUID().toString());
                TraceAudit saved = auditRepository.save(audit);
                return AuditResponse.from(saved);
        }

        @Transactional
        public void revokeShareLink(UUID auditId, AuthenticatedUser principal) {
                TraceAudit audit = auditRepository.findById(auditId)
                                .filter(a -> a.getUser().getId().equals(principal.id()))
                                .orElseThrow(() -> new ApiException("Audit not found", HttpStatus.NOT_FOUND));

                if (!Boolean.TRUE.equals(audit.getIsPublic())) {
                        return;
                }

                audit.setIsPublic(false);
                audit.setShareToken(null);

                auditRepository.save(audit);
        }

        @Transactional(readOnly = true)
        public AuditResponse getSharedReport(String token) {
                TraceAudit audit = auditRepository.findByShareToken(token)
                                .filter(a -> Boolean.TRUE.equals(a.getIsPublic()))
                                .orElseThrow(() -> new ApiException("Shared report not found", HttpStatus.NOT_FOUND));
                return AuditResponse.from(audit);
        }
}
