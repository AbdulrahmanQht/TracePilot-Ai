import { z } from "zod";

export const AuditStatusSchema = z.enum(["PENDING", "PROCESSING", "COMPLETE", "FAILED"]);
export type AuditStatus = z.infer<typeof AuditStatusSchema>;

export const AuditInputSourceSchema = z.enum([
  "PASTED_TEXT",
  "FILE_UPLOAD",
  "CI_LOG",
  "PR_BUNDLE",
]);
export type AuditInputSource = z.infer<typeof AuditInputSourceSchema>;

export const AgentToolTypeSchema = z.enum([
  "CLAUDE_CODE",
  "CODEX",
  "CURSOR",
  "DEVIN",
  "GITHUB_COPILOT",
  "GENERIC",
]);
export type AgentToolType = z.infer<typeof AgentToolTypeSchema>;
export const AgentToolRequestSchema = AgentToolTypeSchema.optional().default("GENERIC");

export const AgentToolResponseSchema = z.string().transform((v) => {
  const upper = v.toUpperCase();
  return AgentToolTypeSchema.safeParse(upper).success
    ? (upper as AgentToolType)
    : ("GENERIC" as AgentToolType);
});

export const AgentReportResponseSchema = z.object({
  id: z.uuid().optional(),
  agentType: z.enum(["TRACE_LOOP_EFFICIENCY", "BLIND_OUTCOME_VERIFIER", "RELIABILITY_TREND"]),
  findings: z.string(),
  severityScore: z.number().min(0).max(100),
  processingTimeMs: z.number().nullable().optional(),
  createdAt: z.string(),
});
export type AgentReportResponse = z.infer<typeof AgentReportResponseSchema>;

export const AuditResponseSchema = z.object({
  id: z.string().uuid(),
  title: z.string().nullable(),
  repoName: z.string().nullable(),
  agentTool: AgentToolResponseSchema,
  inputSource: AuditInputSourceSchema,
  status: AuditStatusSchema,
  overallScore: z.number().min(0).max(100).nullable(),
  reports: z.array(AgentReportResponseSchema),
  isPublic: z.boolean(),
  shareToken: z.string().nullable(),
  createdAt: z.string(),
  completedAt: z.string().nullable(),
});
export type AuditResponse = z.infer<typeof AuditResponseSchema>;