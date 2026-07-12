import { useState, useEffect } from "react";
import {
  Terminal, ArrowRight, Activity, TrendingUp,
  GitBranch, Zap, CheckCircle2, XCircle, AlertTriangle,
  ChevronRight, Menu, X, Database, Server, Cpu, Radio,
  Eye, Lock, BarChart3,
} from "lucide-react";

// ── Palette ──────────────────────────────────────────────────────
const G = {
  forest:      "#1E3A2F",
  forestDark:  "#162D23",
  forestCode:  "#0F1A14",
  sage:        "#A8D5A2",
  brown:       "#3B2012",
  crimson:     "#8B1A1A",
  parchment:   "#F4F1EA",
  card:        "#FDFAF3",
  muted:       "#E5E0D5",
  ink:         "#0D0D0D",
  inkMuted:    "#5C5348",
  amberCode:   "#C8A86A",
  leafCode:    "#C8D8A8",
  mintCode:    "#6A9E7A",
};

// ── Reusable style atoms ─────────────────────────────────────────
const NB   = `border-2 border-[${G.ink}]`;
const SHD  = `shadow-[4px_4px_0px_#0D0D0D]`;
const SHDS = `shadow-[3px_3px_0px_#0D0D0D]`;
const FONT_DISPLAY = { fontFamily: "'Archivo Black', sans-serif" };
const FONT_BODY    = { fontFamily: "'Archivo', sans-serif" };
const FONT_MONO    = { fontFamily: "'JetBrains Mono', monospace" };

// ── Data ─────────────────────────────────────────────────────────
const agents = [
  {
    icon: <Activity size={22} />,
    name: "Trace Loop & Efficiency Agent",
    tag: "LOOP_EFFICIENCY",
    color: G.brown,
    desc: "Detects repeated tool cycles, missing stop conditions, unbounded retries, and circular plans. Spots when your agent ran the same search 7 times and called it progress.",
    bullets: ["Repeated tool call detection", "Missing stop-condition flags", "Estimated wasted steps", "Loop pattern classification"],
  },
  {
    icon: <Eye size={22} />,
    name: "Blind Outcome Verifier",
    tag: "BLIND_VERIFIER",
    color: G.crimson,
    desc: "Judges task completion using observable evidence only — commands, diffs, test output, final state. Never sees the agent's self-rationale. The structural differentiator.",
    bullets: ["Evidence-only verdict", "Self-claim withheld by design", "CONTRADICTED / UNVERIFIED scoring", "Missing evidence enumeration"],
  },
  {
    icon: <TrendingUp size={22} />,
    name: "Reliability Trend Agent",
    tag: "RELIABILITY_TREND",
    color: G.forest,
    desc: "Scores whether a repo + agent-tool combination is becoming more or less reliable over time. Compounds across runs, surfaces drift before it becomes a pattern.",
    bullets: ["Cross-run reliability scoring", "IMPROVING / DECLINING trend", "Per-repo + per-tool grouping", "Historical signal summary"],
  },
];

const steps = [
  {
    num: "01",
    title: "Paste your trace",
    body: "Drop in a raw agent transcript, CI log, terminal output, or PR bundle. One field required. Title, repo name, and agent tool are optional — they improve reports.",
    color: G.brown,
    textColor: G.parchment,
  },
  {
    num: "02",
    title: "Three agents audit in parallel",
    body: "The Loop Efficiency agent sees the full trace. The Blind Verifier sees only extracted evidence — never the agent's self-rationale. The Reliability agent adds historical context.",
    color: G.parchment,
    textColor: G.ink,
  },
  {
    num: "03",
    title: "Get a structured trust report",
    body: "A scored, structured report with findings, severity ratings, top fixes, and an outcome verdict. Persisted over time so you can track whether your agents are improving.",
    color: G.forest,
    textColor: G.parchment,
  },
];

const stack = [
  { icon: <Server size={20} />,   label: "Spring Boot 3.x",     sub: "Java 21 · REST + Security",  color: G.forest  },
  { icon: <Zap size={20} />,      label: "FastAPI + CrewAI",     sub: "Python 3.11 · AI Worker",    color: G.brown   },
  { icon: <Cpu size={20} />,      label: "React + TypeScript",   sub: "Vite · Bun · shadcn/ui",     color: G.forest  },
  { icon: <Database size={20} />, label: "PostgreSQL 16",        sub: "JSONB · Flyway migrations",  color: G.brown   },
  { icon: <Radio size={20} />,    label: "RabbitMQ 3.13",        sub: "Durable job queue · DLQ",    color: G.forest  },
  { icon: <BarChart3 size={20} />,label: "OpenTelemetry + Jaeger",sub: "Distributed tracing · OTLP",color: G.brown   },
];

const verdicts = [
  { label: "LIKELY_COMPLETE",  color: G.forest,  icon: <CheckCircle2 size={11} /> },
  { label: "UNVERIFIED",       color: G.brown,   icon: <AlertTriangle size={11} /> },
  { label: "LIKELY_INCOMPLETE",color: "#7A4A28", icon: <AlertTriangle size={11} /> },
  { label: "CONTRADICTED",     color: G.crimson, icon: <XCircle size={11} /> },
];

