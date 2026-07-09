package com.tracepilot.api.Services;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.mail.MailException;

import com.tracepilot.api.DTO.Request.LoginRequest;
import com.tracepilot.api.DTO.Request.RegisterRequest;
import com.tracepilot.api.DTO.Response.AuthResponse;
import com.tracepilot.api.DTO.Response.UserSummaryResponse;
import com.tracepilot.api.Entities.RefreshToken;
import com.tracepilot.api.Entities.User;
import com.tracepilot.api.Enums.OAuthProvider;
import com.tracepilot.api.Enums.UserRoles;
import com.tracepilot.api.Exceptions.ApiException;
import com.tracepilot.api.Repositories.UserRepository;
import com.tracepilot.api.Security.AuthenticatedUser;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final EmailService emailService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService,
            RefreshTokenService refreshTokenService, EmailService emailService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.emailService = emailService;
    }

    @Transactional
    public AuthResult registerUser(RegisterRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();
        log.debug("Initiating user entity creation for username: {}", normalizedEmail);

        if (userRepository.findByEmail(normalizedEmail).isPresent()) {
            log.warn("Registration rejected: Email {} is already in use", normalizedEmail);
            throw new ApiException("Email is already in use", HttpStatus.CONFLICT);
        }

        var user = new User();
        user.setEmail(normalizedEmail);
        user.setDisplayName(request.displayName());
        String hashedPassword = passwordEncoder.encode(request.password());
        user.setPasswordHash(hashedPassword);

        String verificationToken = UUID.randomUUID().toString();
        user.setVerificationToken(verificationToken);
        user.setVerificationTokenExpiresAt(Instant.now().plus(24, ChronoUnit.HOURS));

        log.debug("Saving user entity to the database...");
        User savedUser = userRepository.save(user);
        log.info("User registered in database with ID: {}", savedUser.getId());

        try {
            log.debug("Sending user a verification email...");
            emailService.sendVerificationEmail(savedUser.getEmail(), verificationToken);
            log.info("Sent an email to User with ID: {}", savedUser.getId());
        } catch (MailException e) {
            log.error("Failed to send verification email to user {}", savedUser.getId(), e);
            throw new ApiException(
                    "Unable to send verification email. Please try again later.",
                    HttpStatus.SERVICE_UNAVAILABLE);
        }

        log.debug("Generating token access for {}", savedUser.getId());
        String accessToken = jwtService.generateAccessToken(savedUser);
        Instant expiresAt = jwtService.calculateExpiryDate().toInstant();

        String rawRefreshToken = refreshTokenService.issue(savedUser);

        var userSummary = new UserSummaryResponse(
                savedUser.getId(),
                savedUser.getEmail(),
                savedUser.getDisplayName(),
                savedUser.getRole().name());

        log.info("Successfully generated AuthResponse and completed registration for user ID: {}", savedUser.getId());

        var response = new AuthResponse(accessToken, "Bearer", expiresAt, userSummary);
        return new AuthResult(response, rawRefreshToken);
    }

    @Transactional
    public AuthResult loginUser(LoginRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();
        log.debug("Initiating login sequence for email: {}", normalizedEmail);

        User user = userRepository.findByEmail(
                normalizedEmail)
                .orElseThrow(() -> {
                    log.warn("Login rejected: No account found for email {}", normalizedEmail);
                    return new ApiException("Invalid email or password", HttpStatus.UNAUTHORIZED);
                });

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            log.warn("Login rejected: Password mismatch for user ID {}", user.getId());
            throw new ApiException("Invalid email or password", HttpStatus.UNAUTHORIZED);
        }

        log.debug("Credentials verified. Generating token access for {}", user.getId());
        String accessToken = jwtService.generateAccessToken(user);
        Instant expiresAt = jwtService.calculateExpiryDate().toInstant();

        String rawRefreshToken = refreshTokenService.issue(user);

        var userSummary = new UserSummaryResponse(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getRole().name());

        log.info("Successfully authenticated and generated AuthResponse for user ID: {}", user.getId());

        var response = new AuthResponse(accessToken, "Bearer", expiresAt, userSummary);
        return new AuthResult(response, rawRefreshToken);
    }

    @Transactional
    public AuthResult refreshToken(String rawRefreshToken) {
        RefreshToken oldToken = refreshTokenService.validate(rawRefreshToken);
        User user = oldToken.getUser();

        String newRawRefreshToken = refreshTokenService.rotate(oldToken);

        String accessToken = jwtService.generateAccessToken(user);
        Instant expiresAt = jwtService.calculateExpiryDate().toInstant();

        var userSummary = new UserSummaryResponse(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getRole().name());

        log.info("Successfully refreshed tokens for user ID: {}", user.getId());

        var response = new AuthResponse(accessToken, "Bearer", expiresAt, userSummary);
        return new AuthResult(response, newRawRefreshToken);
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        try {
            RefreshToken token = refreshTokenService.validate(rawRefreshToken);
            refreshTokenService.revoke(token);
            log.info("User {} logged out, refresh token revoked", token.getUser().getId());
        } catch (ApiException e) {
            log.debug("Logout called with an already-invalid refresh token, treating as no-op");
        }
    }

    @Transactional
    public void verifyEmail(String token) {
        User user = userRepository.findByVerificationToken(token)
                .orElseThrow(() -> {
                    log.warn("Email verification failed: no user found for token");
                    return new ApiException("Invalid or expired verification link", HttpStatus.BAD_REQUEST);
                });

        if (user.getVerificationTokenExpiresAt().isBefore(Instant.now())) {
            log.warn("Email verification failed: token expired for user {}", user.getId());
            throw new ApiException("Invalid or expired verification link", HttpStatus.BAD_REQUEST);
        }

        user.setIsVerified(true);
        user.setVerificationToken(null);
        user.setVerificationTokenExpiresAt(null);
        userRepository.save(user);

        log.info("Email verified for user {}", user.getId());
    }

    @Transactional
    public User processOAuthAfterLogin(String email, String displayName, OAuthProvider provider, String oAuthId) {
        if (email == null || email.isBlank()) {
            log.error("OAuth login rejected: provider did not supply an email");
            throw new ApiException("OAuth provider did not return an email", HttpStatus.BAD_REQUEST);
        }
        String normalizedEmail = email.trim().toLowerCase();
        log.debug("Processing OAuth login for email: {} using provider: {}", normalizedEmail, provider);

        if (provider == null) {
            log.error("OAuth login rejected: provider is null");
            throw new ApiException("Invalid OAuth provider", HttpStatus.BAD_REQUEST);
        }

        if (oAuthId == null || oAuthId.isBlank()) {
            log.error("OAuth login rejected: missing OAuth identifier");
            throw new ApiException("Invalid OAuth account", HttpStatus.BAD_REQUEST);
        }

        return userRepository.findByEmail(normalizedEmail).map(existingUser -> {
            log.debug("Existing user found with ID {}", existingUser.getId());

            // If they previously signed up with email/password, link their OAuth provider
            if (existingUser.getOAuthProvider() == null) {
                log.info("Linking {} account to existing user {}", provider, existingUser.getId());
                existingUser.setOAuthProvider(provider);
                existingUser.setOAuthId(oAuthId);
                existingUser.setIsVerified(true);
                existingUser.setVerificationToken(null);
                existingUser.setVerificationTokenExpiresAt(null);

                User savedUser = userRepository.save(existingUser);
                log.info("Successfully linked OAuth account for user {}", savedUser.getId());
                return savedUser;
            }
            if (existingUser.getOAuthProvider() != provider ||
                    !oAuthId.equals(existingUser.getOAuthId())) {

                log.warn("OAuth login rejected: account {} is already linked to a different OAuth identity",
                        existingUser.getId());

                throw new ApiException("This account is already linked to another OAuth account.", HttpStatus.CONFLICT);
            }

            log.debug("OAuth login successful for existing user {}", existingUser.getId());

            return existingUser;
        }).orElseGet(() -> {
            log.info("Creating new user from {} OAuth login: {}", provider, normalizedEmail);
            User newUser = new User();
            newUser.setEmail(normalizedEmail);
            newUser.setDisplayName(displayName);
            newUser.setOAuthProvider(provider);
            newUser.setOAuthId(oAuthId);
            newUser.setRole(UserRoles.USER);
            newUser.setIsVerified(true);

            User savedUser = userRepository.save(newUser);
            log.info("Created new OAuth user with ID {}", savedUser.getId());
            return savedUser;
        });
    }

    @Transactional
    public void forgotPassword(String email) {
        String normalizedEmail = email.trim().toLowerCase();

        userRepository.findByEmail(normalizedEmail).ifPresent(user -> {
            if (user.getPasswordHash() == null) {
                log.info("Password reset requested for OAuth-only account {}, skipping", user.getId());
                return;
            }

            String resetToken = UUID.randomUUID().toString();
            user.setResetPasswordToken(resetToken);
            user.setResetPasswordTokenExpiresAt(Instant.now().plus(1, ChronoUnit.HOURS));
            userRepository.save(user);

            try {
                emailService.sendPasswordResetEmail(user.getEmail(), resetToken);
            } catch (MailException e) {
                log.error("Failed to send password reset email to user {}", user.getId(), e);
            }
        });

        log.info("Password reset requested for email: {}", normalizedEmail);
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        User user = userRepository.findByResetPasswordToken(token)
                .orElseThrow(() -> {
                    log.warn("Password reset failed: no user found for token");
                    return new ApiException("Invalid or expired reset link", HttpStatus.BAD_REQUEST);
                });

        if (user.getResetPasswordTokenExpiresAt().isBefore(Instant.now())) {
            log.warn("Password reset failed: token expired for user {}", user.getId());
            throw new ApiException("Invalid or expired reset link", HttpStatus.BAD_REQUEST);
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setResetPasswordToken(null);
        user.setResetPasswordTokenExpiresAt(null);
        userRepository.save(user);

        refreshTokenService.revokeAllForUser(user.getId());

        log.info("Password reset completed for user {}", user.getId());
    }

    @Transactional
    public void resendVerificationEmail(String email) {
        String normalizedEmail = email.trim().toLowerCase();

        User user = userRepository.findByEmail(normalizedEmail).orElse(null);

        if (user == null) {
            log.warn("Verification resend requested for unknown email.");
            return;
        }

        if (user.getIsVerified()) {
            log.info("Verification resend requested for already verified user {}", user.getId());
            return;
        }

        String verificationToken = UUID.randomUUID().toString();

        user.setVerificationToken(verificationToken);
        user.setVerificationTokenExpiresAt(
                Instant.now().plus(24, ChronoUnit.HOURS));

        userRepository.save(user);

        try {
            emailService.sendVerificationEmail(
                    user.getEmail(),
                    verificationToken);
        } catch (MailException e) {
            log.error("Failed to resend verification email to user {}", user.getId(), e);
            throw new ApiException(
                    "Unable to send verification email. Please try again later.",
                    HttpStatus.SERVICE_UNAVAILABLE);
        }

        log.info("Verification email resent for user {}", user.getId());
    }

    @Transactional
    public void changePassword(
            AuthenticatedUser principal,
            String currentPassword,
            String newPassword) {

        User user = userRepository.findById(principal.id())
                .orElseThrow(() -> new ApiException("User not found.", HttpStatus.NOT_FOUND));

        if (user.getPasswordHash() == null) {
            throw new ApiException(
                    "Password changes are not available for OAuth accounts.",
                    HttpStatus.BAD_REQUEST);
        }

        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new ApiException(
                    "Current password is incorrect.",
                    HttpStatus.UNAUTHORIZED);
        }

        if (passwordEncoder.matches(newPassword, user.getPasswordHash())) {
            throw new ApiException(
                    "New password must be different from the current password.",
                    HttpStatus.BAD_REQUEST);
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        refreshTokenService.revokeAllForUser(user.getId());

        log.info("Password changed for user {}", user.getId());
    }
}
