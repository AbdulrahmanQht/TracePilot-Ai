package com.tracepilot.api.Util;

import java.text.Normalizer;
import java.util.regex.Pattern;

import org.springframework.http.HttpStatus;

import com.tracepilot.api.Exceptions.ApiException;

public final class TraceScopeGuard {

    private TraceScopeGuard() {
    }

    private static final int MIN_TRACE_CHARS = 40;

    private static final Pattern[] TRACE_SIGNALS = {
            Pattern.compile("(?m)^\\s*[$#>]\\s+\\S+"), // shell prompts
            Pattern.compile("(?i)\\b(npm|pip|pytest|jest|gradle|mvn|go test|cargo|junit)\\b"),
            Pattern.compile("(?i)\\b(traceback|stack trace|exception|error:|exit code)\\b"),
            Pattern.compile("(?i)\\b(diff --git|\\+\\+\\+ b/|--- a/|@@ -\\d)"),
            Pattern.compile("(?i)\\b(tool_call|function_call|\"role\"\\s*:\\s*\"(tool|assistant)\")\\b"),
            Pattern.compile("(?i)\\b(passed|failed|assert(ion)?|test_case|it\\(['\"])\\b"),
            Pattern.compile("(?m)^[\\w./-]+\\.(java|py|js|ts|tsx|jsx|go|rs|rb|cpp|c|json|yaml|yml):\\d+"), // file:line
            Pattern.compile("\\b\\d{4}-\\d{2}-\\d{2}[T ]\\d{2}:\\d{2}:\\d{2}\\b"), // timestamps
    };

    private static final Pattern[] OFF_TOPIC_SIGNALS = {
            Pattern.compile("(?i)\\b(write me a (poem|story|essay|song))\\b"),
            Pattern.compile("(?i)\\b(what('?s| is) your (opinion|favorite))\\b"),
            Pattern.compile("(?i)\\b(help me with my (homework|essay|resume))\\b"),
            Pattern.compile("(?i)\\b(recipe for|how to cook)\\b"),
    };

    private static final int MIN_SIGNAL_MATCHES = 2;


    public static String canonicalize(String text) {
        if (text == null) {
            return null;
        }
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFKC);
        return normalized.replaceAll("[\\u200B-\\u200F\\u202A-\\u202E\\uFEFF\\u00AD]", "");
    }

    public static void validateLooksLikeTrace(String rawTrace) {
        String canonical = canonicalize(rawTrace);

        if (canonical == null || canonical.strip().length() < MIN_TRACE_CHARS) {
            throw new ApiException(
                    "Trace payload is too short to be a coding-agent execution trace.",
                    HttpStatus.BAD_REQUEST);
        }

        int matches = 0;
        for (Pattern p : TRACE_SIGNALS) {
            if (p.matcher(canonical).find()) {
                matches++;
            }
        }

        boolean offTopic = false;
        for (Pattern p : OFF_TOPIC_SIGNALS) {
            if (p.matcher(canonical).find()) {
                offTopic = true;
                break;
            }
        }

        if (matches < MIN_SIGNAL_MATCHES || offTopic) {
            throw new ApiException(
                    "This doesn't look like a coding-agent execution trace. TracePilot audits "
                            + "coding-agent execution logs (commands, diffs, test output, tool calls) only.",
                    HttpStatus.UNPROCESSABLE_ENTITY);
        }
    }
}
