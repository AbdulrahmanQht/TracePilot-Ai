package com.tracepilot.api.Repositories;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tracepilot.api.Entities.AgentReport;

public interface AgentReportRepository extends JpaRepository<AgentReport, UUID> {

    List<AgentReport> findByAuditId(UUID auditId);
}