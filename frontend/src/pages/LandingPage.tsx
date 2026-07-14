import { useState, useEffect, useRef } from "react";
import { Link } from "react-router-dom";
import { useDocumentTitle } from "@/hooks/useDocumentTitle";
import {
  Terminal, ArrowRight, Shield, Activity, TrendingUp,
  GitBranch, Zap, CheckCircle2, XCircle, AlertTriangle,
  ChevronRight, Menu, X, Database, Server, Cpu, Radio,
  Eye, Lock, BarChart3,
} from "lucide-react";

const cx = (...classes: (string | false | undefined | null)[]) =>
  classes.filter(Boolean).join(" ");

const TICKER_TEXT =
  "TRACE LOOP EFFICIENCY ✦ BLIND OUTCOME VERIFIER ✦ RELIABILITY TREND ✦ " +
  "PROMPT ISOLATION ✦ DISTRIBUTED TRACING ✦ PYDANTIC CONTRACTS ✦ " +
  "RABBITMQ DISPATCH ✦ JWT AUTHENTICATION ✦ POSTGRESQL JSONB ✦ ";

function MiniReport() {
  const agents = [
    { label: "Trace Loop & Efficiency", score: 42, color: "var(--color-brown)" },
    { label: "Blind Outcome Verifier",  score: 18, color: "var(--color-crimson)" },
    { label: "Reliability Trend",       score: 55, color: "var(--color-forest)" },
  ];
  return (
    <div
      className={cx("border-neo", "shadow-neo")}
      style={{ background: "var(--color-card)", fontFamily: "'Archivo', sans-serif" }}
    >
      {/* header */}
      <div
        className="flex items-center justify-between px-4 py-3 border-b-2 border-black"
        style={{ background: "var(--color-ink)" }}
      >
        <span style={{ fontFamily: "var(--font-mono)", color: "var(--color-sage)", fontSize: 11, letterSpacing: "0.1em" }}>
          AUDIT REPORT · RUN_ID 8f3a2c
        </span>
        <span
          className={cx("border-neo")}
          style={{ fontFamily: "var(--font-mono)", background: "var(--color-crimson)", color: "#fff", fontSize: 10, padding: "2px 8px", letterSpacing: "0.08em" }}
        >
          CONTRADICTED
        </span>
      </div>

      {/* score */}
      <div className="px-4 py-4 border-b-2 border-black">
        <div className="flex items-end gap-3">
          <span style={{ fontFamily: "var(--font-display)", fontSize: 56, lineHeight: 1, color: "var(--color-crimson)" }}>31</span>
          <div>
            <div style={{ fontFamily: "var(--font-mono)", fontSize: 11, color: "var(--color-ink-muted)" }}>/100</div>
            <div style={{ fontFamily: "var(--font-mono)", fontSize: 10, color: "var(--color-ink-muted)" }}>TRUST SCORE</div>
          </div>
        </div>
      </div>

      {/* agent scores */}
      <div className="px-4 py-4 flex flex-col gap-3 border-b-2 border-black">
        {agents.map((a) => (
          <div key={a.label}>
            <div className="flex justify-between mb-1">
              <span style={{ fontFamily: "var(--font-mono)", fontSize: 10, color: "var(--color-ink)" }}>{a.label}</span>
              <span style={{ fontFamily: "var(--font-mono)", fontSize: 10, color: a.color, fontWeight: 700 }}>{a.score}</span>
            </div>
            <div className={cx("border-neo")} style={{ height: 10, background: "var(--color-muted)", position: "relative" }}>
              <div
                style={{
                  position: "absolute", top: 0, left: 0,
                  height: "100%", width: `${a.score}%`,
                  background: a.color,
                }}
              />
            </div>
          </div>
        ))}
      </div>

      {/* verdict legend */}
      <div className="px-4 py-3 grid grid-cols-2 gap-2">
        {[
          { v: "CONTRADICTED", color: "var(--color-crimson)" },
          { v: "UNVERIFIED",   color: "#7A4A28" },
          { v: "LIKELY_COMPLETE", color: "var(--color-forest)" },
          { v: "LIKELY_INCOMPLETE", color: "var(--color-brown)" },
        ].map((item) => (
          <div key={item.v} className="flex items-center gap-2">
            <div style={{ width: 10, height: 10, background: item.color, border: "1.5px solid black", flexShrink: 0 }} />
            <span style={{ fontFamily: "var(--font-mono)", fontSize: 9, color: "var(--color-ink-muted)", letterSpacing: "0.05em" }}>{item.v}</span>
          </div>
        ))}
      </div>
    </div>
  );
}

