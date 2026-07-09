package com.tracepilot.api.Repositories;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.tracepilot.api.Entities.User;
import com.tracepilot.api.Enums.OAuthProvider;

public interface UserRepository extends JpaRepository<User, UUID> {
    
    Optional<User> findByEmail(String email);

    Optional<User> findByOAuthProviderAndOAuthId(OAuthProvider oAuthProvider, String oAuthId);

    boolean existsByEmail(String email);

    Optional<User> findByVerificationToken(String verificationToken);

    Optional<User> findByResetPasswordToken(String resetPasswordToken);

    boolean existsByOAuthProviderAndOAuthId(OAuthProvider oAuthProvider, String oAuthId);

    @Modifying
    @Query("UPDATE User u SET u.auditCountToday = u.auditCountToday + 1 WHERE u.id = :id")
    int incrementAuditCount(@Param("id") UUID id);
}
