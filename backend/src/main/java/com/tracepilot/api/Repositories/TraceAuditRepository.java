package com.tracepilot.api.Repositories;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.EntityGraph;

import com.tracepilot.api.Enums.AuditStatus;
import com.tracepilot.api.Entities.TraceAudit;

public interface TraceAuditRepository extends JpaRepository<TraceAudit, UUID> {

    Optional<TraceAudit> findByUserIdAndTraceHash(UUID userId, String traceHash);

    Optional<TraceAudit> findByShareToken(String shareToken);

    Page<TraceAudit> findByUserId(UUID userId, Pageable pageable);

    @Query("SELECT a FROM TraceAudit a JOIN FETCH a.user WHERE a.suspiciousContent = true")
    Page<TraceAudit> findBySuspiciousContentTrue(Pageable pageable);

    List<TraceAudit> findByStatusInAndCreatedAtBefore(List<AuditStatus> statuses, Instant cutoff);
}