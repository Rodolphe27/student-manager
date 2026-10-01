package com.student_manager.feature.auth;

/**
 * Application service for account registration, login, and the current-account
 * lookup, backing {@link AuthController}.
 */
public interface AuthService {

    /**
     * Creates a new account with the requested role (ADMIN-only).
     *
     * @param request the registration payload
     * @return an account summary for the newly created user
     */
    AuthResponse register(RegisterRequest request);

    /**
     * Authenticates an account by username and password.
     *
     * @param request the login credentials
     * @return an account summary for the authenticated user
     */
    AuthResponse login(LoginRequest request);

    /**
     * Looks up the account behind the current session.
     *
     * @param username the session principal's username
     * @return an account summary for that user
     */
    AuthResponse currentAccount(String username);
}
