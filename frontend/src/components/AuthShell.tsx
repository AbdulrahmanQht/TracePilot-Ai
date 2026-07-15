import type { ReactNode } from "react";
import { Link } from "react-router";
import { Terminal } from "lucide-react";

export default function AuthShell({ children }: { children: ReactNode }) {
  return (
    <div className="flex min-h-screen flex-col bg-background font-body">

      {/* Top bar */}
      <header className="flex h-14 items-center justify-between border-b-2 border-black bg-primary px-8">
        <Link to="/" className="flex items-center gap-2.5">
          <div className="flex h-8 w-8 items-center justify-center border-2 border-black bg-secondary">
            <Terminal size={13} className="text-primary-foreground" />
          </div>

          <span className="font-display text-base tracking-[-0.02em] text-primary-foreground">
            TracePilot
            <span className="underline decoration-2">.AI</span>
          </span>
        </Link>

        <span className="font-body text-xs text-primary-foreground/50">
          Coding-Agent Trust Auditor
        </span>
      </header>

      {/* Center content */}
      <div className="flex flex-1 items-center justify-center px-4 py-12">
        {children}
      </div>

      {/* Footer */}
      <footer className="flex items-center justify-between border-t-2 border-black bg-muted px-8 py-3">
        <span className="font-mono text-[10px] text-muted-foreground">
          TracePilot.AI v3.0.0
        </span>

        <div className="flex gap-4">
          {["Privacy", "Terms", "Docs"].map((l) => (
            <a
              key={l}
              href="#"
              className="font-body text-[11px] text-muted-foreground hover:underline"
            >
              {l}
            </a>
          ))}
        </div>
      </footer>
    </div>
  );
}