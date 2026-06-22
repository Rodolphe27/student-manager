package com.student_manager.feature.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthDTO.AuthResponse> register(
            @RequestBody AuthDTO.RegisterRequest request) {
        log.info("POST /api/auth/register");
        return ResponseEntity.status(201).body(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthDTO.AuthResponse> login(
            @RequestBody AuthDTO.LoginRequest request) {
        log.info("POST /api/auth/login");
        return ResponseEntity.ok(authService.login(request));
    }
}
