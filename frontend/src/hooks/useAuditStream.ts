import { useEffect, useRef, useState } from "react";
import { apiClient, getAccessToken } from "@/api/client";
import { AuditResponseSchema, type AuditResponse } from "@/schemas/audit";
import { AuditProgressMessageSchema, type AuditProgressMessage } from "@/schemas/audit-progress";

type AgentType = AuditProgressMessage["agentType"];

interface UseAuditStreamOptions {
  enabled?: boolean;
  onFinal?: (audit: AuditResponse) => void;
}

interface UseAuditStreamResult {
  agentEvents: Partial<Record<AgentType, AuditProgressMessage>>;
  connected: boolean;
  /** true once the stream has failed and callers should fall back to polling */
  streamFailed: boolean;
}

/**
 * The backend's SSE endpoint (GET /audits/:id/stream) is authenticated via the
 * `Authorization: Bearer <token>` header, same as every other endpoint. The
 * browser's native EventSource can't set custom headers, so this reads the
 * stream manually with fetch() + a ReadableStream reader and parses the
 * `event:`/`data:` frames by hand.
 */
export function useAuditStream(
  auditId: string | undefined,
  { enabled = true, onFinal }: UseAuditStreamOptions = {}
): UseAuditStreamResult {
  const [agentEvents, setAgentEvents] = useState<Partial<Record<AgentType, AuditProgressMessage>>>({});
  const [connected, setConnected] = useState(false);
  const [streamFailed, setStreamFailed] = useState(false);
  const onFinalRef = useRef(onFinal);
  useEffect(() => {
      onFinalRef.current = onFinal;
  });

  useEffect(() => {
    if (!auditId || !enabled) return;

    const controller = new AbortController();
    let cancelled = false;

    async function connect() {
      try {
        const baseURL = apiClient.defaults.baseURL ?? "";
        const token = getAccessToken();

        const res = await fetch(`${baseURL}/audits/${auditId}/stream`, {
          method: "GET",
          headers: {
            Accept: "text/event-stream",
            ...(token ? { Authorization: `Bearer ${token}` } : {}),
          },
          credentials: "include",
          signal: controller.signal,
        });

        if (!res.ok || !res.body) {
          throw new Error(`Stream request failed with status ${res.status}`);
        }

        setConnected(true);

        const reader = res.body.getReader();
        const decoder = new TextDecoder();
        let buffer = "";

        while (!cancelled) {
          const { value, done } = await reader.read();
          if (done) break;

          buffer += decoder.decode(value, { stream: true });
          const frames = buffer.split("\n\n");
          buffer = frames.pop() ?? "";

          for (const frame of frames) {
            let eventName = "message";
            const dataLines: string[] = [];

            for (const line of frame.split("\n")) {
              if (line.startsWith("event:")) eventName = line.slice(6).trim();
              else if (line.startsWith("data:")) dataLines.push(line.slice(5).trim());
            }
            if (dataLines.length === 0) continue;

            const rawData = dataLines.join("\n");
            let json: unknown;
            try {
              json = JSON.parse(rawData);
            } catch {
              continue;
            }

            if (eventName === "progress") {
              const parsed = AuditProgressMessageSchema.safeParse(json);
              if (parsed.success) {
                setAgentEvents((prev) => ({ ...prev, [parsed.data.agentType]: parsed.data }));
              }
            } else if (eventName === "status") {
              const parsed = AuditResponseSchema.safeParse(json);
              if (parsed.success) {
                onFinalRef.current?.(parsed.data);
              }
            }
          }
        }
      } catch (err) {
        if (cancelled || (err instanceof DOMException && err.name === "AbortError")) return;
        setStreamFailed(true);
      } finally {
        if (!cancelled) setConnected(false);
      }
    }

    connect();

    return () => {
      cancelled = true;
      controller.abort();
    };
  }, [auditId, enabled]);

  return { agentEvents, connected, streamFailed };
}