import { z } from "zod";

const SeveritySchema = z.enum(["HIGH", "MEDIUM", "LOW"]);
export type FindingSeverity = z.infer<typeof SeveritySchema>;

export const LoopEfficiencyFindingSchema = z.object({
  issue_title: z.string(),
  severity: SeveritySchema,
  repeated_pattern: z.string(),
  evidence: z.array(z.string()),
  why_it_failed: z.string(),
  better_loop_rule: z.string(),
});

export const LoopEfficiencyFindingsSchema = z.object({
  severity_score: z.number(),
  dominant_loop_type: z.string().nullable().optional(),
  estimated_wasted_steps: z.number(),
  findings: z.array(LoopEfficiencyFindingSchema),
});
export type LoopEfficiencyFindings = z.infer<typeof LoopEfficiencyFindingsSchema>;

export const BlindOutcomeFindingSchema = z.object({
  issue_title: z.string(),
  severity: SeveritySchema,
  observable_evidence: z.array(z.string()),
  missing_evidence: z.array(z.string()),
  trust_impact: z.string(),
  recommended_verification: z.string(),
});

export const OutcomeVerdictSchema = z.enum([
  "LIKELY_COMPLETE",
  "UNVERIFIED",
  "LIKELY_INCOMPLETE",
  "CONTRADICTED",
]);
export type OutcomeVerdict = z.infer<typeof OutcomeVerdictSchema>;

export const BlindOutcomeFindingsSchema = z.object({
  severity_score: z.number(),
  outcome_verdict: OutcomeVerdictSchema,
  findings: z.array(BlindOutcomeFindingSchema),
});
export type BlindOutcomeFindings = z.infer<typeof BlindOutcomeFindingsSchema>;

export const ReliabilityTrendFindingSchema = z.object({
  issue_title: z.string(),
  severity: SeveritySchema,
  trend_signal: z.string(),
  evidence: z.array(z.string()),
  recommendation: z.string(),
});

export const TrendDirectionSchema = z.enum([
  "IMPROVING",
  "STABLE",
  "DECLINING",
  "INSUFFICIENT_HISTORY",
]);
export type TrendDirection = z.infer<typeof TrendDirectionSchema>;

export const ReliabilityTrendFindingsSchema = z.object({
  severity_score: z.number(),
  current_reliability_score: z.number(),
  previous_reliability_score: z.number().nullable().optional(),
  trend_direction: TrendDirectionSchema,
  findings: z.array(ReliabilityTrendFindingSchema),
});
export type ReliabilityTrendFindings = z.infer<typeof ReliabilityTrendFindingsSchema>;

/** Parses `AgentReport.findings` (a raw JSON string) based on its agentType. */
export function parseAgentFindings(
  agentType: "TRACE_LOOP_EFFICIENCY" | "BLIND_OUTCOME_VERIFIER" | "RELIABILITY_TREND",
  raw: string
) {
  let json: unknown;
  try {
    json = JSON.parse(raw);
  } catch {
    return null;
  }

  switch (agentType) {
    case "TRACE_LOOP_EFFICIENCY": {
      const parsed = LoopEfficiencyFindingsSchema.safeParse(json);
      return parsed.success ? parsed.data : null;
    }
    case "BLIND_OUTCOME_VERIFIER": {
      const parsed = BlindOutcomeFindingsSchema.safeParse(json);
      return parsed.success ? parsed.data : null;
    }
    case "RELIABILITY_TREND": {
      const parsed = ReliabilityTrendFindingsSchema.safeParse(json);
      return parsed.success ? parsed.data : null;
    }
  }
}