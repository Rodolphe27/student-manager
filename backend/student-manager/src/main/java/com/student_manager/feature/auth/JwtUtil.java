package com.student_manager.feature.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Slf4j
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    public String generateToken(String username, String role) {
        return Jwts.builder()
                .subject(username)                    // ✅ new API
                .claim("role", role)
                .issuedAt(new Date())                 // ✅ new API
                .expiration(new Date(System.currentTimeMillis() + expiration)) // ✅ new API
                .signWith(getSigningKey())             // ✅ new API (algorithm auto-detected)
                .compact();
    }

    public String extractUsername(String token) {
        return getClaims(token).getSubject();
    }

    public String extractRole(String token) {
        return getClaims(token).get("role", String.class);
    }

    public boolean isTokenValid(String token) {
        try {
            getClaims(token);
            return true;
        } catch (Exception e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    private Claims getClaims(String token) {
        return Jwts.parser()                         // ✅ new API (parserBuilder() deprecated)
                .verifyWith(getSigningKey())          // ✅ new API
                .build()
                .parseSignedClaims(token)            // ✅ new API (parseClaimsJws() deprecated)
                .getPayload();                       // ✅ new API (getBody() deprecated)
    }
}
