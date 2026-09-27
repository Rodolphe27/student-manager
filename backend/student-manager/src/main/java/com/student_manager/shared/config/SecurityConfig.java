package com.student_manager.shared.config;

import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.List;

import com.student_manager.feature.auth.JwtFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.Arrays;

/**
 * Central Spring Security configuration: stateless JWT authentication, CORS,
 * HSTS, and the per-endpoint authorization rules for every feature area
 * (auth, courses, students, teachers, enrollments).
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtFilter jwtFilter;

    @Value("${cors.allowed-origins:http://localhost:5173,http://localhost}")
    private String allowedOrigins;

    @Value("${cors.allowed-origin-patterns:}")
    private String allowedOriginPatterns;

    /**
     * Builds the main security filter chain: disables CSRF (stateless JWT
     * API), enforces stateless sessions, enables HSTS, installs the
     * per-endpoint authorization rules, and inserts {@link JwtFilter} ahead of
     * Spring Security's own username/password filter.
     *
     * @param http the {@link HttpSecurity} builder to configure
     * @return the configured {@link SecurityFilterChain}
     * @throws Exception if the security configuration fails to build
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(headers -> headers
                        // Railway/Vercel terminate TLS upstream; emit HSTS so browsers
                        // pin HTTPS even though the app itself sees forwarded HTTP.
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .maxAgeInSeconds(31_536_000)))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()
                        // Requires a valid JWT to view — was permitAll(), which let anyone on the
                        // internet browse the full API surface (including staff-only endpoint
                        // shapes) with no login. To use Swagger UI locally, paste a Bearer token
                        // into its "Authorize" dialog, same as testing any other protected endpoint.
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").authenticated()
                        .requestMatchers("/actuator/health/**").permitAll()

                        // Course catalogue: any authenticated user may browse it;
                        // only staff may create / update / delete courses.
                        .requestMatchers(HttpMethod.GET, "/api/courses/**").authenticated()
                        .requestMatchers("/api/courses/**").hasAnyRole("TEACHER", "ADMIN")

                        // A student may look up their own record and their own
                        // enrolment list (the "My Courses" view).
                        .requestMatchers(HttpMethod.GET, "/api/students/me").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/enrollments/student/**").authenticated()

                        // Student roster: staff may read it; only ADMIN may mutate it.
                        // (Covers POST /api/students/{id}/invite too — ADMIN-only, same as
                        // every other non-GET student operation.)
                        .requestMatchers(HttpMethod.GET, "/api/students/**").hasAnyRole("TEACHER", "ADMIN")
                        .requestMatchers("/api/students/**").hasRole("ADMIN")

                        // Teacher roster: same shape as students — staff may read, only
                        // ADMIN may mutate or issue an invite.
                        .requestMatchers(HttpMethod.GET, "/api/teachers/**").hasAnyRole("TEACHER", "ADMIN")
                        .requestMatchers("/api/teachers/**").hasRole("ADMIN")

                        // A student may enrol themselves in a course; the controller's
                        // @PreAuthorize (via ownershipGuard) then restricts the studentId
                        // in the request body to their own — this rule alone would let
                        // any student enrol anyone.
                        .requestMatchers(HttpMethod.POST, "/api/enrollments").hasAnyRole("STUDENT", "TEACHER", "ADMIN")

                        // Every other enrolment operation (list all, confirm/cancel, grade)
                        // is staff-only; deleting an enrolment is ADMIN-only.
                        .requestMatchers(HttpMethod.DELETE, "/api/enrollments/**").hasRole("ADMIN")
                        .requestMatchers("/api/enrollments/**").hasAnyRole("TEACHER", "ADMIN")

                        .anyRequest().authenticated())
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
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
