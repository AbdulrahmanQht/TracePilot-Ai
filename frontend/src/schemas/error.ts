import { z } from "zod";

export const ApiErrorResponseSchema = z.object({
  message: z.string(),
  status: z.number(),
  timestamp: z.string(),
});
export type ApiErrorResponse = z.infer<typeof ApiErrorResponseSchema>;

export const SpringDefaultErrorSchema = z.object({
  timestamp: z.string(),
  status: z.number(),
  error: z.string(),
  path: z.string().optional(),
});

export function parseApiError(data: unknown): { message: string; status?: number } {
  const apiErr = ApiErrorResponseSchema.safeParse(data);
  if (apiErr.success) return { message: apiErr.data.message, status: apiErr.data.status };

  const springErr = SpringDefaultErrorSchema.safeParse(data);
  if (springErr.success) return { message: springErr.data.error, status: springErr.data.status };

  return { message: "Something went wrong. Please try again." };
}

export type ApiError = ReturnType<typeof parseApiError>;