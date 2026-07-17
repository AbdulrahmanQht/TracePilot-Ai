import { z } from "zod";

const ComponentStatus = z.enum(["UP", "DOWN"]);

export const HealthResponseSchema = z.object({
  status: z.enum(["UP", "DOWN"]),
  components: z.object({
    db: ComponentStatus,
    worker: ComponentStatus,
    rabbitmq: ComponentStatus,
    tracing: ComponentStatus,
  }),
});

export type HealthResponse = z.infer<typeof HealthResponseSchema>;