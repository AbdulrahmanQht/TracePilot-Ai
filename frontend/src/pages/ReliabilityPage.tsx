import { useState, useMemo } from "react";
import { useQueries } from "@tanstack/react-query";
import {
  LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip,
  ResponsiveContainer, ReferenceLine
} from "recharts";
import { BarChart2 } from "lucide-react";
import type { ValueType, NameType } from "recharts/types/component/DefaultTooltipContent";
import type { Payload } from "recharts/types/component/DefaultTooltipContent";
import { useAuditList } from "@/hooks/useAudit";
import { useReliabilityTrend, reliabilityKeys } from "@/hooks/useReliability";
import { getReliabilityTrend } from "@/api/reliability";
import { AgentToolTypeSchema } from "@/schemas/audit";
import { Label } from "@/components/ui/label";
import { Progress, ProgressTrack, ProgressIndicator } from "@/components/ui/progress";
import {
  Select, SelectContent, SelectItem, SelectTrigger, SelectValue,
} from "@/components/ui/select";

const LINE_COLOR = "#1E3A2F";

function formatEnumLabel(v: string): string {
  return v.split("_").map((w) => w.charAt(0) + w.slice(1).toLowerCase()).join(" ");
}

type CustomTooltipProps = {
  active?: boolean;
  payload?: Payload<ValueType, NameType>[];
  label?: string;
};

function CustomTooltip({ active, payload, label }: CustomTooltipProps) {
  if (!active || !payload?.length) return null;

  return (
    <div
      className="border-2 border-black px-4 py-3 min-w-[160px] bg-card"
      style={{ fontFamily: "var(--font-body)" }}
    >
      <p
        style={{
          fontFamily: "var(--font-mono)",
          fontSize: 10,
          color: "var(--muted-foreground)",
          marginBottom: 6,
        }}
      >
        {label}
      </p>

      {payload.map((p) => (
        <div key={String(p.name)} className="flex items-center justify-between gap-4">
          <span
            style={{
              fontFamily: "var(--font-body)",
              fontSize: 12,
              color: "var(--foreground)",
            }}
          >
            {p.name}
          </span>

          <span
            style={{
              fontFamily: "var(--font-display)",
              fontSize: 14,
              color: p.color,
            }}
          >
            {p.value}
          </span>
        </div>
      ))}
    </div>
  );
}

function FilterSelect({
  value, onValueChange, opts, label, formatOpt,
}: {
  value: string; onValueChange: (v: string) => void; opts: string[]; label: string; formatOpt?: (o: string) => string;
}) {
  return (
    <div className="space-y-1">
      <Label variant="muted">{label}</Label>
      <Select value={value} onValueChange={(v) => v != null && onValueChange(v)}>
        <SelectTrigger className="min-w-[140px]">
          <SelectValue>
            {formatOpt ? formatOpt(value) : value}
          </SelectValue>
        </SelectTrigger>
        <SelectContent>
          {opts.map((o) => (
            <SelectItem key={o} value={o}>{formatOpt ? formatOpt(o) : o}</SelectItem>
          ))}
        </SelectContent>
      </Select>
    </div>
  );
}

const TOOL_OPTS = AgentToolTypeSchema.options;

