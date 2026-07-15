import { useState } from "react";
import { useNavigate } from "react-router";
import { useForm, Controller, useWatch } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { Send, Terminal, Zap, AlertTriangle } from "lucide-react";
import { z } from "zod";
import { AuditRequestSchema, type AuditRequest } from "@/schemas/audit-request";
import { AgentToolTypeSchema, AuditInputSourceSchema } from "@/schemas/audit";
import { useSubmitAudit } from "@/hooks/useAudit";
import { useAuditList } from "@/hooks/useAudit";
import { useCurrentUser } from "@/hooks/useUser";
import { TraceEditor } from "@/components/TraceEditor";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Badge } from "@/components/ui/badge";
import { Progress, ProgressTrack, ProgressIndicator } from "@/components/ui/progress";
import { Alert, AlertDescription } from "@/components/ui/alert";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";

const TOOL_OPTIONS = AgentToolTypeSchema.options;
const SOURCE_OPTIONS = AuditInputSourceSchema.options;
const DAILY_AUDIT_LIMIT = 10;

function verdictVariant(score: number | null): "verdict-contradicted" | "verdict-unverified" | "verdict-complete" | "verdict-incomplete" {
  if (score === null) return "verdict-unverified";
  if (score >= 80) return "verdict-complete";
  if (score >= 50) return "verdict-incomplete";
  return "verdict-contradicted";
}

