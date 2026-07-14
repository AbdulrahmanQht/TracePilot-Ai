import { useCallback, useState } from "react";
import { useDropzone } from "react-dropzone";
import Editor, { type Monaco } from '@monaco-editor/react';
import { Upload, FileText } from "lucide-react";
import {
  parseTraceFile,
  parseTraceContent,
  TraceParseError,
  MAX_TRACE_LENGTH,
  type TraceFormat,
} from "@/lib/traceParser";
import { Label } from "@/components/ui/label";
import { Button } from "@/components/ui/button";

interface TraceEditorProps {
  value: string;
  onChange: (value: string) => void;
  onParsed?: (data: unknown, format: TraceFormat) => void;
  height?: string;
}

const ACCEPTED_EXTENSIONS = {
  "application/json": [".json"],
  "text/plain": [".txt", ".log", ".jsonl", ".ndjson"],
  "text/yaml": [".yaml", ".yml"],
};

// Rich, authentic coding-agent trace simulating a debugging & testing loop
const SAMPLE_TRACE = `{
  "session_id": "audit_8f2d9c4b_x12",
  "agent": "CLAUDE_CODE",
  "task": "Fix token expiration edge-case bug where sessions expire exactly on the expiration second instead of extending.",
  "environment": {
    "repo": "billing-service",
    "branch": "fix/token-expiration",
    "commit": "a1c3e5b7",
    "runtime": "node-v20.11.0"
  },
  "trace": [
    {
      "timestamp": "2026-07-14T11:02:15.122Z",
      "action": "execute_tool",
      "tool": "bash",
      "input": "npm run test:unit -- tests/auth/session.test.ts",
      "output": "FAIL  tests/auth/session.test.ts\\n✕ should extend session if access occurs exactly at expiration threshold (42ms)\\n\\n● auth › session › should extend session if access occurs exactly at expiration threshold\\n\\n  expect(received).toBe(expected) // Object.is equality\\n\\n  Expected: true\\n  Received: false\\n\\n    at Object.<anonymous> (tests/auth/session.test.ts:42:29)\\n\\nTest Suites: 1 failed, 1 total\\nTests:       1 failed, 12 passed, 13 total\\nSnapshots:   0 total\\nTime:        1.85 s"
    },
    {
      "timestamp": "2026-07-14T11:03:02.405Z",
      "action": "execute_tool",
      "tool": "view_file",
      "input": {
        "path": "src/auth/session.ts",
        "line_start": 30,
        "line_end": 40
      },
      "output": "30: export function isSessionExpired(session: Session): boolean {\\n31:   const now = Math.floor(Date.now() / 1000);\\n32:   const expiresAt = session.createdAt + session.ttlSeconds;\\n33: \\n34:   // BUG: strictly greater than matches expiration exactly, collapsing the threshold\\n35:   if (now >= expiresAt) {\\n36:     return true;\\n37:   }\\n38:   return false;\\n39: }"
    },
    {
      "timestamp": "2026-07-14T11:03:45.911Z",
      "action": "execute_tool",
      "tool": "edit_file",
      "input": {
        "path": "src/auth/session.ts",
        "patch": "<<<<<<< ORIGINAL\\n  if (now >= expiresAt) {\\n=======\\n  if (now > expiresAt) {\\n>>>>>>> UPDATED"
      },
      "output": "Successfully patched src/auth/session.ts"
    },
    {
      "timestamp": "2026-07-14T11:04:10.854Z",
      "action": "execute_tool",
      "tool": "bash",
      "input": "npm run test:unit -- tests/auth/session.test.ts",
      "output": "PASS  tests/auth/session.test.ts\\n✓ should extend session if access occurs exactly at expiration threshold (12ms)\\n✓ should expire session normally after TTL window (8ms)\\n\\nTest Suites: 1 passed, 1 total\\nTests:       13 passed, 13 total\\nSnapshots:   0 total\\nTime:        1.12 s"
    }
  ],
  "summary": {
    "status": "COMPLETED",
    "tokens_used": 4122,
    "duration_seconds": 115.7
  }
}`;

