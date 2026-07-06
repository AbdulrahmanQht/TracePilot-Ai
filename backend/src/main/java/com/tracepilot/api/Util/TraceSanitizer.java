package com.tracepilot.api.Util;

import java.util.regex.Pattern;

public final class TraceSanitizer {

    private TraceSanitizer() {
    }

    // Regex patterns for common secrets
    private static final Pattern JWT_PATTERN = Pattern.compile("eyJ[a-zA-Z0-9_-]+\\.[a-zA-Z0-9_-]+\\.[a-zA-Z0-9_-]+");
    private static final Pattern BEARER_TOKEN_PATTERN = Pattern.compile("(?i)(bearer\\s+)[a-zA-Z0-9_\\-\\.]+");
    private static final Pattern AWS_KEY_PATTERN = Pattern.compile("(?i)(AKIA|ASIA)[0-9A-Z]{16}");
    private static final Pattern PRIVATE_KEY_PATTERN = Pattern.compile("-----BEGIN [A-Z ]+ PRIVATE KEY-----[\\s\\S]*?-----END [A-Z ]+ PRIVATE KEY-----");
    private static final Pattern GITHUB_TOKEN_PATTERN = Pattern.compile("(gh[pousr]_[a-zA-Z0-9]{36})");

    public static String redactSecrets(String rawTrace) {
        if (rawTrace == null || rawTrace.isBlank()) {
            return rawTrace;
        }

        String sanitized = rawTrace;

        // Mask JWTs
        sanitized = JWT_PATTERN.matcher(sanitized).replaceAll("[REDACTED_JWT]");

        // Mask generic Bearer tokens (keeps the "Bearer " prefix for context, masks the token)
        sanitized = BEARER_TOKEN_PATTERN.matcher(sanitized).replaceAll("$1[REDACTED_TOKEN]");

        // Mask AWS Keys
        sanitized = AWS_KEY_PATTERN.matcher(sanitized).replaceAll("[REDACTED_AWS_KEY]");

        // Mask Private Keys
        sanitized = PRIVATE_KEY_PATTERN.matcher(sanitized).replaceAll("[REDACTED_PRIVATE_KEY]");

        // Mask GitHub Tokens
        sanitized = GITHUB_TOKEN_PATTERN.matcher(sanitized).replaceAll("[REDACTED_GITHUB_TOKEN]");

        sanitized = sanitized.replaceAll("(?i)(ignore previous instructions|system prompt|you are a)",
                "[REDACTED_PROMPT_INJECTION_ATTEMPT]");

        return sanitized;
    }
}