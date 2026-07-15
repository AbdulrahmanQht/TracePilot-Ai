import { useParams, useNavigate, Link } from "react-router";
import { Share2, Zap, Terminal, BarChart2, ExternalLink } from "lucide-react";
import { useSharedReport } from "@/hooks/useAudit";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import {
  parseAgentFindings,
  type LoopEfficiencyFindings,
  type BlindOutcomeFindings,
  type ReliabilityTrendFindings,
} from "@/schemas/agent-findings";

type AgentType = "TRACE_LOOP_EFFICIENCY" | "BLIND_OUTCOME_VERIFIER" | "RELIABILITY_TREND";

// helpers (module scope)

function severityBadgeVariant(s: string) {
  switch (s) {
    case "HIGH": return "severity-high" as const;
    case "MEDIUM": return "severity-medium" as const;
    case "LOW": return "severity-low" as const;
    default: return "severity-low" as const;
  }
}

function verdictBadgeVariant(v: string) {
  switch (v) {
    case "CONTRADICTED": return "verdict-contradicted" as const;
    case "UNVERIFIED": return "verdict-unverified" as const;
    case "LIKELY_COMPLETE": return "verdict-complete" as const;
    default: return "verdict-incomplete" as const;
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

// ScoreDial (module scope)
function ScoreDial({ score }: { score: number }) {
  const fill = score >= 70 ? "var(--primary)" : score >= 40 ? "#B87D2F" : "var(--destructive)";
  return (
    <div className="flex flex-col items-center gap-1">
      <div className="relative w-24 h-24 border-4 border-black flex items-center justify-center bg-background">
        <svg className="absolute inset-0 w-full h-full" style={{ transform: "rotate(-90deg)" }}>
          <circle cx="48" cy="48" r="38" fill="none" stroke="var(--muted)" strokeWidth="8" />
          <circle
            cx="48" cy="48" r="38" fill="none" stroke={fill} strokeWidth="8"
            strokeDasharray={`${(score / 100) * 239} 239`} strokeLinecap="square"
          />
        </svg>
        <span style={{ fontFamily: "var(--font-display)", fontSize: 24, letterSpacing: "-0.04em", zIndex: 1 }}>
          {score}
        </span>
      </div>
      <span style={{ fontFamily: "var(--font-mono)", fontSize: 9, letterSpacing: "0.08em", color: "var(--muted-foreground)" }}>
        / 100
      </span>
    </div>
  );
}

// findings rendering (reused pattern from AuditDetailPage)
function FindingsList({
  items,
}: {
  items: { severity: string; title: string; body: string; evidence: string[]; footer?: string | undefined }[];
}) {
  if (items.length === 0) return null;
  return (
    <div className="px-5 py-4 space-y-3">
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
  const meta = AGENT_META[agentType];
  const parsed = parseAgentFindings(agentType, rawFindings);

  return (
    <div className="border-2 border-black bg-card">
      <div className="border-b-2 border-black px-5 py-4 flex items-center gap-3 bg-muted">
        <div className="border-2 border-black p-1.5 bg-primary text-primary-foreground">{meta.icon}</div>
        <div>
          <div style={{ fontFamily: "var(--font-display)", fontSize: 13 }}>{meta.label}</div>
          <div style={{ fontFamily: "var(--font-mono)", fontSize: 10, color: "var(--muted-foreground)" }}>
            Score: {severityScore}/100
          </div>
        </div>
      </div>
      {!parsed && (
        <div className="px-5 py-4" style={{ fontFamily: "var(--font-mono)", fontSize: 11, color: "var(--muted-foreground)" }}>
          Findings failed to parse for this report.
        </div>
      )}
      {parsed && agentType === "TRACE_LOOP_EFFICIENCY" && <LoopEfficiencyBody f={parsed as LoopEfficiencyFindings} />}
      {parsed && agentType === "BLIND_OUTCOME_VERIFIER" && <BlindOutcomeBody f={parsed as BlindOutcomeFindings} />}
      {parsed && agentType === "RELIABILITY_TREND" && <ReliabilityTrendBody f={parsed as ReliabilityTrendFindings} />}
    </div>
  );
}

// derive an overall verdict badge from the Blind Outcome Verifier report, if present
function deriveVerdict(reports: { agentType: string; findings: string }[]): string | null {
  const blind = reports.find((r) => r.agentType === "BLIND_OUTCOME_VERIFIER");
  if (!blind) return null;
  const parsed = parseAgentFindings("BLIND_OUTCOME_VERIFIER", blind.findings);
  return parsed ? (parsed as BlindOutcomeFindings).outcome_verdict : null;
}

// Nav (shared, unauthenticated)
function PublicNav() {
  return (
    <nav className="border-b-2 border-black px-8 h-14 flex items-center justify-between bg-primary">
      <Link to="/" className="flex items-center gap-2.5">
        <div className="w-7 h-7 border-2 border-black flex items-center justify-center bg-secondary">
          <Terminal size={12} className="text-secondary-foreground" />
        </div>
        <span style={{ fontFamily: "var(--font-display)", color: "var(--primary-foreground)", fontSize: 16 }}>
          TracePilot.AI
        </span>
      </Link>
      <div className="flex items-center gap-3">
        <span style={{ fontFamily: "var(--font-mono)", fontSize: 10, color: "rgba(244,241,234,0.5)", letterSpacing: "0.06em" }}>
          PUBLIC REPORT
        </span>
        <Button variant="outline" size="sm" onClick={() => (window.location.href = "/login")} className="flex items-center gap-1">
          Sign In <ExternalLink size={9} className="ml-1" />
        </Button>
      </div>
    </nav>
  );
}

export default function SharedReportPage() {
  const { token } = useParams<{ token: string }>();
  const navigate = useNavigate();
  const { data: audit, isLoading, isError } = useSharedReport(token ?? "", { enabled: !!token });

  if (!token || isError || (!isLoading && (!audit || audit.status !== "COMPLETE"))) {
    return (
      <div className="min-h-screen flex flex-col bg-background" style={{ fontFamily: "var(--font-body)" }}>
        <PublicNav />
        <div className="flex-1 flex items-center justify-center px-6">
          <div className="border-2 border-black px-10 py-8 text-center shadow-[4px_4px_0px_#0D0D0D] bg-card">
            <Share2 size={32} style={{ color: "var(--muted-foreground)", margin: "0 auto 12px" }} />
            <h2 style={{ fontFamily: "var(--font-display)", fontSize: 20 }}>Report not found</h2>
            <p style={{ fontFamily: "var(--font-body)", color: "var(--muted-foreground)", marginTop: 8, fontSize: 14 }}>
              This share link is invalid or the audit is not yet complete.
            </p>
            <Button variant="default" onClick={() => navigate("/")} className="mt-6">
              Go to TracePilot.AI
            </Button>
          </div>
        </div>
      </div>
    );
  }

  if (isLoading || !audit) {
    return <div className="min-h-screen flex flex-col bg-background" style={{ fontFamily: "var(--font-body)" }} />;
  }

  const verdict = deriveVerdict(audit.reports);

  return (
    <div className="min-h-screen flex flex-col bg-background" style={{ fontFamily: "var(--font-body)" }}>
      <PublicNav />
      <div className="flex-1 px-6 py-8 max-w-[900px] mx-auto w-full space-y-6">
        <div style={{ fontFamily: "var(--font-mono)", fontSize: 10, color: "var(--muted-foreground)" }}>
          TracePilot.AI / shared / {token}
        </div>

        <div className="border-2 border-black shadow-[4px_4px_0px_#0D0D0D] bg-card">
          <div className="border-b-2 border-black px-6 py-4 flex items-start justify-between gap-4 flex-wrap bg-primary">
            <div>
              <h1 style={{ fontFamily: "var(--font-display)", color: "var(--primary-foreground)", fontSize: 20, letterSpacing: "-0.02em" }}>
                {audit.title || `Audit #${audit.id.slice(0, 8)}`}
              </h1>
              <div style={{ fontFamily: "var(--font-mono)", color: "rgba(244,241,234,0.55)", fontSize: 11, marginTop: 4 }}>
                {[
                  audit.repoName,
                  formatEnumLabel(audit.agentTool),
                  new Date(audit.createdAt).toLocaleDateString("en-US", { month: "long", day: "numeric", year: "numeric" }),
                ].filter(Boolean).join(" · ")}
              </div>
            </div>
            <Badge variant="status-complete" className="text-xs px-3 py-1">COMPLETE</Badge>
          </div>

          {audit.overallScore != null && (
            <div className="flex flex-wrap items-center gap-6 px-7 py-6">
              <ScoreDial score={audit.overallScore} />
              <div className="flex-1 min-w-[180px]">
                <div className="flex items-center gap-3 mb-2 flex-wrap">
                  <span style={{ fontFamily: "var(--font-display)", fontSize: 22, letterSpacing: "-0.02em" }}>
                    {audit.overallScore < 40 ? "Unreliable" : audit.overallScore < 70 ? "Uncertain" : "Trustworthy"}
                  </span>
                  {verdict && (
                    <Badge variant={verdictBadgeVariant(verdict)} className="text-xs px-2 py-1">
                      {formatEnumLabel(verdict)}
                    </Badge>
                  )}
                </div>
              </div>
            </div>
          )}
        </div>

        {audit.reports.length > 0 && (
          <div className="space-y-4">
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

        <div className="border-2 border-black px-7 py-6 flex flex-wrap items-center justify-between gap-4 shadow-[4px_4px_0px_#0D0D0D] bg-primary">
          <div>
            <p style={{ fontFamily: "var(--font-display)", color: "var(--primary-foreground)", fontSize: 16 }}>
              Audit your own AI agents
            </p>
            <p style={{ fontFamily: "var(--font-body)", color: "rgba(244,241,234,0.6)", fontSize: 13, marginTop: 4 }}>
              TracePilot.AI catches loops, hallucinated completions, and reliability regressions.
            </p>
          </div>
          <Button variant="outline" onClick={() => navigate("/register")}>
            Get Started Free →
          </Button>
        </div>

        <div className="text-center pb-4">
          <span style={{ fontFamily: "var(--font-mono)", fontSize: 10, color: "var(--muted-foreground)", letterSpacing: "0.06em" }}>
            Generated by TracePilot.AI — Coding-Agent Trust Auditor
          </span>
        </div>
      </div>
    </div>
  );
}