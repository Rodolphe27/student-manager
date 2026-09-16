package com.student_manager.feature.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

public class AuthDTO {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RegisterRequest {
        @NotBlank(message = "Username is required")
        private String username;

        @NotBlank(message = "Email is required")
        @Email(message = "Email is invalid")
        private String email;

        @NotBlank(message = "Password is required")
        private String password;

        /**
         * Optional code from an ADMIN-issued RegistrationInvite. When present,
         * it — not this request — determines the account's role and links it to
         * the invite's target Student/Teacher profile. Absent means a plain
         * self-registration, always STUDENT with no profile link.
         */
        private String registrationCode;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoginRequest {
        @NotBlank(message = "Username is required")
        private String username;

        @NotBlank(message = "Password is required")
        private String password;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuthResponse {
        private String token;
        private String username;
        private String email;
        private Role role;
    }
}