const stats = [
  { value: "3",     label: "Concurrent CrewAI agents"     },
  { value: "100%",  label: "Evidence-only blind verifier" },
  { value: "SHA-256", label: "Trace hash deduplication"   },
  { value: "∞",     label: "Historical reliability runs"  },
];

// ── Ticker ───────────────────────────────────────────────────────
const TICKER_ITEMS = [
  "TRACE LOOP EFFICIENCY", "BLIND OUTCOME VERIFIER", "RELIABILITY TREND",
  "PROMPT ISOLATION", "DISTRIBUTED TRACING", "PYDANTIC CONTRACTS",
  "RABBITMQ DISPATCH", "JWT AUTHENTICATION", "POSTGRESQL JSONB",
];

function Ticker() {
  return (
    <div
      className="border-y-2 border-black overflow-hidden py-3 select-none"
      style={{ background: G.ink }}
    >
      <div className="flex gap-0" style={{ animation: "ticker 30s linear infinite", width: "max-content" }}>
        {[...TICKER_ITEMS, ...TICKER_ITEMS, ...TICKER_ITEMS].map((item, i) => (
          <span key={i} className="flex items-center gap-4 px-6">
            <span style={{ ...FONT_DISPLAY, color: G.sage, fontSize: 11, letterSpacing: "0.12em" }}>
              {item}
            </span>
            <span style={{ color: G.forest, fontSize: 16, lineHeight: 1 }}>✦</span>
          </span>
        ))}
      </div>
    </div>
  );
}

// ── Mini report card (hero illustration) ─────────────────────────
function MiniReport() {
  return (
    <div className={`border-2 border-black ${SHD}`} style={{ background: G.card }}>
      {/* Header */}
      <div className="border-b-2 border-black px-4 py-3 flex items-center justify-between" style={{ background: G.crimson }}>
        <span style={{ ...FONT_DISPLAY, color: G.parchment, fontSize: 11, letterSpacing: "0.06em" }}>
          TRUST REPORT · a3f7c81b
        </span>
        <span style={{ ...FONT_DISPLAY, color: G.parchment, fontSize: 10 }}>CLAUDE_CODE</span>
      </div>
      {/* Score row */}
      <div className="px-4 py-4 flex items-center gap-4 border-b-2 border-black" style={{ background: G.parchment }}>
        <div>
          <div style={{ ...FONT_MONO, fontSize: 9, color: G.inkMuted, letterSpacing: "0.08em" }}>OVERALL SCORE</div>
          <div style={{ ...FONT_DISPLAY, fontSize: 44, lineHeight: 1, letterSpacing: "-0.04em", color: G.ink }}>31<span style={{ fontSize: 18, color: G.inkMuted }}>/100</span></div>
        </div>
        <div className="flex-1 space-y-2">
          {[{ label: "LOOP EFF.", score: 28 }, { label: "BLIND VER.", score: 31 }, { label: "RELIABILITY", score: 34 }].map(r => (
            <div key={r.label} className="flex items-center gap-2">
              <span style={{ ...FONT_MONO, fontSize: 8, color: G.inkMuted, width: 64 }}>{r.label}</span>
              <div className="flex-1 h-2 border border-black bg-white overflow-hidden">
                <div style={{ height: "100%", width: `${r.score}%`, background: G.crimson }} />
              </div>
              <span style={{ ...FONT_MONO, fontSize: 9, fontWeight: 700, width: 20, textAlign: "right" }}>{r.score}</span>
            </div>
          ))}
        </div>
      </div>
      {/* Verdict */}
      <div className="px-4 py-3 border-b-2 border-black">
        <div style={{ ...FONT_MONO, fontSize: 9, color: G.inkMuted, marginBottom: 6, letterSpacing: "0.06em" }}>BLIND OUTCOME VERDICT</div>
        <div className="flex items-center gap-2">
          <span className="border-2 border-black px-2 py-1 flex items-center gap-1" style={{ background: G.crimson }}>
            <XCircle size={9} style={{ color: G.parchment }} />
            <span style={{ ...FONT_DISPLAY, fontSize: 10, color: G.parchment }}>CONTRADICTED</span>
          </span>
          <span style={{ ...FONT_BODY, fontSize: 11, color: G.inkMuted }}>
            Agent claimed tests pass. They don't.
          </span>
        </div>
      </div>
      {/* Top fix */}
      <div className="px-4 py-3" style={{ background: G.muted }}>
        <div style={{ ...FONT_MONO, fontSize: 9, color: G.inkMuted, marginBottom: 4, letterSpacing: "0.06em" }}>TOP FIX</div>
        <div style={{ ...FONT_BODY, fontSize: 11, color: G.ink }}>
          Run the exact failing test after the final patch and inspect the changed branch before marking complete.
        </div>
      </div>
    </div>
  );
}

