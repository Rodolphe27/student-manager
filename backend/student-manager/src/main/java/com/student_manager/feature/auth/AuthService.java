package com.student_manager.feature.auth;

public interface AuthService {
    AuthDTO.AuthResponse register(AuthDTO.RegisterRequest request);
    AuthDTO.AuthResponse login(AuthDTO.LoginRequest request);
}
