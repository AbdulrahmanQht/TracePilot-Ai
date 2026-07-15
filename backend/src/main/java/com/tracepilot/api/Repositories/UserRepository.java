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
    @Query(value = """
            UPDATE users
            SET
                audit_count_today = CASE
                    WHEN last_audit_date IS DISTINCT FROM CURRENT_DATE THEN 1
                    ELSE audit_count_today + 1
                END,
                last_audit_date = CURRENT_DATE
            WHERE id = :id
              AND (
                last_audit_date IS DISTINCT FROM CURRENT_DATE
                OR audit_count_today < :dailyLimit
              )
            """, nativeQuery = true)
    int incrementAuditCountIfUnderLimit(@Param("id") UUID id, @Param("dailyLimit") int dailyLimit);
}
