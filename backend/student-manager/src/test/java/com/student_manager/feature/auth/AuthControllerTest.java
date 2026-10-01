package com.student_manager.feature.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    // Accounts are created by an ADMIN only (POST /api/auth/register), so every
    // registration call below is made as one.
    private static RequestPostProcessor asAdmin() {
        RequestPostProcessor principal = user("admin-user").roles("ADMIN");
        return request -> csrf().postProcessRequest(principal.postProcessRequest(request));
    }

    @Test
    void registerWithABlankUsernameIsRejectedAsABadRequest() throws Exception {
        RegisterRequest request = new RegisterRequest("", "user@example.com", "Password123!", Role.STUDENT);

        mockMvc.perform(post("/api/auth/register").with(asAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registerWithABlankPasswordIsRejectedAsABadRequest() throws Exception {
        RegisterRequest request = new RegisterRequest("passwordtestuser", "user@example.com", "", Role.STUDENT);

        mockMvc.perform(post("/api/auth/register").with(asAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registerWithAnInvalidEmailIsRejectedAsABadRequest() throws Exception {
        RegisterRequest request = new RegisterRequest("emailtestuser", "not-an-email", "Password123!", Role.STUDENT);

        mockMvc.perform(post("/api/auth/register").with(asAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void adminCreatedAccountGetsTheRequestedRole() throws Exception {
        RegisterRequest request = new RegisterRequest("newteacher", "newteacher@example.com", "Password123!", Role.TEACHER);

        mockMvc.perform(post("/api/auth/register").with(asAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("TEACHER"));
    }

    @Test
    void anAnonymousCallerCannotRegisterAnAccountAtAll() throws Exception {
        // Regression test for issue #43: an anonymous caller must not be able to
        // mint any account, least of all an elevated one, via the register endpoint.
        String bodyWithAdminRole = """
                {"username":"escalator","email":"escalator@example.com","password":"Password123!","role":"ADMIN"}
                """;

        mockMvc.perform(post("/api/auth/register").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyWithAdminRole))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginWithABlankUsernameIsRejectedAsABadRequest() throws Exception {
        LoginRequest request = new LoginRequest("", "Password123!");

        mockMvc.perform(post("/api/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void loginWithAnUnknownUserReturnsAGenericUnauthorized() throws Exception {
        LoginRequest request = new LoginRequest("no-such-user-xyz", "whatever");

        mockMvc.perform(post("/api/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid username or password"));
    }

    @Test
    void loginWithTheWrongPasswordReturnsTheSameGenericUnauthorized() throws Exception {
        RegisterRequest signup =
                new RegisterRequest("pwdcheckuser", "pwdcheckuser@example.com", "Password123!", Role.STUDENT);
        mockMvc.perform(post("/api/auth/register").with(asAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signup)))
                .andExpect(status().isCreated());

        LoginRequest badLogin = new LoginRequest("pwdcheckuser", "WrongPassword!");

        mockMvc.perform(post("/api/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badLogin)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid username or password"));
    }
}
