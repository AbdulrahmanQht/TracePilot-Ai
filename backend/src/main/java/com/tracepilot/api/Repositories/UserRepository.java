package com.tracepilot.api.Repositories;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tracepilot.api.Entities.User;
import com.tracepilot.api.Enums.OAuthProvider;


public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);

    Optional<User> findByOAuthProviderAndOAuthId(OAuthProvider oAuthProvider, String oAuthId);
    
    boolean existsByEmail(String email);
    
    boolean existsByOAuthProviderAndOAuthId(OAuthProvider oAuthProvider, String oAuthId);
}
