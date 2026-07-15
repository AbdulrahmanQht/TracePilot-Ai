package com.tracepilot.api.Services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.*;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.HttpStatus;

import com.tracepilot.api.DTO.Request.UpdateUserRequest;
import com.tracepilot.api.DTO.Response.UserProfileResponse;
import com.tracepilot.api.Entities.User;
import com.tracepilot.api.Enums.UserRoles;
import com.tracepilot.api.Exceptions.ApiException;
import com.tracepilot.api.Repositories.UserRepository;
import com.tracepilot.api.Security.AuthenticatedUser;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    private UserService userService;
    private AuthenticatedUser principal;
    private User user;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository);
        UUID userId = UUID.randomUUID();
        principal = new AuthenticatedUser(userId, "abdulrahman@example.com", UserRoles.USER);
        user = new User();
        user.setId(userId);
        user.setDisplayName("Abdulrahman");
        user.setEmail("abdulrahman@example.com");
        user.setRole(UserRoles.USER);
        user.setIsVerified(true);
        user.setCreatedAt(Instant.now());
        user.setAuditCountToday(2);
        user.setLastAuditDate(LocalDate.now());
    }

    // ----- getCurrentUser -----

    @Test
    void getCurrentUser_returnsProfile_whenUserExists() {
        when(userRepository.findById(principal.id())).thenReturn(Optional.of(user));

        UserProfileResponse response = userService.getCurrentUser(principal);

        assertThat(response.id()).isEqualTo(user.getId());
        assertThat(response.displayName()).isEqualTo("Abdulrahman");
        assertThat(response.auditCountToday()).isEqualTo(2);
        verify(userRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void getCurrentUser_resetsAuditCount_whenLastAuditDateIsStale() {
        user.setAuditCountToday(7);
        user.setLastAuditDate(LocalDate.now().minusDays(1));
        when(userRepository.findById(principal.id())).thenReturn(Optional.of(user));

        UserProfileResponse response = userService.getCurrentUser(principal);

        assertThat(response.auditCountToday()).isEqualTo(0);
        assertThat(user.getLastAuditDate()).isEqualTo(LocalDate.now());
        verify(userRepository).save(user);
    }

    @Test
    void getCurrentUser_resetsAuditCount_whenLastAuditDateIsNull() {
        user.setAuditCountToday(3);
        user.setLastAuditDate(null);
        when(userRepository.findById(principal.id())).thenReturn(Optional.of(user));

        UserProfileResponse response = userService.getCurrentUser(principal);

        assertThat(response.auditCountToday()).isEqualTo(0);
        assertThat(user.getLastAuditDate()).isEqualTo(LocalDate.now());
        verify(userRepository).save(user);
    }

    @Test
    void getCurrentUser_throwsNotFound_whenUserMissing() {
        when(userRepository.findById(principal.id())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> userService.getCurrentUser(principal))
                .isInstanceOf(ApiException.class)
                .hasMessage("User not found.")
                .extracting(ex -> ((ApiException) ex).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void getCurrentUser_propagatesDataAccessException() {
        when(userRepository.findById(principal.id())).thenThrow(new DataAccessResourceFailureException("db down"));
        assertThatThrownBy(() -> userService.getCurrentUser(principal))
                .isInstanceOf(DataAccessResourceFailureException.class);
    }

    // ----- updateCurrentUser -----

    @Test
    void updateCurrentUser_updatesDisplayName_andReturnsUpdatedProfile() {
        when(userRepository.findById(principal.id())).thenReturn(Optional.of(user));
        UpdateUserRequest request = new UpdateUserRequest("New Name");

        UserProfileResponse response = userService.updateCurrentUser(principal, request);

        assertThat(user.getDisplayName()).isEqualTo("New Name");
        assertThat(response.displayName()).isEqualTo("New Name");
        verify(userRepository).save(user);
    }

    @Test
    void updateCurrentUser_throwsNotFound_whenUserMissing() {
        when(userRepository.findById(principal.id())).thenReturn(Optional.empty());
        UpdateUserRequest request = new UpdateUserRequest("New Name");

        assertThatThrownBy(() -> userService.updateCurrentUser(principal, request))
                .isInstanceOf(ApiException.class)
                .hasMessage("User not found.");
        verify(userRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void updateCurrentUser_propagatesDataAccessException() {
        when(userRepository.findById(principal.id())).thenThrow(new DataAccessResourceFailureException("db down"));
        UpdateUserRequest request = new UpdateUserRequest("New Name");

        assertThatThrownBy(() -> userService.updateCurrentUser(principal, request))
                .isInstanceOf(DataAccessResourceFailureException.class);
    }

    // ----- deleteCurrentUser -----

    @Test
    void deleteCurrentUser_deletesUser_whenUserExists() {
        when(userRepository.findById(principal.id())).thenReturn(Optional.of(user));

        userService.deleteCurrentUser(principal);

        verify(userRepository).delete(user);
    }

    @Test
    void deleteCurrentUser_throwsNotFound_whenUserMissing() {
        when(userRepository.findById(principal.id())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.deleteCurrentUser(principal))
                .isInstanceOf(ApiException.class)
                .hasMessage("User not found.");
        verify(userRepository, never()).delete(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void deleteCurrentUser_propagatesDataAccessException() {
        when(userRepository.findById(principal.id())).thenThrow(new DataAccessResourceFailureException("db down"));

        assertThatThrownBy(() -> userService.deleteCurrentUser(principal))
                .isInstanceOf(DataAccessResourceFailureException.class);
    }
}
