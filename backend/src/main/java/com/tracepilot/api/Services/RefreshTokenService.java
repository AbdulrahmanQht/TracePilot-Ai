package com.tracepilot.api.Services;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tracepilot.api.Config.JwtConfig;
import com.tracepilot.api.Entities.RefreshToken;
import com.tracepilot.api.Entities.User;
import com.tracepilot.api.Exceptions.ApiException;
import com.tracepilot.api.Repositories.RefreshTokenRepository;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class RefreshTokenService {
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtConfig jwtConfig;
    private final SecureRandom secureRandom = new SecureRandom();

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository, JwtConfig jwtConfig) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtConfig = jwtConfig;
    }

    @Transactional
    public String issue(User user) {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);

        String tokenHash = hashToken(rawToken);

        var refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setTokenHash(tokenHash);
        refreshToken.setRevoked(false);

        Instant expiresAt = Instant.now().plus(jwtConfig.refreshExpiryDays(), ChronoUnit.DAYS);
        refreshToken.setExpiresAt(expiresAt);

        refreshTokenRepository.save(refreshToken);
        log.info("Successfully issued new refresh token for user identity: {}", user.getId());

        return rawToken;
    }

    public String hashToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            log.warn("Refresh token hashing failed: raw token is null or empty");
            throw new ApiException("Refresh token is missing", HttpStatus.UNAUTHORIZED);
        }
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            log.error("SHA-256 algorithm unavailable in JVM runtime", e);
            throw new ApiException("Unable to process refresh token", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public RefreshToken validate(String rawToken) {
        String computedHash = hashToken(rawToken);

        RefreshToken token = refreshTokenRepository.findByTokenHash(computedHash)
                .orElseThrow(() -> {
                    log.warn("Refresh token validation failed: no matching hash found");
                    return new ApiException("Invalid refresh token", HttpStatus.UNAUTHORIZED);
                });

        if (token.getRevoked()) {
            log.warn("Refresh token validation failed: token already revoked -> user {}", token.getUser().getId());
            throw new ApiException("Refresh token has been revoked", HttpStatus.UNAUTHORIZED);
        }

        if (token.getExpiresAt().isBefore(Instant.now())) {
            log.warn("Refresh token validation failed: token expired -> user {}", token.getUser().getId());
            throw new ApiException("Refresh token has expired", HttpStatus.UNAUTHORIZED);
        }

        return token;
    }

    @Transactional
    public String rotate(RefreshToken oldToken) {
        log.info("Rotating refresh token id: {}", oldToken.getId());

        revoke(oldToken);

        return issue(oldToken.getUser());
    }

    @Transactional
    public void revoke(RefreshToken token) {
        token.setRevoked(true);
        refreshTokenRepository.save(token);
        log.debug("Revoked refresh token id: {}", token.getId());
    }

    @Transactional
    public void revokeAllForUser(UUID userId) {
        List<RefreshToken> activeTokens = refreshTokenRepository.findByUserIdAndRevokedFalse(userId);
        if (!activeTokens.isEmpty()) {
            activeTokens.forEach(token -> token.setRevoked(true));
            refreshTokenRepository.saveAll(activeTokens);
            log.info("Revoked {} active refresh tokens for user: {}", activeTokens.size(), userId);
        }
    }
}