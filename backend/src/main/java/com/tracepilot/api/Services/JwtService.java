package com.tracepilot.api.Services;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.tracepilot.api.Config.JwtConfig;
import com.tracepilot.api.Entities.User;
import javax.crypto.SecretKey;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import lombok.extern.slf4j.Slf4j;

import com.tracepilot.api.Exceptions.ApiException;
import com.tracepilot.api.Enums.UserRoles;
import org.springframework.http.HttpStatus;

@Service
@Slf4j
public class JwtService {
    private static final String ISSUER = "tracepilot-api";
    private final JwtConfig jwtConfig;
    private final SecretKey signingKey;

    public JwtService(JwtConfig jwtConfig) {
        this.jwtConfig = jwtConfig;
        this.signingKey = Keys.hmacShaKeyFor(jwtConfig.secret().getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", user.getRole().name());
        claims.put("email", user.getEmail());
        claims.put("displayName", user.getDisplayName());

        return Jwts.builder()
                .id(java.util.UUID.randomUUID().toString())
                .claims(claims)
                .subject(user.getId().toString())
                .issuer(ISSUER)
                .issuedAt(new Date())
                .expiration(calculateExpiryDate())
                .signWith(signingKey)
                .compact();
    }

    public Date calculateExpiryDate() {
        long currentTimeMillis = System.currentTimeMillis();
        long expiryTimeMillis = currentTimeMillis + jwtConfig.accessExpiryMs();
        return new Date(expiryTimeMillis);
    }

    public Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public java.util.UUID extractUserId(Claims claims) {
        return java.util.UUID.fromString(claims.getSubject());
    }

    public String extractEmail(Claims claims) {
        return claims.get("email", String.class);
    }

    public UserRoles extractRole(Claims claims) {
        return UserRoles.valueOf(claims.get("role", String.class));
    }

    public Claims validateAccessToken(String token) {
        if (token == null || token.isBlank()) {
            log.warn("JWT validation failed: Token is null or empty");
            throw new ApiException("Access token is missing", HttpStatus.UNAUTHORIZED);
        }
        try {
            Claims claims = extractAllClaims(token);

            if (!ISSUER.equals(claims.getIssuer())) {
                log.warn("JWT validation failed: Invalid issuer");
                throw new ApiException("Invalid access token", HttpStatus.UNAUTHORIZED);
            }

            return claims;
        } catch (ExpiredJwtException e) {
            log.warn("JWT validation failed: Token has expired");
            throw new ApiException("Access token has expired", HttpStatus.UNAUTHORIZED);
        } catch (JwtException e) {
            log.warn("JWT validation failed", e);
            throw new ApiException("Invalid access token", HttpStatus.UNAUTHORIZED);
        }
    }

}