import { apiClient } from "./client";
import { createPageSchema } from "../schemas/page";
import { AuditResponseSchema, type AuditResponse } from "../schemas/audit";
import { AuditRequestSchema, type AuditRequest } from "../schemas/audit-request";

const AuditPageSchema = createPageSchema(AuditResponseSchema);

export async function submitAudit(payload: AuditRequest): Promise<AuditResponse> {
  const body = AuditRequestSchema.parse(payload);
  const { data } = await apiClient.post("/audits", body);
  return AuditResponseSchema.parse(data);
}

export async function getAudit(id: string): Promise<AuditResponse> {
  const { data } = await apiClient.get(`/audits/${id}`);
  return AuditResponseSchema.parse(data);
}

export async function listAudits(params?: { page?: number; size?: number; sort?: string }) {
  const { data } = await apiClient.get("/audits", { params });
  return AuditPageSchema.parse(data);
}

export async function deleteAudit(id: string): Promise<void> {
  await apiClient.delete(`/audits/${id}`);
}

export async function shareAudit(id: string): Promise<AuditResponse> {
  const { data } = await apiClient.post(`/audits/${id}/share`);
  return AuditResponseSchema.parse(data);
}

export async function revokeShareLink(id: string): Promise<void> {
  await apiClient.delete(`/audits/${id}/share`);
}

export async function retryAudit(id: string): Promise<AuditResponse> {
  const { data } = await apiClient.post(`/audits/${id}/retry`);
  return AuditResponseSchema.parse(data);
}

export async function getSharedReport(token: string): Promise<AuditResponse> {
  const { data } = await apiClient.get(`/shared/${token}`);
  return AuditResponseSchema.parse(data);
}