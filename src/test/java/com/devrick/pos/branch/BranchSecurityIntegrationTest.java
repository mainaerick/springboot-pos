package com.devrick.pos.branch;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.devrick.pos.branch.dto.CreateBranchRequest;
import com.devrick.pos.common.enums.Role;
import com.devrick.pos.security.dto.LoginRequest;
import com.devrick.pos.tenant.entity.Tenant;
import com.devrick.pos.tenant.entity.TenantStatus;
import com.devrick.pos.tenant.repository.TenantRepository;
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
class BranchSecurityIntegrationTest {

    private static final String DEFAULT_TENANT_CODE = "DEFAULT";

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TenantRepository tenantRepository;

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
    void branchApiEnforcesTenantIsolationAndRoleBasedAccess() throws Exception {
        Tenant defaultTenant = defaultTenant();
        Tenant otherTenant = tenantRepository.saveAndFlush(createTenant("Second Business", "SECOND"));

        User admin = user(defaultTenant, "admin@example.com", "Password123", Role.ADMIN);
        User cashier = user(defaultTenant, "cashier@example.com", "Password123", Role.CASHIER);
        User otherAdmin = user(otherTenant, "other.admin@example.com", "Password123", Role.ADMIN);
        userRepository.saveAndFlush(admin);
        userRepository.saveAndFlush(cashier);
        userRepository.saveAndFlush(otherAdmin);

        AuthTokens adminTokens = login("admin@example.com", "Password123");
        AuthTokens cashierTokens = login("cashier@example.com", "Password123");
        AuthTokens otherTokens = login("other.admin@example.com", "Password123");

        String createdBranch = mockMvc.perform(post("/api/v1/branches")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminTokens.accessToken()))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateBranchRequest(
                                " Nairobi CBD ",
                                "nrb-cbd",
                                "nairobi@example.com",
                                "+254712345678",
                                "Kimathi Street",
                                "2nd Floor",
                                "Nairobi",
                                "Nairobi",
                                "00100",
                                "ke"))))
                .andExpect(status().isCreated())
                .andExpect(header().exists(HttpHeaders.LOCATION))
                .andExpect(jsonPath("$.code").value("NRB-CBD"))
                .andExpect(jsonPath("$.countryCode").value("KE"))
                .andExpect(jsonPath("$.active").value(true))
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);

        JsonNode createdBody = objectMapper.readTree(createdBranch);
        UUID branchId = UUID.fromString(createdBody.get("id").asText());

        mockMvc.perform(get("/api/v1/branches").header(HttpHeaders.AUTHORIZATION, bearer(cashierTokens.accessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));

        mockMvc.perform(get("/api/v1/branches/{branchId}", branchId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(cashierTokens.accessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Nairobi CBD"));

        mockMvc.perform(get("/api/v1/branches/{branchId}", UUID.randomUUID())
                        .header(HttpHeaders.AUTHORIZATION, bearer(cashierTokens.accessToken())))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/v1/branches/{branchId}", branchId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(otherTokens.accessToken())))
                .andExpect(status().isNotFound());

        mockMvc.perform(
                        post("/api/v1/branches")
                                .header(HttpHeaders.AUTHORIZATION, bearer(cashierTokens.accessToken()))
                                .contentType(APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(new CreateBranchRequest(
                                        "Cashier Branch",
                                        "CASHIER",
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null))))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/v1/branches/{branchId}", branchId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminTokens.accessToken()))
                        .contentType(APPLICATION_JSON)
                        .content(
                                """
                        {
                          "name": "Westlands",
                          "email": "westlands@example.com",
                          "phone": "+254700000000",
                          "addressLine1": "Waiyaki Way",
                          "addressLine2": "Suite 4",
                          "city": "Nairobi",
                          "stateOrCounty": "Nairobi",
                          "postalCode": "00200",
                          "countryCode": "ke"
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Westlands"))
                .andExpect(jsonPath("$.code").value("NRB-CBD"));

        mockMvc.perform(patch("/api/v1/branches/{branchId}/status", branchId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminTokens.accessToken()))
                        .contentType(APPLICATION_JSON)
                        .content("{\"active\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        mockMvc.perform(patch("/api/v1/branches/{branchId}/status", branchId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminTokens.accessToken()))
                        .contentType(APPLICATION_JSON)
                        .content("{\"active\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true));

        mockMvc.perform(get("/api/v1/branches")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminTokens.accessToken()))
                        .param("search", "nrb"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));

        mockMvc.perform(get("/api/v1/branches")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminTokens.accessToken()))
                        .param("active", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));

        mockMvc.perform(get("/api/v1/branches")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminTokens.accessToken()))
                        .param("active", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));

        mockMvc.perform(get("/api/v1/branches")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminTokens.accessToken()))
                        .param("sort", "unsupported,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(branchId.toString()));
    }

    @Test
    void branchValidationAndDuplicateResponsesAreReturned() throws Exception {
        Tenant defaultTenant = defaultTenant();
        User admin = user(defaultTenant, "validator@example.com", "Password123", Role.ADMIN);
        userRepository.saveAndFlush(admin);
        AuthTokens tokens = login("validator@example.com", "Password123");

        mockMvc.perform(post("/api/v1/branches")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokens.accessToken()))
                        .contentType(APPLICATION_JSON)
                        .content(
                                """
                        {
                          "name": "",
                          "code": "A"
                        }
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"));

        mockMvc.perform(post("/api/v1/branches")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokens.accessToken()))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateBranchRequest(
                                "Nairobi CBD",
                                "nrb-cbd",
                                "bad-email",
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                "ke"))))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/v1/branches")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokens.accessToken()))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateBranchRequest(
                                "Nairobi CBD",
                                "nrb-cbd",
                                "nairobi@example.com",
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                "k"))))
                .andExpect(status().isBadRequest());
    }

    private AuthTokens login(String email, String password) throws Exception {
        String loginResponse = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(email, password))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);

        JsonNode loginBody = objectMapper.readTree(loginResponse);
        return new AuthTokens(loginBody.get("accessToken").asText(), loginBody.get("refreshToken").asText());
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private Tenant defaultTenant() {
        return tenantRepository.findByCodeIgnoreCase(DEFAULT_TENANT_CODE).orElseThrow();
    }

    private Tenant createTenant(String name, String code) {
        Tenant tenant = new Tenant();
        tenant.setName(name);
        tenant.setCode(code);
        tenant.setStatus(TenantStatus.ACTIVE);
        return tenant;
    }

    private User user(Tenant tenant, String email, String password, Role role) {
        User user = new User();
        user.setFirstName("Test");
        user.setLastName("User");
        user.setEmail(email);
        user.setTenant(tenant);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(role);
        user.setEnabled(true);
        user.setMustChangePassword(false);
        return user;
    }

    private record AuthTokens(String accessToken, String refreshToken) {}
}
