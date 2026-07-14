import * as yaml from "js-yaml";

export type TraceFormat = "json" | "jsonl" | "yaml" | "text";

export interface ParsedTrace {
  format: TraceFormat;
  raw: string;
  data: unknown;
}

export class TraceParseError extends Error {
  constructor(
    message: string,
    public readonly format: TraceFormat,
    public readonly line?: number,
  ) {
    super(message);
    this.name = "TraceParseError";
  }
}

export function readFileAsText(file: File, encoding = "UTF-8"): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => resolve(reader.result as string);
    reader.onerror = () => reject(reader.error);
    reader.readAsText(file, encoding);
  });
}

export function detectTraceFormat(filename: string): TraceFormat {
  const ext = filename.split(".").pop()?.toLowerCase();

  switch (ext) {
    case "json":
      return "json";
    case "jsonl":
    case "ndjson":
      return "jsonl";
    case "yaml":
    case "yml":
      return "yaml";
    default:
      return "text";
  }
}

export function parseTraceContent(rawText: string, format: TraceFormat): unknown {
  switch (format) {
    case "json":
      return parseJson(rawText);
    case "jsonl":
      return parseJsonl(rawText);
    case "yaml":
      return parseYaml(rawText);
    case "text":
      return rawText;
  }
}

function parseJson(rawText: string): unknown {
  try {
    return JSON.parse(rawText);
  } catch (err) {
    throw new TraceParseError(
      `Invalid JSON: ${err instanceof Error ? err.message : "unknown parse error"}`,
      "json",
    );
  }
}

function parseJsonl(rawText: string): unknown[] {
  const lines = rawText.split("\n").filter((line) => line.trim().length > 0);
  const results: unknown[] = [];

  lines.forEach((line, index) => {
    try {
      results.push(JSON.parse(line));
    } catch (err) {
      throw new TraceParseError(
        `Invalid JSON on line ${index + 1}: ${err instanceof Error ? err.message : "unknown parse error"}`,
        "jsonl",
        index + 1,
      );
    }
  });

  return results;
}

function parseYaml(rawText: string): unknown {
  try {
    return yaml.load(rawText);
  } catch (err) {
    throw new TraceParseError(
      `Invalid YAML: ${err instanceof Error ? err.message : "unknown parse error"}`,
      "yaml",
    );
  }
}

export const MAX_TRACE_LENGTH = 80_000; // matches AuditRequest.rawTrace @Size(max = 80000)
const MAX_FILE_BYTES = MAX_TRACE_LENGTH * 4; // worst case: 4 bytes/char in UTF-8

const SUPPORTED_EXTENSIONS = new Set([
  "txt",
  "log",
  "json",
  "jsonl",
  "ndjson",
  "yaml",
  "yml",
]);

export function isSupportedTraceFile(filename: string): boolean {
  const ext = filename.split(".").pop()?.toLowerCase();
  return ext !== undefined && SUPPORTED_EXTENSIONS.has(ext);
}

export async function parseTraceFile(file: File, encoding = "UTF-8"): Promise<ParsedTrace> {
  if (file.size > MAX_FILE_BYTES) {
    throw new TraceParseError(
      `File too large (${(file.size / 1024).toFixed(0)}KB, max ~${(MAX_FILE_BYTES / 1024).toFixed(0)}KB)`,
      "text",
    );
  }

  if (!isSupportedTraceFile(file.name)) {
    throw new TraceParseError(
      "Unsupported file type. Supported formats: .txt, .log, .json, .jsonl, .ndjson, .yaml, .yml",
      "text",
    );
  }

  const raw = (await readFileAsText(file, encoding)).replace(/^\uFEFF/, "");

  if (raw.length > MAX_TRACE_LENGTH) {
    throw new TraceParseError(
      `Trace exceeds ${MAX_TRACE_LENGTH.toLocaleString()} characters`,
      "text",
    );
  }

  if (raw.includes("\0")) {
    throw new TraceParseError("File appears to be binary, not text", "text");
  }

  return {
    format: detectTraceFormat(file.name),
    raw,
    data: raw,
  };
}