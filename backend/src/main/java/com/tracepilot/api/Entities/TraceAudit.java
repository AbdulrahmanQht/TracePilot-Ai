package com.tracepilot.api.Entities;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.tracepilot.api.Enums.AuditInputSource;
import com.tracepilot.api.Enums.AuditStatus;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "trace_audits")
@Getter
@Setter
@NoArgsConstructor
public class TraceAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "title", length = 150)
    private String title;

    @Column(name = "repo_name", length = 150)
    private String repoName;

    @Column(name = "raw_trace", nullable = false)
    private String rawTrace;
    
    @Column(name = "trace_hash", nullable = false, length = 64, columnDefinition = "TEXT")
    private String traceHash;

    @Column(name = "extracted_evidence", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String extractedEvidence;

    @Column(name = "withheld_claims", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String withheldClaims;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "input_source", columnDefinition = "audit_input_source", nullable = false)
    private AuditInputSource inputSource = AuditInputSource.PASTED_TEXT;

    @Column(name = "agent_tool", nullable = false, length = 30)
    private String agentTool = "generic";

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status", columnDefinition = "audit_status", nullable = false)
    private AuditStatus status = AuditStatus.PENDING;

    @Min(value = 0, message = "Overall score must be at least 0")
    @Max(value = 100, message = "Overall score cannot be higher than 100")
    @Column(name = "overall_score")
    private Integer overallScore;

    @Column(name = "is_public", nullable = false)
    private Boolean isPublic = false;

    @Column(name = "share_token", length = 100)
    private String shareToken;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "suspicious_content", nullable = false)
    private Boolean suspiciousContent = false;

    @OneToMany(mappedBy = "audit", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AgentReport> agentReports = new ArrayList<>();

    @OneToMany(mappedBy = "audit", fetch = FetchType.LAZY)
    private List<ReliabilityHistory> reliabilityHistory = new ArrayList<>();
}