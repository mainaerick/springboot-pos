package com.devrick.pos.security;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.devrick.pos.common.enums.Role;
import com.devrick.pos.security.dto.LoginRequest;
import com.devrick.pos.user.dto.CreateUserRequest;
import com.devrick.pos.user.dto.UpdateUserRequest;
import com.devrick.pos.user.entity.User;
import com.devrick.pos.user.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@Transactional
class AuthenticationSecurityIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    @Test
    void loginAuthenticationAndProtectedEndpointsWorkTogether() throws Exception {
        userRepository.saveAndFlush(createUser("john.doe@example.com", "Password123", Role.ADMIN));
        userRepository.saveAndFlush(createUser("jane.cashier@example.com", "Password123", Role.CASHIER));

        AuthTokens adminTokens = login("john.doe@example.com", "Password123");
        AuthTokens cashierTokens = login("jane.cashier@example.com", "Password123");

        mockMvc.perform(get("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION, bearer(adminTokens.accessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("john.doe@example.com"))
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.lastName").value("Doe"));

        mockMvc.perform(get("/api/v1/users")).andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/users").header(HttpHeaders.AUTHORIZATION, bearer(adminTokens.accessToken())))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/users").header(HttpHeaders.AUTHORIZATION, bearer(cashierTokens.accessToken())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));

        String createdUserResponse = mockMvc.perform(post("/api/v1/users")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminTokens.accessToken()))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateUserRequest(
                                "Alice", "Owner", "alice.owner@example.com", "Password123!", Role.MANAGER))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("MANAGER"))
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);

        JsonNode createdUserBody = objectMapper.readTree(createdUserResponse);
        UUID createdUserId = UUID.fromString(createdUserBody.get("id").asText());

        mockMvc.perform(put("/api/v1/users/{id}", createdUserId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminTokens.accessToken()))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateUserRequest(
                                "Alice", "Owner", "alice.owner@example.com", true, Role.ACCOUNTANT))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ACCOUNTANT"));

        mockMvc.perform(post("/api/v1/users")
                        .header(HttpHeaders.AUTHORIZATION, bearer(cashierTokens.accessToken()))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateUserRequest(
                                "Bob", "Cashier", "bob.cashier@example.com", "Password123!", Role.ADMIN))))
                .andExpect(status().isForbidden());

        mockMvc.perform(
                        post("/api/v1/users")
                                .header(HttpHeaders.AUTHORIZATION, bearer(adminTokens.accessToken()))
                                .contentType(APPLICATION_JSON)
                                .content(
                                        """
                        {
                          "firstName": "Invalid",
                          "lastName": "Role",
                          "email": "invalid.role@example.com",
                          "password": "Password123!",
                          "role": "NOT_A_ROLE"
                        }
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"));

        String refreshResponse = mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new com.devrick.pos.security.dto.RefreshTokenRequest(adminTokens.refreshToken()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.expiresIn").value(900))
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);

        JsonNode refreshBody = objectMapper.readTree(refreshResponse);
        String refreshedAccessToken = refreshBody.get("accessToken").asText();

        mockMvc.perform(get("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION, bearer(refreshedAccessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("john.doe@example.com"));

        mockMvc.perform(post("/api/v1/auth/logout")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminTokens.accessToken())))
                .andExpect(status().isNoContent());

        mockMvc.perform(
                        post("/api/v1/auth/refresh")
                                .contentType(APPLICATION_JSON)
                                .content(
                                        """
                        {
                          "refreshToken": "bad.refresh.token"
                        }
                        """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));

        mockMvc.perform(get("/api/v1/users").header(HttpHeaders.AUTHORIZATION, "Bearer bad.token.here"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginExposesMustChangePasswordFlagForTemporaryAccounts() throws Exception {
        User bootstrapUser = createUser("bootstrap.admin@example.com", "Password123", Role.SUPER_ADMIN);
        bootstrapUser.setMustChangePassword(true);
        userRepository.saveAndFlush(bootstrapUser);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest("bootstrap.admin@example.com", "Password123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mustChangePassword").value(true));
    }

    private AuthTokens login(String email, String password) throws Exception {
        String loginResponse = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(email, password))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(900))
                .andExpect(jsonPath("$.mustChangePassword").value(false))
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);

        JsonNode responseBody = objectMapper.readTree(loginResponse);
        return new AuthTokens(
                responseBody.get("accessToken").asText(),
                responseBody.get("refreshToken").asText());
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private User createUser(String email, String rawPassword, Role role) {
        User user = new User();
        user.setFirstName("John");
        user.setLastName(role == Role.CASHIER ? "Cashier" : "Doe");
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRole(role);
        user.setEnabled(true);
        user.setMustChangePassword(false);
        return user;
    }

    private record AuthTokens(String accessToken, String refreshToken) {}
}
