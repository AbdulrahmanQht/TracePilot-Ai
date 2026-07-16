import { apiClient } from "./client";
import { HealthResponseSchema, type HealthResponse } from "../schemas/health";

export async function fetchHealth(): Promise<HealthResponse> {
  const { data } = await apiClient.get("/health");
  return HealthResponseSchema.parse(data);
}