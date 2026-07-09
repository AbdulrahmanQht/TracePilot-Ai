package com.tracepilot.api.Util;

import java.util.regex.Pattern;

public final class TraceSanitizer {

    private TraceSanitizer() {
    }

    // Secret-redaction patterns
    private static final Pattern JWT_PATTERN = Pattern.compile("eyJ[a-zA-Z0-9_-]+\\.[a-zA-Z0-9_-]+\\.[a-zA-Z0-9_-]+");
    private static final Pattern BEARER_TOKEN_PATTERN = Pattern.compile("(?i)(bearer\\s+)[a-zA-Z0-9_\\-\\.]+");
    private static final Pattern AWS_KEY_PATTERN = Pattern.compile("(?i)(AKIA|ASIA)[0-9A-Z]{16}");
    private static final Pattern PRIVATE_KEY_PATTERN = Pattern
            .compile("-----BEGIN [A-Z ]+ PRIVATE KEY-----[\\s\\S]*?-----END [A-Z ]+ PRIVATE KEY-----");
    private static final Pattern GITHUB_TOKEN_PATTERN = Pattern.compile("(gh[pousr]_[a-zA-Z0-9]{36})");

    private static final Pattern[] INJECTION_PATTERNS = {
            Pattern.compile("(?i)ignore (all |any )?(previous|prior|above) instructions"),
            Pattern.compile("(?i)disregard (all |any )?(previous|prior|above) instructions"),
            Pattern.compile("(?i)system\\s*:\\s*"),
            Pattern.compile("(?i)you are now"),
            Pattern.compile("(?i)new instructions\\s*:"),
            Pattern.compile("(?i)forget (all |any )?(previous|prior) (instructions|context)"),
            Pattern.compile("(?i)act as (if you|a|an)"),
            Pattern.compile("(?i)reveal (your |the )?(system prompt|instructions)"),
            Pattern.compile("</?(system|assistant|user)>"),
            Pattern.compile("(?i)begin (new |admin |developer )?(prompt|instructions)"),
    };

    public static String redactSecrets(String rawTrace) {
        if (rawTrace == null || rawTrace.isBlank()) {
            return rawTrace;
        }

        String sanitized = rawTrace;
        sanitized = JWT_PATTERN.matcher(sanitized).replaceAll("[REDACTED_JWT]");
        sanitized = BEARER_TOKEN_PATTERN.matcher(sanitized).replaceAll("$1[REDACTED_TOKEN]");
        sanitized = AWS_KEY_PATTERN.matcher(sanitized).replaceAll("[REDACTED_AWS_KEY]");
        sanitized = PRIVATE_KEY_PATTERN.matcher(sanitized).replaceAll("[REDACTED_PRIVATE_KEY]");
        sanitized = GITHUB_TOKEN_PATTERN.matcher(sanitized).replaceAll("[REDACTED_GITHUB_TOKEN]");

        return sanitized;
    }

    public static boolean detectInjectionAttempt(String rawTrace) {
        if (rawTrace == null || rawTrace.isBlank()) {
            return false;
        }
        for (Pattern pattern : INJECTION_PATTERNS) {
            if (pattern.matcher(rawTrace).find()) {
                return true;
            }
        }
        return false;
    }
}