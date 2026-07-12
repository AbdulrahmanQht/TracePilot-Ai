import { z } from "zod";

export const createPageSchema = <T extends z.ZodTypeAny>(contentSchema: T) =>
  z.object({
    content: z.array(contentSchema),
    totalPages: z.number().nonnegative(),
    totalElements: z.number().nonnegative(),
    number: z.number().nonnegative(),
    size: z.number().positive(),
  });

export type Page<TSchema extends z.ZodTypeAny> = z.infer<ReturnType<typeof createPageSchema<TSchema>>>;