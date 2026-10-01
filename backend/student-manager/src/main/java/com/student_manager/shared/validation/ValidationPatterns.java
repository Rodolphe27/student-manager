package com.student_manager.shared.validation;

/**
 * Regular expressions and messages shared by every request that carries account
 * credentials, so the rules cannot drift apart between endpoints.
 */
public final class ValidationPatterns {

    public static final String USERNAME = "^[a-zA-Z0-9_.-]{3,32}$";
    public static final String USERNAME_MESSAGE =
            "Username must be 3-32 characters: letters, digits, '.', '_' or '-'";

    public static final String PASSWORD = "^(?=.*[A-Za-z])(?=.*\\d).{8,}$";
    public static final String PASSWORD_MESSAGE =
            "Password must be at least 8 characters and include a letter and a digit";

    private ValidationPatterns() {
    }
}
