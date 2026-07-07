package com.tracepilot.api.Entities;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "reliability_history")
@Getter
@Setter
@NoArgsConstructor
public class ReliabilityHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "audit_id")
    private TraceAudit audit;

    @Column(name = "repo_name", length = 150)
    private String repoName;

    @Column(name = "agent_tool", nullable = false, length = 30)
    private String agentTool;

    @Column(name = "reliability_score", nullable = false)
    private int reliabilityScore;

    @Column(name = "signal_summary", columnDefinition = "jsonb", nullable = false)
    @JdbcTypeCode(SqlTypes.JSON)
    private String signalSummary;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;
}