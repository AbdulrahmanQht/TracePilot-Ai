package com.tracepilot.api.Services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import com.tracepilot.api.Config.JwtConfig;
import com.tracepilot.api.Entities.RefreshToken;
import com.tracepilot.api.Entities.User;
import com.tracepilot.api.Exceptions.ApiException;
import com.tracepilot.api.Repositories.RefreshTokenRepository;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    private RefreshTokenService refreshTokenService;
    private User user;

    @BeforeEach
    void setUp() {
        JwtConfig jwtConfig = new JwtConfig("secret", 900_000L, 7, true);
        refreshTokenService = new RefreshTokenService(refreshTokenRepository, jwtConfig);

        user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("abdulrahman@example.com");
    }

    @Test
    void issue_savesTokenWithHashedValue_andReturnsRawToken() {
        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        when(refreshTokenRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

        String rawToken = refreshTokenService.issue(user);

        assertThat(rawToken).isNotBlank();

        RefreshToken saved = captor.getValue();
        assertThat(saved.getUser()).isEqualTo(user);
        assertThat(saved.getRevoked()).isFalse();
        assertThat(saved.getTokenHash()).isEqualTo(refreshTokenService.hashToken(rawToken));
        assertThat(saved.getTokenHash()).isNotEqualTo(rawToken);
        assertThat(saved.getExpiresAt()).isAfter(Instant.now());
    }

    @Test
    void issue_generatesDifferentTokens_onSuccessiveCalls() {
        when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        String token1 = refreshTokenService.issue(user);
        String token2 = refreshTokenService.issue(user);

        assertThat(token1).isNotEqualTo(token2);
    }

    @Test
    void hashToken_isDeterministic() {
        String hash1 = refreshTokenService.hashToken("raw-token-value");
        String hash2 = refreshTokenService.hashToken("raw-token-value");

        assertThat(hash1).isEqualTo(hash2);
        assertThat(hash1).hasSize(64); // SHA-256 hex digest
    }

    @Test
    void hashToken_throwsUnauthorized_whenTokenIsNullOrBlank() {
        assertThatThrownBy(() -> refreshTokenService.hashToken(null))
                .isInstanceOf(ApiException.class)
                .hasMessage("Refresh token is missing")
                .extracting(ex -> ((ApiException) ex).getStatus())
                .isEqualTo(HttpStatus.UNAUTHORIZED);

        assertThatThrownBy(() -> refreshTokenService.hashToken("  "))
                .isInstanceOf(ApiException.class)
                .hasMessage("Refresh token is missing");
    }

    @Test
    void validate_returnsToken_whenValidAndNotExpiredOrRevoked() {
        RefreshToken token = new RefreshToken();
        token.setUser(user);
        token.setRevoked(false);
        token.setExpiresAt(Instant.now().plus(1, ChronoUnit.DAYS));

        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(token));

        RefreshToken result = refreshTokenService.validate("raw-token");

        assertThat(result).isEqualTo(token);
    }

    @Test
    void validate_throwsUnauthorized_whenNoMatchingTokenFound() {
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> refreshTokenService.validate("unknown-token"))
                .isInstanceOf(ApiException.class)
                .hasMessage("Invalid refresh token")
                .extracting(ex -> ((ApiException) ex).getStatus())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void validate_throwsUnauthorized_andRevokesAllTokens_whenTokenAlreadyRevoked() {
        RefreshToken token = new RefreshToken();
        token.setUser(user);
        token.setRevoked(true);
        token.setExpiresAt(Instant.now().plus(1, ChronoUnit.DAYS));

        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(token));
        when(refreshTokenRepository.findByUserIdAndRevokedFalse(user.getId())).thenReturn(List.of());

        assertThatThrownBy(() -> refreshTokenService.validate("stolen-token"))
                .isInstanceOf(ApiException.class)
                .hasMessage("Refresh token has been revoked")
                .extracting(ex -> ((ApiException) ex).getStatus())
                .isEqualTo(HttpStatus.UNAUTHORIZED);

        verify(refreshTokenRepository).findByUserIdAndRevokedFalse(user.getId());
    }

    @Test
    void validate_throwsUnauthorized_whenTokenExpired() {
        RefreshToken token = new RefreshToken();
        token.setUser(user);
        token.setRevoked(false);
        token.setExpiresAt(Instant.now().minus(1, ChronoUnit.DAYS));

        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> refreshTokenService.validate("expired-token"))
                .isInstanceOf(ApiException.class)
                .hasMessage("Refresh token has expired")
                .extracting(ex -> ((ApiException) ex).getStatus())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void rotate_revokesOldToken_andIssuesNewOne() {
        RefreshToken oldToken = new RefreshToken();
        oldToken.setUser(user);
        oldToken.setRevoked(false);

        when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        String newRawToken = refreshTokenService.rotate(oldToken);

        assertThat(oldToken.getRevoked()).isTrue();
        assertThat(newRawToken).isNotBlank();
        verify(refreshTokenRepository, times(2)).save(any(RefreshToken.class));
    }

    @Test
    void revoke_marksTokenAsRevoked_andSaves() {
        RefreshToken token = new RefreshToken();
        token.setUser(user);
        token.setRevoked(false);

        refreshTokenService.revoke(token);

        assertThat(token.getRevoked()).isTrue();
        verify(refreshTokenRepository).save(token);
    }

    @Test
    void revokeAllForUser_revokesEachActiveToken() {
        RefreshToken token1 = new RefreshToken();
        token1.setRevoked(false);
        RefreshToken token2 = new RefreshToken();
        token2.setRevoked(false);

        when(refreshTokenRepository.findByUserIdAndRevokedFalse(user.getId()))
                .thenReturn(List.of(token1, token2));

        refreshTokenService.revokeAllForUser(user.getId());

        assertThat(token1.getRevoked()).isTrue();
        assertThat(token2.getRevoked()).isTrue();
        verify(refreshTokenRepository).saveAll(List.of(token1, token2));
    }

    @Test
    void revokeAllForUser_doesNothing_whenNoActiveTokensExist() {
        when(refreshTokenRepository.findByUserIdAndRevokedFalse(user.getId())).thenReturn(List.of());

        refreshTokenService.revokeAllForUser(user.getId());

        verify(refreshTokenRepository, times(0)).saveAll(any());
    }
}
