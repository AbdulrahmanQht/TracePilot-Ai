import { useEffect, useRef, useState } from "react";
import { fetchHealth } from "../api/health";

export type SystemStatus = "checking" | "healthy" | "unhealthy";

const POLL_INTERVAL_MS = 30_000;

export function useSystemHealth(): SystemStatus {
  const [status, setStatus] = useState<SystemStatus>("checking");
  const mounted = useRef(true);

  useEffect(() => {
    mounted.current = true;

    async function check() {
      try {
        const health = await fetchHealth();
        if (!mounted.current) return;
        setStatus(health.status === "UP" ? "healthy" : "unhealthy");
      } catch {
        if (!mounted.current) return;
        setStatus("unhealthy");
      }
    }

    check();
    const interval = setInterval(check, POLL_INTERVAL_MS);

    return () => {
      mounted.current = false;
      clearInterval(interval);
    };
  }, []);

  return status;
}