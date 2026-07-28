package com.freshlab.freshdoctor.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.freshlab.freshdoctor.dto.SignupRequest;
import com.freshlab.freshdoctor.dto.SignupResponse;
import com.freshlab.freshdoctor.dto.LoginRequest;
import com.freshlab.freshdoctor.dto.LoginResponse;
import com.freshlab.freshdoctor.exception.DuplicateEmailException;
import com.freshlab.freshdoctor.exception.InvalidCredentialsException;
import com.freshlab.freshdoctor.exception.GlobalExceptionHandler;
import com.freshlab.freshdoctor.service.AuthService;
import com.freshlab.freshdoctor.service.EmailVerificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static com.freshlab.freshdoctor.support.MockMvcTestSupport.standaloneSetup;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private AuthService authService;
    private EmailVerificationService emailVerificationService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        authService = mock(AuthService.class);
        emailVerificationService = mock(EmailVerificationService.class);
        mockMvc = standaloneSetup(new AuthController(authService, emailVerificationService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void signupReturnsCreated() throws Exception {
        when(authService.signup(any(SignupRequest.class))).thenReturn(
                new SignupResponse(
                        1L, "user@example.com", "fresh-user", "서울",
                        LocalDateTime.of(2026, 7, 17, 12, 0)
                )
        );

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                signupRequest()
                        )))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(1L))
                .andExpect(jsonPath("$.email").value("user@example.com"))
                .andExpect(jsonPath("$.nickname").value("fresh-user"))
                .andExpect(jsonPath("$.region").value("서울"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void signupReturnsConflictForDuplicateEmail() throws Exception {
        when(authService.signup(any(SignupRequest.class))).thenThrow(new DuplicateEmailException());

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                signupRequest()
                        )))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_EMAIL"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.path").value("/api/auth/signup"));
    }

    @Test
    void signupReturnsBadRequestForInvalidInput() throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"invalid\",\"password\":\"short\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    void loginReturnsAccessToken() throws Exception {
        when(authService.login(any(LoginRequest.class))).thenReturn(
                new LoginResponse("signed.jwt.token", "Bearer", 3600L)
        );

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest("user@example.com", "password123")
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("signed.jwt.token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(3600L));
    }

    @Test
    void loginReturnsUnauthorizedForInvalidCredentials() throws Exception {
        when(authService.login(any(LoginRequest.class))).thenThrow(new InvalidCredentialsException());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest("user@example.com", "wrong-password")
                        )))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    private SignupRequest signupRequest() {
        return new SignupRequest(
                "user@example.com", "password123", "password123", "fresh-user", "서울"
        );
    }
}
