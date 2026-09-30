package com.student_manager.shared.config;

import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

import java.util.Arrays;

/**
 * Central Spring Security configuration: session-cookie authentication (the
 * session itself lives in Postgres via Spring Session JDBC), CSRF protection,
 * CORS, HSTS, and the per-endpoint authorization rules for every feature area
 * (auth, courses, students, teachers, enrollments).
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Value("${cors.allowed-origins:http://localhost:5173,http://localhost}")
    private String allowedOrigins;

    @Value("${cors.allowed-origin-patterns:}")
    private String allowedOriginPatterns;

    /**
     * Builds the main security filter chain: session-based authentication
     * (established by {@code AuthController} on login), CSRF via an
     * {@code XSRF-TOKEN} cookie echoed back in the {@code X-XSRF-TOKEN} header,
     * HSTS, the per-endpoint authorization rules, and {@code POST /api/auth/logout}.
     * A request without a valid session gets 401; a valid session with the
     * wrong role gets 403.
     *
     * @param http the {@link HttpSecurity} builder to configure
     * @return the configured {@link SecurityFilterChain}
     * @throws Exception if the security configuration fails to build
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // Cookie auth is sent automatically by the browser, so state-changing requests
                // need a CSRF token. The token cookie is JS-readable (by design) and axios
                // copies it into the X-XSRF-TOKEN header on same-origin requests.
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler()))
                .securityContext(ctx -> ctx.securityContextRepository(securityContextRepository()))
                // No session / expired session → 401 (the frontend logs out on 401).
                .exceptionHandling(e -> e.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .logout(logout -> logout
                        .logoutUrl("/api/auth/logout")
                        .logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler(HttpStatus.NO_CONTENT))
                        .deleteCookies("SESSION"))
                .headers(headers -> headers
                        // Render/Vercel terminate TLS upstream; emit HSTS so browsers
                        // pin HTTPS even though the app itself sees forwarded HTTP.
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .maxAgeInSeconds(31_536_000)))
                .authorizeHttpRequests(auth -> auth
                        // Spring Boot's error page. Rejections such as a missing CSRF token
                        // (403) are rendered by forwarding to /error; if /error itself needed a
                        // login, an anonymous caller would get 401 instead of the real status.
                        .requestMatchers("/error").permitAll()
                        // "Who am I?" — used by the frontend on startup to restore the session.
                        .requestMatchers(HttpMethod.GET, "/api/auth/me").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        // Accounts are created by an ADMIN only — no public self-registration.
                        .requestMatchers(HttpMethod.POST, "/api/auth/register").hasRole("ADMIN")
                        // API docs require a login so the staff-only endpoint shapes are not
                        // public. Log in to the app in the same browser first; Swagger UI then
                        // reuses the session cookie.
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").authenticated()
                        .requestMatchers("/actuator/health/**").permitAll()

                        // Terms: every user sees them (courses show their term); ADMIN adds them.
                        .requestMatchers(HttpMethod.GET, "/api/terms/**").authenticated()
                        .requestMatchers("/api/terms/**").hasRole("ADMIN")

                        // Course catalogue: any authenticated user may browse it;
                        // only staff may create / update / delete courses.
                        // The id/code/title dropdown list feeds staff forms only.
                        .requestMatchers(HttpMethod.GET, "/api/courses/options").hasAnyRole("TEACHER", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/courses/**").authenticated()
                        .requestMatchers("/api/courses/**").hasAnyRole("TEACHER", "ADMIN")

                        // A student may look up their own record and their own
                        // enrolment list (the "My Courses" view).
                        .requestMatchers(HttpMethod.GET, "/api/students/me").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/enrollments/student/**").authenticated()

                        // Student roster: staff may read it; only ADMIN may mutate it.
                        .requestMatchers(HttpMethod.GET, "/api/students/**").hasAnyRole("TEACHER", "ADMIN")
                        .requestMatchers("/api/students/**").hasRole("ADMIN")

                        // Teacher roster: same shape as students — staff may read, only
                        // ADMIN may mutate.
                        .requestMatchers(HttpMethod.GET, "/api/teachers/**").hasAnyRole("TEACHER", "ADMIN")
                        .requestMatchers("/api/teachers/**").hasRole("ADMIN")

                        // A student may enrol themselves in a course; the controller's
                        // @PreAuthorize (via ownershipGuard) then restricts the studentId
                        // in the request body to their own — this rule alone would let
                        // any student enrol anyone.
                        .requestMatchers(HttpMethod.POST, "/api/enrollments").hasAnyRole("STUDENT", "TEACHER", "ADMIN")

                        // A student may withdraw their own PENDING enrolment; the controller's
                        // @PreAuthorize (canCancelEnrollment) enforces "own" and "pending".
                        .requestMatchers(HttpMethod.PATCH, "/api/enrollments/*/cancel").hasAnyRole("STUDENT", "TEACHER", "ADMIN")

                        // Only a student acknowledges their own grade ("new grade" notification).
                        .requestMatchers(HttpMethod.PATCH, "/api/enrollments/*/grade-seen").hasRole("STUDENT")

                        // Every other enrolment operation (list all, confirm/cancel, grade)
                        // is staff-only; deleting an enrolment is ADMIN-only.
                        .requestMatchers(HttpMethod.DELETE, "/api/enrollments/**").hasRole("ADMIN")
                        .requestMatchers("/api/enrollments/**").hasAnyRole("TEACHER", "ADMIN")

                        .anyRequest().authenticated());

        return http.build();
    }

    /**
     * Where the logged-in {@code SecurityContext} is kept between requests: the
     * HTTP session, which Spring Session JDBC persists in Postgres. Shared with
     * {@code SessionLogin}, which saves the context on login/register.
     *
     * @return an {@link HttpSessionSecurityContextRepository}
     */
    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    /**
     * Password encoder used for hashing and verifying account passwords.
     *
     * @return a {@link BCryptPasswordEncoder} bean
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // TODO(SEC-9) [MEDIUM]: allowed-origin-patterns is fully operator-configurable and combined
    // with allowCredentials(true) below — if this property is ever set to a broad wildcard in
    // production it becomes a permissive credentialed CORS policy. Validate/reject wildcard
    // patterns at startup, or drop pattern-based origins entirely in favor of an explicit list.
    /**
     * Builds the CORS configuration applied to every endpoint, from the
     * {@code cors.allowed-origins} and {@code cors.allowed-origin-patterns}
     * properties.
     *
     * @return a {@link CorsConfigurationSource} registered for all paths
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.asList(allowedOrigins.split(",")));
        if (!allowedOriginPatterns.isBlank()) {
            config.setAllowedOriginPatterns(Arrays.asList(allowedOriginPatterns.split(",")));
        }
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

}
