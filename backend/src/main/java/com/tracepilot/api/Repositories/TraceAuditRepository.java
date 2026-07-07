package com.tracepilot.api.Repositories;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.tracepilot.api.Entities.TraceAudit;

public interface TraceAuditRepository extends JpaRepository<TraceAudit, UUID> {

    Optional<TraceAudit> findByUserIdAndTraceHash(UUID userId, String traceHash);

    Optional<TraceAudit> findByShareToken(String shareToken);

    Page<TraceAudit> findByUserId(UUID userId, Pageable pageable);
}