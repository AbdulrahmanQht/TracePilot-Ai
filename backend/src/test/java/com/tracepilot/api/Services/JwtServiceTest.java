package com.tracepilot.api.Services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Date;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import com.tracepilot.api.Config.JwtConfig;
import com.tracepilot.api.Entities.User;
import com.tracepilot.api.Enums.UserRoles;
import com.tracepilot.api.Exceptions.ApiException;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

class JwtServiceTest {

    private static final String SECRET = "test-signing-key-that-is-definitely-long-enough-for-hs256-1234567890";

    private JwtService jwtService;
    private JwtConfig jwtConfig;
    private User user;

    @BeforeEach
    void setUp() {
        jwtConfig = new JwtConfig(SECRET, 900_000L, 7);
        jwtService = new JwtService(jwtConfig);

        user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("abdulrahman@example.com");
        user.setDisplayName("Abdulrahman");
        user.setRole(UserRoles.USER);
    }

    @Test
    void generateAccessToken_producesTokenWithExpectedClaims() {
        String token = jwtService.generateAccessToken(user);

        assertThat(token).isNotBlank();

        Claims claims = jwtService.extractAllClaims(token);
        assertThat(claims.getSubject()).isEqualTo(user.getId().toString());
        assertThat(claims.getIssuer()).isEqualTo("tracepilot-api");
        assertThat(claims.get("role", String.class)).isEqualTo("USER");
        assertThat(claims.get("email", String.class)).isEqualTo("abdulrahman@example.com");
        assertThat(claims.get("displayName", String.class)).isEqualTo("Abdulrahman");
    }

    @Test
    void calculateExpiryDate_isInTheFuture_byConfiguredWindow() {
        long before = System.currentTimeMillis();
        Date expiry = jwtService.calculateExpiryDate();
        long after = System.currentTimeMillis();

        assertThat(expiry.getTime()).isBetween(before + jwtConfig.accessExpiryMs() - 1000,
                after + jwtConfig.accessExpiryMs() + 1000);
    }

    @Test
    void extractUserId_returnsUuidFromSubjectClaim() {
        String token = jwtService.generateAccessToken(user);
        Claims claims = jwtService.extractAllClaims(token);

        assertThat(jwtService.extractUserId(claims)).isEqualTo(user.getId());
    }

    @Test
    void extractEmail_returnsEmailClaim() {
        String token = jwtService.generateAccessToken(user);
        Claims claims = jwtService.extractAllClaims(token);

        assertThat(jwtService.extractEmail(claims)).isEqualTo(user.getEmail());
    }

    @Test
    void extractRole_returnsRoleClaimAsEnum() {
        String token = jwtService.generateAccessToken(user);
        Claims claims = jwtService.extractAllClaims(token);

        assertThat(jwtService.extractRole(claims)).isEqualTo(UserRoles.USER);
    }

    @Test
    void validateAccessToken_returnsClaims_forValidToken() {
        String token = jwtService.generateAccessToken(user);

        Claims claims = jwtService.validateAccessToken(token);

        assertThat(claims.getSubject()).isEqualTo(user.getId().toString());
    }

    @Test
    void validateAccessToken_throwsUnauthorized_whenTokenIsNull() {
        assertThatThrownBy(() -> jwtService.validateAccessToken(null))
                .isInstanceOf(ApiException.class)
                .hasMessage("Access token is missing")
                .extracting(ex -> ((ApiException) ex).getStatus())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void validateAccessToken_throwsUnauthorized_whenTokenIsBlank() {
        assertThatThrownBy(() -> jwtService.validateAccessToken("   "))
                .isInstanceOf(ApiException.class)
                .hasMessage("Access token is missing");
    }

    @Test
    void validateAccessToken_throwsUnauthorized_whenTokenIsMalformed() {
        assertThatThrownBy(() -> jwtService.validateAccessToken("not-a-real-jwt"))
                .isInstanceOf(ApiException.class)
                .hasMessage("Invalid access token")
                .extracting(ex -> ((ApiException) ex).getStatus())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void validateAccessToken_throwsUnauthorized_whenTokenIsExpired() {
        String expiredToken = Jwts.builder()
                .subject(user.getId().toString())
                .issuer("tracepilot-api")
                .issuedAt(new Date(System.currentTimeMillis() - 20_000))
                .expiration(new Date(System.currentTimeMillis() - 10_000))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(java.nio.charset.StandardCharsets.UTF_8)))
                .compact();

        assertThatThrownBy(() -> jwtService.validateAccessToken(expiredToken))
                .isInstanceOf(ApiException.class)
                .hasMessage("Access token has expired")
                .extracting(ex -> ((ApiException) ex).getStatus())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void validateAccessToken_throwsUnauthorized_whenIssuerIsWrong() {
        String wrongIssuerToken = Jwts.builder()
                .subject(user.getId().toString())
                .issuer("some-other-service")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(java.nio.charset.StandardCharsets.UTF_8)))
                .compact();

        assertThatThrownBy(() -> jwtService.validateAccessToken(wrongIssuerToken))
                .isInstanceOf(ApiException.class)
                .hasMessage("Invalid access token")
                .extracting(ex -> ((ApiException) ex).getStatus())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void validateAccessToken_throwsUnauthorized_whenSignedWithDifferentKey() {
        String differentKeyToken = Jwts.builder()
                .subject(user.getId().toString())
                .issuer("tracepilot-api")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(Keys.hmacShaKeyFor(
                        "a-completely-different-signing-key-value-1234567890abcdef".getBytes(
                                java.nio.charset.StandardCharsets.UTF_8)))
                .compact();

        assertThatThrownBy(() -> jwtService.validateAccessToken(differentKeyToken))
                .isInstanceOf(ApiException.class)
                .hasMessage("Invalid access token");
    }
}
