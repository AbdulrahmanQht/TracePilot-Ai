package com.tracepilot.api.Entities;

import java.util.UUID;
import java.time.Instant;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.tracepilot.api.Enums.TypesOfAgent;

@Entity
@Table(name = "agent_reports", uniqueConstraints = @UniqueConstraint(name = "uq_audit_agent", columnNames = {
        "audit_id", "agent_type" }))
@Getter
@Setter
@NoArgsConstructor
public class AgentReport {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "audit_id", nullable = false)
    private TraceAudit audit;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "agent_type", columnDefinition = "types_of_agents", nullable = false)
    private TypesOfAgent agentType;

    @Column(name = "findings", columnDefinition = "jsonb", nullable = false)
    @JdbcTypeCode(SqlTypes.JSON)
    private String findings;

    @Min(value = 0, message = "Severity score must be at least 0")
    @Max(value = 100, message = "Severity score cannot be higher than 100")
    @Column(name = "severity_score", nullable = false)
    private int severityScore;

    @Column(name = "processing_time_ms")
    private Integer processingTimeMs;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;
}