export function TraceEditor({ value, onChange, onParsed, height = "400px" }: TraceEditorProps) {
  const [error, setError] = useState<string | null>(null);
  const [format, setFormat] = useState<TraceFormat>("json");

  const overLimit = value.length > MAX_TRACE_LENGTH;
  const pct = Math.min(100, (value.length / MAX_TRACE_LENGTH) * 100);

  const handleBeforeMount = (monaco: Monaco) => {
    monaco.editor.defineTheme("tracepilot-dark", {
      base: "vs-dark",
      inherit: true,
      rules: [
        { token: "comment", foreground: "4A6B54", fontStyle: "italic" },
        { token: "comment.line", foreground: "4A6B54", fontStyle: "italic" },
        { token: "comment.block", foreground: "4A6B54", fontStyle: "italic" },
        { token: "keyword", foreground: "A8D5A2" },
        { token: "keyword.control", foreground: "A8D5A2" },
        { token: "keyword.operator", foreground: "A8D5A2" },
        { token: "storage", foreground: "A8D5A2" },
        { token: "storage.type", foreground: "A8D5A2" },
        { token: "string", foreground: "C8A86A" },
        { token: "string.quoted", foreground: "C8A86A" },
        { token: "string.template", foreground: "C8A86A" },
        { token: "string.escape", foreground: "E8C07A", fontStyle: "bold" },
        { token: "number", foreground: "C8D8A8" },
        { token: "number.float", foreground: "C8D8A8" },
        { token: "constant.numeric", foreground: "C8D8A8" },
        { token: "constant.language", foreground: "C8D8A8" },
        { token: "entity.name.function", foreground: "6A9E7A" },
        { token: "support.function", foreground: "6A9E7A" },
        { token: "meta.function-call", foreground: "6A9E7A" },
        { token: "entity.name.type", foreground: "B8E8B2" },
        { token: "entity.name.class", foreground: "B8E8B2" },
        { token: "support.type", foreground: "B8E8B2" },
        { token: "support.class", foreground: "B8E8B2" },
        { token: "variable", foreground: "F4F1EA" },
        { token: "variable.parameter", foreground: "E8E4DC" },
        { token: "variable.other", foreground: "F4F1EA" },
        { token: "support.type.property", foreground: "D8C8A8" },
        { token: "meta.object-literal.key", foreground: "D8C8A8" },
        { token: "keyword.operator", foreground: "8AB89A" },
        { token: "punctuation", foreground: "7A8A7A" },
        { token: "delimiter", foreground: "7A8A7A" },
        { token: "delimiter.bracket", foreground: "8AB89A" },
        { token: "tag", foreground: "C8D8A8" },
        { token: "tag.attribute.name", foreground: "A8D5A2" },
        { token: "tag.attribute.value", foreground: "C8A86A" },
        { token: "invalid", foreground: "F87878", fontStyle: "underline" },
        { token: "invalid.deprecated", foreground: "C87878", fontStyle: "italic underline" },
        { token: "string.regexp", foreground: "D8A86A" },
        { token: "constant.regexp", foreground: "D8A86A" },
      ],
      colors: {
        "editor.background": "#0F1A14",
        "editor.foreground": "#F4F1EA",
        "editorCursor.foreground": "#A8D5A2",
        "editorCursor.background": "#0F1A14",
        "editor.selectionBackground": "#1E3A2F88",
        "editor.selectionHighlightBackground": "#1E3A2F44",
        "editor.inactiveSelectionBackground": "#1E3A2F55",
        "editor.wordHighlightBackground": "#3B201244",
        "editor.wordHighlightStrongBackground": "#3B201266",
        "editor.lineHighlightBackground": "#162D2360",
        "editor.lineHighlightBorder": "#1E3A2F00",
        "editorGutter.background": "#0C1610",
        "editorLineNumber.foreground": "#3A5240",
        "editorLineNumber.activeForeground": "#A8D5A2",
        "editor.findMatchBackground": "#C8A86A55",
        "editor.findMatchHighlightBackground": "#C8A86A30",
        "editor.findMatchBorder": "#C8A86A",
        "editorIndentGuide.background1": "#1E3A2F",
        "editorIndentGuide.activeBackground1": "#2E5A4F",
        "editorRuler.foreground": "#1E3A2F",
        "editorWhitespace.foreground": "#2E4A38",
        "editorBracketHighlight.foreground1": "#A8D5A2",
        "editorBracketHighlight.foreground2": "#C8A86A",
        "editorBracketHighlight.foreground3": "#C8D8A8",
        "editorBracketHighlight.unexpectedBracket.foreground": "#8B1A1A",
        "editorError.foreground": "#8B1A1A",
        "editorWarning.foreground": "#B87D2F",
        "editorInfo.foreground": "#6A9E7A",
        "scrollbar.shadow": "#00000000",
        "scrollbarSlider.background": "#1E3A2F88",
        "scrollbarSlider.hoverBackground": "#1E3A2FAA",
        "scrollbarSlider.activeBackground": "#1E3A2FCC",
        "minimap.background": "#0C1610",
        "minimap.selectionHighlight": "#1E3A2F",
        "minimapSlider.background": "#1E3A2F66",
        "minimapSlider.hoverBackground": "#1E3A2F99",
        "editorWidget.background": "#162D23",
        "editorWidget.border": "#0D0D0D",
        "editorWidget.foreground": "#F4F1EA",
        "editorSuggestWidget.background": "#162D23",
        "editorSuggestWidget.border": "#0D0D0D",
        "editorSuggestWidget.foreground": "#F4F1EA",
        "editorSuggestWidget.selectedBackground": "#1E3A2F",
        "editorSuggestWidget.selectedForeground": "#F4F1EA",
        "editorSuggestWidget.highlightForeground": "#A8D5A2",
        "editorHoverWidget.background": "#162D23",
        "editorHoverWidget.border": "#0D0D0D",
        "peekView.border": "#1E3A2F",
        "peekViewEditor.background": "#0F1A14",
        "peekViewResult.background": "#162D23",
        "peekViewTitle.background": "#1E3A2F",
        "peekViewTitleLabel.foreground": "#F4F1EA",
        "peekViewTitleDescription.foreground": "#A8D5A2",
        "editorOverviewRuler.border": "#0D0D0D",
        "editorOverviewRuler.errorForeground": "#8B1A1A",
        "editorOverviewRuler.warningForeground": "#B87D2F",
        "editorOverviewRuler.selectionHighlightForeground": "#1E3A2F",
      },
    });
  };

  const handleDrop = useCallback(
    async (acceptedFiles: File[]) => {
      const file = acceptedFiles[0];
      if (!file) return;

      setError(null);

      try {
        const parsed = await parseTraceFile(file);
        setFormat(parsed.format);
        onChange(parsed.raw);
        onParsed?.(parsed.data, parsed.format);
      } catch (err) {
        if (err instanceof TraceParseError) {
          setError(`Could not parse ${err.format.toUpperCase()} file: ${err.message}`);
        } else {
          setError("Could not read file.");
        }
      }
    },
    [onChange, onParsed],
  );

  const { getRootProps, getInputProps, isDragActive, open } = useDropzone({
    onDrop: handleDrop,
    accept: ACCEPTED_EXTENSIONS,
    multiple: false,
    noClick: true, // Prevents clicks on the editor workspace from launching the file opener
  });

  const handleEditorChange = (val: string | undefined) => {
    const text = val ?? "";
    onChange(text);

    if (text.length > MAX_TRACE_LENGTH) {
      setError(
        `Trace exceeds ${MAX_TRACE_LENGTH.toLocaleString()} characters (${text.length.toLocaleString()} found)`,
      );
      return;
    }

    if (!onParsed) {
      setError(null);
      return;
    }

    try {
      const data = parseTraceContent(text, format);
      setError(null);
      onParsed(data, format);
    } catch (err) {
      if (err instanceof TraceParseError) {
        setError(`Could not parse ${err.format.toUpperCase()}: ${err.message}`);
      }
    }
  };

  return (
    <div className="space-y-2">
      {/* Header Row */}
      <div className="flex items-center justify-between">
        <Label variant="default">
          AGENT TRACE <span className="text-destructive">*</span>
        </Label>
        <span
          style={{
            fontFamily: "var(--font-mono)",
            fontSize: "10px",
            color: overLimit ? "var(--destructive)" : "var(--muted-foreground)",
          }}
        >
          {value.length.toLocaleString()} / {MAX_TRACE_LENGTH.toLocaleString()}
        </span>
      </div>

      {/* Editor Frame */}
      <div {...getRootProps()} className="relative">
        <input {...getInputProps()} />

        {isDragActive && (
          <div
            className="absolute inset-0 flex items-center justify-center z-10 border-2 border-dashed border-[#A8D5A2]"
            style={{ background: "rgba(15,26,20,0.9)" }}
          >
            <span style={{ fontFamily: "var(--font-display)", color: "#A8D5A2", fontSize: "16px" }}>
              Drop file to load
            </span>
          </div>
        )}

        {/* Monaco Canvas Box */}
        <div className={`border-2 border-b-0 border-black rounded-t-sm overflow-hidden ${overLimit ? "border-destructive" : ""}`}>
          <Editor
            beforeMount={handleBeforeMount}
            theme="tracepilot-dark"
            height={height}
            defaultLanguage="json"
            value={value}
            onChange={handleEditorChange}
            options={{
              minimap: { enabled: false },
              fontSize: 14,
              wordWrap: "on",
              scrollBeyondLastLine: false,
            }}
          />
        </div>

        {/* Custom Figma Footer Panel */}
        <div className={`border-2 border-t-0 border-black bg-[#0F1A14] px-5 pb-3 pt-1 rounded-b-sm ${overLimit ? "border-destructive" : ""}`}>
          <div className="border-t border-black/30 mb-2">
            <div
              className="h-0.5 mt-1 transition-[width] duration-300"
              style={{ width: `${pct}%`, background: overLimit ? "var(--destructive)" : "#A8D5A2" }}
            />
          </div>
          <div className="flex items-center gap-3">
            <Button
              type="button"
              variant="ghost"
              size="sm"
              onClick={open} // File explorer trigger mapped uniquely here
              className="flex items-center gap-1.5 h-auto py-1.5 px-3 text-[10px] border border-black/40 font-mono hover:border-white/60 bg-white/5 hover:bg-white/10"
              style={{ color: "rgba(244,241,234,0.6)" }}
            >
              <Upload size={11} /> Upload file
            </Button>
            <Button
              type="button"
              variant="ghost"
              size="sm"
              onClick={() => handleEditorChange(SAMPLE_TRACE)}
              className="flex items-center gap-1.5 h-auto py-1.5 px-3 text-[10px] border border-black/40 font-mono hover:border-white/60 bg-white/5 hover:bg-white/10"
              style={{ color: "rgba(244,241,234,0.6)" }}
            >
              <FileText size={11} /> Load sample
            </Button>
            {value && (
              <Button
                type="button"
                variant="ghost"
                size="sm"
                onClick={() => handleEditorChange("")}
                className="flex items-center gap-1.5 h-auto py-1.5 px-3 text-[10px] border border-black/40 font-mono hover:border-white/60 bg-white/5 hover:bg-white/10"
                style={{ color: "rgba(244,241,234,0.5)" }}
              >
                Clear
              </Button>
            )}
          </div>
        </div>
      </div>

      {error && (
        <p style={{ fontSize: "12px", color: "#8B1A1A", marginTop: "4px" }}>
          {error}
        </p>
      )}
    </div>
  );
}