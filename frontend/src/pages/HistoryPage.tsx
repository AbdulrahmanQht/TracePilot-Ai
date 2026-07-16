import { useState, useMemo } from "react";
import { useNavigate } from "react-router";
import { Search, Filter, Clock, ChevronRight, ChevronLeft, Bot } from "lucide-react";
import { useAuditList } from "@/hooks/useAudit";
import { useDocumentTitle } from "@/hooks/useDocumentTitle";
import { AuditStatusSchema, AgentToolTypeSchema } from "@/schemas/audit";
import { parseAgentFindings, type BlindOutcomeFindings } from "@/schemas/agent-findings";
import type { AuditResponse } from "@/schemas/audit";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";

const PAGE_SIZE = 7;

const STATUS_OPTS = ["ALL", ...AuditStatusSchema.options] as const;
const TOOL_OPTS = ["ALL", ...AgentToolTypeSchema.options] as const;

// helpers (module scope)
function statusBadgeVariant(s: AuditResponse["status"]) {
  switch (s) {
    case "COMPLETE": return "status-complete" as const;
    case "FAILED": return "status-failed" as const;
    case "PROCESSING": return "status-processing" as const;
    default: return "status-pending" as const;
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


function deriveVerdict(reports: AuditResponse["reports"]): string | null {
  const blind = reports.find((r) => r.agentType === "BLIND_OUTCOME_VERIFIER");
  if (!blind) return null;
  const parsed = parseAgentFindings("BLIND_OUTCOME_VERIFIER", blind.findings);
  return parsed ? (parsed as BlindOutcomeFindings).outcome_verdict : null;
}

function getAuditTitle(a: AuditResponse) {
  return a.title?.trim() || `Audit ${a.id.slice(0, 8)}`;
}

export default function HistoryPage() {
  useDocumentTitle("Audits History Page");
  const navigate = useNavigate();
  const [search, setSearch] = useState("");
  const [status, setStatus] = useState<(typeof STATUS_OPTS)[number]>("ALL");
  const [tool, setTool] = useState<(typeof TOOL_OPTS)[number]>("ALL");
  const [page, setPage] = useState(0);

  const { data, isLoading, isError } = useAuditList({ page, size: PAGE_SIZE });
  const audits = data?.content ?? [];
  
  const filtered = useMemo(() => {
  const searchTerm = search.toLowerCase();

  return audits.filter((a) => {
    const displayTitle = getAuditTitle(a);

    if (status !== "ALL" && a.status !== status) return false;
    if (tool !== "ALL" && a.agentTool !== tool) return false;

    if (
      search &&
      !displayTitle.toLowerCase().includes(searchTerm) &&
      !a.repoName?.toLowerCase().includes(searchTerm)
    ) {
      return false;
    }

    return true;
  });
}, [audits, status, tool, search]);

  const stats = useMemo(() => {
    const complete = audits.filter((a) => a.status === "COMPLETE").length;
    const failed = audits.filter((a) => a.status === "FAILED").length;
    const scored = audits.filter((a) => a.overallScore != null);
    const avg = scored.length
      ? Math.round(scored.reduce((s, a) => s + (a.overallScore ?? 0), 0) / scored.length)
      : null;
    return { complete, failed, avg };
  }, [audits]);

  return (
    <div className="bg-background min-h-screen" style={{ fontFamily: "var(--font-body)" }}>
      <div className="border-b-2 border-black px-8 py-5 bg-card">
        <div className="flex items-center gap-3 mb-1">
          <div className="border-2 border-black p-2 bg-secondary">
            <Clock size={14} className="text-secondary-foreground" />
          </div>
          <h1 style={{ fontFamily: "var(--font-display)", fontSize: 22, letterSpacing: "-0.02em" }}>
            Audit History
          </h1>
          <Badge variant="muted" className="font-mono text-xs">
            {data?.totalElements ?? 0} total
          </Badge>
        </div>
        <p style={{ fontFamily: "var(--font-body)", fontSize: 13, color: "var(--muted-foreground)" }}>
          All submitted traces and their audit outcomes.
        </p>
      </div>

      <div className="px-8 py-6 space-y-5">
        <div className="flex flex-wrap items-start gap-5">
          <div className="flex flex-col gap-1.5 flex-1 min-w-[180px] max-w-xs">
            <span
              className="flex items-center gap-1"
              style={{ fontFamily: "var(--font-display)", fontSize: 10, letterSpacing: "0.08em", color: "var(--muted-foreground)" }}
            >
              <Search size={11} />
              SEARCH
            </span>
            <div className="relative">
              <Search size={13} className="absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground pointer-events-none" />
              <Input
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                placeholder="Search this page by title or repo…"
                className="pl-9 bg-card"
              />
            </div>
          </div>

          <div className="flex flex-col gap-1.5">
            <span
              className="flex items-center gap-1"
              style={{ fontFamily: "var(--font-display)", fontSize: 10, letterSpacing: "0.08em", color: "var(--muted-foreground)" }}
            >
              <Filter size={11} />
              AUDIT STATUS
            </span>
            <div className="flex items-center gap-2 flex-wrap">
              {STATUS_OPTS.map((s) => (
                <Button
                  key={s}
                  variant={status === s ? "default" : "outline"}
                  onClick={() => setStatus(s)}
                  className="h-auto py-1.5 px-3 text-xs"
                >
                  {s}
                </Button>
              ))}
            </div>
          </div>

          <div className="flex flex-col gap-1.5">
            <span
              className="flex items-center gap-1"
              style={{ fontFamily: "var(--font-display)", fontSize: 10, letterSpacing: "0.08em", color: "var(--muted-foreground)" }}
            >
              <Bot size={11} />
              AGENT TOOL
            </span>
            <div className="flex items-center gap-2 flex-wrap">
              {TOOL_OPTS.map((t) => (
                <Button
                  key={t}
                  variant={tool === t ? "secondary" : "outline"}
                  onClick={() => setTool(t)}
                  className="h-auto py-1.5 px-3 text-[10px]"
                >
                  {t === "ALL" ? "ALL" : formatEnumLabel(t)}
                </Button>
              ))}
            </div>
          </div>
        </div>

        <div className="border-2 border-black bg-card">
          <div
            className="hidden md:grid border-b-2 border-black px-5 py-2.5 bg-muted"
            style={{ gridTemplateColumns: "1fr 120px 120px 70px 80px 32px" }}
          >
            {["TITLE / REPO", "AGENT TOOL", "DATE", "SCORE", "STATUS", ""].map((h) => (
              <span
                key={h}
                style={{ fontFamily: "var(--font-display)", fontSize: 10, letterSpacing: "0.08em", color: "var(--muted-foreground)" }}
              >
                {h}
              </span>
            ))}
          </div>

          {isError && (
            <div className="px-6 py-12 text-center">
              <p style={{ fontFamily: "var(--font-body)", color: "var(--destructive)", fontSize: 14 }}>
                Failed to load audit history.
              </p>
            </div>
          )}

          {isLoading && !isError && (
            <div className="px-6 py-12 text-center">
              <p style={{ fontFamily: "var(--font-body)", color: "var(--muted-foreground)", fontSize: 14 }}>
                Loading…
              </p>
            </div>
          )}

          {!isLoading && !isError && filtered.length === 0 && (
            <div className="px-6 py-12 text-center">
              <p style={{ fontFamily: "var(--font-body)", color: "var(--muted-foreground)", fontSize: 14 }}>
                No audits match your filters.
              </p>
            </div>
          )}

          {!isLoading && !isError && filtered.length > 0 && (
            <div className="divide-y-2 divide-black/10">
              {filtered.map((a) => {
                const verdict = deriveVerdict(a.reports);
                return (
                  <button
                    key={a.id}
                    onClick={() => navigate(`/app/audits/${a.id}`)}
                    className="w-full text-left px-5 py-4 hover:bg-[#F0EDE4] transition-colors"
                  >
                    {/* Mobile layout */}
                    <div className="md:hidden space-y-1">
                      <div className="flex items-start justify-between gap-2">
                        <span style={{ fontFamily: "var(--font-display)", fontSize: 13 }} className="line-clamp-1">
                          {getAuditTitle(a)}
                        </span>
                        <ChevronRight size={14} style={{ color: "var(--muted-foreground)", flexShrink: 0 }} />
                      </div>
                      <div style={{ fontFamily: "var(--font-mono)", fontSize: 10, color: "var(--muted-foreground)" }}>
                        {[a.repoName, formatEnumLabel(a.agentTool)].filter(Boolean).join(" · ")}
                      </div>
                      <div className="flex items-center gap-2 flex-wrap mt-1">
                        <Badge variant={statusBadgeVariant(a.status)}>{a.status}</Badge>
                        {verdict && <Badge variant={verdictBadgeVariant(verdict)}>{formatEnumLabel(verdict)}</Badge>}
                        {a.overallScore != null && (
                          <span style={{ fontFamily: "var(--font-mono)", fontSize: 12, fontWeight: 700 }}>
                            {a.overallScore}/100
                          </span>
                        )}
                      </div>
                    </div>

                    {/* Desktop layout */}
                    <div
                      className="hidden md:grid items-center gap-2"
                      style={{ gridTemplateColumns: "1fr 120px 120px 70px 80px 32px" }}
                    >
                      <div className="min-w-0">
                        <div style={{ fontFamily: "var(--font-display)", fontSize: 13 }} className="truncate">
                          {getAuditTitle(a)}
                        </div>
                        <div style={{ fontFamily: "var(--font-mono)", fontSize: 10, color: "var(--muted-foreground)" }}>
                          {a.repoName ?? "—"}
                        </div>
                      </div>
                      <span style={{ fontFamily: "var(--font-mono)", fontSize: 11 }}>{formatEnumLabel(a.agentTool)}</span>
                      <span style={{ fontFamily: "var(--font-mono)", fontSize: 11, color: "var(--muted-foreground)" }}>
                        {new Date(a.createdAt).toLocaleDateString("en-US", { month: "short", day: "numeric" })}
                      </span>
                      <div>
                        {a.overallScore != null && verdict ? (
                          <Badge variant={verdictBadgeVariant(verdict)}>{a.overallScore}</Badge>
                        ) : a.overallScore != null ? (
                          <span style={{ fontFamily: "var(--font-mono)", fontSize: 12, fontWeight: 700 }}>{a.overallScore}</span>
                        ) : (
                          <span style={{ fontFamily: "var(--font-mono)", fontSize: 11, color: "var(--muted-foreground)" }}>—</span>
                        )}
                      </div>
                      <Badge variant={statusBadgeVariant(a.status)}>{a.status}</Badge>
                      <ChevronRight size={14} style={{ color: "var(--muted-foreground)" }} />
                    </div>
                  </button>
                );
              })}
            </div>
          )}

          {data && data.totalPages > 1 && (
            <div className="border-t-2 border-black px-5 py-3 flex items-center justify-between">
              <Button
                variant="outline"
                size="sm"
                disabled={page === 0}
                onClick={() => setPage((p) => Math.max(0, p - 1))}
                className="flex items-center gap-1"
              >
                <ChevronLeft size={12} /> Prev
              </Button>
              <span style={{ fontFamily: "var(--font-mono)", fontSize: 11, color: "var(--muted-foreground)" }}>
                Page {data.number + 1} of {data.totalPages}
              </span>
              <Button
                variant="outline"
                size="sm"
                disabled={page + 1 >= data.totalPages}
                onClick={() => setPage((p) => p + 1)}
                className="flex items-center gap-1"
              >
                Next <ChevronRight size={12} />
              </Button>
            </div>
          )}
        </div>

        <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
          {[
            { label: "TOTAL (this page)", value: audits.length },
            { label: "COMPLETE", value: stats.complete },
            { label: "FAILED", value: stats.failed },
            { label: "AVG SCORE", value: stats.avg ?? "—" },
          ].map(({ label, value }) => (
            <div key={label} className="border-2 border-black px-5 py-4 bg-card">
              <div style={{ fontFamily: "var(--font-mono)", fontSize: 9, color: "var(--muted-foreground)", letterSpacing: "0.1em" }}>
                {label}
              </div>
              <div style={{ fontFamily: "var(--font-display)", fontSize: 28, letterSpacing: "-0.03em", marginTop: 4 }}>
                {value}
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}