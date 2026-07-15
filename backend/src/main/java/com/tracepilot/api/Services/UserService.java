package com.tracepilot.api.Services;

import java.time.LocalDate;

import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tracepilot.api.DTO.Request.UpdateUserRequest;
import com.tracepilot.api.DTO.Response.UserProfileResponse;
import com.tracepilot.api.Entities.User;
import com.tracepilot.api.Exceptions.ApiException;
import com.tracepilot.api.Repositories.UserRepository;
import com.tracepilot.api.Security.AuthenticatedUser;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserProfileResponse getCurrentUser(AuthenticatedUser principal) {
        log.info("Retrieving profile for user ID: {}", principal.id());

        try {
            User user = userRepository.findById(principal.id())
                    .orElseThrow(() -> {
                        log.warn("User not found with ID: {}", principal.id());
                        return new ApiException("User not found.", HttpStatus.NOT_FOUND);
                    });

            syncDailyAuditCount(user);

            log.info("Successfully retrieved profile for user ID: {}", principal.id());

            return new UserProfileResponse(
                    user.getId(),
                    user.getDisplayName(),
                    user.getEmail(),
                    user.getRole(),
                    user.getIsVerified(),
                    user.getCreatedAt(),
                    user.getAuditCountToday());

        } catch (DataAccessException ex) {
            log.error("Failed to retrieve profile for user ID: {}", principal.id(), ex);
            throw ex;
        }
    }

    private void syncDailyAuditCount(User user) {
        LocalDate today = LocalDate.now();

        if (user.isAuditCountStale(today)) {
            log.info(
                    "Resetting daily audit count for user ID: {} (previous last_audit_date: {}, previous count: {})",
                    user.getId(), user.getLastAuditDate(), user.getAuditCountToday());
            user.setAuditCountToday(0);
            user.setLastAuditDate(today);
            userRepository.save(user);
        } else {
            log.debug("Daily audit count still current for user ID: {} (last_audit_date: {})",
                    user.getId(), user.getLastAuditDate());
        }
    }

    @Transactional
    public UserProfileResponse updateCurrentUser(
            AuthenticatedUser principal,
            UpdateUserRequest request) {

        log.info("Updating profile for user ID: {}", principal.id());

        try {
            User user = userRepository.findById(principal.id())
                    .orElseThrow(() -> {
                        log.warn("User not found with ID: {}", principal.id());
                        return new ApiException("User not found.", HttpStatus.NOT_FOUND);
                    });

            user.setDisplayName(request.displayName());

            userRepository.save(user);

            log.info("Successfully updated profile for user ID: {}", principal.id());

            return new UserProfileResponse(
                    user.getId(),
                    user.getDisplayName(),
                    user.getEmail(),
                    user.getRole(),
                    user.getIsVerified(),
                    user.getCreatedAt(),
                    user.getAuditCountToday());

        } catch (DataAccessException ex) {
            log.error("Failed to update profile for user ID: {}", principal.id(), ex);
            throw ex;
        }
    }

    @Transactional
    public void deleteCurrentUser(AuthenticatedUser principal) {

        log.info("Deleting account for user ID: {}", principal.id());

        try {
            User user = userRepository.findById(principal.id())
                    .orElseThrow(() -> {
                        log.warn("User not found with ID: {}", principal.id());
                        return new ApiException("User not found.", HttpStatus.NOT_FOUND);
                    });

            userRepository.delete(user);

            log.info("Successfully deleted account for user ID: {}", principal.id());

        } catch (DataAccessException ex) {
            log.error("Failed to delete account for user ID: {}", principal.id(), ex);
            throw ex;
        }
    }
}