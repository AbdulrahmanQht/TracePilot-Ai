package com.tracepilot.api.Services;

import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService,
            RefreshTokenService refreshTokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
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

        log.debug("Saving user entity to the database...");
        User savedUser = userRepository.save(user);
        log.info("User registered in database with ID: {}", savedUser.getId());

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
}
