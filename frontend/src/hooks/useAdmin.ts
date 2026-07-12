import { useQuery, type UseQueryOptions } from "@tanstack/react-query";
import { getUsers, getFlaggedAudits } from "../api/admin";
import type { ApiError } from "../schemas/error";

type UsersResponse = Awaited<ReturnType<typeof getUsers>>;
type AuditsResponse = Awaited<ReturnType<typeof getFlaggedAudits>>;

interface PaginationParams {
  page?: number;
  size?: number;
}

export const adminKeys = {
  all: ["admin"] as const,
  users: (params?: PaginationParams) => [...adminKeys.all, "users", params] as const,
  audits: (params?: PaginationParams) => [...adminKeys.all, "audits", params] as const,
};

export function useAdminUsers(
  params?: PaginationParams,
  options?: Omit<UseQueryOptions<UsersResponse, ApiError>, "queryKey" | "queryFn">
) {
  return useQuery({
    queryKey: adminKeys.users(params),
    queryFn: () => getUsers(params),
    ...options,
  });
}

export function useAdminFlaggedAudits(
  params?: PaginationParams,
  options?: Omit<UseQueryOptions<AuditsResponse, ApiError>, "queryKey" | "queryFn">
) {
  return useQuery({
    queryKey: adminKeys.audits(params),
    queryFn: () => getFlaggedAudits(params),
    ...options,
  });
}