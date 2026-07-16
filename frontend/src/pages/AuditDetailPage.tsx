// src/pages/AuditDetailPage.tsx
import { useState } from "react";
import { useParams, useNavigate } from "react-router";
import { useQueryClient } from "@tanstack/react-query";
import {
  ArrowLeft, Share2, Zap, Terminal, BarChart2,
  XCircle, Copy, CheckCircle, Link2Off, RefreshCw
} from "lucide-react";
import { useAudit, useShareAudit, useRevokeShareLink, useRetryAudit, auditKeys } from "@/hooks/useAudit";
import { useDocumentTitle } from "@/hooks/useDocumentTitle";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import {
  parseAgentFindings,
  type LoopEfficiencyFindings,
  type BlindOutcomeFindings,
  type ReliabilityTrendFindings,
} from "@/schemas/agent-findings";
import type { AuditResponse } from "@/schemas/audit";

type AgentType = "TRACE_LOOP_EFFICIENCY" | "BLIND_OUTCOME_VERIFIER" | "RELIABILITY_TREND";

const TERMINAL_STATUSES: AuditResponse["status"][] = ["COMPLETE", "FAILED"];

// helpers (module scope)
function statusBadgeVariant(s: AuditResponse["status"]) {
  switch (s) {
    case "COMPLETE": return "status-complete" as const;
    case "FAILED": return "status-failed" as const;
    case "PROCESSING": return "status-processing" as const;
    default: return "status-pending" as const;
  }
}

function severityBadgeVariant(s: string) {
  switch (s) {
    case "HIGH": return "severity-high" as const;
    case "MEDIUM": return "severity-medium" as const;
    case "LOW": return "severity-low" as const;
    default: return "severity-low" as const;
  }
}

function formatEnumLabel(v: string): string {
  return v
    .split("_")
    .map((w) => w.charAt(0) + w.slice(1).toLowerCase())
    .join(" ");
}

const AGENT_META: Record<AgentType, { label: string; icon: React.ReactNode }> = {
  TRACE_LOOP_EFFICIENCY: { label: "Loop Efficiency Agent", icon: <Zap size={13} /> },
  BLIND_OUTCOME_VERIFIER: { label: "Blind Outcome Verifier", icon: <Terminal size={13} /> },
  RELIABILITY_TREND: { label: "Reliability Trend Agent", icon: <BarChart2 size={13} /> },
};

// ScoreDial
function ScoreDial({ score }: { score: number }) {
  const fill = score >= 70 ? "var(--primary)" : score >= 40 ? "#B87D2F" : "var(--destructive)";
  return (
    <div className="flex flex-col items-center gap-1">
      <div className="relative w-20 h-20 border-4 border-black flex items-center justify-center bg-background">
        <svg className="absolute inset-0 w-full h-full" style={{ transform: "rotate(-90deg)" }}>
          <circle cx="40" cy="40" r="32" fill="none" stroke="var(--muted)" strokeWidth="8" />
          <circle
            cx="40" cy="40" r="32" fill="none" stroke={fill} strokeWidth="8"
            strokeDasharray={`${(score / 100) * 201} 201`} strokeLinecap="square"
          />
        </svg>
        <span style={{ fontFamily: "var(--font-display)", fontSize: 20, letterSpacing: "-0.04em", zIndex: 1 }}>
          {score}
        </span>
      </div>
      <span style={{ fontFamily: "var(--font-mono)", fontSize: 9, letterSpacing: "0.08em", color: "var(--muted-foreground)" }}>
        / 100
      </span>
    </div>
  );
}

// per-agent-type findings bodies (module scope)
function FindingsList({
  items,
}: {
  items: { severity: string; title: string; body: string; evidence: string[]; footer?: string | undefined }[];
}) {
  if (items.length === 0) return null;
  return (
    <div className="px-5 py-4">
      <div style={{ fontFamily: "var(--font-display)", fontSize: 11, letterSpacing: "0.06em", marginBottom: 10 }}>
        FINDINGS
      </div>
      <div className="space-y-3">
        {items.map((item, i) => (
          <div key={i} className="border border-black/20 p-3 bg-background">
            <div className="flex items-center gap-2 mb-1.5 flex-wrap">
              <Badge variant={severityBadgeVariant(item.severity)} className="text-[9px]">
                {item.severity}
              </Badge>
              <span style={{ fontFamily: "var(--font-display)", fontSize: 13 }}>{item.title}</span>
            </div>
            <p style={{ fontFamily: "var(--font-body)", fontSize: 13, color: "var(--muted-foreground)", marginBottom: item.evidence.length ? 8 : 0 }}>
              {item.body}
            </p>
            {item.evidence.length > 0 && (
              <ul className="list-disc pl-5 space-y-1">
                {item.evidence.map((e, j) => (
                  <li key={j} style={{ fontFamily: "var(--font-mono)", fontSize: 11, color: "var(--muted-foreground)" }}>{e}</li>
                ))}
              </ul>
            )}
            {item.footer && (
              <p className="mt-2" style={{ fontFamily: "var(--font-mono)", fontSize: 11, color: "var(--foreground)" }}>
                {item.footer}
              </p>
            )}
          </div>
        ))}
      </div>
    </div>
  );
}

