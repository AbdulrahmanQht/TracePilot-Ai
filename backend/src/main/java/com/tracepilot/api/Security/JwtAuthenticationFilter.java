package com.tracepilot.api.Security;

import com.tracepilot.api.Enums.UserRoles;
import com.tracepilot.api.Services.JwtService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.UUID;

@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        log.info("Authorization header: {}", authHeader);
        
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.debug("No Bearer token for {} {}", request.getMethod(), request.getRequestURI());

            filterChain.doFilter(request, response);
            return;
        }

        final String token = authHeader.substring(7);

        try {
            Claims claims = jwtService.validateAccessToken(token);

            UUID userId = jwtService.extractUserId(claims);
            String email = jwtService.extractEmail(claims);
            UserRoles userRole = jwtService.extractRole(claims);

            String authority = "ROLE_" + userRole.name();

            AuthenticatedUser principal = new AuthenticatedUser(userId, email, userRole);

            var authToken = new UsernamePasswordAuthenticationToken(
                    principal,
                    null,
                    Collections.singletonList(new SimpleGrantedAuthority(authority)));

            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authToken);

            log.debug(
                    "Authenticated user: id={}, email={}, role={}, endpoint={} {}",
                    userId,
                    email,
                    userRole,
                    request.getMethod(),
                    request.getRequestURI());

        } catch (Exception e) {
            SecurityContextHolder.clearContext();

            log.warn(
                    "JWT authentication failed for {} {}: {}",
                    request.getMethod(),
                    request.getRequestURI(),
                    e.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}