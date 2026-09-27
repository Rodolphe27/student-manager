package com.student_manager.feature.auth;

/**
 * Application service for account registration and login, backing
 * {@link AuthController}.
 */
public interface AuthService {

    /**
     * Registers a new account, optionally claiming a registration invite.
     *
     * @param request the registration payload
     * @return a JWT and account summary for the newly created user
     */
    AuthResponse register(RegisterRequest request);

    /**
     * Authenticates an account by username and password.
     *
     * @param request the login credentials
     * @return a JWT and account summary for the authenticated user
     */
    AuthResponse login(LoginRequest request);
}