function LoopEfficiencyBody({ f }: { f: LoopEfficiencyFindings }) {
  return (
    <>
      <div className="px-5 py-3 border-b-2 border-black/20 flex gap-6 flex-wrap" style={{ fontFamily: "var(--font-mono)", fontSize: 11 }}>
        <span>Wasted steps: <strong>{f.estimated_wasted_steps}</strong></span>
        {f.dominant_loop_type && <span>Dominant loop: <strong>{f.dominant_loop_type}</strong></span>}
      </div>
      <FindingsList
        items={f.findings.map((x) => ({
          severity: x.severity,
          title: x.issue_title,
          body: x.why_it_failed,
          evidence: x.evidence,
          footer: x.better_loop_rule ? `Better rule: ${x.better_loop_rule}` : undefined,
        }))}
      />
    </>
  );
}

function BlindOutcomeBody({ f }: { f: BlindOutcomeFindings }) {
  return (
    <>
      <div className="px-5 py-3 border-b-2 border-black/20 flex gap-6 flex-wrap" style={{ fontFamily: "var(--font-mono)", fontSize: 11 }}>
        <span>Outcome: <strong>{formatEnumLabel(f.outcome_verdict)}</strong></span>
      </div>
      <FindingsList
        items={f.findings.map((x) => ({
          severity: x.severity,
          title: x.issue_title,
          body: x.trust_impact,
          evidence: x.observable_evidence,
          footer: x.recommended_verification ? `Recommended: ${x.recommended_verification}` : undefined,
        }))}
      />
    </>
  );
}

function ReliabilityTrendBody({ f }: { f: ReliabilityTrendFindings }) {
  return (
    <>
      <div className="px-5 py-3 border-b-2 border-black/20 flex gap-6 flex-wrap" style={{ fontFamily: "var(--font-mono)", fontSize: 11 }}>
        <span>Current: <strong>{f.current_reliability_score}</strong></span>
        {f.previous_reliability_score != null && <span>Previous: <strong>{f.previous_reliability_score}</strong></span>}
        <span>Trend: <strong>{formatEnumLabel(f.trend_direction)}</strong></span>
      </div>
      <FindingsList
        items={f.findings.map((x) => ({
          severity: x.severity,
          title: x.issue_title,
          body: x.trend_signal,
          evidence: x.evidence,
          footer: x.recommendation ? `Recommendation: ${x.recommendation}` : undefined,
        }))}
      />
    </>
  );
}

function AgentCard({
  agentType,
  severityScore,
  rawFindings,
}: {
  agentType: AgentType;
  severityScore: number;
  rawFindings: string;
}) {
  const [open, setOpen] = useState(true);
  const meta = AGENT_META[agentType];
  const parsed = parseAgentFindings(agentType, rawFindings);

  return (
    <div className="border-2 border-black bg-card">
      <Button
        variant="ghost"
        onClick={() => setOpen((o) => !o)}
        className="w-full flex items-center justify-between px-5 py-4 h-auto hover:bg-[#F0EDE4] border-transparent hover:border-transparent rounded-none"
      >
        <div className="flex items-center gap-3">
          <div className="border-2 border-black p-1.5 bg-primary text-primary-foreground">{meta.icon}</div>
          <span style={{ fontFamily: "var(--font-display)", fontSize: 13 }}>{meta.label}</span>
        </div>
        <div className="flex items-center gap-3">
          <Badge variant="secondary" className="text-sm px-2 py-0.5">{severityScore}</Badge>
          <span style={{ fontFamily: "var(--font-mono)", fontSize: 11, color: "var(--muted-foreground)" }}>
            {open ? "▲" : "▼"}
          </span>
        </div>
      </Button>
      {open && (
        <div className="border-t-2 border-black">
          {!parsed && (
            <div className="px-5 py-4" style={{ fontFamily: "var(--font-mono)", fontSize: 11, color: "var(--muted-foreground)" }}>
              Findings failed to parse for this report.
            </div>
          )}
          {parsed && agentType === "TRACE_LOOP_EFFICIENCY" && <LoopEfficiencyBody f={parsed as LoopEfficiencyFindings} />}
          {parsed && agentType === "BLIND_OUTCOME_VERIFIER" && <BlindOutcomeBody f={parsed as BlindOutcomeFindings} />}
          {parsed && agentType === "RELIABILITY_TREND" && <ReliabilityTrendBody f={parsed as ReliabilityTrendFindings} />}
        </div>
      )}
    </div>
  );
}

