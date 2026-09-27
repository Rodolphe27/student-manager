package com.student_manager.feature.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

// TODO(SEC-5) [HIGH]: no revocation/logout mechanism — a token stays valid for the full
// jwt.expiration window (default 24h) with no server-side denylist or refresh-token rotation,
// so a leaked/stolen token can't be invalidated before it expires. Add a jti denylist (e.g.
// Redis) checked in JwtFilter, or move to short-lived access tokens + refresh tokens.
@Slf4j
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(String username, String role) {
        return Jwts.builder()
                .subject(username)
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey())
                .compact();
    }

    public String extractUsername(String token) {
        return getClaims(token).getSubject();
    }

    public String extractRole(String token) {
        return getClaims(token).get("role", String.class);
    }

    // TODO(SEC-13) [LOW]: catches generic Exception instead of io.jsonwebtoken.JwtException —
    // masks unrelated bugs (e.g. NPEs) as "invalid token" and hides real parsing failures.
    public boolean isTokenValid(String token) {
        try {
            getClaims(token);
            return true;
        } catch (Exception e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    // jjwt 0.12's non-deprecated parsing API: parserBuilder()/parseClaimsJws()/
    // getBody() are all superseded by parser()/parseSignedClaims()/getPayload().
    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
