package com.student_manager.shared.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Single place that logs one line per HTTP request: method, path, response
 * status and duration in ms. Registered automatically as a {@code @Component}
 * — Spring Boot picks up any {@code Filter} bean and adds it to the servlet
 * filter chain, no wiring in {@code SecurityConfig} needed.
 * <p>
 * Replaces the per-method {@code log.info("GET /api/...")} calls that used to
 * be hand-written into every controller. Those lines are left in place but
 * commented out for reference — they were easy to forget on a new endpoint,
 * duplicated between controller and service, and never added information this
 * filter doesn't already capture.
 * <p>
 * Health/liveness probes are excluded so container-platform polling (Render
 * etc., see {@code management.endpoint.health.probes} in application.yml)
 * doesn't spam the log every few seconds.
 */
@Slf4j
@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

    /**
     * Excludes actuator endpoints (health/liveness probes) from request logging.
     *
     * @param request the incoming HTTP request
     * @return {@code true} if the request path starts with {@code /actuator}
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/actuator");
    }

    /**
     * Times the downstream filter chain and logs one line with the request
     * method, path, resulting status code, and duration in milliseconds.
     *
     * @param request     the incoming HTTP request
     * @param response    the outgoing HTTP response
     * @param filterChain the remaining filter chain to invoke
     * @throws ServletException if the underlying filter chain throws one
     * @throws IOException      if the underlying filter chain throws one
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {
        long start = System.currentTimeMillis();
        try {
            filterChain.doFilter(request, response);
        } finally {
            long durationMs = System.currentTimeMillis() - start;
            log.info("{} {} -> {} ({} ms)",
                    request.getMethod(), request.getRequestURI(), response.getStatus(), durationMs);
        }
    }
}
