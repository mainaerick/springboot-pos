package com.devrick.pos.security.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.devrick.pos.common.enums.Role;
import com.devrick.pos.exception.GlobalExceptionHandler;
import com.devrick.pos.security.dto.LoginResponse;
import com.devrick.pos.security.dto.RefreshTokenResponse;
import com.devrick.pos.security.service.AuthenticationService;
import com.devrick.pos.user.dto.UserResponse;
import java.security.Principal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class AuthenticationControllerTest {

    @Mock
    private AuthenticationService authenticationService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AuthenticationController(authenticationService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void loginReturnsTokenResponse() throws Exception {
        when(authenticationService.login(any()))
                .thenReturn(new LoginResponse("access-token", "refresh-token", "Bearer", 900, false));

        mockMvc.perform(
                        post("/api/v1/auth/login")
                                .contentType(APPLICATION_JSON)
                                .content(
                                        """
                                {
                                  "email": "john@example.com",
                                  "password": "Password123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(900))
                .andExpect(jsonPath("$.mustChangePassword").value(false));
    }

    @Test
    void refreshReturnsNewAccessToken() throws Exception {
        when(authenticationService.refresh(any())).thenReturn(new RefreshTokenResponse("new-access-token", 900));

        mockMvc.perform(
                        post("/api/v1/auth/refresh")
                                .contentType(APPLICATION_JSON)
                                .content(
                                        """
                        {
                          "refreshToken": "refresh-token"
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-access-token"))
                .andExpect(jsonPath("$.expiresIn").value(900));
    }

    @Test
    void logoutReturnsNoContent() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout").principal(() -> "john@example.com"))
                .andExpect(status().isNoContent());

        verify(authenticationService).logout();
    }

    @Test
    void meReturnsCurrentUser() throws Exception {
        when(authenticationService.getCurrentUser(any(Principal.class)))
                .thenReturn(new UserResponse(
                        UUID.fromString("11111111-1111-1111-1111-111111111111"),
                        "John",
                        "Doe",
                        "john@example.com",
                        Role.ADMIN,
                        true,
                        Instant.parse("2026-08-02T10:00:00Z"),
                        Instant.parse("2026-08-02T10:05:00Z")));

        mockMvc.perform(get("/api/v1/auth/me").principal(() -> "john@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("john@example.com"))
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.lastName").value("Doe"));
    }
}
