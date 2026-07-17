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

class AuthFlowIntegrationTest extends IntegrationTestBase {

    private String uniqueEmail() {
        return "it-" + UUID.randomUUID() + "@example.com";
    }

    private HttpEntity<String> jsonBody(String json) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(json, headers);
    }
    private String extractRefreshCookie(ResponseEntity<?> response) {
        String setCookie = response.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
        assertThat(setCookie).as("Set-Cookie header must be present").isNotNull();
        assertThat(setCookie).contains("refreshToken=");
        return setCookie.split(";", 2)[0];
    }

    @Test
    void registerThenLogin_returnsMatchingUser() {
        String email = uniqueEmail();
        String registerBody = """
                {"email":"%s","password":"SuperSecret123!","displayName":"Integration Tester"}
                """.formatted(email);

        ResponseEntity<AuthResponse> registerResponse = restTemplate.postForEntity(
                "/api/v1/auth/register", jsonBody(registerBody), AuthResponse.class);

        assertThat(registerResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(registerResponse.getBody()).isNotNull();
        assertThat(registerResponse.getBody().user().email()).isEqualTo(email);
        assertThat(registerResponse.getBody().user().role()).isEqualTo("USER");
        assertThat(registerResponse.getBody().accessToken()).isNotBlank();
        extractRefreshCookie(registerResponse);

        String loginBody = """
                {"email":"%s","password":"SuperSecret123!"}
                """.formatted(email);
        ResponseEntity<AuthResponse> loginResponse = restTemplate.postForEntity(
                "/api/v1/auth/login", jsonBody(loginBody), AuthResponse.class);

        assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(loginResponse.getBody()).isNotNull();
        assertThat(loginResponse.getBody().user().id()).isEqualTo(registerResponse.getBody().user().id());
    }

    @Test
    void registeringTheSameEmailTwice_isRejectedWithConflict() {
        String email = uniqueEmail();
        String body = """
                {"email":"%s","password":"SuperSecret123!","displayName":"Dup"}
                """.formatted(email);

        ResponseEntity<AuthResponse> first = restTemplate.postForEntity(
                "/api/v1/auth/register", jsonBody(body), AuthResponse.class);
        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<String> second = restTemplate.postForEntity(
                "/api/v1/auth/register", jsonBody(body), String.class);
        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void loginWithWrongPassword_isRejected() {
        String email = uniqueEmail();
        String registerBody = """
                {"email":"%s","password":"SuperSecret123!","displayName":"Wrong Pw"}
                """.formatted(email);
        restTemplate.postForEntity("/api/v1/auth/register", jsonBody(registerBody), AuthResponse.class);

        String badLogin = """
                {"email":"%s","password":"totally-wrong"}
                """.formatted(email);
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/v1/auth/login", jsonBody(badLogin), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void refreshToken_issuesANewAccessTokenAndRotatesTheRefreshCookie() {
        String email = uniqueEmail();
        String registerBody = """
                {"email":"%s","password":"SuperSecret123!","displayName":"Refresher"}
                """.formatted(email);
        ResponseEntity<AuthResponse> registerResponse = restTemplate.postForEntity(
                "/api/v1/auth/register", jsonBody(registerBody), AuthResponse.class);
        String refreshCookie = extractRefreshCookie(registerResponse);

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.COOKIE, refreshCookie);
        ResponseEntity<AuthResponse> refreshResponse = restTemplate.exchange(
                "/api/v1/auth/refresh", HttpMethod.POST, new HttpEntity<>(headers), AuthResponse.class);

        assertThat(refreshResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(refreshResponse.getBody()).isNotNull();
        assertThat(refreshResponse.getBody().accessToken()).isNotEqualTo(registerResponse.getBody().accessToken());

        String rotatedCookie = extractRefreshCookie(refreshResponse);
        assertThat(rotatedCookie).isNotEqualTo(refreshCookie);

        HttpHeaders staleHeaders = new HttpHeaders();
        staleHeaders.add(HttpHeaders.COOKIE, refreshCookie);
        ResponseEntity<String> staleRefresh = restTemplate.exchange(
                "/api/v1/auth/refresh", HttpMethod.POST, new HttpEntity<>(staleHeaders), String.class);
        assertThat(staleRefresh.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void logout_revokesTheRefreshTokenSoItCanNoLongerBeUsed() {
        String email = uniqueEmail();
        String registerBody = """
                {"email":"%s","password":"SuperSecret123!","displayName":"Logout Tester"}
                """.formatted(email);
        ResponseEntity<AuthResponse> registerResponse = restTemplate.postForEntity(
                "/api/v1/auth/register", jsonBody(registerBody), AuthResponse.class);
        String refreshCookie = extractRefreshCookie(registerResponse);

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.COOKIE, refreshCookie);
        ResponseEntity<Void> logoutResponse = restTemplate.exchange(
                "/api/v1/auth/logout", HttpMethod.POST, new HttpEntity<>(headers), Void.class);
        assertThat(logoutResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        ResponseEntity<String> refreshAfterLogout = restTemplate.exchange(
                "/api/v1/auth/refresh", HttpMethod.POST, new HttpEntity<>(headers), String.class);
        assertThat(refreshAfterLogout.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void changePassword_requiresTheCorrectCurrentPasswordAndInvalidatesOldSessions() {
        String email = uniqueEmail();
        String registerBody = """
                {"email":"%s","password":"OldPassword123!","displayName":"Password Changer"}
                """.formatted(email);
        ResponseEntity<AuthResponse> registerResponse = restTemplate.postForEntity(
                "/api/v1/auth/register", jsonBody(registerBody), AuthResponse.class);
        String accessToken = registerResponse.getBody().accessToken();
        String refreshCookie = extractRefreshCookie(registerResponse);

        HttpHeaders authHeaders = new HttpHeaders();
        authHeaders.setContentType(MediaType.APPLICATION_JSON);
        authHeaders.setBearerAuth(accessToken);

        String wrongCurrent = """
                {"currentPassword":"not-the-real-password","newPassword":"NewPassword123!"}
                """;
        ResponseEntity<String> rejected = restTemplate.exchange(
                "/api/v1/auth/change-password", HttpMethod.POST,
                new HttpEntity<>(wrongCurrent, authHeaders), String.class);
        assertThat(rejected.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        String correctChange = """
                {"currentPassword":"OldPassword123!","newPassword":"NewPassword123!"}
                """;
        ResponseEntity<Void> changed = restTemplate.exchange(
                "/api/v1/auth/change-password", HttpMethod.POST,
                new HttpEntity<>(correctChange, authHeaders), Void.class);
        assertThat(changed.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        HttpHeaders cookieHeaders = new HttpHeaders();
        cookieHeaders.add(HttpHeaders.COOKIE, refreshCookie);
        ResponseEntity<String> staleRefresh = restTemplate.exchange(
                "/api/v1/auth/refresh", HttpMethod.POST, new HttpEntity<>(cookieHeaders), String.class);
        assertThat(staleRefresh.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        String oldLogin = """
                {"email":"%s","password":"OldPassword123!"}
                """.formatted(email);
        ResponseEntity<String> oldLoginResponse = restTemplate.postForEntity(
                "/api/v1/auth/login", jsonBody(oldLogin), String.class);
        assertThat(oldLoginResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        // the new password works
        String newLogin = """
                {"email":"%s","password":"NewPassword123!"}
                """.formatted(email);
        ResponseEntity<AuthResponse> newLoginResponse = restTemplate.postForEntity(
                "/api/v1/auth/login", jsonBody(newLogin), AuthResponse.class);
        assertThat(newLoginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
