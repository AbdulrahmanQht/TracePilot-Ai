import { apiClient } from "./client";
import { ReliabilityListSchema } from "../schemas/reliability";

export async function getReliabilityTrend(params: {
  repoName: string;
  agentTool: string;
  limit?: number;
}) {
  const { data } = await apiClient.get("/reliability", { params });
  return ReliabilityListSchema.parse(data);
}