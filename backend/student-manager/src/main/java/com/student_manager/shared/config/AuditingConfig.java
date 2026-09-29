package com.student_manager.shared.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * Supplies the "who" for Spring Data auditing ({@code @CreatedBy} /
 * {@code @LastModifiedBy} on {@code BaseEntity}): the username of the
 * logged-in session. Anonymous requests (e.g. self-registration) and
 * background work have no auditor, so those fields stay {@code null}.
 */
@Configuration
public class AuditingConfig {

    /**
     * The current auditor, resolved per save from the security context.
     *
     * @return an {@link AuditorAware} yielding the logged-in username, if any
     */
    @Bean
    public AuditorAware<String> auditorAware() {
        return () -> {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null
                    || !authentication.isAuthenticated()
                    || authentication instanceof AnonymousAuthenticationToken) {
                return Optional.empty();
            }
            return Optional.ofNullable(authentication.getName());
        };
    }
}