export default function LandingPage() {
  useDocumentTitle("Landing Page");
  const [scrolled, setScrolled] = useState(false);
  const [menuOpen, setMenuOpen] = useState(false);
  const tickerRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const onScroll = () => setScrolled(window.scrollY > 60);
    window.addEventListener("scroll", onScroll, { passive: true });
    return () => window.removeEventListener("scroll", onScroll);
  }, []);

  return (
    <div style={{ fontFamily: "var(--font-body)", background: "var(--color-parchment)", minHeight: "100vh" }}>

      {/* ── global keyframes ── */}
      <style>{`
        @keyframes ticker {
          from { transform: translateX(0); }
          to   { transform: translateX(-50%); }
        }
        @keyframes rawPulse {
          0%, 100% { opacity: 1; }
          50%       { opacity: 0.3; }
        }
        .ticker-inner { animation: ticker 28s linear infinite; }
        .live-dot     { animation: rawPulse 1.2s ease-in-out infinite; }
        .nb-btn {
          display: inline-flex; align-items: center; gap: 8px;
          border: 2px solid black; padding: 10px 22px;
          font-family: 'Archivo Black', sans-serif; font-size: 14px;
          cursor: pointer; transition: transform 0.1s, box-shadow 0.1s;
          box-shadow: 3px 3px 0px #0D0D0D; text-decoration: none;
          white-space: nowrap;
        }
        .nb-btn:hover {
          transform: translate(2px, 2px);
          box-shadow: none;
        }
        .nb-card {
          border: 2px solid black;
          box-shadow: 4px 4px 0px #0D0D0D;
        }
      `}</style>

      {/* NAV*/}
      <nav
        className="fixed top-0 left-0 right-0 z-50 transition-colors duration-300"
        style={{
          background: scrolled ? "var(--color-forest)" : "var(--color-parchment)",
          borderBottom: `2px solid ${"var(--color-ink)"}`,
        }}
      >
        <div className="max-w-7xl mx-auto px-4 sm:px-6 flex items-center justify-between h-16">
          {/* logo */}
          <Link to="/" className="flex items-center gap-2 no-underline">
            <div
              className={cx("border-neo")}
              style={{ background: "var(--color-brown)", padding: "6px 8px", flexShrink: 0 }}
            >
              <Terminal size={18} color={"var(--color-sage)"} />
            </div>
            <span
              style={{
                fontFamily: "var(--font-display)",
                fontSize: 16,
                color: scrolled ? "var(--color-parchment)" : "var(--color-ink)",
                letterSpacing: "0.02em",
              }}
            >
              TracePilot
            </span>
          </Link>

          {/* desktop links */}
          <div className="hidden md:flex items-center gap-6">
            {["How it works", "Agents", "Architecture", "Docs"].map((item) => (
              <a
                key={item}
                href={`#${item.toLowerCase().replace(/\s+/g, "-")}`}
                style={{
                  fontFamily: "var(--font-body)",
                  fontSize: 14,
                  color: scrolled ? "var(--color-sage)" : "var(--color-ink)",
                  textDecoration: "none",
                  fontWeight: 600,
                  transition: "opacity 0.15s",
                }}
                onMouseEnter={(e) => (e.currentTarget.style.opacity = "0.7")}
                onMouseLeave={(e) => (e.currentTarget.style.opacity = "1")}
              >
                {item}
              </a>
            ))}
          </div>

          {/* desktop ctas */}
          <div className="hidden md:flex items-center gap-3">
            <Link
              to="/login"
              style={{
                fontFamily: "var(--font-body)",
                fontSize: 14, fontWeight: 600,
                color: scrolled ? "var(--color-sage)" : "var(--color-ink)",
                textDecoration: "none",
              }}
            >
              Sign In
            </Link>
            <Link
              to="/app/submit"
              className="nb-btn"
              style={{ background: "var(--color-brown)", color: "var(--color-parchment)" }}
            >
              Start Auditing
              <ArrowRight size={14} />
            </Link>
          </div>

          {/* mobile hamburger */}
          <button
            className="md:hidden p-2"
            style={{ background: "transparent", border: "none", cursor: "pointer" }}
            onClick={() => setMenuOpen(!menuOpen)}
            aria-label="Toggle menu"
          >
            {menuOpen
              ? <X size={24} color={scrolled ? "var(--color-parchment)" : "var(--color-ink)"} />
              : <Menu size={24} color={scrolled ? "var(--color-parchment)" : "var(--color-ink)"} />}
          </button>
        </div>

        {/* mobile dropdown */}
        {menuOpen && (
          <div
            className="md:hidden border-t-2 border-black"
            style={{ background: scrolled ? "var(--color-forest)" : "var(--color-parchment)" }}
          >
            {["How it works", "Agents", "Architecture", "Docs"].map((item) => (
              <a
                key={item}
                href={`#${item.toLowerCase().replace(/\s+/g, "-")}`}
                className="block px-6 py-3 border-b border-black"
                style={{
                  fontFamily: "var(--font-body)", fontSize: 14, fontWeight: 600,
                  color: scrolled ? "var(--color-parchment)" : "var(--color-ink)",
                  textDecoration: "none",
                }}
                onClick={() => setMenuOpen(false)}
              >
                {item}
              </a>
            ))}
            <div className="px-6 py-4 flex flex-col gap-3">
              <Link to="/login" style={{ color: scrolled ? "var(--color-sage)" : "var(--color-ink)", fontWeight: 600, textDecoration: "none" }}>
                Sign In
              </Link>
              <Link
                to="/app/submit"
                className="nb-btn"
                style={{ background: "var(--color-brown)", color: "var(--color-parchment)" }}
                onClick={() => setMenuOpen(false)}
              >
                Start Auditing <ArrowRight size={14} />
              </Link>
            </div>
          </div>
        )}
      </nav>

      {/* HERO*/}
      <section
        id="hero"
        style={{ background: "var(--color-forest)", paddingTop: 112, paddingBottom: 80 }}
      >
        <div className="max-w-7xl mx-auto px-4 sm:px-6 grid md:grid-cols-2 gap-12 items-center">
          {/* left */}
          <div>
            {/* badge */}
            <div
              className={cx("border-neo", "inline-flex items-center gap-2 mb-6")}
              style={{ background: "var(--color-brown)", padding: "6px 14px" }}
            >
              <span
                className="live-dot"
                style={{ width: 8, height: 8, borderRadius: "50%", background: "var(--color-sage)", display: "inline-block" }}
              />
              <span style={{ fontFamily: "var(--font-mono)", color: "var(--color-parchment)", fontSize: 12, letterSpacing: "0.1em" }}>
                v3.0.0 · OPEN BETA
              </span>
            </div>

            {/* headline */}
            <h1
              style={{
                fontFamily: "var(--font-display)",
                fontSize: "clamp(44px, 6vw, 72px)",
                lineHeight: 1.05,
                color: "var(--color-parchment)",
                marginBottom: 24,
              }}
            >
              Stop trusting.
              <br />Start
              <br />
              <span style={{ position: "relative", display: "inline-block" }}>
                verifying.
                <svg
                  aria-hidden="true"
                  style={{ position: "absolute", bottom: -4, left: 0, width: "100%", height: 8, overflow: "visible" }}
                  viewBox="0 0 200 8" preserveAspectRatio="none"
                >
                  <path d="M0 6 Q50 0 100 6 Q150 12 200 6" stroke={"var(--color-sage)"} strokeWidth="3.5" fill="none" strokeLinecap="round" />
                </svg>
              </span>
            </h1>

            <p style={{ fontFamily: "var(--font-body)", color: "var(--color-sage)", fontSize: 17, lineHeight: 1.65, maxWidth: 480, marginBottom: 32 }}>
              TracePilot AI runs three independent agents over your AI agent&apos;s execution
              trace — detecting hallucinations, outcome contradictions, and reliability
              regressions before they reach production.
            </p>

            {/* CTAs */}
            <div className="flex flex-wrap gap-4 mb-12">
              <Link
                to="/app/submit"
                className="nb-btn"
                style={{ background: "var(--color-sage)", color: "var(--color-ink)" }}
              >
                Submit Your First Trace
                <ArrowRight size={16} />
              </Link>
              <a
                href="https://github.com/AbdulrahmanQht/TracePilot-Ai"
                target="_blank"
                rel="noopener noreferrer"
                className="nb-btn"
                style={{ background: "transparent", color: "var(--color-parchment)", borderColor: "var(--color-parchment)" }}
              >
                <GitBranch size={16} />
                View on GitHub
              </a>
            </div>

            {/* stats 2x2 */}
            <div className="grid grid-cols-2 gap-3" style={{ maxWidth: 400 }}>
              {[
                { value: "3", label: "Independent Agents" },
                { value: "100%", label: "Blind Verifier" },
                { value: "SHA-256", label: "Trace Hashing" },
                { value: "Free", label: "No API Key Needed" },
              ].map((s) => (
                <div
                  key={s.label}
                  className={cx("border-neo")}
                  style={{ background: "var(--color-forest-dark)", padding: "14px 16px" }}
                >
                  <div style={{ fontFamily: "var(--font-display)", fontSize: 26, color: "var(--color-sage)", lineHeight: 1 }}>{s.value}</div>
                  <div style={{ fontFamily: "var(--font-mono)", fontSize: 10, color: "var(--color-parchment)", marginTop: 4, letterSpacing: "0.08em" }}>
                    {s.label.toUpperCase()}
                  </div>
                </div>
              ))}
            </div>
          </div>

          {/* right – mini report */}
          <div className="flex justify-center md:justify-end">
            <div style={{ width: "100%", maxWidth: 380 }}>
              <MiniReport />
            </div>
          </div>
        </div>
      </section>

      {/* TICKER*/}
      <div
        style={{ background: "var(--color-ink)", borderTop: `2px solid black`, borderBottom: `2px solid black`, overflow: "hidden", padding: "12px 0" }}
        ref={tickerRef}
      >
        <div className="ticker-inner" style={{ display: "flex", whiteSpace: "nowrap", width: "max-content" }}>
          {[...Array(2)].map((_, i) => (
            <span
              key={i}
              style={{ fontFamily: "var(--font-mono)", color: "var(--color-sage)", fontSize: 13, letterSpacing: "0.12em", paddingRight: 40 }}
            >
              {TICKER_TEXT}
            </span>
          ))}
        </div>
      </div>

      {/* PROBLEM */}
      <section id="how-it-works" style={{ background: "var(--color-card)", padding: "96px 0" }}>
        <div className="max-w-7xl mx-auto px-4 sm:px-6 grid md:grid-cols-2 gap-12 items-start">
          {/* left */}
          <div>
            <div style={{ fontFamily: "var(--font-mono)", fontSize: 11, color: "var(--color-brown)", letterSpacing: "0.15em", marginBottom: 16 }}>
              THE PROBLEM
            </div>
            <h2 style={{ fontFamily: "var(--font-display)", fontSize: "clamp(28px, 4vw, 42px)", color: "var(--color-ink)", lineHeight: 1.15, marginBottom: 24 }}>
              Can you trust what your AI agent just did?
            </h2>
            <p style={{ fontFamily: "var(--font-body)", color: "var(--color-ink-muted)", fontSize: 16, lineHeight: 1.7, marginBottom: 16 }}>
              Modern AI agents operate across dozens of tool calls, nested reasoning steps, and opaque
              model outputs. When something goes wrong — or appears to go right but didn&apos;t — you
              have no systematic way to know.
            </p>
            <p style={{ fontFamily: "var(--font-body)", color: "var(--color-ink-muted)", fontSize: 16, lineHeight: 1.7, marginBottom: 32 }}>
              You can read the logs. You can squint at the trace. But without structured, independent
              verification, you&apos;re flying blind every time your agent runs.
            </p>

            <div className="flex flex-col gap-3">
              {[
                { icon: <XCircle size={16} color={"var(--color-crimson)"} />, text: "Agent claims success but the task silently failed" },
                { icon: <XCircle size={16} color={"var(--color-crimson)"} />, text: "Hallucinated tool outputs accepted without verification" },
                { icon: <AlertTriangle size={16} color="#7A4A28" />, text: "Reliability degrading across runs with no alert" },
                { icon: <AlertTriangle size={16} color="#7A4A28" />, text: "No audit trail for compliance or debugging" },
              ].map((row, i) => (
                <div
                  key={i}
                  className={cx("border-neo")}
                  style={{ background: "var(--color-parchment)", padding: "12px 16px", display: "flex", alignItems: "center", gap: 12 }}
                >
                  {row.icon}
                  <span style={{ fontFamily: "var(--font-body)", fontSize: 14, color: "var(--color-ink)" }}>{row.text}</span>
                </div>
              ))}
            </div>
          </div>

          {/* right – terminal card */}
          <div className={cx("border-neo", "shadow-neo")}>
            {/* header */}
            <div
              style={{ background: "var(--color-ink)", padding: "10px 16px", display: "flex", alignItems: "center", gap: 8, borderBottom: "2px solid black" }}
            >
              {["#FF5F56", "#FFBD2E", "#27C93F"].map((c) => (
                <div key={c} style={{ width: 12, height: 12, borderRadius: "50%", background: c }} />
              ))}
              <span style={{ fontFamily: "var(--font-mono)", color: "var(--color-sage)", fontSize: 11, marginLeft: 8 }}>agent_trace.log</span>
            </div>

            {/* code body */}
            <div style={{ background: "#0D1210", padding: "20px 20px", fontFamily: "'JetBrains Mono', monospace", fontSize: 12, lineHeight: 1.65 }}>
              <div style={{ color: "var(--color-sage)" }}>&gt; agent.run("run_test_suite")</div>
              <div style={{ color: "var(--color-amber-code)", marginTop: 8 }}>
                <span style={{ color: "var(--color-mint-code)" }}>tool_call:</span> bash("pytest tests/ -q")
              </div>
              <div style={{ color: "#888", marginLeft: 16 }}>stdout: "collecting ..."</div>
              <div style={{ color: "#888", marginLeft: 16 }}>stdout: "1 passed in 0.3s"</div>
              <div style={{ color: "var(--color-amber-code)", marginTop: 8 }}>
                <span style={{ color: "var(--color-mint-code)" }}>agent_output:</span>
              </div>
              <div style={{ color: "var(--color-parchment)", marginLeft: 16 }}>
                "All tests passed. The implementation
              </div>
              <div style={{ color: "var(--color-parchment)", marginLeft: 16 }}>
                &nbsp;is correct and ready to deploy."
              </div>
              <div style={{ marginTop: 12, borderTop: "1px dashed #333", paddingTop: 12, color: "var(--color-crimson)" }}>
                # ACTUAL pytest output (hidden in trace):
              </div>
              <div style={{ color: "#e57373", marginLeft: 16 }}>FAILED tests/test_core.py::test_main</div>
              <div style={{ color: "#e57373", marginLeft: 16 }}>FAILED tests/test_auth.py::test_login</div>
              <div style={{ color: "#e57373", marginLeft: 16 }}>2 failed, 1 passed in 0.8s</div>
            </div>

            {/* footer */}
            <div
              style={{ background: "var(--color-crimson)", padding: "10px 16px", borderTop: "2px solid black", display: "flex", alignItems: "center", gap: 8 }}
            >
              <XCircle size={14} color="#fff" />
              <span style={{ fontFamily: "var(--font-mono)", color: "#fff", fontSize: 11, letterSpacing: "0.1em" }}>
                VERDICT: CONTRADICTED — Output contradicts tool evidence
              </span>
            </div>
          </div>
        </div>
      </section>

      {/*  HOW IT WORKS */}
      <section id="agents" style={{ background: "var(--color-parchment)", padding: "96px 0" }}>
        <div className="max-w-7xl mx-auto px-4 sm:px-6">
          {/* header row */}
          <div className="flex flex-col md:flex-row md:items-end md:justify-between gap-6 mb-12">
            <div>
              <div style={{ fontFamily: "var(--font-mono)", fontSize: 11, color: "var(--color-brown)", letterSpacing: "0.15em", marginBottom: 12 }}>
                HOW IT WORKS
              </div>
              <h2 style={{ fontFamily: "var(--font-display)", fontSize: "clamp(28px, 4vw, 44px)", color: "var(--color-ink)", lineHeight: 1.1 }}>
                Three steps to a trust report.
              </h2>
            </div>
            <Link
              to="/app/submit"
              className="nb-btn"
              style={{ background: "var(--color-forest)", color: "var(--color-parchment)", alignSelf: "flex-start" }}
            >
              Try It Now <ArrowRight size={15} />
            </Link>
          </div>

          {/* steps joined panel */}
          <div className={cx("border-neo", "shadow-neo")} style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(260px, 1fr))" }}>
            {[
              {
                num: "01", title: "Paste your trace", bg: "var(--color-brown)", textColor: "var(--color-parchment)",
                icon: <Terminal size={28} />,
                desc: "Copy your agent's execution trace — LangChain, CrewAI, raw JSON, or plain log output — and paste it into the submission form.",
              },
              {
                num: "02", title: "Three agents audit in parallel", bg: "var(--color-parchment)", textColor: "var(--color-ink)",
                icon: <Activity size={28} />,
                desc: "Three specialized AI agents independently analyse your trace from different angles: efficiency, outcome verification, and reliability trend.",
              },
              {
                num: "03", title: "Get a structured trust report", bg: "var(--color-forest)", textColor: "var(--color-parchment)",
                icon: <CheckCircle2 size={28} />,
                desc: "Within seconds you receive a scored trust report with verdicts, per-agent findings, and a SHA-256 hash for audit trail purposes.",
              },
            ].map((step, i, arr) => (
              <div
                key={step.num}
                style={{
                  background: step.bg,
                  padding: "36px 32px",
                  borderRight: i < arr.length - 1 ? "2px solid black" : "none",
                  position: "relative",
                }}
              >
                {/* arrow between steps */}
                {i < arr.length - 1 && (
                  <div
                    style={{
                      position: "absolute", right: -18, top: "50%", transform: "translateY(-50%)",
                      zIndex: 2, background: "var(--color-muted)", border: "2px solid black",
                      width: 34, height: 34, display: "flex", alignItems: "center", justifyContent: "center",
                    }}
                  >
                    <ChevronRight size={16} color={"var(--color-ink)"} />
                  </div>
                )}
                <div style={{ color: step.textColor, marginBottom: 16 }}>{step.icon}</div>
                <div style={{ fontFamily: "var(--font-mono)", fontSize: 11, color: step.textColor === "var(--color-ink)" ? "var(--color-ink-muted)" : "var(--color-sage)", letterSpacing: "0.12em", marginBottom: 8 }}>
                  STEP {step.num}
                </div>
                <h3 style={{ fontFamily: "var(--font-display)", fontSize: 20, color: step.textColor, marginBottom: 14, lineHeight: 1.2 }}>
                  {step.title}
                </h3>
                <p style={{ fontFamily: "var(--font-body)", fontSize: 14, color: step.textColor === "var(--color-ink)" ? "var(--color-ink-muted)" : step.textColor, lineHeight: 1.65, opacity: 0.9 }}>
                  {step.desc}
                </p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* AGENTS */}
      <section id="architecture" style={{ background: "var(--color-muted)", padding: "96px 0" }}>
        <div className="max-w-7xl mx-auto px-4 sm:px-6">
          <div style={{ fontFamily: "var(--font-mono)", fontSize: 11, color: "var(--color-brown)", letterSpacing: "0.15em", marginBottom: 12 }}>
            THE THREE AGENTS
          </div>
          <h2 style={{ fontFamily: "var(--font-display)", fontSize: "clamp(28px, 4vw, 44px)", color: "var(--color-ink)", lineHeight: 1.1, marginBottom: 48 }}>
            Each agent sees exactly what it should.
          </h2>

          {/* 3 agent cards */}
          <div className="grid md:grid-cols-3 gap-6 mb-10">
            {[
              {
                title: "Trace Loop & Efficiency Agent",
                headerBg: "var(--color-brown)", icon: <Activity size={22} color={"var(--color-parchment)"} />,
                bullets: [
                  "Detects infinite loops and circular reasoning",
                  "Measures tool-call efficiency ratios",
                  "Flags redundant or wasteful steps",
                  "Scores reasoning chain coherence",
                ],
              },
              {
                title: "Blind Outcome Verifier",
                headerBg: "var(--color-crimson)", icon: <Eye size={22} color="#fff" />,
                bullets: [
                  "Receives ONLY the final claimed output",
                  "Never sees the reasoning trace",
                  "Independently evaluates claim validity",
                  "Detects hallucinated or fabricated results",
                ],
              },
              {
                title: "Reliability Trend Agent",
                headerBg: "var(--color-forest)", icon: <TrendingUp size={22} color={"var(--color-sage)"} />,
                bullets: [
                  "Compares run against historical baselines",
                  "Tracks score degradation over time",
                  "Surfaces regression patterns early",
                  "Provides trend-based trust confidence",
                ],
              },
            ].map((card) => (
              <div key={card.title} className={cx("border-neo", "shadow-neo")} style={{ background: "var(--color-card)" }}>
                <div
                  style={{
                    background: card.headerBg, padding: "16px 20px",
                    display: "flex", alignItems: "center", gap: 12,
                    borderBottom: "2px solid black",
                  }}
                >
                  {card.icon}
                  <span style={{ fontFamily: "var(--font-display)", fontSize: 15, color: "#fff", lineHeight: 1.2 }}>
                    {card.title}
                  </span>
                </div>
                <div className="p-5 flex flex-col gap-3">
                  {card.bullets.map((b) => (
                    <div key={b} className="flex items-start gap-3">
                      <CheckCircle2 size={14} color={"var(--color-forest)"} style={{ marginTop: 2, flexShrink: 0 }} />
                      <span style={{ fontFamily: "var(--font-body)", fontSize: 13, color: "var(--color-ink-muted)", lineHeight: 1.5 }}>{b}</span>
                    </div>
                  ))}
                </div>
              </div>
            ))}
          </div>

          {/* isolation callout */}
          <div className={cx("border-neo", "shadow-neo")}>
            <div
              style={{ background: "var(--color-ink)", padding: "12px 20px", borderBottom: "2px solid black", display: "flex", alignItems: "center", gap: 10 }}
            >
              <Lock size={14} color={"var(--color-sage)"} />
              <span style={{ fontFamily: "var(--font-mono)", color: "var(--color-sage)", fontSize: 11, letterSpacing: "0.1em" }}>
                BLIND VERIFIER ISOLATION — ai-worker/app/trace_extractor.py
              </span>
            </div>
            <div style={{ background: "var(--color-forest-code)", padding: "24px 28px", fontFamily: "'JetBrains Mono', monospace", fontSize: 12, lineHeight: 1.75 }}>
              <div style={{ color: "#888" }}># One parse splits the trace into two groups —</div>
              <div style={{ color: "#888" }}># the verifier only ever sees "evidence".</div>
              <br />
              <div>
                <span style={{ color: "var(--color-sage)" }}>class</span>
                <span style={{ color: "var(--color-amber-code)" }}> ExtractedTraceEvidence</span>
                <span style={{ color: "var(--color-parchment)" }}>(BaseModel):</span>
              </div>
              <div style={{ marginLeft: 24 }}>
                <span style={{ color: "var(--color-leaf-code)" }}>commands_run</span>
                <span style={{ color: "var(--color-parchment)" }}>: list[str]</span>
              </div>
              <div style={{ marginLeft: 24 }}>
                <span style={{ color: "var(--color-leaf-code)" }}>test_outputs</span>
                <span style={{ color: "var(--color-parchment)" }}>: list[str]</span>
              </div>
              <div style={{ marginLeft: 24 }}>
                <span style={{ color: "var(--color-leaf-code)" }}>final_state_signals</span>
                <span style={{ color: "var(--color-parchment)" }}>: list[str]</span>
                <span style={{ color: "#888" }}>  # observable only</span>
              </div>
              <br />
              <div>
                <span style={{ color: "var(--color-sage)" }}>class</span>
                <span style={{ color: "var(--color-amber-code)" }}> WithheldAgentClaims</span>
                <span style={{ color: "var(--color-parchment)" }}>(BaseModel):</span>
              </div>
              <div style={{ marginLeft: 24 }}>
                <span style={{ color: "var(--color-leaf-code)" }}>final_claims</span>
                <span style={{ color: "var(--color-parchment)" }}>: list[str]</span>
              </div>
              <div style={{ marginLeft: 24 }}>
                <span style={{ color: "var(--color-leaf-code)" }}>self_rationale</span>
                <span style={{ color: "var(--color-parchment)" }}>: list[str]</span>
              </div>
              <div style={{ marginLeft: 24 }}>
                <span style={{ color: "var(--color-leaf-code)" }}>completion_assertions</span>
                <span style={{ color: "var(--color-parchment)" }}>: list[str]</span>
                <span style={{ color: "var(--color-crimson)" }}>  # never sent to verifier</span>
              </div>
              <br />
              <div>
                <span style={{ color: "var(--color-sage)" }}>def</span>
                <span style={{ color: "var(--color-amber-code)" }}> _build_blind_task</span>
                <span style={{ color: "var(--color-parchment)" }}>(extracted_evidence: str) -&gt; Task:</span>
              </div>
              <div style={{ marginLeft: 24, color: "#888" }}>
                # ^ signature has no raw_trace, no claims param —
              </div>
              <div style={{ marginLeft: 24, color: "#888" }}>
                # this function physically cannot reference them.
              </div>
              <div style={{ marginLeft: 24 }}>
                <span style={{ color: "var(--color-sage)" }}>return</span>
                <span style={{ color: "var(--color-parchment)" }}> Task(</span>
              </div>
              <div style={{ marginLeft: 48 }}>
                <span style={{ color: "var(--color-leaf-code)" }}>agent</span>
                <span style={{ color: "var(--color-parchment)" }}>=blind_outcome_agent,</span>
              </div>
              <div style={{ marginLeft: 48 }}>
                <span style={{ color: "var(--color-leaf-code)" }}>description</span>
                <span style={{ color: "var(--color-parchment)" }}>=f</span>
                <span style={{ color: "var(--color-mint-code)" }}>"""</span>
              </div>
              <div style={{ marginLeft: 72, color: "#888" }}>
                You have NOT been given the agent's
              </div>
              <div style={{ marginLeft: 72, color: "#888" }}>
                final claims or self-rationale.
              </div>
              <div style={{ marginLeft: 72 }}>
                <span style={{ color: "var(--color-crimson)" }}>{"{extracted_evidence}"}</span>
                <span style={{ color: "#888" }}>  # evidence only</span>
              </div>
              <div style={{ marginLeft: 24 }}>
                <span style={{ color: "var(--color-parchment)" }}>)</span>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* ARCHITECTURE */}
      <section style={{ background: "var(--color-card)", padding: "96px 0" }}>
        <div className="max-w-7xl mx-auto px-4 sm:px-6">
          <div style={{ fontFamily: "var(--font-mono)", fontSize: 11, color: "var(--color-brown)", letterSpacing: "0.15em", marginBottom: 12 }}>
            ARCHITECTURE
          </div>
          <h2 style={{ fontFamily: "var(--font-display)", fontSize: "clamp(28px, 4vw, 44px)", color: "var(--color-ink)", lineHeight: 1.1, marginBottom: 48 }}>
            Polyglot. Decoupled. Observable.
          </h2>

          {/* stack tiles */}
          <div className="grid grid-cols-2 md:grid-cols-3 gap-4 mb-10">
            {[
              { name: "Spring Boot", sub: "API Gateway + Auth", icon: <Server size={20} />, color: "var(--color-forest)" },
              { name: "FastAPI + CrewAI", sub: "Python Agent Workers", icon: <Cpu size={20} />, color: "var(--color-brown)" },
              { name: "React + TypeScript", sub: "Frontend Interface", icon: <Zap size={20} />, color: "var(--color-crimson)" },
              { name: "PostgreSQL", sub: "JSONB Trace Storage", icon: <Database size={20} />, color: "#2C5282" },
              { name: "RabbitMQ", sub: "Message Broker", icon: <Radio size={20} />, color: "#7B3F00" },
              { name: "OTel + Jaeger", sub: "Distributed Tracing", icon: <BarChart3 size={20} />, color: "#5B21B6" },
            ].map((tile) => (
              <div
                key={tile.name}
                className={cx("border-neo")}
                style={{ background: "var(--color-parchment)", padding: "20px", boxShadow: "4px 4px 0px #0D0D0D" }}
              >
                <div
                  style={{
                    background: tile.color, border: "2px solid black",
                    width: 40, height: 40,
                    display: "flex", alignItems: "center", justifyContent: "center",
                    color: "#fff", marginBottom: 12,
                  }}
                >
                  {tile.icon}
                </div>
                <div style={{ fontFamily: "var(--font-display)", fontSize: 15, color: "var(--color-ink)", lineHeight: 1.2, marginBottom: 4 }}>{tile.name}</div>
                <div style={{ fontFamily: "var(--font-mono)", fontSize: 10, color: "var(--color-ink-muted)", letterSpacing: "0.07em" }}>{tile.sub}</div>
              </div>
            ))}
          </div>

          {/* architecture diagram */}
          <div className={cx("border-neo", "shadow-neo")}>
            <div style={{ background: "var(--color-ink)", padding: "12px 20px", borderBottom: "2px solid black" }}>
              <span style={{ fontFamily: "var(--font-mono)", color: "var(--color-sage)", fontSize: 11, letterSpacing: "0.1em" }}>
                SYSTEM FLOW DIAGRAM
              </span>
            </div>
            <div style={{ background: "var(--color-forest-code)", padding: "28px 32px", fontFamily: "'JetBrains Mono', monospace", fontSize: 12, lineHeight: 2 }}>
              <div style={{ color: "var(--color-parchment)" }}>
                <span style={{ color: "var(--color-amber-code)" }}>React UI</span>
                <span style={{ color: "var(--color-sage)" }}> ──POST /api/trace──▶ </span>
                <span style={{ color: "var(--color-amber-code)" }}>Spring Boot Gateway</span>
                <span style={{ color: "var(--color-sage)" }}> ──JWT verify──▶ </span>
                <span style={{ color: "var(--color-leaf-code)" }}>PostgreSQL</span>
              </div>
              <div style={{ color: "#555", marginLeft: 180 }}>│</div>
              <div style={{ color: "var(--color-parchment)" }}>
                <span style={{ color: "#555", marginRight: 8 }}>{"                           "}</span>
                <span style={{ color: "var(--color-sage)" }}>└── publish ──▶ </span>
                <span style={{ color: "var(--color-amber-code)" }}>RabbitMQ</span>
                <span style={{ color: "var(--color-sage)" }}> ──dispatch──▶ </span>
                <span style={{ color: "var(--color-amber-code)" }}>Python Workers</span>
              </div>
              <div style={{ color: "#555", marginLeft: 340 }}>│</div>
              <div style={{ color: "var(--color-parchment)" }}>
                <span style={{ color: "#555" }}>{"                                                 "}</span>
                <span style={{ color: "var(--color-sage)" }}>├── </span>
                <span style={{ color: "var(--color-leaf-code)" }}>TraceLoopAgent</span>
                <span style={{ color: "#888" }}> (CrewAI)</span>
              </div>
              <div style={{ color: "var(--color-parchment)" }}>
                <span style={{ color: "#555" }}>{"                                                 "}</span>
                <span style={{ color: "var(--color-sage)" }}>├── </span>
                <span style={{ color: "var(--color-leaf-code)" }}>BlindVerifierAgent</span>
                <span style={{ color: "#888" }}> (CrewAI)</span>
              </div>
              <div style={{ color: "var(--color-parchment)" }}>
                <span style={{ color: "#555" }}>{"                                                 "}</span>
                <span style={{ color: "var(--color-sage)" }}>└── </span>
                <span style={{ color: "var(--color-leaf-code)" }}>ReliabilityAgent</span>
                <span style={{ color: "#888" }}> (CrewAI)</span>
              </div>
              <div style={{ color: "#555" }}>{"                                                           │"}</div>
              <div style={{ color: "var(--color-parchment)" }}>
                <span style={{ color: "#555" }}>{"                                                 "}</span>
                <span style={{ color: "var(--color-sage)" }}>└── results ──▶ </span>
                <span style={{ color: "var(--color-amber-code)" }}>Spring Boot</span>
                <span style={{ color: "var(--color-sage)" }}> ──▶ </span>
                <span style={{ color: "var(--color-leaf-code)" }}>PostgreSQL</span>
                <span style={{ color: "var(--color-sage)" }}> ──▶ </span>
                <span style={{ color: "var(--color-amber-code)" }}>React UI</span>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* CTA SECTION */}
      <section style={{ background: "var(--color-forest)", padding: "96px 0" }}>
        <div className="max-w-3xl mx-auto px-4 sm:px-6 text-center">
          <div
            className={cx("border-neo", "inline-block mb-6")}
            style={{ background: "var(--color-brown)", padding: "6px 16px" }}
          >
            <span style={{ fontFamily: "var(--font-mono)", color: "var(--color-parchment)", fontSize: 11, letterSpacing: "0.15em" }}>
              FREE TO USE · NO SETUP REQUIRED
            </span>
          </div>

          <h2 style={{ fontFamily: "var(--font-display)", fontSize: "clamp(32px, 5vw, 52px)", color: "var(--color-parchment)", lineHeight: 1.1, marginBottom: 20 }}>
            Ready to audit your agents?
          </h2>

          <p style={{ fontFamily: "var(--font-body)", color: "var(--color-sage)", fontSize: 17, lineHeight: 1.7, marginBottom: 40, maxWidth: 520, margin: "0 auto 40px" }}>
            Paste a trace, get a trust report in seconds. No API key, no infrastructure,
            no billing page. Just open beta access while we build together.
          </p>

          <div className="flex flex-wrap justify-center gap-4">
            <Link
              to="/app/submit"
              className="nb-btn"
              style={{ background: "var(--color-sage)", color: "var(--color-ink)", fontSize: 15, padding: "13px 28px" }}
            >
              Submit Your First Trace
              <ArrowRight size={17} />
            </Link>
            <Link
              to="/login"
              className="nb-btn"
              style={{ background: "transparent", color: "var(--color-parchment)", borderColor: "var(--color-parchment)", fontSize: 15, padding: "13px 28px" }}
            >
              Sign In
              <Shield size={17} />
            </Link>
          </div>
        </div>
      </section>

      {/* FOOTER */}
      <footer style={{ background: "var(--color-brown)", borderTop: "2px solid black", padding: "64px 0 32px" }}>
        <div className="max-w-7xl mx-auto px-4 sm:px-6">
          <div className="grid grid-cols-2 md:grid-cols-4 gap-8 mb-12">
            {/* brand */}
            <div className="col-span-2 md:col-span-1">
              <div className="flex items-center gap-2 mb-4">
                <div className={cx("border-neo")} style={{ background: "var(--color-ink)", padding: "6px 8px" }}>
                  <Terminal size={16} color={"var(--color-sage)"} />
                </div>
                <span style={{ fontFamily: "var(--font-display)", fontSize: 15, color: "var(--color-parchment)" }}>TracePilot</span>
              </div>
              <p style={{ fontFamily: "var(--font-body)", color: "var(--color-sage)", fontSize: 13, lineHeight: 1.65, maxWidth: 220 }}>
                Structured AI agent auditing. Three independent agents. One trust score.
              </p>
            </div>

            {/* product */}
            <div>
              <div style={{ fontFamily: "var(--font-mono)", color: "var(--color-sage)", fontSize: 10, letterSpacing: "0.15em", marginBottom: 16 }}>PRODUCT</div>
              {[
                { label: "Submit Trace", to: "/app/submit" },
                { label: "Dashboard", to: "/app" },
                { label: "Run History", to: "/app/history" },
                { label: "Sign In", to: "/login" },
              ].map((l) => (
                <Link
                  key={l.label}
                  to={l.to}
                  style={{ fontFamily: "var(--font-body)", display: "block", color: "var(--color-parchment)", fontSize: 13, marginBottom: 10, textDecoration: "none", opacity: 0.85 }}
                >
                  {l.label}
                </Link>
              ))}
            </div>

            {/* docs */}
            <div>
              <div style={{ fontFamily: "var(--font-mono)", color: "var(--color-sage)", fontSize: 10, letterSpacing: "0.15em", marginBottom: 16 }}>DOCS</div>
              {[
                "Getting Started", "API Reference", "Agent Scores", "Verdict Types", "Open Source",
              ].map((l) => (
                <a
                  key={l}
                  href="#"
                  style={{ fontFamily: "var(--font-body)", display: "block", color: "var(--color-parchment)", fontSize: 13, marginBottom: 10, textDecoration: "none", opacity: 0.85 }}
                >
                  {l}
                </a>
              ))}
            </div>

            {/* status */}
            <div>
              <div style={{ fontFamily: "var(--font-mono)", color: "var(--color-sage)", fontSize: 10, letterSpacing: "0.15em", marginBottom: 16 }}>STATUS</div>
              <div className={cx("border-neo")} style={{ background: "var(--color-forest)", padding: "12px 14px", display: "inline-flex", alignItems: "center", gap: 8 }}>
                <span className="live-dot" style={{ width: 8, height: 8, borderRadius: "50%", background: "var(--color-sage)", display: "inline-block" }} />
                <span style={{ fontFamily: "var(--font-mono)", color: "var(--color-sage)", fontSize: 11 }}>ALL SYSTEMS GO</span>
              </div>
              <p style={{ fontFamily: "var(--font-mono)", color: "var(--color-sage)", fontSize: 10, marginTop: 12, opacity: 0.7 }}>
                Open Beta · v3.0.0
              </p>
            </div>
          </div>

          {/* bottom bar */}
          <div style={{ borderTop: "2px solid rgba(255,255,255,0.15)", paddingTop: 24, display: "flex", flexWrap: "wrap", gap: 16, justifyContent: "space-between", alignItems: "center" }}>
            <span style={{ fontFamily: "var(--font-mono)", color: "var(--color-sage)", fontSize: 10, opacity: 0.6, letterSpacing: "0.08em" }}>
              © 2026 TracePilot
            </span>
            <span style={{ fontFamily: "var(--font-mono)", color: "var(--color-sage)", fontSize: 10, opacity: 0.6, letterSpacing: "0.08em" }}>
              BUILT WITH CREWAI (FastApi) · SPRING BOOT · REACT (TS) · PostgreSQL
            </span>
          </div>
        </div>
      </footer>
    </div>
  );
}
