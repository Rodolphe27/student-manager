package com.student_manager.feature.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Turns a successfully authenticated account into a logged-in HTTP session.
 * The session (persisted in Postgres by Spring Session JDBC) is identified by
 * the HttpOnly {@code SESSION} cookie, so no credential is ever exposed to
 * JavaScript. The principal name is the username and the single authority is
 * {@code ROLE_<role>} — the same shape {@code SecurityConfig}'s role rules and
 * {@code OwnershipGuard} expect.
 */
@Component
@RequiredArgsConstructor
public class SessionLogin {

    private final SecurityContextRepository securityContextRepository;

    /**
     * Stores the account as the authenticated principal of a (new) session.
     *
     * @param account  the authenticated account's summary
     * @param request  the current request
     * @param response the current response
     */
    public void start(AuthResponse account, HttpServletRequest request, HttpServletResponse response) {
        // Session fixation defence: never keep a pre-login session id.
        HttpSession existing = request.getSession(false);
        if (existing != null) {
            request.changeSessionId();
        }

        UsernamePasswordAuthenticationToken authentication = UsernamePasswordAuthenticationToken.authenticated(
                account.getUsername(),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + account.getRole().name())));

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);
    }
}
