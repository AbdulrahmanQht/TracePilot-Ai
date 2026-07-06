package com.tracepilot.api.Util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.springframework.http.HttpStatus;

import com.tracepilot.api.Exceptions.ApiException;

public final class TraceInputGuard {

    private static final int MAX_TRACE_CHARS = 80_000;

    private TraceInputGuard() {
    }

    public static void validate(String rawTrace) {
        if (rawTrace == null || rawTrace.isBlank()) {
            throw new ApiException("Trace payload cannot be empty.", HttpStatus.BAD_REQUEST);
        }
        if (rawTrace.length() > MAX_TRACE_CHARS) {
            throw new ApiException("Trace payload exceeds the 80,000 character limit.", HttpStatus.PAYLOAD_TOO_LARGE);
        }
    }

    public static String computeNormalizedHash(String rawTrace) {
        // Normalize whitespace and casing to ensure identical runs generate the same hash
        String normalized = rawTrace.trim().replaceAll("\\s+", " ").toLowerCase();
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(normalized.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }
}