export default function SubmitPage() {
  const navigate = useNavigate();
  const [serverError, setServerError] = useState<string | null>(null);

  type AuditFormInput = z.input<typeof AuditRequestSchema>;

  const { control, register, handleSubmit, formState: { errors } } =
    useForm<AuditFormInput>({
      resolver: zodResolver(AuditRequestSchema),
      defaultValues: {
        rawTrace: "",
        title: "",
        repoName: "",
        agentTool: "GENERIC",
        inputSource: "PASTED_TEXT",
      },
    });

  const rawTrace = useWatch({ control, name: "rawTrace" });
  const submitMutation = useSubmitAudit({
    onSuccess: (audit) => {
      navigate(`/app/audits/${audit.id}/processing`);
    },
    onError: (err) => {
      setServerError(err.message);
    },
  });

  const { data: recentAuditsPage } = useAuditList({ page: 0, size: 3 });
  const recentAudits = recentAuditsPage?.content ?? [];

  const { data: currentUser } = useCurrentUser();
  const auditsUsed = currentUser?.auditCountToday ?? 0;
  const quotaPct = Math.min(100, (auditsUsed / DAILY_AUDIT_LIMIT) * 100);

  function onSubmit(values: AuditFormInput) {
    setServerError(null);
    const payload: AuditRequest = {
      ...values,
      agentTool: values.agentTool ?? "GENERIC",
      title: values.title?.trim() ? values.title.trim() : undefined,
      repoName: values.repoName?.trim() ? values.repoName.trim() : undefined,
    };
    submitMutation.mutate(payload);
  }

  return (
    <div className="bg-background min-h-screen" style={{ fontFamily: "var(--font-body)" }}>

      <div className="border-b-2 border-black px-8 py-5 bg-card">
        <div className="flex items-center gap-3 mb-1">
          <div className="border-2 border-black p-2 bg-primary">
            <Send size={14} className="text-primary-foreground" />
          </div>
          <h1 style={{ fontFamily: "var(--font-display)", fontSize: 22, letterSpacing: "-0.02em" }}>Submit Trace</h1>
        </div>
        <p style={{ fontFamily: "var(--font-body)", fontSize: 13, color: "var(--muted-foreground)" }}>
          Paste or upload a coding-agent transcript to start an audit.
        </p>
      </div>

      <div className="px-8 py-7 grid grid-cols-1 xl:grid-cols-3 gap-6">

        <form onSubmit={handleSubmit(onSubmit)} className="xl:col-span-2 space-y-5">

          <div className="space-y-2">
            
            <Controller
              name="rawTrace"
              control={control}
              render={({ field }) => (
                <TraceEditor
                  value={field.value}
                  onChange={field.onChange}
                  height="420px"
                />
              )}
            />

            {errors.rawTrace && (
              <Alert variant="destructive">
                <AlertTriangle size={13} />
                <AlertDescription style={{ fontFamily: "var(--font-body)", fontSize: 12 }}>
                  {errors.rawTrace.message}
                </AlertDescription>
              </Alert>
            )}

            {serverError && (
              <Alert variant="destructive">
                <AlertTriangle size={13} />
                <AlertDescription style={{ fontFamily: "var(--font-body)", fontSize: 12 }}>
                  {serverError}
                </AlertDescription>
              </Alert>
            )}
          </div>

          <div className="border-2 border-black bg-card">
            <div className="border-b-2 border-black px-5 py-3 bg-muted">
              <span style={{ fontFamily: "var(--font-display)", fontSize: 11, letterSpacing: "0.06em" }}>OPTIONAL METADATA</span>
              <span style={{ fontFamily: "var(--font-body)", fontSize: 11, color: "var(--muted-foreground)", marginLeft: 12 }}>
                Improves report quality but not required
              </span>
            </div>
            <div className="p-5 grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div className="sm:col-span-2 space-y-1">
                <Label variant="default">TITLE</Label>
                <Input {...register("title")}
                  placeholder="e.g. Billing discount fix — claimed tests pass" />
              </div>
              <div className="space-y-1">
                <Label variant="default">REPOSITORY</Label>
                <Input {...register("repoName")}
                  placeholder="e.g. billing-service" />
              </div>
              <div className="space-y-1">
                <Label variant="default">AGENT TOOL</Label>
                <Controller
                  name="agentTool"
                  control={control}
                  render={({ field }) => (
                    <Select value={field.value} onValueChange={field.onChange}>
                      <SelectTrigger>
                        <SelectValue />
                      </SelectTrigger>
                      <SelectContent>
                        {TOOL_OPTIONS.map(t => <SelectItem key={t} value={t}>{t}</SelectItem>)}
                      </SelectContent>
                    </Select>
                  )}
                />
              </div>
              <div className="sm:col-span-2 space-y-1">
                <Label variant="default">INPUT SOURCE</Label>
                <Controller
                  name="inputSource"
                  control={control}
                  render={({ field }) => (
                    <div className="flex gap-2 flex-wrap">
                      {SOURCE_OPTIONS.map(s => (
                        <Button key={s} type="button"
                          variant={field.value === s ? "default" : "outline"}
                          onClick={() => field.onChange(s)}
                          className={`text-xs h-auto py-1.5 px-3 ${field.value === s ? "shadow-[2px_2px_0px_#0D0D0D]" : ""}`}>
                          {s.replace("_", " ")}
                        </Button>
                      ))}
                    </div>
                  )}
                />
              </div>
            </div>
          </div>

          <Button type="submit" disabled={!rawTrace?.trim() || submitMutation.isPending} variant="default"
            className="w-full flex items-center justify-center gap-2 py-4 text-base">
            {submitMutation.isPending
              ? <span style={{ fontFamily: "var(--font-mono)", fontSize: 12, letterSpacing: "0.1em" }}>QUEUING AUDIT…</span>
              : <><Terminal size={16} /> Start Audit</>}
          </Button>
        </form>

        <div className="space-y-4">
          <div className="border-2 border-black shadow-[4px_4px_0px_#0D0D0D] bg-card">
            <div className="border-b-2 border-black px-5 py-3 bg-secondary">
              <span style={{ fontFamily: "var(--font-display)", color: "var(--secondary-foreground)", fontSize: 12, letterSpacing: "0.06em" }}>WHAT WE AUDIT</span>
            </div>
            <div className="divide-y-2 divide-black">
              {[
                { icon: <Zap size={13} />, label: "Loop Efficiency", desc: "Repeated tool calls, missing stop conditions, wasted retries" },
                { icon: <Terminal size={13} />, label: "Blind Outcome Verification", desc: "Independent verdict from observable evidence only" },
                { icon: <AlertTriangle size={13} />, label: "Reliability Trend", desc: "Cross-run scoring for your repo + agent combination" },
              ].map(({ icon, label, desc }) => (
                <div key={label} className="flex gap-3 px-5 py-4">
                  <div className="border-2 border-black p-1.5 h-fit shrink-0 bg-primary text-primary-foreground">{icon}</div>
                  <div>
                    <div style={{ fontFamily: "var(--font-display)", fontSize: 12 }}>{label}</div>
                    <div style={{ fontFamily: "var(--font-body)", fontSize: 12, color: "var(--muted-foreground)", marginTop: 2 }}>{desc}</div>
                  </div>
                </div>
              ))}
            </div>
          </div>
          
          {recentAudits.length > 0 && (
            <div className="border-2 border-black bg-card">
              <div className="border-b-2 border-black px-5 py-3 bg-muted">
                <span
                  style={{
                    fontFamily: "var(--font-display)",
                    fontSize: 11,
                    letterSpacing: "0.06em",
                  }}
                >
                  RECENT AUDITS
                </span>
              </div>

              <div className="divide-y-2 divide-black">
                {recentAudits.map((a: import("@/schemas/audit").AuditResponse) => (
                  <button
                    key={a.id}
                    onClick={() => navigate(`/app/audits/${a.id}`)}
                    className="w-full text-left px-5 py-3 hover:bg-[#F0EDE4] transition-colors"
                  >
                    <div className="flex items-start justify-between gap-2">
                      <span
                        style={{
                          fontFamily: "var(--font-display)",
                          fontSize: 12,
                        }}
                        className="truncate"
                      >
                        {a.title ?? "Untitled audit"}
                      </span>

                      <Badge
                        variant={verdictVariant(a.overallScore)}
                        className="shrink-0"
                      >
                        {a.overallScore ?? "—"}
                      </Badge>
                    </div>

                    <div
                      style={{
                        fontFamily: "var(--font-mono)",
                        fontSize: 10,
                        color: "var(--muted-foreground)",
                        marginTop: 2,
                      }}
                    >
                      {a.repoName ?? "—"} · {a.agentTool}
                    </div>
                  </button>
                ))}
              </div>
            </div>
          )}

          <div className="border-2 border-black px-5 py-4 bg-muted">
            <div style={{ fontFamily: "var(--font-mono)", fontSize: 9, color: "var(--muted-foreground)", letterSpacing: "0.1em", marginBottom: 8 }}>
              DAILY AUDIT QUOTA
            </div>
            <div className="flex items-end gap-1 mb-2">
              <span style={{ fontFamily: "var(--font-display)", fontSize: 28, letterSpacing: "-0.02em" }}>{auditsUsed}</span>
              <span style={{ fontFamily: "var(--font-body)", fontSize: 13, color: "var(--muted-foreground)", marginBottom: 3 }}> / {DAILY_AUDIT_LIMIT} used</span>
            </div>
            <Progress value={quotaPct}>
              <ProgressTrack size="lg">
                <ProgressIndicator color="primary" />
              </ProgressTrack>
            </Progress>
          </div>
        </div>
      </div>
    </div>
  );
}