// ── Isolation code snippet ────────────────────────────────────────
function CodeSnippet() {
  return (
    <div className="border-2 border-black" style={{ background: G.forestCode }}>
      <div className="border-b-2 border-black px-4 py-2.5 flex items-center gap-2" style={{ background: G.ink }}>
        <div className="w-2.5 h-2.5 border border-black/40" style={{ background: G.crimson }} />
        <div className="w-2.5 h-2.5 border border-black/40" style={{ background: "#C8900A" }} />
        <div className="w-2.5 h-2.5 border border-black/40" style={{ background: G.sage }} />
        <span style={{ ...FONT_MONO, fontSize: 10, color: "rgba(244,241,234,0.4)", marginLeft: 8 }}>
          blind_verifier_gate.py
        </span>
      </div>
      <div className="px-5 py-4 text-[11px] leading-[1.8]" style={FONT_MONO}>
        <div><span style={{ color: G.mintCode }}>class </span><span style={{ color: G.leafCode }}>PromptInputSeparator</span><span style={{ color: G.parchment }}>:</span></div>
        <div style={{ color: "rgba(244,241,234,0.3)", paddingLeft: 16 }}>&quot;Withholds agent claims from blind verifier.&quot;</div>
        <div style={{ paddingLeft: 16, marginTop: 8 }}>
          <span style={{ color: G.mintCode }}>def </span>
          <span style={{ color: G.sage }}>build_blind_payload</span>
          <span style={{ color: G.parchment }}>(self, evidence) -&gt; dict:</span>
        </div>
        <div style={{ paddingLeft: 32 }}><span style={{ color: G.mintCode }}>return </span><span style={{ color: G.parchment }}>{"{"}</span></div>
        {['"task_goal"', '"commands_run"', '"test_outputs"', '"diff_snippets"'].map(k => (
          <div key={k} style={{ paddingLeft: 48 }}>
            <span style={{ color: G.amberCode }}>{k}</span>
            <span style={{ color: "rgba(244,241,234,0.4)" }}>: </span>
            <span style={{ color: G.parchment }}>evidence.{k.replace(/"/g, "")},</span>
          </div>
        ))}
        <div style={{ paddingLeft: 48, color: "rgba(244,241,234,0.25)" }}># withheld_claims — excluded by design</div>
        <div style={{ paddingLeft: 32, color: G.parchment }}>{"}"}</div>
      </div>
    </div>
  );
}

// ── Main component ────────────────────────────────────────────────
export default function App() {
  const [navOpen, setNavOpen] = useState(false);
  const [scrolled, setScrolled] = useState(false);

  useEffect(() => {
    const onScroll = () => setScrolled(window.scrollY > 10);
    window.addEventListener("scroll", onScroll);
    return () => window.removeEventListener("scroll", onScroll);
  }, []);

  return (
    <div style={{ background: G.parchment, color: G.ink, fontFamily: "'Archivo', sans-serif", minHeight: "100vh" }}>

      {/* ════════════════════════ NAV ════════════════════════ */}
      <nav
        className={`sticky top-0 z-50 border-b-2 border-black transition-all`}
        style={{ background: scrolled ? G.forest : G.parchment }}
      >
        <div className="max-w-[1400px] mx-auto px-8 h-16 flex items-center justify-between">
          {/* Logo */}
          <div className="flex items-center gap-2.5">
            <div className="w-8 h-8 border-2 border-black flex items-center justify-center" style={{ background: scrolled ? G.brown : G.forest }}>
              <Terminal size={13} style={{ color: G.parchment }} />
            </div>
            <span style={{ ...FONT_DISPLAY, fontSize: 18, letterSpacing: "-0.02em", color: scrolled ? G.parchment : G.ink }}>
              TracePilot<span style={{ textDecoration: "underline", textDecorationThickness: 2 }}>.AI</span>
            </span>
          </div>

          {/* Desktop nav links */}
          <div className="hidden md:flex items-center gap-0">
            {["How it works", "Agents", "Architecture", "Docs"].map((link) => (
              <a
                key={link}
                href="#"
                className="px-4 py-2 text-sm font-bold border-r-2 border-black last:border-r-0 hover:underline"
                style={{ ...FONT_DISPLAY, fontSize: 12, color: scrolled ? G.parchment : G.ink, letterSpacing: "0.04em" }}
              >
                {link}
              </a>
            ))}
          </div>

          {/* CTAs */}
          <div className="hidden md:flex items-center gap-3">
            <a
              href="#"
              className="px-4 py-2 border-2 border-black text-sm font-black hover:translate-x-[2px] hover:translate-y-[2px] transition-transform"
              style={{ ...FONT_DISPLAY, background: scrolled ? "rgba(244,241,234,0.15)" : "transparent", color: scrolled ? G.parchment : G.ink, fontSize: 12 }}
            >
              Sign In
            </a>
            <a
              href="#"
              className={`px-4 py-2 border-2 border-black text-sm font-black flex items-center gap-1.5 ${SHDS} hover:translate-x-[2px] hover:translate-y-[2px] hover:shadow-none transition-all`}
              style={{ ...FONT_DISPLAY, background: scrolled ? G.parchment : G.forest, color: scrolled ? G.forest : G.parchment, fontSize: 12 }}
            >
              Start Auditing
              <ArrowRight size={12} />
            </a>
          </div>

          {/* Mobile menu toggle */}
          <button className="md:hidden border-2 border-black p-1.5" style={{ background: scrolled ? "rgba(244,241,234,0.15)" : G.muted }} onClick={() => setNavOpen(!navOpen)}>
            {navOpen ? <X size={18} style={{ color: scrolled ? G.parchment : G.ink }} /> : <Menu size={18} style={{ color: scrolled ? G.parchment : G.ink }} />}
          </button>
        </div>

        {/* Mobile nav */}
        {navOpen && (
          <div className="md:hidden border-t-2 border-black px-8 py-4 space-y-2" style={{ background: G.forest }}>
            {["How it works", "Agents", "Architecture", "Docs", "Sign In"].map((link) => (
              <a key={link} href="#" className="block py-2 text-sm font-black border-b border-black/20" style={{ ...FONT_DISPLAY, color: G.parchment }}>
                {link}
              </a>
            ))}
            <a href="#" className="block mt-3 px-4 py-2.5 border-2 border-black text-center font-black" style={{ ...FONT_DISPLAY, background: G.parchment, color: G.forest }}>
              Start Auditing →
            </a>
          </div>
        )}
      </nav>

      {/* ════════════════════════ HERO ════════════════════════ */}
      <section className="border-b-2 border-black" style={{ background: G.forest }}>
        <div className="max-w-[1400px] mx-auto px-8 py-20 grid grid-cols-1 lg:grid-cols-2 gap-12 items-start">

          {/* Left */}
          <div>
            {/* Badge */}
            <div className="inline-flex items-center gap-2 border-2 border-black px-3 py-1.5 mb-6" style={{ background: G.brown }}>
              <span className="w-1.5 h-1.5 rounded-full" style={{ background: G.sage, animation: "rawPulse 1.8s ease-in-out infinite" }} />
              <span style={{ ...FONT_DISPLAY, color: G.parchment, fontSize: 10, letterSpacing: "0.1em" }}>
                v3.0.0 · OPEN BETA
              </span>
            </div>

            <h1 style={{ ...FONT_DISPLAY, fontSize: "clamp(40px, 5vw, 64px)", lineHeight: 1.0, letterSpacing: "-0.025em", color: G.parchment, marginBottom: 24 }}>
              Stop trusting.<br />
              Start<br />
              <span style={{ textDecoration: "underline", textDecorationThickness: 3, textDecorationColor: G.sage }}>
                verifying.
              </span>
            </h1>

            <p style={{ ...FONT_BODY, fontSize: 16, color: "rgba(244,241,234,0.75)", lineHeight: 1.7, maxWidth: 480, marginBottom: 32 }}>
              TracePilot AI audits your coding-agent runs. Paste a transcript, get a
              structured trust report — loop analysis, blind outcome verification, and
              reliability trends across every run.
            </p>

            {/* CTA row */}
            <div className="flex flex-wrap gap-3 mb-10">
              <a
                href="#"
                className={`flex items-center gap-2 px-6 py-3.5 border-2 border-black ${SHD} hover:translate-x-[2px] hover:translate-y-[2px] hover:shadow-none transition-all`}
                style={{ ...FONT_DISPLAY, background: G.parchment, color: G.forest, fontSize: 14 }}
              >
                Submit Your First Trace
                <ArrowRight size={14} />
              </a>
              <a
                href="#"
                className="flex items-center gap-2 px-6 py-3.5 border-2 border-black hover:bg-white/10 transition-colors"
                style={{ ...FONT_DISPLAY, color: G.parchment, fontSize: 14 }}
              >
                <GitBranch size={14} />
                View on GitHub
              </a>
            </div>

            {/* Stat strip */}
            <div className="grid grid-cols-2 gap-3 max-w-sm">
              {stats.map(({ value, label }) => (
                <div key={label} className="border-2 border-black px-4 py-3" style={{ background: G.forestDark }}>
                  <div style={{ ...FONT_DISPLAY, fontSize: 22, color: G.sage, letterSpacing: "-0.02em" }}>{value}</div>
                  <div style={{ ...FONT_MONO, fontSize: 9, color: "rgba(244,241,234,0.5)", marginTop: 2, letterSpacing: "0.06em" }}>{label.toUpperCase()}</div>
                </div>
              ))}
            </div>
          </div>

          {/* Right — mini report */}
          <div className="lg:pt-6">
            <MiniReport />

            {/* Verdict legend */}
            <div className="mt-4 border-2 border-black p-4" style={{ background: G.forestDark }}>
              <div style={{ ...FONT_MONO, fontSize: 9, color: "rgba(244,241,234,0.4)", letterSpacing: "0.1em", marginBottom: 10 }}>
                POSSIBLE VERDICTS
              </div>
              <div className="flex flex-wrap gap-2">
                {verdicts.map(({ label, color, icon }) => (
                  <span key={label} className="flex items-center gap-1.5 border border-black px-2 py-1" style={{ background: color }}>
                    <span style={{ color: G.parchment }}>{icon}</span>
                    <span style={{ ...FONT_DISPLAY, fontSize: 9, color: G.parchment }}>{label}</span>
                  </span>
                ))}
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* ════════════════ TICKER ════════════════ */}
      <Ticker />

      {/* ════════════════ PROBLEM ════════════════ */}
      <section className="border-b-2 border-black" style={{ background: G.card }}>
        <div className="max-w-[1400px] mx-auto px-8 py-20 grid grid-cols-1 lg:grid-cols-2 gap-12 items-center">

          <div>
            <div className="inline-block border-2 border-black px-3 py-1 mb-6" style={{ background: G.muted }}>
              <span style={{ ...FONT_DISPLAY, fontSize: 10, letterSpacing: "0.1em", color: G.inkMuted }}>THE PROBLEM</span>
            </div>
            <h2 style={{ ...FONT_DISPLAY, fontSize: "clamp(28px, 3.5vw, 44px)", lineHeight: 1.05, letterSpacing: "-0.02em", marginBottom: 20 }}>
              Can you trust what your AI agent just did?
            </h2>
            <p style={{ ...FONT_BODY, fontSize: 15, color: G.inkMuted, lineHeight: 1.7, marginBottom: 16 }}>
              AI coding agents are getting better at <em>sounding</em> done. They report test suites
              passing when they aren't. They loop through the same tool call seven times and call it
              progress. They make changes with no stop condition and claim success.
            </p>
            <p style={{ ...FONT_BODY, fontSize: 15, color: G.inkMuted, lineHeight: 1.7, marginBottom: 28 }}>
              TracePilot doesn&apos;t ask <strong>"can this LLM solve bugs?"</strong> — it asks
              <strong> "can you trust what this agent just claimed it did?"</strong> That&apos;s a
              different question, and it deserves a different tool.
            </p>

            <div className="space-y-2">
              {[
                "Agents misreport completion when tests still fail",
                "Repeated tool cycles with no new information gained",
                "No independent verification of self-reported outcomes",
                "No cross-run visibility into whether agents are improving",
              ].map((point) => (
                <div key={point} className="flex items-start gap-3 border-2 border-black px-4 py-3" style={{ background: G.parchment }}>
                  <XCircle size={14} style={{ color: G.crimson, marginTop: 1, flexShrink: 0 }} />
                  <span style={{ ...FONT_BODY, fontSize: 13, color: G.ink }}>{point}</span>
                </div>
              ))}
            </div>
          </div>

          {/* Terminal quote box */}
          <div>
            <div className={`border-2 border-black ${SHD}`}>
              <div className="border-b-2 border-black px-4 py-3 flex items-center gap-2" style={{ background: G.ink }}>
                <span style={{ ...FONT_DISPLAY, color: G.parchment, fontSize: 11, letterSpacing: "0.06em" }}>AGENT TRANSCRIPT · billing-service</span>
              </div>
              <div className="p-5 space-y-3" style={{ background: G.forestCode, ...FONT_MONO, fontSize: 12, lineHeight: 1.7 }}>
                <div style={{ color: G.mintCode }}># Agent's final message</div>
                <div style={{ color: G.parchment }}>
                  <span style={{ color: "rgba(244,241,234,0.4)" }}>[claude_code] </span>
                  All tests are passing. The fix for the billing calculation has been applied and verified. The PR is ready for review.
                </div>
                <div style={{ color: G.amberCode, marginTop: 8 }}># What the trace actually shows</div>
                <div style={{ color: G.parchment }}>
                  <span style={{ color: "rgba(244,241,234,0.4)" }}>[npm test]    </span>
                  <span style={{ color: G.crimson }}>FAIL</span> src/billing.test.ts
                </div>
                <div style={{ color: "rgba(244,241,234,0.5)", paddingLeft: 16 }}>
                  ● BillingCalc › should apply discount<br />
                  &nbsp;&nbsp;Expected: 85.00<br />
                  &nbsp;&nbsp;Received: 100.00
                </div>
                <div style={{ color: "rgba(244,241,234,0.3)", marginTop: 8 }}># Same failure, before and after the patch.</div>
              </div>
              <div className="border-t-2 border-black px-4 py-3 flex items-center gap-2" style={{ background: G.crimson }}>
                <XCircle size={13} style={{ color: G.parchment }} />
                <span style={{ ...FONT_DISPLAY, color: G.parchment, fontSize: 11, letterSpacing: "0.06em" }}>
                  BLIND VERIFIER: CONTRADICTED · Score 31/100
                </span>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* ════════════════ HOW IT WORKS ════════════════ */}
      <section className="border-b-2 border-black" style={{ background: G.parchment }}>
        <div className="max-w-[1400px] mx-auto px-8 py-20">
          <div className="flex items-end justify-between mb-12 flex-wrap gap-4">
            <div>
              <div className="inline-block border-2 border-black px-3 py-1 mb-4" style={{ background: G.muted }}>
                <span style={{ ...FONT_DISPLAY, fontSize: 10, letterSpacing: "0.1em", color: G.inkMuted }}>HOW IT WORKS</span>
              </div>
              <h2 style={{ ...FONT_DISPLAY, fontSize: "clamp(28px, 3.5vw, 44px)", lineHeight: 1.05, letterSpacing: "-0.02em" }}>
                Three steps to a trust report.
              </h2>
            </div>
            <a
              href="#"
              className={`flex items-center gap-2 px-5 py-3 border-2 border-black ${SHDS} hover:translate-x-[2px] hover:translate-y-[2px] hover:shadow-none transition-all`}
              style={{ ...FONT_DISPLAY, background: G.forest, color: G.parchment, fontSize: 12 }}
            >
              See a sample report <ArrowRight size={12} />
            </a>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-0 border-2 border-black">
            {steps.map((step, i) => (
              <div
                key={step.num}
                className="p-8 relative"
                style={{
                  background: step.color,
                  borderRight: i < steps.length - 1 ? "2px solid #0D0D0D" : "none",
                }}
              >
                <div
                  className="border-2 border-black w-12 h-12 flex items-center justify-center mb-6"
                  style={{ background: step.textColor === G.parchment ? G.parchment : G.ink }}
                >
                  <span style={{ ...FONT_DISPLAY, fontSize: 16, color: step.textColor === G.parchment ? G.ink : G.parchment }}>
                    {step.num}
                  </span>
                </div>
                <h3 style={{ ...FONT_DISPLAY, fontSize: 20, letterSpacing: "-0.01em", color: step.textColor, marginBottom: 12 }}>
                  {step.title}
                </h3>
                <p style={{ ...FONT_BODY, fontSize: 14, color: step.textColor === G.parchment ? "rgba(244,241,234,0.75)" : G.inkMuted, lineHeight: 1.65 }}>
                  {step.body}
                </p>
                {i < steps.length - 1 && (
                  <div className="absolute -right-5 top-1/2 -translate-y-1/2 z-10 w-8 h-8 border-2 border-black flex items-center justify-center hidden md:flex" style={{ background: G.parchment }}>
                    <ChevronRight size={14} />
                  </div>
                )}
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* ════════════════ AGENTS ════════════════ */}
      <section className="border-b-2 border-black" style={{ background: G.muted }}>
        <div className="max-w-[1400px] mx-auto px-8 py-20">
          <div className="mb-12">
            <div className="inline-block border-2 border-black px-3 py-1 mb-4" style={{ background: G.parchment }}>
              <span style={{ ...FONT_DISPLAY, fontSize: 10, letterSpacing: "0.1em", color: G.inkMuted }}>THE THREE AGENTS</span>
            </div>
            <h2 style={{ ...FONT_DISPLAY, fontSize: "clamp(28px, 3.5vw, 44px)", lineHeight: 1.05, letterSpacing: "-0.02em" }}>
              Each agent sees exactly what it should.
            </h2>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-5">
            {agents.map((agent) => (
              <div key={agent.tag} className={`border-2 border-black ${SHD} flex flex-col`} style={{ background: G.card }}>
                {/* Card header */}
                <div className="border-b-2 border-black px-5 py-4 flex items-center justify-between" style={{ background: agent.color }}>
                  <span style={{ color: G.parchment }}>{agent.icon}</span>
                  <span style={{ ...FONT_MONO, fontSize: 9, color: "rgba(244,241,234,0.6)", letterSpacing: "0.08em" }}>{agent.tag}</span>
                </div>

                <div className="p-5 flex-1">
                  <h3 style={{ ...FONT_DISPLAY, fontSize: 17, lineHeight: 1.2, marginBottom: 12 }}>{agent.name}</h3>
                  <p style={{ ...FONT_BODY, fontSize: 13, color: G.inkMuted, lineHeight: 1.65, marginBottom: 16 }}>{agent.desc}</p>
                  <div className="space-y-2">
                    {agent.bullets.map((b) => (
                      <div key={b} className="flex items-center gap-2">
                        <CheckCircle2 size={11} style={{ color: agent.color, flexShrink: 0 }} />
                        <span style={{ ...FONT_BODY, fontSize: 12, color: G.inkMuted }}>{b}</span>
                      </div>
                    ))}
                  </div>
                </div>

                <div className="border-t-2 border-black px-5 py-3" style={{ background: G.parchment }}>
                  <span style={{ ...FONT_MONO, fontSize: 10, color: G.inkMuted, letterSpacing: "0.06em" }}>
                    AGENT TYPE: {agent.tag}
                  </span>
                </div>
              </div>
            ))}
          </div>

          {/* Isolation callout */}
          <div className="mt-5 border-2 border-black grid grid-cols-1 lg:grid-cols-2" style={{ background: G.card }}>
            <div className="p-8 border-b-2 lg:border-b-0 lg:border-r-2 border-black">
              <div className="flex items-center gap-3 mb-4">
                <div className="border-2 border-black p-2" style={{ background: G.crimson }}>
                  <Lock size={16} style={{ color: G.parchment }} />
                </div>
                <span style={{ ...FONT_DISPLAY, fontSize: 14 }}>The Structural Differentiator</span>
              </div>
              <p style={{ ...FONT_BODY, fontSize: 14, color: G.inkMuted, lineHeight: 1.7, marginBottom: 12 }}>
                The Blind Outcome Verifier is <strong>never</strong> given the original agent&apos;s
                self-rationale or final completion claim. This is enforced at the prompt-construction
                level and covered by an isolated test suite.
              </p>
              <p style={{ ...FONT_BODY, fontSize: 14, color: G.inkMuted, lineHeight: 1.7 }}>
                It evaluates only extracted evidence: commands run, diffs, test output, touched files,
                and final state signals. The same evidence a human reviewer would look at.
              </p>
            </div>
            <div className="p-6">
              <CodeSnippet />
            </div>
          </div>
        </div>
      </section>

      {/* ════════════════ ARCHITECTURE ════════════════ */}
      <section className="border-b-2 border-black" style={{ background: G.card }}>
        <div className="max-w-[1400px] mx-auto px-8 py-20">
          <div className="mb-12">
            <div className="inline-block border-2 border-black px-3 py-1 mb-4" style={{ background: G.muted }}>
              <span style={{ ...FONT_DISPLAY, fontSize: 10, letterSpacing: "0.1em", color: G.inkMuted }}>ARCHITECTURE</span>
            </div>
            <h2 style={{ ...FONT_DISPLAY, fontSize: "clamp(28px, 3.5vw, 44px)", lineHeight: 1.05, letterSpacing: "-0.02em" }}>
              Polyglot. Decoupled. Observable.
            </h2>
          </div>

          {/* Stack grid */}
          <div className="grid grid-cols-2 md:grid-cols-3 gap-4 mb-10">
            {stack.map(({ icon, label, sub, color }) => (
              <div key={label} className={`border-2 border-black ${SHDS} flex gap-4 items-start p-5 hover:translate-x-[2px] hover:translate-y-[2px] hover:shadow-none transition-all cursor-default`} style={{ background: G.parchment }}>
                <div className="border-2 border-black p-2.5 shrink-0" style={{ background: color }}>
                  <span style={{ color: G.parchment }}>{icon}</span>
                </div>
                <div>
                  <div style={{ ...FONT_DISPLAY, fontSize: 13 }}>{label}</div>
                  <div style={{ ...FONT_MONO, fontSize: 10, color: G.inkMuted, marginTop: 3 }}>{sub}</div>
                </div>
              </div>
            ))}
          </div>

          {/* Architecture diagram strip */}
          <div className="border-2 border-black overflow-x-auto" style={{ background: G.forestCode }}>
            <div className="border-b-2 border-black px-5 py-3 flex items-center gap-2" style={{ background: G.ink }}>
              <span style={{ ...FONT_DISPLAY, color: G.parchment, fontSize: 11, letterSpacing: "0.06em" }}>SYSTEM DIAGRAM</span>
            </div>
            <div className="px-6 py-6 min-w-[600px]" style={{ ...FONT_MONO, fontSize: 11, lineHeight: 1.8 }}>
              <div className="flex items-center gap-0 flex-wrap">
                {[
                  { label: "React SPA", sub: "Vite + Bun", color: G.forest },
                  { label: "Spring Boot API", sub: "Auth · Persist · Rate limit", color: G.brown },
                  { label: "RabbitMQ", sub: "audit.jobs queue", color: "#5A3A00" },
                  { label: "Python Worker", sub: "FastAPI + CrewAI", color: G.crimson },
                ].map((node, i, arr) => (
                  <div key={node.label} className="flex items-center">
                    <div className="border-2 border-black px-4 py-3 min-w-[140px]" style={{ background: node.color }}>
                      <div style={{ ...FONT_DISPLAY, color: G.parchment, fontSize: 11 }}>{node.label}</div>
                      <div style={{ ...FONT_MONO, color: "rgba(244,241,234,0.5)", fontSize: 9, marginTop: 2 }}>{node.sub}</div>
                    </div>
                    {i < arr.length - 1 && (
                      <div className="flex items-center px-2" style={{ color: G.sage }}>
                        <div className="w-8 border-t-2 border-dashed" style={{ borderColor: G.sage }} />
                        <ChevronRight size={12} style={{ color: G.sage, marginLeft: -2 }} />
                      </div>
                    )}
                  </div>
                ))}
              </div>
              <div className="mt-4" style={{ color: "rgba(244,241,234,0.3)", fontSize: 10 }}>
                ↳ PostgreSQL ← Spring Boot (JDBC) &nbsp;·&nbsp; Jaeger ← OTel auto-instrumentation (Spring Boot + FastAPI) &nbsp;·&nbsp; audit.results → Spring Boot consumer
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* ════════════════ CTA ════════════════ */}
      <section className="border-b-2 border-black" style={{ background: G.forest }}>
        <div className="max-w-[1400px] mx-auto px-8 py-24 text-center">
          <div className="inline-block border-2 border-black px-3 py-1 mb-6" style={{ background: G.forestDark }}>
            <span style={{ ...FONT_MONO, color: G.sage, fontSize: 10, letterSpacing: "0.1em" }}>FREE TO USE · NO SETUP REQUIRED</span>
          </div>
          <h2 style={{ ...FONT_DISPLAY, fontSize: "clamp(32px, 4vw, 56px)", lineHeight: 1.0, letterSpacing: "-0.025em", color: G.parchment, marginBottom: 20 }}>
            Ready to audit your agents?
          </h2>
          <p style={{ ...FONT_BODY, fontSize: 16, color: "rgba(244,241,234,0.65)", lineHeight: 1.7, maxWidth: 480, margin: "0 auto 40px" }}>
            Paste your first trace in 30 seconds. No account required to see a report.
            Sign up to save history and track reliability over time.
          </p>
          <div className="flex justify-center gap-4 flex-wrap">
            <a
              href="#"
              className={`flex items-center gap-2 px-8 py-4 border-2 border-black ${SHD} hover:translate-x-[2px] hover:translate-y-[2px] hover:shadow-none transition-all`}
              style={{ ...FONT_DISPLAY, background: G.parchment, color: G.forest, fontSize: 14 }}
            >
              Submit Your First Trace
              <ArrowRight size={14} />
            </a>
            <a
              href="#"
              className="flex items-center gap-2 px-8 py-4 border-2 border-black hover:bg-white/10 transition-colors"
              style={{ ...FONT_DISPLAY, color: G.parchment, fontSize: 14 }}
            >
              <GitBranch size={14} />
              <strong>View on GitHub</strong>
            </a>
          </div>
        </div>
      </section>

      {/* ════════════════ FOOTER ════════════════ */}
      <footer className="border-t-0" style={{ background: G.brown }}>
        <div className="max-w-[1400px] mx-auto px-8 py-12">
          <div className="grid grid-cols-1 md:grid-cols-4 gap-8 border-b-2 border-black/30 pb-10 mb-8">
            {/* Brand */}
            <div className="md:col-span-2">
              <div className="flex items-center gap-2.5 mb-4">
                <div className="w-8 h-8 border-2 border-black/40 flex items-center justify-center" style={{ background: G.parchment }}>
                  <Terminal size={13} style={{ color: G.brown }} />
                </div>
                <span style={{ ...FONT_DISPLAY, fontSize: 18, color: G.parchment, letterSpacing: "-0.02em" }}>
                  TracePilot<span style={{ textDecoration: "underline" }}>.AI</span>
                </span>
              </div>
              <p style={{ ...FONT_BODY, fontSize: 13, color: "rgba(244,241,234,0.6)", lineHeight: 1.7, maxWidth: 340 }}>
                An automated audit platform for AI coding-agent runs. Detects loops, verifies outcomes blindly, and tracks reliability over time.
              </p>
            </div>

            {/* Links */}
            {[
              { heading: "Product", links: ["Submit Trace", "Reports", "Reliability", "History", "Share"] },
              { heading: "Docs", links: ["API Reference", "Architecture", "Agent Schema", "GitHub", "Changelog"] },
            ].map(({ heading, links }) => (
              <div key={heading}>
                <div style={{ ...FONT_DISPLAY, color: G.parchment, fontSize: 11, letterSpacing: "0.1em", marginBottom: 14 }}>{heading.toUpperCase()}</div>
                <div className="space-y-2">
                  {links.map((link) => (
                    <a key={link} href="#" className="block hover:underline" style={{ ...FONT_BODY, fontSize: 13, color: "rgba(244,241,234,0.55)" }}>{link}</a>
                  ))}
                </div>
              </div>
            ))}
          </div>

          <div className="flex items-center justify-between flex-wrap gap-4">
            <span style={{ ...FONT_MONO, fontSize: 11, color: "rgba(244,241,234,0.35)" }}>
              © 2025 TracePilot AI · v3.0.0
            </span>
            <div className="flex items-center gap-2 border border-black/30 px-3 py-1.5" style={{ background: G.forestDark }}>
              <span className="w-1.5 h-1.5 rounded-full" style={{ background: G.sage, animation: "rawPulse 1.8s ease-in-out infinite" }} />
              <span style={{ ...FONT_MONO, fontSize: 10, color: G.sage, letterSpacing: "0.08em" }}>ALL SYSTEMS OPERATIONAL</span>
            </div>
          </div>
        </div>
      </footer>

      <style>{`
        @keyframes rawPulse {
          0%, 100% { opacity: 1; }
          50% { opacity: 0.2; }
        }
        @keyframes ticker {
          from { transform: translateX(0); }
          to   { transform: translateX(-33.333%); }
        }
        * { scrollbar-width: none; }
        *::-webkit-scrollbar { display: none; }
      `}</style>
    </div>
  );
}