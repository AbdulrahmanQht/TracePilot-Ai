package com.tracepilot.api.Services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.tracepilot.api.DTO.Response.AdminFlaggedAuditResponse;
import com.tracepilot.api.DTO.Response.AdminUserResponse;
import com.tracepilot.api.Entities.TraceAudit;
import com.tracepilot.api.Entities.User;
import com.tracepilot.api.Enums.AuditStatus;
import com.tracepilot.api.Enums.UserRoles;
import com.tracepilot.api.Repositories.TraceAuditRepository;
import com.tracepilot.api.Repositories.UserRepository;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private TraceAuditRepository traceAuditRepository;

    private AdminService adminService;

    @BeforeEach
    void setUp() {
        adminService = new AdminService(userRepository, traceAuditRepository);
    }

    private User sampleUser() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setDisplayName("Abdulrahman");
        user.setEmail("abdulrahman@example.com");
        user.setRole(UserRoles.USER);
        user.setIsVerified(true);
        user.setCreatedAt(Instant.now());
        user.setAuditCountToday(3);
        return user;
    }

    private TraceAudit flaggedAudit(User owner) {
        TraceAudit audit = new TraceAudit();
        audit.setId(UUID.randomUUID());
        audit.setStatus(AuditStatus.COMPLETE);
        audit.setSuspiciousContent(true);
        audit.setCreatedAt(Instant.now());
        audit.setUser(owner);
        return audit;
    }

    // ----- getUsers -----

    @Test
    void getUsers_throwsIllegalArgument_whenPageableIsNull() {
        assertThatThrownBy(() -> adminService.getUsers(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Pageable cannot be null.");
    }

    @Test
    void getUsers_mapsUserEntitiesToAdminUserResponse() {
        User user = sampleUser();
        Pageable pageable = PageRequest.of(0, 10);
        when(userRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(user)));

        Page<AdminUserResponse> result = adminService.getUsers(pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        AdminUserResponse response = result.getContent().get(0);
        assertThat(response.id()).isEqualTo(user.getId());
        assertThat(response.displayName()).isEqualTo("Abdulrahman");
        assertThat(response.email()).isEqualTo("abdulrahman@example.com");
        assertThat(response.role()).isEqualTo(UserRoles.USER);
        assertThat(response.verified()).isTrue();
        assertThat(response.auditCountToday()).isEqualTo(3);
    }

    @Test
    void getUsers_propagatesDataAccessException() {
        Pageable pageable = PageRequest.of(0, 10);
        when(userRepository.findAll(pageable)).thenThrow(new DataAccessResourceFailureException("db down"));

        assertThatThrownBy(() -> adminService.getUsers(pageable))
                .isInstanceOf(DataAccessResourceFailureException.class);
    }

    // ----- getFlaggedAudits -----

    @Test
    void getFlaggedAudits_throwsIllegalArgument_whenPageableIsNull() {
        assertThatThrownBy(() -> adminService.getFlaggedAudits(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Pageable cannot be null.");
    }

    @Test
    void getFlaggedAudits_mapsAuditAndOwner_toResponse() {
        User owner = sampleUser();
        TraceAudit audit = flaggedAudit(owner);
        Pageable pageable = PageRequest.of(0, 10);
        when(traceAuditRepository.findBySuspiciousContentTrue(pageable)).thenReturn(new PageImpl<>(List.of(audit)));

        Page<AdminFlaggedAuditResponse> result = adminService.getFlaggedAudits(pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        AdminFlaggedAuditResponse response = result.getContent().get(0);
        assertThat(response.id()).isEqualTo(audit.getId());
        assertThat(response.status()).isEqualTo(AuditStatus.COMPLETE);
        assertThat(response.suspicious()).isTrue();
        assertThat(response.user().id()).isEqualTo(owner.getId());
        assertThat(response.user().displayName()).isEqualTo("Abdulrahman");
        assertThat(response.user().email()).isEqualTo("abdulrahman@example.com");
    }

    @Test
    void getFlaggedAudits_propagatesDataAccessException() {
        Pageable pageable = PageRequest.of(0, 10);
        when(traceAuditRepository.findBySuspiciousContentTrue(pageable))
                .thenThrow(new DataAccessResourceFailureException("db down"));

        assertThatThrownBy(() -> adminService.getFlaggedAudits(pageable))
                .isInstanceOf(DataAccessResourceFailureException.class);
    }
}
