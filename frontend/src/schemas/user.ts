import { z } from "zod";

export const UserProfileResponseSchema = z.object({
  id: z.uuid(),
  displayName: z.string().nullable(),
  email: z.email(),
  role: z.enum(["USER", "ADMIN"]),
  verified: z.boolean(),
  createdAt: z.string(),
  auditCountToday: z.int().nonnegative(),
});
export type UserProfileResponse = z.infer<typeof UserProfileResponseSchema>;

export const UpdateUserRequestSchema = z.object({
  displayName: z.string().min(3).max(50),
});
export type UpdateUserRequest = z.infer<typeof UpdateUserRequestSchema>;