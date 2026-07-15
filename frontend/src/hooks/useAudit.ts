import { useQuery, useMutation, type UseQueryOptions, type UseMutationOptions } from "@tanstack/react-query";
import {
  submitAudit, getAudit, listAudits, deleteAudit,
  shareAudit, revokeShareLink, getSharedReport, retryAudit
} from "../api/audits";
import type { AuditResponse } from "../schemas/audit";
import type { AuditRequest } from "../schemas/audit-request";
import type { ApiError } from "../schemas/error";

interface PaginationParams {
  page?: number;
  size?: number;
}

export const auditKeys = {
  all: ["audits"] as const,
  lists: () => [...auditKeys.all, "list"] as const,
  list: (params?: PaginationParams) => [...auditKeys.lists(), params] as const,
  details: () => [...auditKeys.all, "detail"] as const,
  detail: (id: string) => [...auditKeys.details(), id] as const,
  shared: (token: string) => ["shared-audit", token] as const,
};


export function useAudit(
  id: string,
  options?: Omit<UseQueryOptions<AuditResponse, ApiError>, "queryKey" | "queryFn">
) {
  return useQuery({
    queryKey: auditKeys.detail(id),
    queryFn: () => getAudit(id),
    ...options,
  });
}

export function useAuditList(
  params?: PaginationParams,
  options?: Omit<UseQueryOptions<Awaited<ReturnType<typeof listAudits>>, ApiError>, "queryKey" | "queryFn">
) {
  return useQuery({
    queryKey: auditKeys.list(params),
    queryFn: () => listAudits(params),
    ...options,
  });
}

export function useSharedReport(
  token: string,
  options?: Omit<UseQueryOptions<AuditResponse, ApiError>, "queryKey" | "queryFn">
) {
  return useQuery({
    queryKey: auditKeys.shared(token),
    queryFn: () => getSharedReport(token),
    ...options,
  });
}

// --- MUTATIONS ---
export function useSubmitAudit(options?: UseMutationOptions<AuditResponse, ApiError, AuditRequest>) {
  return useMutation({ mutationFn: submitAudit, ...options });
}

export function useDeleteAudit(options?: UseMutationOptions<void, ApiError, string>) {
  return useMutation({ mutationFn: deleteAudit, ...options });
}

export function useShareAudit(options?: UseMutationOptions<AuditResponse, ApiError, string>) {
  return useMutation({ mutationFn: shareAudit, ...options });
}

export function useRevokeShareLink(options?: UseMutationOptions<void, ApiError, string>) {
  return useMutation({ mutationFn: revokeShareLink, ...options });
}

export function useRetryAudit(options?: UseMutationOptions<AuditResponse, ApiError, string>) {
  return useMutation({ mutationFn: retryAudit, ...options });
}