export default function ReliabilityPage() {
  // Pull a page of the user's audits purely to derive which repos/tools they
  // actually have data for — there's no dedicated "list distinct repos" endpoint.
  const { data: auditPage, isLoading: auditsLoading } = useAuditList({ page: 0, size: 100 });

  const repoOptions = useMemo(() => {
    const set = new Set<string>();
    for (const a of auditPage?.content ?? []) if (a.repoName) set.add(a.repoName);
    return Array.from(set).sort();
  }, [auditPage]);

  const toolOptions = useMemo(() => {
    const set = new Set<string>();
    for (const a of auditPage?.content ?? []) set.add(a.agentTool);
    return Array.from(set).sort();
  }, [auditPage]);

  const [repo, setRepo] = useState<string | null>(null);
  const [tool, setTool] = useState<string | null>(null);

  const effectiveRepo = repo ?? repoOptions[0] ?? null;
  const effectiveTool =
  tool ??
  (toolOptions.includes("GENERIC") ? "GENERIC" : toolOptions[0]) ??
  "GENERIC";

  const { data: series, isLoading: seriesLoading } = useReliabilityTrend(
    { repoName: effectiveRepo ?? "", agentTool: effectiveTool, limit: 30 },
    { enabled: !!effectiveRepo }
  );

  const chartData = useMemo(() => {
    if (!series) return [];
    return series
      .slice()
      .sort((a, b) => a.recordedAt.localeCompare(b.recordedAt))
      .map((r) => ({
        date: new Date(r.recordedAt).toLocaleDateString("en-US", { month: "short", day: "numeric" }),
        score: r.reliabilityScore,
      }));
  }, [series]);

  const avg = chartData.length
    ? Math.round(chartData.reduce((s, p) => s + p.score, 0) / chartData.length)
    : null;
  const trend = chartData.length >= 2 ? chartData.at(-1)!.score - chartData[0]!.score : 0;

  // Latest score per repo, scoped to the selected tool, one query per repo.
  const perRepoQueries = useQueries({
    queries: repoOptions.map((r) => ({
      queryKey: reliabilityKeys.trend({ repoName: r, agentTool: effectiveTool, limit: 5 }),
      queryFn: () => getReliabilityTrend({ repoName: r, agentTool: effectiveTool, limit: 5 }),
      enabled: !!effectiveTool,
    })),
  });

  const perRepoRows = repoOptions.map((r, i) => {
    const q = perRepoQueries[i];
    const pts = (q?.data ?? []).slice().sort((a, b) => a.recordedAt.localeCompare(b.recordedAt));
    const latest = pts.at(-1)?.reliabilityScore ?? null;
    const first = pts[0]?.reliabilityScore ?? null;
    const delta = latest != null && first != null ? latest - first : null;
    return { repo: r, runs: pts.length, latest, delta, isLoading: q?.isLoading ?? false };
  });

  return (
    <div className="bg-background min-h-screen" style={{ fontFamily: "var(--font-body)" }}>
      <div className="border-b-2 border-black px-8 py-5 bg-card">
        <div className="flex items-center gap-3 mb-1">
          <div className="border-2 border-black p-2 bg-primary">
            <BarChart2 size={14} className="text-primary-foreground" />
          </div>
          <h1 style={{ fontFamily: "var(--font-display)", fontSize: 22, letterSpacing: "-0.02em" }}>
            Reliability Trends
          </h1>
        </div>
        <p style={{ fontFamily: "var(--font-body)", fontSize: 13, color: "var(--muted-foreground)" }}>
          Score trajectory over time for a repository and agent tool.
        </p>
      </div>

      <div className="px-8 py-6 space-y-5">
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
          {[
            { label: "AVG SCORE", value: avg ?? "—", unit: avg != null ? "/100" : "" },
            { label: "RUNS SHOWN", value: chartData.length, unit: "" },
            { label: "TREND", value: chartData.length >= 2 ? (trend >= 0 ? `+${trend}` : String(trend)) : "—", unit: chartData.length >= 2 ? "pts" : "" },
            { label: "REPOS TRACKED", value: repoOptions.length, unit: "" },
          ].map(({ label, value, unit }) => (
            <div key={label} className="border-2 border-black px-5 py-4 bg-card">
              <div style={{ fontFamily: "var(--font-mono)", fontSize: 9, color: "var(--muted-foreground)", letterSpacing: "0.1em" }}>{label}</div>
              <div className="flex items-end gap-1 mt-1">
                <span style={{ fontFamily: "var(--font-display)", fontSize: 28, letterSpacing: "-0.03em" }}>{value}</span>
                {unit && <span style={{ fontFamily: "var(--font-body)", fontSize: 12, color: "var(--muted-foreground)", marginBottom: 3 }}>{unit}</span>}
              </div>
            </div>
          ))}
        </div>

        <div className="border-2 border-black shadow-[4px_4px_0px_#0D0D0D] bg-card">
          <div className="border-b-2 border-black px-6 py-4 flex flex-wrap items-end gap-4 justify-between bg-muted">
            <div style={{ fontFamily: "var(--font-display)", fontSize: 12, letterSpacing: "0.04em" }}>RELIABILITY SCORE OVER TIME</div>
            <div className="flex gap-4">
              {repoOptions.length > 0 && (
                <FilterSelect value={effectiveRepo ?? ""} onValueChange={setRepo} opts={repoOptions} label="REPO" />
              )}
              <FilterSelect value={effectiveTool} onValueChange={setTool} opts={toolOptions.length ? toolOptions : TOOL_OPTS} label="TOOL" formatOpt={formatEnumLabel} />
            </div>
          </div>
          <div className="p-6">
            {auditsLoading || seriesLoading ? (
              <div className="h-64 flex items-center justify-center border-2 border-dashed border-black/20">
                <p style={{ fontFamily: "var(--font-body)", color: "var(--muted-foreground)", fontSize: 14 }}>Loading…</p>
              </div>
            ) : !effectiveRepo || chartData.length === 0 ? (
              <div className="h-64 flex items-center justify-center border-2 border-dashed border-black/20">
                <p style={{ fontFamily: "var(--font-body)", color: "var(--muted-foreground)", fontSize: 14 }}>
                  {repoOptions.length === 0 ? "No audits with a repo name found." : "No reliability data for selected filters."}
                </p>
              </div>
            ) : (
              <ResponsiveContainer width="100%" height={300}>
                <LineChart data={chartData} margin={{ top: 4, right: 16, bottom: 4, left: -8 }}>
                  <CartesianGrid stroke="var(--muted)" strokeWidth={1} vertical={false} />
                  <XAxis
                    dataKey="date"
                    tick={{ fontFamily: "var(--font-mono)", fontSize: 10, fill: "var(--muted-foreground)" }}
                    tickLine={false} axisLine={{ stroke: "var(--foreground)", strokeWidth: 2 }}
                  />
                  <YAxis
                    domain={[0, 100]}
                    tick={{ fontFamily: "var(--font-mono)", fontSize: 10, fill: "var(--muted-foreground)" }}
                    tickLine={false} axisLine={false}
                  />
                  <Tooltip content={<CustomTooltip />} />
                  <ReferenceLine y={70} stroke="var(--primary)" strokeDasharray="4 3" strokeWidth={1.5} />
                  <ReferenceLine y={40} stroke="var(--destructive)" strokeDasharray="4 3" strokeWidth={1.5} />
                  <Line
                    name={`${effectiveRepo} / ${formatEnumLabel(effectiveTool)}`}
                    type="monotone" dataKey="score"
                    stroke={LINE_COLOR} strokeWidth={2.5}
                    dot={{ fill: LINE_COLOR, r: 4, strokeWidth: 2, stroke: "var(--foreground)" }}
                    activeDot={{ r: 6, strokeWidth: 2, stroke: "var(--foreground)" }}
                    connectNulls={false}
                  />
                </LineChart>
              </ResponsiveContainer>
            )}
            {chartData.length > 0 && (
              <div className="flex flex-wrap gap-4 mt-4 pt-4 border-t-2 border-black/10">
                <div className="flex items-center gap-2">
                  <div className="w-4 h-0.5 border-t-2" style={{ borderColor: LINE_COLOR }} />
                  <span style={{ fontFamily: "var(--font-mono)", fontSize: 10, color: "var(--muted-foreground)" }}>
                    {effectiveRepo} / {formatEnumLabel(effectiveTool)}
                  </span>
                </div>
                <div className="flex items-center gap-2 ml-auto">
                  <div className="w-4 border-t-2 border-dashed border-primary" />
                  <span style={{ fontFamily: "var(--font-mono)", fontSize: 9, color: "var(--muted-foreground)" }}>70 — TRUSTWORTHY</span>
                </div>
                <div className="flex items-center gap-2">
                  <div className="w-4 border-t-2 border-dashed border-destructive" />
                  <span style={{ fontFamily: "var(--font-mono)", fontSize: 9, color: "var(--muted-foreground)" }}>40 — UNCERTAIN</span>
                </div>
              </div>
            )}
          </div>
        </div>

        {repoOptions.length > 0 && (
          <div className="border-2 border-black bg-card">
            <div className="border-b-2 border-black px-5 py-3 bg-muted">
              <span style={{ fontFamily: "var(--font-display)", fontSize: 11, letterSpacing: "0.06em" }}>
                LATEST SCORE PER REPO · {formatEnumLabel(effectiveTool)}
              </span>
            </div>
            <div className="divide-y-2 divide-black/10">
              {perRepoRows.map(({ repo: r, runs, latest, delta, isLoading }) => (
                <div key={r} className="flex items-center gap-4 px-5 py-4">
                  <div className="flex-1 min-w-0">
                    <div style={{ fontFamily: "var(--font-display)", fontSize: 13 }} className="truncate">{r}</div>
                    <div style={{ fontFamily: "var(--font-mono)", fontSize: 10, color: "var(--muted-foreground)" }}>
                      {isLoading ? "loading…" : `${runs} runs tracked`}
                    </div>
                  </div>
                  {latest != null ? (
                    <>
                      <Progress value={latest} className="w-32">
                        <ProgressTrack size="sm">
                          <ProgressIndicator color={latest >= 70 ? "primary" : latest >= 40 ? "secondary" : "destructive"} />
                        </ProgressTrack>
                      </Progress>
                      <span style={{ fontFamily: "var(--font-display)", fontSize: 16, minWidth: 36, textAlign: "right" }}>{latest}</span>
                      <span
                        style={{
                          fontFamily: "var(--font-mono)", fontSize: 10, minWidth: 32, textAlign: "right",
                          color: (delta ?? 0) >= 0 ? "var(--primary)" : "var(--destructive)",
                        }}
                      >
                        {delta != null ? `${delta >= 0 ? "+" : ""}${delta}` : "—"}
                      </span>
                    </>
                  ) : (
                    <span style={{ fontFamily: "var(--font-mono)", fontSize: 11, color: "var(--muted-foreground)" }}>
                      {isLoading ? "…" : "no data"}
                    </span>
                  )}
                </div>
              ))}
            </div>
          </div>
        )}
      </div>
    </div>
  );
}