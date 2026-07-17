package com.tracepilot.api.Util;

import java.util.regex.Pattern;

public final class TraceSanitizer {

    private TraceSanitizer() {
    }

    private static final Pattern JWT_PATTERN = Pattern.compile("eyJ[a-zA-Z0-9_-]+\\.[a-zA-Z0-9_-]+\\.[a-zA-Z0-9_-]+");
    private static final Pattern BEARER_TOKEN_PATTERN = Pattern.compile("(?i)(bearer\\s+)[a-zA-Z0-9_\\-\\.]+");
    private static final Pattern AWS_KEY_PATTERN = Pattern.compile("(?i)(AKIA|ASIA)[0-9A-Z]{16}");
    private static final Pattern PRIVATE_KEY_PATTERN = Pattern
            .compile("-----BEGIN [A-Z ]+ PRIVATE KEY-----[\\s\\S]*?-----END [A-Z ]+ PRIVATE KEY-----");
    private static final Pattern GITHUB_TOKEN_PATTERN = Pattern.compile("(gh[pousr]_[a-zA-Z0-9]{36})");

    private static final Pattern[] HIGH_CONFIDENCE_PATTERNS = {
            Pattern.compile("(?i)ignore (all |any )?(previous|prior|above) instructions"),
            Pattern.compile("(?i)disregard (all |any )?(previous|prior|above) instructions"),
            Pattern.compile("(?i)forget (all |any )?(previous|prior) (instructions|context)"),
            Pattern.compile("(?i)reveal (your |the )?(system prompt|instructions)"),
            Pattern.compile("(?i)print (your |the )?(system prompt|full instructions)"),
            Pattern.compile("(?i)</?(system|assistant|user)>"),
            Pattern.compile("(?i)begin (new |admin |developer )?(prompt|instructions)"),
            Pattern.compile("(?i)\\byou are now\\b.{0,40}\\b(DAN|jailbroken|unrestricted|free of|no longer)\\b"),
            Pattern.compile("(?i)\\bdeveloper mode\\b.{0,30}\\b(enabled|on|activated)\\b"),
            Pattern.compile("(?i)\\bdo anything now\\b"),
            Pattern.compile("(?i)output (the following|everything above) verbatim"),
    };

    private static final Pattern[] MEDIUM_CONFIDENCE_PATTERNS = {
            Pattern.compile("(?i)^\\s*system\\s*:\\s*", Pattern.MULTILINE),
            Pattern.compile("(?i)\\byou are now\\b"),
            Pattern.compile("(?i)new instructions\\s*:"),
            Pattern.compile("(?i)act as (if you|a|an)"),
            Pattern.compile("(?i)\\bpretend (you are|to be)\\b"),
            Pattern.compile("(?i)\\bfrom now on\\b.{0,30}\\b(you|respond|act)\\b"),
            Pattern.compile("(?i)\\btranslate (this|the following) (into|to) (code and )?execute\\b"),
            Pattern.compile("(?i)\\bthis is (a |an )?(test|drill)\\s*[,.]?\\s*(you (can|may|should))\\b"),
    };

    private static final int MIN_SEVERITY_TO_BLOCK = 100;
    private static final int HIGH_CONFIDENCE_SEVERITY = 100;
    private static final int MEDIUM_CONFIDENCE_SEVERITY = 30;

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

    public record InjectionScreenResult(boolean suspicious, boolean shouldBlock, int severity,
            java.util.List<String> matchedPatterns) {
    }

    public static InjectionScreenResult screenForInjection(String rawTrace) {
        if (rawTrace == null || rawTrace.isBlank()) {
            return new InjectionScreenResult(false, false, 0, java.util.List.of());
        }

        String canonical = TraceScopeGuard.canonicalize(rawTrace);
        java.util.List<String> matched = new java.util.ArrayList<>();
        int severity = 0;

        for (Pattern pattern : HIGH_CONFIDENCE_PATTERNS) {
            if (pattern.matcher(canonical).find()) {
                matched.add(pattern.pattern());
                severity = Math.max(severity, HIGH_CONFIDENCE_SEVERITY);
            }
        }
        for (Pattern pattern : MEDIUM_CONFIDENCE_PATTERNS) {
            if (pattern.matcher(canonical).find()) {
                matched.add(pattern.pattern());
                severity += MEDIUM_CONFIDENCE_SEVERITY;
            }
        }
        severity = Math.min(severity, 100);

        boolean suspicious = !matched.isEmpty();
        boolean shouldBlock = severity >= MIN_SEVERITY_TO_BLOCK;
        return new InjectionScreenResult(suspicious, shouldBlock, severity, matched);
    }

    @Deprecated
    public static boolean detectInjectionAttempt(String rawTrace) {
        return screenForInjection(rawTrace).suspicious();
    }
}
