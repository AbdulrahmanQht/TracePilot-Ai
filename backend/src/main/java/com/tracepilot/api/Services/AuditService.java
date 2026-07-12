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
        private final AuditEmitterRegistry auditEmitterRegistry;

        public AuditService(TraceAuditRepository auditRepository,
                        UserRepository userRepository,
                        ReliabilityHistoryRepository historyRepository,
                        RabbitTemplate rabbitTemplate,
                        AuditEmitterRegistry auditEmitterRegistry) {
                this.auditRepository = auditRepository;
                this.userRepository = userRepository;
                this.historyRepository = historyRepository;
                this.rabbitTemplate = rabbitTemplate;
                this.auditEmitterRegistry = auditEmitterRegistry;
        }

        @Transactional
        public AuditResponse initiateAudit(AuditRequest request, AuthenticatedUser principal) {
                TraceInputGuard.validate(request.rawTrace());
                String safeTrace = TraceSanitizer.redactSecrets(request.rawTrace());
                boolean suspicious = TraceSanitizer.detectInjectionAttempt(safeTrace);

                String traceHash = TraceInputGuard.computeNormalizedHash(safeTrace);
                Optional<TraceAudit> cachedAudit = auditRepository.findByUserIdAndTraceHash(principal.id(), traceHash);

                if (cachedAudit.isPresent()) {
                        log.info("Cache hit for user {} on trace hash {}", principal.id(), traceHash);
                        return AuditResponse.from(cachedAudit.get());
                }

                User user = userRepository.findById(principal.id())
                                .orElseThrow(() -> new IllegalStateException("User not found"));

                var newAudit = new TraceAudit();
                newAudit.setUser(user);
                newAudit.setTitle(request.title());
                newAudit.setRepoName(request.repoName());
                newAudit.setAgentTool(request.agentTool() != null ? request.agentTool() : "GENERIC");
                newAudit.setInputSource(
                                request.inputSource() != null ? request.inputSource() : AuditInputSource.PASTED_TEXT);
                newAudit.setRawTrace(safeTrace);
                newAudit.setTraceHash(traceHash);
                newAudit.setSuspiciousContent(suspicious);
                TraceAudit savedAudit = auditRepository.saveAndFlush(newAudit);

                Pageable topFive = PageRequest.of(0, 5);
                List<AuditJobMessage.PriorReliability> priorHistory = historyRepository
                                .findByUserIdAndRepoNameAndAgentToolOrderByRecordedAtDesc(
                                                user.getId(), request.repoName(), request.agentTool(), topFive)
                                .stream()
                                .map(history -> new AuditJobMessage.PriorReliability(
                                                history.getReliabilityScore(),
                                                history.getSignalSummary().toString(),
                                                history.getRecordedAt().toString()))
                                .toList();

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

                log.debug("Publishing job {} to RabbitMQ", savedAudit.getId());
                rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE_NAME, "audit.job", jobMessage, message -> {
                        MessageProperties messageProperties = message.getMessageProperties();

                        TextMapSetter<MessageProperties> setter = (carrier, key, value) -> carrier.setHeader(key,
                                        value);
                        GlobalOpenTelemetry.getPropagators().getTextMapPropagator()
                                        .inject(Context.current(), messageProperties, setter);

                        return message;
                });

                return AuditResponse.from(savedAudit);
        }

        @Transactional(readOnly = true)
        public AuditResponse getExistingByHash(AuditRequest request, AuthenticatedUser principal) {
                String traceHash = TraceInputGuard
                                .computeNormalizedHash(TraceSanitizer.redactSecrets(request.rawTrace()));
                return auditRepository.findByUserIdAndTraceHash(principal.id(), traceHash)
                                .map(AuditResponse::from)
                                .orElseThrow(() -> new IllegalStateException(
                                                "Unique violation but no matching audit found"));
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
