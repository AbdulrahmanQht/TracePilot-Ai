import { useEffect, useMemo } from "react";
import { useParams, useNavigate } from "react-router";
import { useQueryClient } from "@tanstack/react-query";
import { Check, XCircle, Zap, Terminal, BarChart2 } from "lucide-react";
import { useAudit, auditKeys } from "@/hooks/useAudit";
import { useAuditStream } from "@/hooks/useAuditStream";
import type { AuditResponse } from "@/schemas/audit";
import { Button } from "@/components/ui/button";

type AgentType = "TRACE_LOOP_EFFICIENCY" | "BLIND_OUTCOME_VERIFIER" | "RELIABILITY_TREND";

const AGENT_STEPS: { type: AgentType; label: string; icon: React.ReactNode }[] = [
  { type: "TRACE_LOOP_EFFICIENCY", label: "Loop Efficiency Agent", icon: <Zap size={11} /> },
  { type: "BLIND_OUTCOME_VERIFIER", label: "Blind Outcome Verifier", icon: <Terminal size={11} /> },
  { type: "RELIABILITY_TREND", label: "Reliability Trend Agent", icon: <BarChart2 size={11} /> },
];

const TERMINAL_STATUSES: AuditResponse["status"][] = ["COMPLETE", "FAILED"];

export default function ProcessingPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const { data: audit, isLoading, isError } = useAudit(id ?? "", {
    enabled: !!id,
  });

  const alreadyTerminal = !!audit && TERMINAL_STATUSES.includes(audit.status);

  const { agentEvents, streamFailed } = useAuditStream(id, {
    enabled: !!id && !alreadyTerminal,
    onFinal: (finalAudit) => {
      if (!id) return;
      queryClient.setQueryData(auditKeys.detail(id), finalAudit);
      navigate(`/app/audits/${id}`, { replace: true });
    },
  });

  // If the audit was already COMPLETE/FAILED by the time we loaded it (e.g. the
  // user navigated here directly, or landed after the SSE race already resolved
  // on first subscribe), skip straight to the detail page.
  useEffect(() => {
    if (id && alreadyTerminal) {
      navigate(`/app/audits/${id}`, { replace: true });
    }
  }, [id, alreadyTerminal, navigate]);

  // Fallback: if the SSE connection itself fails (proxy/network issue), poll
  // the audit resource directly instead of leaving the user stuck.
  useEffect(() => {
    if (!streamFailed || !id) return;
    const iv = setInterval(() => {
      queryClient.invalidateQueries({ queryKey: auditKeys.detail(id) });
    }, 3000);
    return () => clearInterval(iv);
  }, [streamFailed, id, queryClient]);

  const allAgentsDone = useMemo(
    () => AGENT_STEPS.every((s) => agentEvents[s.type]?.status === "DONE"),
    [agentEvents]
  );

  if (!id) {
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

  if (isError) {
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

  return (
    <div className="min-h-screen flex flex-col items-center justify-center gap-8 px-6 bg-background">
      <div className="border-2 border-black w-full max-w-md bg-card">
        <div className="border-b-2 border-black px-6 py-4 bg-primary">
          <p style={{ fontFamily: "var(--font-display)", color: "var(--primary-foreground)", fontSize: 14 }}>
            AUDIT IN PROGRESS
          </p>
          <p
            style={{
              fontFamily: "var(--font-mono)",
              color: "rgba(244,241,234,0.55)",
              fontSize: 10,
              marginTop: 2,
              letterSpacing: "0.06em",
            }}
          >
            {isLoading ? "Loading…" : audit?.title || `Audit #${id.slice(0, 8)}`}
          </p>
        </div>

        <div className="divide-y-2 divide-black/10">
          {/* Queued is implicit: if this page is rendering, the audit row exists and the job was published. */}
          <StepRow label="Queued" done active={false} />

          {AGENT_STEPS.map(({ type, label, icon }) => {
            const event = agentEvents[type];
            const done = event?.status === "DONE";
            const active = event?.status === "STARTED" && !done;
            return (
              <StepRow key={type} label={label} icon={icon} done={done} active={active} />
            );
          })}

          <StepRow label="Generating report" done={false} active={allAgentsDone} />
        </div>

        <div className="px-6 py-4 border-t-2 border-black bg-muted">
          <p
            style={{
              fontFamily: "var(--font-mono)",
              fontSize: 10,
              color: "var(--muted-foreground)",
              letterSpacing: "0.06em",
            }}
          >
            {streamFailed
              ? "Live updates unavailable · polling for status…"
              : "Page will update automatically · Est. 60–120s"}
          </p>
        </div>
      </div>
    </div>
  );
}

function StepRow({
  label,
  icon,
  done,
  active,
}: {
  label: string;
  icon?: React.ReactNode;
  done: boolean;
  active: boolean;
}) {
  return (
    <div className="flex items-center gap-3 px-6 py-3.5">
      <div
        className="w-5 h-5 border-2 border-black flex items-center justify-center shrink-0"
        style={{ background: done ? "var(--primary)" : active ? "var(--secondary)" : "var(--muted)" }}
      >
        {done && <Check size={10} className="text-primary-foreground" />}
        {active && !done && <div className="w-2 h-2 animate-pulse bg-primary-foreground" />}
        {!done && !active && icon && <span className="opacity-30">{icon}</span>}
      </div>
      <span
        style={{
          fontFamily: "var(--font-body)",
          fontSize: 13,
          color: done || active ? "var(--foreground)" : "var(--muted-foreground)",
        }}
      >
        {label}
      </span>
      {active && (
        <span
          style={{ fontFamily: "var(--font-mono)", fontSize: 9, color: "var(--secondary)", letterSpacing: "0.1em" }}
        >
          RUNNING…
        </span>
      )}
    </div>
  );
}