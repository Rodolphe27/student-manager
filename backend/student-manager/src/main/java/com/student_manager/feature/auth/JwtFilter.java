package com.student_manager.feature.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Servlet filter that reads a {@code Bearer} JWT from the {@code Authorization}
 * header, validates it, and — when valid — populates the
 * {@link SecurityContextHolder} with an authenticated principal carrying a
 * single {@code ROLE_*} authority derived from the token's claims. Runs once
 * per request, ahead of Spring Security's own authentication filter.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    /**
     * Extracts and validates the bearer token (if present), authenticates the
     * security context on success, and always continues the filter chain —
     * an absent, invalid, or claim-less token simply leaves the request
     * unauthenticated rather than being rejected here.
     *
     * @param request  the incoming HTTP request
     * @param response the outgoing HTTP response
     * @param chain    the remaining filter chain to invoke
     * @throws ServletException if the underlying filter chain throws one
     * @throws IOException      if the underlying filter chain throws one
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            if (jwtUtil.isTokenValid(token)) {
                String username = jwtUtil.extractUsername(token);
                String role     = jwtUtil.extractRole(token);

                // A token that verifies but carries no subject/role claim is not a
                // usable identity — leave the context unauthenticated rather than
                // granting a bogus "ROLE_null" authority.
                if (username != null && role != null && !role.isBlank()) {
                    UsernamePasswordAuthenticationToken auth =
                            new UsernamePasswordAuthenticationToken(
                                    username,
                                    null,
                                    List.of(new SimpleGrantedAuthority("ROLE_" + role))
                            );

                    SecurityContextHolder.getContext().setAuthentication(auth);
                    log.debug("Authenticated user: {} with role: {}", username, role);
                }
            }
        }

        chain.doFilter(request, response);
    }
}
