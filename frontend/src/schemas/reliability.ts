import { z } from "zod";

export const ReliabilityResponseSchema = z.object({
  id: z.uuid(),
  repoName: z.string(),
  agentTool: z.string(),
  reliabilityScore: z.number(),
  signalSummary: z.string(),
  recordedAt: z.string(),
});
export type ReliabilityResponse = z.infer<typeof ReliabilityResponseSchema>;

export const ReliabilityListSchema = z.array(ReliabilityResponseSchema);