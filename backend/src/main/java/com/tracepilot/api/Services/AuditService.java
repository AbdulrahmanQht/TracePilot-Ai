package com.tracepilot.api.Services;

import com.tracepilot.api.Config.RabbitConfig;
import com.tracepilot.api.DTO.Messages.AuditJobMessage;
import com.tracepilot.api.DTO.Request.AuditRequest;
import com.tracepilot.api.Enums.AuditInputSource;
import com.tracepilot.api.Entities.TraceAudit;
import com.tracepilot.api.Entities.User;
import com.tracepilot.api.Repositories.ReliabilityHistoryRepository;
import com.tracepilot.api.Repositories.TraceAuditRepository;
import com.tracepilot.api.Repositories.UserRepository;
import com.tracepilot.api.Security.AuthenticatedUser;
import com.tracepilot.api.Util.TraceInputGuard;
import com.tracepilot.api.Util.TraceSanitizer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

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
    public TraceAudit initiateAudit(AuditRequest request, AuthenticatedUser principal) {
        TraceInputGuard.validate(request.rawTrace());
        String safeTrace = TraceSanitizer.redactSecrets(request.rawTrace());

        String traceHash = TraceInputGuard.computeNormalizedHash(safeTrace);
        Optional<TraceAudit> cachedAudit = auditRepository.findByUserIdAndTraceHash(principal.id(), traceHash);

        if (cachedAudit.isPresent()) {
            log.info("Cache hit for user {} on trace hash {}", principal.id(), traceHash);
            return cachedAudit.get();
        }

        User user = userRepository.findById(principal.id())
                .orElseThrow(() -> new IllegalStateException("User not found"));

        var newAudit = new TraceAudit();
        newAudit.setUser(user);
        newAudit.setTitle(request.title());
        newAudit.setRepoName(request.repoName());
        newAudit.setAgentTool(request.agentTool() != null ? request.agentTool() : "GENERIC");
        newAudit.setInputSource(request.inputSource() != null ? request.inputSource() : AuditInputSource.PASTED_TEXT);
        newAudit.setRawTrace(safeTrace);
        newAudit.setTraceHash(traceHash);
        TraceAudit savedAudit = auditRepository.save(newAudit);

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
                priorHistory);

        log.debug("Publishing job {} to RabbitMQ", savedAudit.getId());
        rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE_NAME, "audit.job", jobMessage);

        return savedAudit;
    }
}
