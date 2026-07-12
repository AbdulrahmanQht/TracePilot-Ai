import { z } from "zod";
import { UserProfileResponseSchema } from "./user";
import { AuditStatusSchema } from "./audit";

export const AdminUserResponseSchema = UserProfileResponseSchema;
export type AdminUserResponse = z.infer<typeof AdminUserResponseSchema>;

export const AdminUserSummarySchema = z.object({
  id: z.uuid(),
  displayName: z.string().nullable(),
  email: z.email(),
});

export const AdminFlaggedAuditResponseSchema = z.object({
  id: z.uuid(),
  status: AuditStatusSchema,
  suspicious: z.boolean(),
  createdAt: z.string(),
  user: AdminUserSummarySchema,
});
export type AdminFlaggedAuditResponse = z.infer<typeof AdminFlaggedAuditResponseSchema>;