package com.tracepilot.api.Repositories;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;

import com.tracepilot.api.Entities.ReliabilityHistory;

public interface ReliabilityHistoryRepository extends JpaRepository<ReliabilityHistory, UUID> {

    List<ReliabilityHistory> findByUserIdAndRepoNameAndAgentToolOrderByRecordedAtDesc(
            UUID userId, String repoName, String agentTool, Pageable pageable);
}