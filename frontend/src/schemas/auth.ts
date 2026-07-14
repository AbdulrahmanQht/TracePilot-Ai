import { z } from "zod";

export const UserSummarySchema = z.object({
  id: z.uuid(),
  email: z.email(),
  displayName: z.string().nullable(),
  role: z.enum(["USER", "ADMIN"])
});

export type UserSummary = z.infer<typeof UserSummarySchema>;

export const AuthResponseSchema = z.object({
  accessToken: z.string(),
  tokenType: z.string(),
  expiresAt: z.string(), // ISO Timestamp string
  user: UserSummarySchema,
});
export type AuthResponse = z.infer<typeof AuthResponseSchema>;