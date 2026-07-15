package com.tracepilot.api.Services;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.tracepilot.api.Entities.TraceAudit;
import com.tracepilot.api.Enums.AuditStatus;
import com.tracepilot.api.Repositories.TraceAuditRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class StaleAuditReconciler {

    private final TraceAuditRepository auditRepository;
    private final long timeoutMinutes;

    public StaleAuditReconciler(
            TraceAuditRepository auditRepository,
            @Value("${tracepilot.audit.stale-timeout-minutes:20}") long timeoutMinutes) {
        this.auditRepository = auditRepository;
        this.timeoutMinutes = timeoutMinutes;
    }

    // Every 5 minutes; independent of the timeout window itself so it stays responsive.
    @Scheduled(fixedDelayString = "PT5M")
    @Transactional
    public void reconcileStaleAudits() {
        Instant cutoff = Instant.now().minus(Duration.ofMinutes(timeoutMinutes));

        List<TraceAudit> stale = auditRepository.findByStatusInAndCreatedAtBefore(
                List.of(AuditStatus.PENDING, AuditStatus.PROCESSING), cutoff);

        if (stale.isEmpty()) {
            return;
        }

        for (TraceAudit audit : stale) {
            AuditStatus previousStatus = audit.getStatus();
            audit.setStatus(AuditStatus.FAILED);
            audit.setFailureReason(
                    "Audit timed out — no result was received within " + timeoutMinutes + " minutes.");
            auditRepository.save(audit);
            log.warn("Audit {} marked FAILED by stale-audit reconciler (created {}, was stuck in {}).",
                    audit.getId(), audit.getCreatedAt(), previousStatus);
        }

        log.info("Stale-audit reconciler marked {} audit(s) FAILED.", stale.size());
    }
}