package com.tracepilot.api.Services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailSendException;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.tracepilot.api.DTO.Request.LoginRequest;
import com.tracepilot.api.DTO.Request.RegisterRequest;
import com.tracepilot.api.Entities.RefreshToken;
import com.tracepilot.api.Entities.User;
import com.tracepilot.api.Enums.OAuthProvider;
import com.tracepilot.api.Enums.UserRoles;
import com.tracepilot.api.Exceptions.ApiException;
import com.tracepilot.api.Repositories.UserRepository;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private RefreshTokenService refreshTokenService;
    @Mock
    private EmailService emailService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, passwordEncoder, jwtService, refreshTokenService,
                emailService);
    }

    private User persistedUser(UUID id) {
        User user = new User();
        user.setId(id);
        user.setEmail("abdulrahman@example.com");
        user.setDisplayName("Abdulrahman");
        user.setPasswordHash("hashed-password");
        user.setRole(UserRoles.USER);
        return user;
    }

    // ----- registerUser -----

    @Test
    void registerUser_throwsConflict_whenEmailAlreadyInUse() {
        RegisterRequest request = new RegisterRequest("abdulrahman@example.com", "password123", "Abdulrahman");
        when(userRepository.findByEmail("abdulrahman@example.com"))
                .thenReturn(Optional.of(persistedUser(UUID.randomUUID())));

        assertThatThrownBy(() -> authService.registerUser(request))
                .isInstanceOf(ApiException.class)
                .hasMessage("Email is already in use")
                .extracting(ex -> ((ApiException) ex).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);

        verify(userRepository, never()).save(any());
    }

    @Test
    void registerUser_normalizesEmail_toLowerCaseAndTrimmed() {
        RegisterRequest request = new RegisterRequest("  Abdulrahman@Example.com  ", "password123", "Abdulrahman");
        when(userRepository.findByEmail("abdulrahman@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(UUID.randomUUID());
            return u;
        });
        when(jwtService.generateAccessToken(any())).thenReturn("access-token");
        when(jwtService.calculateExpiryDate()).thenReturn(new Date());
        when(refreshTokenService.issue(any())).thenReturn("raw-refresh-token");

        AuthResult result = authService.registerUser(request);

        assertThat(result.response().user().email()).isEqualTo("abdulrahman@example.com");
        verify(userRepository).findByEmail("abdulrahman@example.com");
    }

    @Test
    void registerUser_hashesPassword_andSavesVerificationToken() {
        RegisterRequest request = new RegisterRequest("new@example.com", "password123", "New User");
        when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123")).thenReturn("hashed-pw");
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        when(userRepository.save(captor.capture())).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(UUID.randomUUID());
            return u;
        });
        when(jwtService.generateAccessToken(any())).thenReturn("access-token");
        when(jwtService.calculateExpiryDate()).thenReturn(new Date());
        when(refreshTokenService.issue(any())).thenReturn("raw-refresh-token");

        authService.registerUser(request);

        User savedUser = captor.getValue();
        assertThat(savedUser.getPasswordHash()).isEqualTo("hashed-pw");
        assertThat(savedUser.getVerificationToken()).isNotBlank();
        assertThat(savedUser.getVerificationTokenExpiresAt()).isAfter(Instant.now());
    }

    @Test
    void registerUser_sendsVerificationEmail_andReturnsTokens() {
        RegisterRequest request = new RegisterRequest("new@example.com", "password123", "New User");
        when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123")).thenReturn("hashed-pw");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(UUID.randomUUID());
            return u;
        });
        when(jwtService.generateAccessToken(any())).thenReturn("access-token-123");
        when(jwtService.calculateExpiryDate()).thenReturn(new Date());
        when(refreshTokenService.issue(any())).thenReturn("raw-refresh-token");

        AuthResult result = authService.registerUser(request);

        verify(emailService).sendVerificationEmail(eq("new@example.com"), anyString());
        assertThat(result.response().accessToken()).isEqualTo("access-token-123");
        assertThat(result.response().tokenType()).isEqualTo("Bearer");
        assertThat(result.refreshToken()).isEqualTo("raw-refresh-token");
    }
    
    @Test
    void registerUser_succeedsAndReturnsTokens_regardlessOfEmailServiceOutcome() {
        RegisterRequest request = new RegisterRequest("new@example.com", "password123", "New User");
        when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123")).thenReturn("hashed-pw");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(UUID.randomUUID());
            return u;
        });
        when(jwtService.generateAccessToken(any())).thenReturn("access-token-123");
        when(jwtService.calculateExpiryDate()).thenReturn(new Date());
        when(refreshTokenService.issue(any())).thenReturn("raw-refresh-token");

        AuthResult result = authService.registerUser(request);

        assertThat(result.response().accessToken()).isEqualTo("access-token-123");
        verify(emailService).sendVerificationEmail(eq("new@example.com"), anyString());
    }
    

    // ----- loginUser -----

    @Test
    void loginUser_throwsUnauthorized_whenEmailNotFound() {
        LoginRequest request = new LoginRequest("missing@example.com", "password");
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.loginUser(request))
                .isInstanceOf(ApiException.class)
                .hasMessage("Invalid email or password")
                .extracting(ex -> ((ApiException) ex).getStatus())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void loginUser_throwsUnauthorized_whenPasswordDoesNotMatch() {
        User user = persistedUser(UUID.randomUUID());
        LoginRequest request = new LoginRequest("abdulrahman@example.com", "wrong-password");
        when(userRepository.findByEmail("abdulrahman@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", "hashed-password")).thenReturn(false);

        assertThatThrownBy(() -> authService.loginUser(request))
                .isInstanceOf(ApiException.class)
                .hasMessage("Invalid email or password")
                .extracting(ex -> ((ApiException) ex).getStatus())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void loginUser_returnsTokens_whenCredentialsAreValid() {
        User user = persistedUser(UUID.randomUUID());
        LoginRequest request = new LoginRequest("abdulrahman@example.com", "correct-password");
        when(userRepository.findByEmail("abdulrahman@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("correct-password", "hashed-password")).thenReturn(true);
        when(jwtService.generateAccessToken(user)).thenReturn("access-token");
        when(jwtService.calculateExpiryDate()).thenReturn(new Date());
        when(refreshTokenService.issue(user)).thenReturn("raw-refresh-token");

        AuthResult result = authService.loginUser(request);

        assertThat(result.response().accessToken()).isEqualTo("access-token");
        assertThat(result.refreshToken()).isEqualTo("raw-refresh-token");
        assertThat(result.response().user().id()).isEqualTo(user.getId());
    }

    // ----- refreshToken -----

    @Test
    void refreshToken_rotatesToken_andReturnsNewAccessToken() {
        User user = persistedUser(UUID.randomUUID());
        RefreshToken oldToken = new RefreshToken();
        oldToken.setUser(user);

        when(refreshTokenService.validate("old-raw-token")).thenReturn(oldToken);
        when(refreshTokenService.rotate(oldToken)).thenReturn("new-raw-token");
        when(jwtService.generateAccessToken(user)).thenReturn("new-access-token");
        when(jwtService.calculateExpiryDate()).thenReturn(new Date());

        AuthResult result = authService.refreshToken("old-raw-token");

        assertThat(result.response().accessToken()).isEqualTo("new-access-token");
        assertThat(result.refreshToken()).isEqualTo("new-raw-token");
    }

    // ----- logout -----

    @Test
    void logout_revokesToken_whenValid() {
        User user = persistedUser(UUID.randomUUID());
        RefreshToken token = new RefreshToken();
        token.setUser(user);

        when(refreshTokenService.validate("raw-token")).thenReturn(token);

        authService.logout("raw-token");

        verify(refreshTokenService).revoke(token);
    }

    @Test
    void logout_isNoOp_whenTokenIsAlreadyInvalid() {
        when(refreshTokenService.validate("bad-token"))
                .thenThrow(new ApiException("Invalid refresh token", HttpStatus.UNAUTHORIZED));

        authService.logout("bad-token");

        verify(refreshTokenService, never()).revoke(any());
    }

    // ----- verifyEmail -----

    @Test
    void verifyEmail_throwsBadRequest_whenTokenNotFound() {
        when(userRepository.findByVerificationToken("bad-token")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.verifyEmail("bad-token"))
                .isInstanceOf(ApiException.class)
                .hasMessage("Invalid or expired verification link")
                .extracting(ex -> ((ApiException) ex).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void verifyEmail_throwsBadRequest_whenTokenExpired() {
        User user = persistedUser(UUID.randomUUID());
        user.setVerificationToken("token-1");
        user.setVerificationTokenExpiresAt(Instant.now().minus(1, ChronoUnit.HOURS));
        when(userRepository.findByVerificationToken("token-1")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.verifyEmail("token-1"))
                .isInstanceOf(ApiException.class)
                .hasMessage("Invalid or expired verification link");
    }

    @Test
    void verifyEmail_marksUserVerified_andClearsToken() {
        User user = persistedUser(UUID.randomUUID());
        user.setVerificationToken("token-1");
        user.setVerificationTokenExpiresAt(Instant.now().plus(1, ChronoUnit.HOURS));
        when(userRepository.findByVerificationToken("token-1")).thenReturn(Optional.of(user));

        authService.verifyEmail("token-1");

        assertThat(user.getIsVerified()).isTrue();
        assertThat(user.getVerificationToken()).isNull();
        assertThat(user.getVerificationTokenExpiresAt()).isNull();
        verify(userRepository).save(user);
    }

    // ----- processOAuthAfterLogin -----

    @Test
    void processOAuthAfterLogin_throwsBadRequest_whenEmailMissing() {
        assertThatThrownBy(() -> authService.processOAuthAfterLogin(null, "Name", OAuthProvider.GOOGLE, "oauth-id"))
                .isInstanceOf(ApiException.class)
                .hasMessage("OAuth provider did not return an email")
                .extracting(ex -> ((ApiException) ex).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void processOAuthAfterLogin_throwsBadRequest_whenProviderMissing() {
        assertThatThrownBy(() -> authService.processOAuthAfterLogin("a@b.com", "Name", null, "oauth-id"))
                .isInstanceOf(ApiException.class)
                .hasMessage("Invalid OAuth provider");
    }

    @Test
    void processOAuthAfterLogin_throwsBadRequest_whenOAuthIdMissing() {
        assertThatThrownBy(
                () -> authService.processOAuthAfterLogin("a@b.com", "Name", OAuthProvider.GOOGLE, "  "))
                .isInstanceOf(ApiException.class)
                .hasMessage("Invalid OAuth account");
    }

    @Test
    void processOAuthAfterLogin_linksProvider_whenExistingPasswordUserHasNoProvider() {
        User existing = persistedUser(UUID.randomUUID());
        existing.setOAuthProvider(null);
        when(userRepository.findByEmail("abdulrahman@example.com")).thenReturn(Optional.of(existing));
        when(userRepository.save(existing)).thenReturn(existing);

        User result = authService.processOAuthAfterLogin("Abdulrahman@Example.com", "Name",
                OAuthProvider.GITHUB, "gh-123");

        assertThat(result.getOAuthProvider()).isEqualTo(OAuthProvider.GITHUB);
        assertThat(result.getOAuthId()).isEqualTo("gh-123");
        assertThat(result.getIsVerified()).isTrue();
    }

    @Test
    void processOAuthAfterLogin_throwsConflict_whenLinkedToDifferentProvider() {
        User existing = persistedUser(UUID.randomUUID());
        existing.setOAuthProvider(OAuthProvider.GOOGLE);
        existing.setOAuthId("google-id-1");
        when(userRepository.findByEmail("abdulrahman@example.com")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> authService.processOAuthAfterLogin("abdulrahman@example.com", "Name",
                OAuthProvider.GITHUB, "gh-123"))
                .isInstanceOf(ApiException.class)
                .hasMessage("This account is already linked to another OAuth account.")
                .extracting(ex -> ((ApiException) ex).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void processOAuthAfterLogin_returnsExistingUser_whenSameProviderAndId() {
        User existing = persistedUser(UUID.randomUUID());
        existing.setOAuthProvider(OAuthProvider.GOOGLE);
        existing.setOAuthId("google-id-1");
        when(userRepository.findByEmail("abdulrahman@example.com")).thenReturn(Optional.of(existing));

        User result = authService.processOAuthAfterLogin("abdulrahman@example.com", "Name",
                OAuthProvider.GOOGLE, "google-id-1");

        assertThat(result).isEqualTo(existing);
        verify(userRepository, never()).save(any());
    }

    @Test
    void processOAuthAfterLogin_createsNewUser_whenNoExistingAccount() {
        when(userRepository.findByEmail("brandnew@example.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(UUID.randomUUID());
            return u;
        });

        User result = authService.processOAuthAfterLogin("brandnew@example.com", "Brand New",
                OAuthProvider.GITHUB, "gh-999");

        assertThat(result.getEmail()).isEqualTo("brandnew@example.com");
        assertThat(result.getOAuthProvider()).isEqualTo(OAuthProvider.GITHUB);
        assertThat(result.getOAuthId()).isEqualTo("gh-999");
        assertThat(result.getIsVerified()).isTrue();
        assertThat(result.getRole()).isEqualTo(UserRoles.USER);
    }

    // ----- forgotPassword -----

    @Test
    void forgotPassword_doesNothing_whenNoUserFound() {
        when(userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

        authService.forgotPassword("nobody@example.com");

        verify(userRepository, never()).save(any());
        verify(emailService, never()).sendPasswordResetEmail(anyString(), anyString());
    }

    @Test
    void forgotPassword_skipsOAuthOnlyAccounts() {
        User oauthUser = persistedUser(UUID.randomUUID());
        oauthUser.setPasswordHash(null);
        when(userRepository.findByEmail("abdulrahman@example.com")).thenReturn(Optional.of(oauthUser));

        authService.forgotPassword("abdulrahman@example.com");

        verify(userRepository, never()).save(any());
        verify(emailService, never()).sendPasswordResetEmail(anyString(), anyString());
    }

    @Test
    void forgotPassword_setsResetToken_andSendsEmail_forPasswordAccounts() {
        User user = persistedUser(UUID.randomUUID());
        when(userRepository.findByEmail("abdulrahman@example.com")).thenReturn(Optional.of(user));

        authService.forgotPassword("abdulrahman@example.com");

        assertThat(user.getResetPasswordToken()).isNotBlank();
        assertThat(user.getResetPasswordTokenExpiresAt()).isAfter(Instant.now());
        verify(userRepository).save(user);
        verify(emailService).sendPasswordResetEmail(eq("abdulrahman@example.com"), anyString());
    }


    // ----- resetPassword -----

    @Test
    void resetPassword_throwsBadRequest_whenTokenNotFound() {
        when(userRepository.findByResetPasswordToken("bad-token")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.resetPassword("bad-token", "newPassword123"))
                .isInstanceOf(ApiException.class)
                .hasMessage("Invalid or expired reset link")
                .extracting(ex -> ((ApiException) ex).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void resetPassword_throwsBadRequest_whenTokenExpired() {
        User user = persistedUser(UUID.randomUUID());
        user.setResetPasswordToken("token-1");
        user.setResetPasswordTokenExpiresAt(Instant.now().minus(1, ChronoUnit.HOURS));
        when(userRepository.findByResetPasswordToken("token-1")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.resetPassword("token-1", "newPassword123"))
                .isInstanceOf(ApiException.class)
                .hasMessage("Invalid or expired reset link");
    }

    @Test
    void resetPassword_updatesPasswordAndRevokesAllSessions() {
        User user = persistedUser(UUID.randomUUID());
        user.setResetPasswordToken("token-1");
        user.setResetPasswordTokenExpiresAt(Instant.now().plus(1, ChronoUnit.HOURS));
        when(userRepository.findByResetPasswordToken("token-1")).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("newPassword123")).thenReturn("new-hashed-password");

        authService.resetPassword("token-1", "newPassword123");

        assertThat(user.getPasswordHash()).isEqualTo("new-hashed-password");
        assertThat(user.getResetPasswordToken()).isNull();
        assertThat(user.getResetPasswordTokenExpiresAt()).isNull();
        verify(userRepository).save(user);
        verify(refreshTokenService, times(1)).revokeAllForUser(user.getId());
    }
}
