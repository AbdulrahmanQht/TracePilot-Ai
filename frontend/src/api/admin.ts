import { apiClient } from "./client";
import { createPageSchema } from "../schemas/page";
import { AdminUserResponseSchema, AdminFlaggedAuditResponseSchema } from "../schemas/admin";

const AdminUserPageSchema = createPageSchema(AdminUserResponseSchema);
const AdminFlaggedAuditPageSchema = createPageSchema(AdminFlaggedAuditResponseSchema);

export async function getUsers(params?: { page?: number; size?: number }) {
  const { data } = await apiClient.get("/admin/users", { params });
  return AdminUserPageSchema.parse(data);
}

export async function getFlaggedAudits(params?: { page?: number; size?: number }) {
  const { data } = await apiClient.get("/admin/audits", { params });
  return AdminFlaggedAuditPageSchema.parse(data);
}