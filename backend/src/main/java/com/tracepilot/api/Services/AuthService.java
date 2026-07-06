package com.tracepilot.api.Services;

import com.tracepilot.api.Services.JwtService;
import com.tracepilot.api.Services.RefreshTokenService;
import com.tracepilot.api.Services.AuthResult;
import com.tracepilot.api.Repositories.UserRepository;
import com.tracepilot.api.DTO.Response.AuthResponse;
import com.tracepilot.api.DTO.Response.UserSummaryResponse;
import com.tracepilot.api.DTO.Request.RegisterRequest;
import com.tracepilot.api.DTO.Request.LoginRequest;
import com.tracepilot.api.Entities.User;
import com.tracepilot.api.Entities.RefreshToken;
import com.tracepilot.api.Exceptions.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;
import java.time.Instant;

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
        log.debug("Initiating user entity creation for username: {}", request.email());

        if (userRepository.findByEmail(request.email()).isPresent()) {
            log.warn("Registration rejected: Email {} is already in use", request.email());
            throw new ApiException("Email is already in use", HttpStatus.CONFLICT);
        }

        var user = new User();
        user.setEmail(request.email());
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
        log.debug("Initiating login sequence for email: {}", request.email());

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> {
                    log.warn("Login rejected: No account found for email {}", request.email());
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
}
