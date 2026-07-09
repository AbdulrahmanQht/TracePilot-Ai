package com.tracepilot.api.Util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import com.tracepilot.api.Exceptions.ApiException;

class TraceInputGuardTest {

    @Test
    void validate_throwsBadRequest_whenTraceIsNull() {
        assertThatThrownBy(() -> TraceInputGuard.validate(null))
                .isInstanceOf(ApiException.class)
                .hasMessage("Trace payload cannot be empty.")
                .extracting(ex -> ((ApiException) ex).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void validate_throwsBadRequest_whenTraceIsBlank() {
        assertThatThrownBy(() -> TraceInputGuard.validate("   "))
                .isInstanceOf(ApiException.class)
                .hasMessage("Trace payload cannot be empty.");
    }

    @Test
    void validate_throwsContentTooLarge_whenTraceExceedsMaxLength() {
        String oversized = "a".repeat(80_001);

        assertThatThrownBy(() -> TraceInputGuard.validate(oversized))
                .isInstanceOf(ApiException.class)
                .hasMessage("Trace payload exceeds the 80,000 character limit.")
                .extracting(ex -> ((ApiException) ex).getStatus())
                .isEqualTo(HttpStatus.CONTENT_TOO_LARGE);
    }

    @Test
    void validate_doesNotThrow_whenTraceIsAtExactLimit() {
        String exactly80k = "a".repeat(80_000);

        org.assertj.core.api.Assertions.assertThatCode(() -> TraceInputGuard.validate(exactly80k))
                .doesNotThrowAnyException();
    }

    @Test
    void validate_doesNotThrow_forNormalTrace() {
        org.assertj.core.api.Assertions.assertThatCode(() -> TraceInputGuard.validate("a normal trace payload"))
                .doesNotThrowAnyException();
    }

    @Test
    void computeNormalizedHash_isDeterministic_forIdenticalInput() {
        String trace = "Some Trace Content";

        String hash1 = TraceInputGuard.computeNormalizedHash(trace);
        String hash2 = TraceInputGuard.computeNormalizedHash(trace);

        assertThat(hash1).isEqualTo(hash2);
        assertThat(hash1).hasSize(64); // SHA-256 hex digest length
    }

    @Test
    void computeNormalizedHash_isCaseInsensitive() {
        String hashLower = TraceInputGuard.computeNormalizedHash("hello world");
        String hashUpper = TraceInputGuard.computeNormalizedHash("HELLO WORLD");

        assertThat(hashLower).isEqualTo(hashUpper);
    }

    @Test
    void computeNormalizedHash_normalizesWhitespace() {
        String hashCollapsed = TraceInputGuard.computeNormalizedHash("hello   world");
        String hashSingleSpace = TraceInputGuard.computeNormalizedHash("hello world");
        String hashNewlines = TraceInputGuard.computeNormalizedHash("hello\n\nworld");

        assertThat(hashCollapsed).isEqualTo(hashSingleSpace);
        assertThat(hashNewlines).isEqualTo(hashSingleSpace);
    }

    @Test
    void computeNormalizedHash_trimsLeadingAndTrailingWhitespace() {
        String hashPadded = TraceInputGuard.computeNormalizedHash("  hello world  ");
        String hashUnpadded = TraceInputGuard.computeNormalizedHash("hello world");

        assertThat(hashPadded).isEqualTo(hashUnpadded);
    }

    @Test
    void computeNormalizedHash_differsForDifferentContent() {
        String hash1 = TraceInputGuard.computeNormalizedHash("trace one");
        String hash2 = TraceInputGuard.computeNormalizedHash("trace two");

        assertThat(hash1).isNotEqualTo(hash2);
    }
}
