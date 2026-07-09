package com.tracepilot.api.Services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.dao.DataAccessException;

import com.tracepilot.api.Entities.User;
import com.tracepilot.api.Repositories.UserRepository;
import com.tracepilot.api.Repositories.TraceAuditRepository;
import com.tracepilot.api.DTO.Response.AdminUserResponse;
import com.tracepilot.api.DTO.Response.AdminFlaggedAuditResponse;
import com.tracepilot.api.DTO.Response.AdminUserSummary;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class AdminService {

    private final UserRepository userRepository;
    private final TraceAuditRepository traceAuditRepository;

    public AdminService(
            UserRepository userRepository,
            TraceAuditRepository traceAuditRepository) {
        this.userRepository = userRepository;
        this.traceAuditRepository = traceAuditRepository;
    }

    public Page<AdminUserResponse> getUsers(Pageable pageable) {
        if (pageable == null) {
            log.warn("Attempted to retrieve users with a null Pageable.");
            throw new IllegalArgumentException("Pageable cannot be null.");
        }

        log.info(
                "Admin requested users. page={}, size={}, sort={}",
                pageable.getPageNumber(),
                pageable.getPageSize(),
                pageable.getSort());

        try {
            Page<AdminUserResponse> users = userRepository.findAll(pageable)
                    .map(user -> new AdminUserResponse(
                            user.getId(),
                            user.getDisplayName(),
                            user.getEmail(),
                            user.getRole(),
                            user.getIsVerified(),
                            user.getCreatedAt(),
                            user.getAuditCountToday()));

            log.info(
                    "Retrieved {} users (page {} of {}). Total users={}.",
                    users.getNumberOfElements(),
                    users.getNumber(),
                    users.getTotalPages(),
                    users.getTotalElements());

            return users;
        } catch (DataAccessException ex) {
            log.error(
                    "Failed to retrieve users. page={}, size={}",
                    pageable.getPageNumber(),
                    pageable.getPageSize(),
                    ex);
            throw ex;
        }
    }

    public Page<AdminFlaggedAuditResponse> getFlaggedAudits(Pageable pageable) {
        if (pageable == null) {
            log.warn("Attempted to retrieve flagged audits with a null Pageable.");
            throw new IllegalArgumentException("Pageable cannot be null.");
        }

        log.info(
                "Admin requested flagged audits. page={}, size={}, sort={}",
                pageable.getPageNumber(),
                pageable.getPageSize(),
                pageable.getSort());

        try {
            Page<AdminFlaggedAuditResponse> audits = traceAuditRepository
                    .findBySuspiciousContentTrue(pageable)
                    .map(audit -> new AdminFlaggedAuditResponse(
                            audit.getId(),
                            audit.getStatus(),
                            audit.getSuspiciousContent(),
                            audit.getCreatedAt(),
                            new AdminUserSummary(
                                    audit.getUser().getId(),
                                    audit.getUser().getDisplayName(),
                                    audit.getUser().getEmail())));

            log.info(
                    "Retrieved {} flagged audits (page {} of {}). Total flagged audits={}.",
                    audits.getNumberOfElements(),
                    audits.getNumber(),
                    audits.getTotalPages(),
                    audits.getTotalElements());

            return audits;

        } catch (DataAccessException ex) {
            log.error(
                    "Failed to retrieve flagged audits. page={}, size={}",
                    pageable.getPageNumber(),
                    pageable.getPageSize(),
                    ex);
            throw ex;
        }
    }
}