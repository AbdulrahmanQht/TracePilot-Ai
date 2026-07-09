package com.tracepilot.api.Exceptions;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleApiException_mapsStatusAndMessage() {
        ApiException exception = new ApiException("Audit not found", HttpStatus.NOT_FOUND);

        ResponseEntity<ErrorResponse> response = handler.handleApiException(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Audit not found");
        assertThat(response.getBody().status()).isEqualTo(HttpStatus.NOT_FOUND.value());
    }

    @Test
    void handleApiException_includesTimestampCloseToNow() {
        ApiException exception = new ApiException("Boom", HttpStatus.BAD_REQUEST);
        java.time.Instant before = java.time.Instant.now();

        ResponseEntity<ErrorResponse> response = handler.handleApiException(exception);

        assertThat(response.getBody().timestamp()).isAfterOrEqualTo(before.minusSeconds(1));
    }
}
