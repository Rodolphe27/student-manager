package com.student_manager.feature.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", "test-secret-key-must-be-long-enough-for-hs256-signing");
        ReflectionTestUtils.setField(jwtUtil, "expiration", 3600_000L);
    }

    @Test
    void generatedTokenRoundTripsUsernameAndRole() {
        String token = jwtUtil.generateToken("alice", "ADMIN");

        assertThat(jwtUtil.extractUsername(token)).isEqualTo("alice");
        assertThat(jwtUtil.extractRole(token)).isEqualTo("ADMIN");
        assertThat(jwtUtil.isTokenValid(token)).isTrue();
    }

    @Test
    void expiredTokenIsInvalid() {
        ReflectionTestUtils.setField(jwtUtil, "expiration", -1000L);
        String expiredToken = jwtUtil.generateToken("alice", "STUDENT");

        assertThat(jwtUtil.isTokenValid(expiredToken)).isFalse();
    }

    @Test
    void tokenSignedWithADifferentSecretIsInvalid() {
        String token = jwtUtil.generateToken("alice", "STUDENT");

        JwtUtil otherJwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(otherJwtUtil, "secret", "a-completely-different-secret-key-of-sufficient-length");
        ReflectionTestUtils.setField(otherJwtUtil, "expiration", 3600_000L);

        assertThat(otherJwtUtil.isTokenValid(token)).isFalse();
    }

    @Test
    void malformedTokenIsInvalidAndDoesNotThrow() {
        assertThat(jwtUtil.isTokenValid("not-a-jwt-at-all")).isFalse();
    }
}