export default function AuditDetailPage() {
  useDocumentTitle("Audit Detail Page");
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [copied, setCopied] = useState(false);

  const { data: audit, isLoading, isError } = useAudit(id ?? "", { enabled: !!id });

  const shareMutation = useShareAudit({
  onSuccess: (updated) => {
    if (id) {
      queryClient.setQueryData(auditKeys.detail(id), updated);
    }

    if (updated.shareToken) {
      copyShareLink(updated.shareToken);
    }
  },
});

  const revokeMutation = useRevokeShareLink({
    onSuccess: () => {
      if (!id || !audit) return;
      queryClient.setQueryData(auditKeys.detail(id), {
        ...audit,
        isPublic: false,
        shareToken: null,
      });
    },
  });

  const retryMutation = useRetryAudit({
    onSuccess: (updated) => {
      if (id) {
        queryClient.setQueryData(auditKeys.detail(id), updated);
      }
      navigate(`/app/audits/${updated.id}/processing`);
    },
  });

  function copyShareLink(shareToken: string) {
    const url = `${window.location.origin}/shared/${shareToken}`;
    navigator.clipboard.writeText(url).then(() => {
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    });
  }

  if (!id || isError || (!isLoading && !audit)) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-background">
        <div className="border-2 border-black px-10 py-8 text-center shadow-[4px_4px_0px_#0D0D0D] bg-card">
          <XCircle size={28} className="text-destructive mx-auto mb-3" />
          <p style={{ fontFamily: "var(--font-display)", fontSize: 16 }}>Audit not found</p>
          <Button variant="default" onClick={() => navigate("/app/history")} className="mt-4">
            ← Back to History
          </Button>
        </div>
      </div>
    );
  }

  if (isLoading || !audit) {
    return <div className="min-h-screen flex items-center justify-center bg-background" />;
  }

  if (!TERMINAL_STATUSES.includes(audit.status)) {
    navigate(`/app/audits/${id}/processing`, { replace: true });
    return null;
  }

  const totalProcessingMs = audit.reports.reduce((sum, r) => sum + (r.processingTimeMs ?? 0), 0);

  return (
    <div className="bg-background min-h-screen" style={{ fontFamily: "var(--font-body)" }}>
      <div className="border-b-2 border-black px-8 py-5 flex items-start justify-between gap-4 flex-wrap bg-card">
        <div className="flex items-start gap-3">
          <Button variant="outline" size="icon-sm" onClick={() => navigate(-1)} className="mt-1 shrink-0">
            <ArrowLeft size={13} />
          </Button>
          <div>
            <div className="flex items-center gap-2 flex-wrap mb-1">
              <h1 style={{ fontFamily: "var(--font-display)", fontSize: 20, letterSpacing: "-0.02em" }}>
                {audit.title || `Audit #${audit.id.slice(0, 8)}`}
              </h1>
              <Badge variant={statusBadgeVariant(audit.status)}>{audit.status}</Badge>
            </div>
            <div className="flex items-center gap-3 flex-wrap">
              {[
                audit.repoName,
                formatEnumLabel(audit.agentTool),
                new Date(audit.createdAt).toLocaleDateString("en-US", {
                  month: "short", day: "numeric", year: "numeric", hour: "2-digit", minute: "2-digit",
                }),
              ].filter(Boolean).map((v, i, arr) => (
                <span key={i} style={{ fontFamily: "var(--font-mono)", fontSize: 11, color: "var(--muted-foreground)" }}>
                  {v}{i < arr.length - 1 ? " ·" : ""}
                </span>
              ))}
            </div>
          </div>
        </div>

        <div className="flex items-center gap-2">
          {audit.status === "FAILED" && (
            <Button
              variant="default"
              size="sm"
              onClick={() => retryMutation.mutate(audit.id)}
              disabled={retryMutation.isPending}
              className="flex items-center gap-1.5"
            >
              <RefreshCw size={11} /> {retryMutation.isPending ? "Retrying…" : "Retry Audit"}
            </Button>
          )}
          {audit.isPublic && audit.shareToken ? (
            <>
              <Button variant="outline" onClick={() => copyShareLink(audit.shareToken!)} size="sm" className="flex items-center gap-1.5">
                {copied ? <><CheckCircle size={11} /> Copied!</> : <><Copy size={11} /> Copy Link</>}
              </Button>
              <Button
                variant="outline"
                size="sm"
                onClick={() => revokeMutation.mutate(audit.id)}
                disabled={revokeMutation.isPending}
                className="flex items-center gap-1.5"
              >
                <Link2Off size={11} /> {revokeMutation.isPending ? "Revoking…" : "Revoke Share Link"}
              </Button>
            </>
          ) : (
            <Button
              variant="default"
              size="sm"
              onClick={() => shareMutation.mutate(audit.id)}
              disabled={shareMutation.isPending}
              className="flex items-center gap-1.5"
            >
              <Share2 size={11} /> {shareMutation.isPending ? "Sharing…" : "Share Report"}
            </Button>
          )}
        </div>
      </div>

      <div className="px-8 py-7 space-y-6">
        {audit.status === "FAILED" && (
          <div className="border-2 border-destructive px-6 py-4 flex items-start gap-3 bg-destructive/5">
            <XCircle size={16} className="text-destructive shrink-0 mt-0.5" />
            <div>
              <p style={{ fontFamily: "var(--font-display)", fontSize: 13 }}>This audit failed</p>
              <p style={{ fontFamily: "var(--font-body)", fontSize: 12, color: "var(--muted-foreground)", marginTop: 2 }}>
                {audit.failureReason ?? "No further detail was recorded for this failure."}
              </p>
            </div>
          </div>
        )}
        {audit.overallScore != null && (
          <div className="border-2 border-black shadow-[4px_4px_0px_#0D0D0D] flex flex-wrap items-center gap-6 px-7 py-6 bg-card">
            <ScoreDial score={audit.overallScore} />
            <div className="flex-1 min-w-[200px]">
              <span style={{ fontFamily: "var(--font-display)", fontSize: 24, letterSpacing: "-0.02em" }}>
                {audit.overallScore < 40 ? "Unreliable" : audit.overallScore < 70 ? "Uncertain" : "Trustworthy"}
              </span>
            </div>
          </div>
        )}

        {audit.reports.length > 0 && (
          <div className="space-y-4">
            <div style={{ fontFamily: "var(--font-display)", fontSize: 12, letterSpacing: "0.06em", color: "var(--muted-foreground)" }}>
              AGENT REPORTS
            </div>
            {audit.reports.map((r) => (
              <AgentCard
                key={r.id ?? r.agentType}
                agentType={r.agentType as AgentType}
                severityScore={r.severityScore}
                rawFindings={r.findings}
              />
            ))}
          </div>
        )}

        <div className="border-2 border-black bg-card">
          <div className="border-b-2 border-black px-5 py-3 bg-muted">
            <span style={{ fontFamily: "var(--font-display)", fontSize: 11, letterSpacing: "0.06em" }}>AUDIT METADATA</span>
          </div>
          <div className="grid grid-cols-2 sm:grid-cols-3 divide-x-2 divide-black">
            {[
              { label: "Audit ID", value: audit.id },
              { label: "Input Source", value: formatEnumLabel(audit.inputSource) },
              { label: "Total Processing Time", value: `${(totalProcessingMs / 1000).toFixed(1)}s` },
            ].map(({ label, value }) => (
              <div key={label} className="px-5 py-4">
                <div style={{ fontFamily: "var(--font-mono)", fontSize: 9, color: "var(--muted-foreground)", letterSpacing: "0.08em", marginBottom: 4 }}>
                  {label}
                </div>
                <div style={{ fontFamily: "var(--font-mono)", fontSize: 12, wordBreak: "break-all" }}>{value}</div>
              </div>
            ))}
          </div>
        </div>

        <div className="flex gap-3 pt-2">
          <Button variant="default" onClick={() => navigate("/app/submit")}>+ New Audit</Button>
          <Button variant="outline" onClick={() => navigate("/app/history")}>View History</Button>
        </div>
      </div>
    </div>
  );
}