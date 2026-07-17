import { useNavigate, Link } from "react-router";
import { Terminal, ArrowLeft, Home, Search } from "lucide-react";
import { useDocumentTitle } from "@/hooks/useDocumentTitle";
import { Button } from "@/components/ui/button";

const GLITCH_LINES = [
  "$ tracepilot route --resolve /???",
  "ERR  No route matched for path",
  "ERR  Loader returned null",
  "ERR  Component undefined",
  "WARN Falling back to 404 handler",
];

export default function NotFoundPage() {
  useDocumentTitle("Not Found Page");
  const navigate = useNavigate();

  return (
    <div className="min-h-screen flex flex-col bg-background" style={{ fontFamily: "var(--font-body)" }}>

      <header className="border-b-2 border-black px-8 h-14 flex items-center justify-between shrink-0 bg-primary">
        <Link to="/" className="flex items-center gap-2.5">
          <div className="w-8 h-8 border-2 border-black flex items-center justify-center bg-secondary">
            <Terminal size={13} className="text-primary-foreground" />
          </div>
          <span style={{ fontFamily: "var(--font-display)", color: "var(--primary-foreground)", fontSize: 16, letterSpacing: "-0.02em" }}>
            TracePilot<span style={{ textDecoration: "underline", textDecorationThickness: 2 }}>.AI</span>
          </span>
        </Link>
        <span style={{ fontFamily: "var(--font-mono)", fontSize: 10, color: "rgba(244,241,234,0.35)", letterSpacing: "0.1em" }}>
          HTTP 404
        </span>
      </header>

      <div className="flex-1 flex items-center justify-center px-6 py-16">
        <div className="w-full max-w-[560px] space-y-5">

          <div className="border-4 border-black bg-card" style={{ boxShadow: "7px 7px 0px #0D0D0D" }}>

            <div className="border-b-4 border-black px-7 py-6 flex items-start gap-6 bg-secondary">
              <div className="border-4 border-black px-4 py-2 shrink-0 leading-none"
                style={{ background: "#0F1A14", fontFamily: "var(--font-display)", color: "#A8D5A2", fontSize: 52, letterSpacing: "-0.05em" }}>
                404
              </div>
              <div className="pt-1">
                <h1 style={{ fontFamily: "var(--font-display)", color: "var(--secondary-foreground)", fontSize: 22, letterSpacing: "-0.02em", lineHeight: 1.2 }}>
                  Page not found
                </h1>
                <p style={{ fontFamily: "var(--font-body)", color: "rgba(244,241,234,0.65)", fontSize: 13, marginTop: 6, lineHeight: 1.6 }}>
                  The route you requested doesn&apos;t exist or has moved.
                  No agent report was harmed in the making of this error.
                </p>
              </div>
            </div>

            <div style={{ background: "#0F1A14" }}>
              <div className="border-b-2 border-black/40 px-5 py-2 flex items-center gap-2">
                {["#8B1A1A", "#B87D2F", "#A8D5A2"].map(c => (
                  <div key={c} className="w-2.5 h-2.5 border border-black/40" style={{ background: c }} />
                ))}
                <span style={{ fontFamily: "var(--font-mono)", fontSize: 9, color: "rgba(244,241,234,0.3)", marginLeft: 6, letterSpacing: "0.1em" }}>
                  tracepilot-router · stderr
                </span>
              </div>
              <div className="px-5 py-4 space-y-1.5">
                {GLITCH_LINES.map((line, i) => (
                  <div key={i} className="flex gap-3">
                    <span style={{ fontFamily: "var(--font-mono)", fontSize: 10, color: "#A8D5A2", flexShrink: 0 }}>
                      {String(i + 1).padStart(2, "0")}
                    </span>
                    <span style={{
                      fontFamily: "var(--font-mono)", fontSize: 11, lineHeight: 1.6,
                      color: line.startsWith("ERR") ? "#F8A0A0" : line.startsWith("WARN") ? "#C8A86A" : "rgba(244,241,234,0.45)",
                    }}>
                      {line}
                    </span>
                  </div>
                ))}
              </div>
            </div>

            <div className="px-7 py-5 space-y-4">
              <div style={{ fontFamily: "var(--font-display)", fontSize: 11, letterSpacing: "0.06em", color: "var(--muted-foreground)" }}>
                TRY ONE OF THESE
              </div>
              <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
                {[
                  { label: "Home",          icon: <Home size={13} />,     to: "/",             bg: "var(--primary)"   },
                  { label: "Submit Trace",  icon: <Terminal size={13} />, to: "/app/submit",   bg: "var(--secondary)" },
                  { label: "Audit History", icon: <Search size={13} />,   to: "/app/history",  bg: "var(--foreground)" },
                ].map(({ label, icon, to, bg }) => (
                  <Link key={label} to={to}
                    className="flex items-center justify-center gap-2 py-3 border-2 border-black shadow-[3px_3px_0px_#0D0D0D] hover:translate-x-[2px] hover:translate-y-[2px] hover:shadow-none transition-all"
                    style={{ fontFamily: "var(--font-display)", background: bg, color: "var(--primary-foreground)", fontSize: 12 }}>
                    {icon}{label}
                  </Link>
                ))}
              </div>
            </div>
          </div>

          <Button variant="muted" onClick={() => navigate(-1)}
            className="flex items-center gap-2">
            <ArrowLeft size={13} /> Go back
          </Button>
        </div>
      </div>

      <div className="border-t-2 border-black px-8 py-3 flex items-center justify-between shrink-0 bg-muted">
        <span style={{ fontFamily: "var(--font-mono)", fontSize: 10, color: "var(--muted-foreground)" }}>
          TracePilot
        </span>
      </div>
    </div>
  );
}
