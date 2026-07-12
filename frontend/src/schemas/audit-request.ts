import { z } from "zod";
import { AuditInputSourceSchema, AgentToolRequestSchema } from "./audit";

export const AuditRequestSchema = z.object({
  rawTrace: z.string().min(1).max(80000),
  title: z.string().max(150).optional(),
  repoName: z.string().max(150).optional(),
  agentTool: AgentToolRequestSchema,
  inputSource: AuditInputSourceSchema.optional(),
});
export type AuditRequest = z.infer<typeof AuditRequestSchema>;