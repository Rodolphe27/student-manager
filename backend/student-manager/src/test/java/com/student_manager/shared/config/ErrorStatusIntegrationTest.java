package com.student_manager.shared.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Checks the HTTP status a real client sees for rejected requests. Unlike
 * MockMvc, this runs an actual server, so Spring Security's rejections go
 * through the servlet container's forward to {@code /error} — the step that
 * turned a CSRF 403 into a 401 while {@code /error} required a login.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ErrorStatusIntegrationTest {

    @Autowired private TestRestTemplate rest;

    @Test
    void anonymousPostWithoutCsrfTokenIsForbiddenNotUnauthorized() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<String> response = rest.postForEntity(
                "/api/auth/login",
                new HttpEntity<>("{\"username\":\"nobody\",\"password\":\"wrong\"}", headers),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void protectedEndpointWithoutSessionIsStillUnauthorized() {
        ResponseEntity<String> response = rest.getForEntity("/api/students", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
