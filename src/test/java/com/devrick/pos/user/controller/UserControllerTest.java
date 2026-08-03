package com.devrick.pos.user.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.devrick.pos.common.enums.Role;
import com.devrick.pos.exception.GlobalExceptionHandler;
import com.devrick.pos.user.dto.UserResponse;
import com.devrick.pos.user.service.UserService;
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
class UserControllerTest {

    @Mock
    private UserService userService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new UserController(userService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void createReturnsRoleInResponse() throws Exception {
        when(userService.create(any())).thenReturn(response(Role.MANAGER));

        mockMvc.perform(
                        post("/api/v1/users")
                                .contentType(APPLICATION_JSON)
                                .content(
                                        """
                        {
                          "firstName": "Jane",
                          "lastName": "Doe",
                          "email": "jane@example.com",
                          "password": "Password123!",
                          "role": "MANAGER"
                        }
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("MANAGER"));
    }

    @Test
    void updateReturnsRoleInResponse() throws Exception {
        when(userService.update(any(UUID.class), any())).thenReturn(response(Role.ACCOUNTANT));

        mockMvc.perform(
                        put("/api/v1/users/{id}", UUID.fromString("11111111-1111-1111-1111-111111111111"))
                                .contentType(APPLICATION_JSON)
                                .content(
                                        """
                        {
                          "firstName": "Jane",
                          "lastName": "Doe",
                          "email": "jane@example.com",
                          "enabled": true,
                          "role": "ACCOUNTANT"
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ACCOUNTANT"));
    }

    private UserResponse response(Role role) {
        return new UserResponse(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                "Jane",
                "Doe",
                "jane@example.com",
                role,
                true,
                Instant.parse("2026-08-02T10:00:00Z"),
                Instant.parse("2026-08-02T10:05:00Z"));
    }
}
