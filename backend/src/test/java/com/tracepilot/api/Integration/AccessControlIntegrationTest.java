package com.tracepilot.api.Integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import com.tracepilot.api.DTO.Response.AuthResponse;
import com.tracepilot.api.Entities.User;
import com.tracepilot.api.Enums.UserRoles;

class AccessControlIntegrationTest extends IntegrationTestBase {

    private String registerAndGetAccessToken(String email) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String body = """
                {"email":"%s","password":"SuperSecret123!","displayName":"Access Control Tester"}
                """.formatted(email);
        ResponseEntity<AuthResponse> response = restTemplate.postForEntity(
                "/api/v1/auth/register", new HttpEntity<>(body, headers), AuthResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return response.getBody().accessToken();
    }

    @Test
    void requestsWithNoAuthorizationHeader_areRejectedOnProtectedEndpoints() {
        ResponseEntity<String> response = restTemplate.getForEntity("/api/v1/audits", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void requestsWithAGarbageBearerToken_areRejected() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth("this-is-not-a-real-jwt");
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/audits", HttpMethod.GET, new HttpEntity<>(headers), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void publicEndpoints_remainReachableWithoutAuthentication() {
        ResponseEntity<String> health = restTemplate.getForEntity("/api/v1/health", String.class);
        assertThat(health.getStatusCode()).isIn(HttpStatus.OK, HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    void aRegularUser_isForbiddenFromAdminOnlyEndpoints() {
        String accessToken = registerAndGetAccessToken("access-user-" + UUID.randomUUID() + "@example.com");

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/admin/users", HttpMethod.GET,
                new HttpEntity<>(authHeaders(accessToken)), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void anAdminUser_canReachAdminOnlyEndpoints() {
        String email = "access-admin-" + UUID.randomUUID() + "@example.com";
        String accessToken = registerAndGetAccessToken(email);
        User user = userRepository.findByEmail(email).orElseThrow();
        user.setRole(UserRoles.ADMIN);
        userRepository.save(user);

        HttpHeaders loginHeaders = new HttpHeaders();
        loginHeaders.setContentType(MediaType.APPLICATION_JSON);
        String loginBody = """
                {"email":"%s","password":"SuperSecret123!"}
                """.formatted(email);
        ResponseEntity<AuthResponse> loginResponse = restTemplate.postForEntity(
                "/api/v1/auth/login", new HttpEntity<>(loginBody, loginHeaders), AuthResponse.class);
        assertThat(loginResponse.getBody().user().role()).isEqualTo("ADMIN");

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/admin/users", HttpMethod.GET,
                new HttpEntity<>(authHeaders(loginResponse.getBody().accessToken())), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private HttpHeaders authHeaders(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        return headers;
    }
}
