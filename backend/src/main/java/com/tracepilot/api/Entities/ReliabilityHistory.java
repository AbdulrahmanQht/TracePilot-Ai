package com.tracepilot.api.Entities;

import java.util.UUID;
import java.time.Instant;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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
    @JoinColumn(name = "audit_id") // nullable — matches ON DELETE SET NULL in the DDL
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