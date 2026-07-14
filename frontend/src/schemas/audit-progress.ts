import { z } from "zod";

export const AuditProgressMessageSchema = z.object({
  auditId: z.string(),
  agentType: z.enum(["TRACE_LOOP_EFFICIENCY", "BLIND_OUTCOME_VERIFIER", "RELIABILITY_TREND"]),
  step: z.string().nullable().optional(),
  status: z.enum(["STARTED", "DONE"]),
  message: z.string().nullable().optional(),
});
export type AuditProgressMessage = z.infer<typeof AuditProgressMessageSchema>;