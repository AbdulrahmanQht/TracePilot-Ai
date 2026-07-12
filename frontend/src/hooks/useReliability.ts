import { useQuery, type UseQueryOptions } from "@tanstack/react-query";
import { getReliabilityTrend } from "../api/reliability";
import type { ApiError } from "../schemas/error";

interface ReliabilityParams {
  repoName: string;
  agentTool: string;
  limit?: number;
}

type ReliabilityResponse = Awaited<ReturnType<typeof getReliabilityTrend>>;

export const reliabilityKeys = {
  all: ["reliability"] as const,
  trend: (params: ReliabilityParams) => [...reliabilityKeys.all, "trend", params] as const,
};

export function useReliabilityTrend(
  params: ReliabilityParams,
  options?: Omit<UseQueryOptions<ReliabilityResponse, ApiError>, "queryKey" | "queryFn">
) {
  return useQuery({
    queryKey: reliabilityKeys.trend(params),
    queryFn: () => getReliabilityTrend(params),
    ...options,
  });
}