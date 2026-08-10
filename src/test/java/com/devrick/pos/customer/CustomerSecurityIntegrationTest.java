package com.devrick.pos.customer;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
class CustomerSecurityIntegrationTest {

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
    void customerApiEnforcesTenantIsolationRoleChecksAndValidation() throws Exception {
        Tenant defaultTenant = defaultTenant();
        Tenant otherTenant = tenantRepository.saveAndFlush(createTenant("Second Business", "SECOND"));

        User admin = user(defaultTenant, "admin@example.com", "Password123", Role.ADMIN);
        User cashier = user(defaultTenant, "cashier@example.com", "Password123", Role.CASHIER);
        User manager = user(defaultTenant, "manager@example.com", "Password123", Role.MANAGER);
        User inventoryClerk = user(defaultTenant, "inventory@example.com", "Password123", Role.INVENTORY_CLERK);
        User otherAdmin = user(otherTenant, "other.admin@example.com", "Password123", Role.ADMIN);
        userRepository.saveAndFlush(admin);
        userRepository.saveAndFlush(cashier);
        userRepository.saveAndFlush(manager);
        userRepository.saveAndFlush(inventoryClerk);
        userRepository.saveAndFlush(otherAdmin);

        AuthTokens adminTokens = login("admin@example.com", "Password123");
        AuthTokens cashierTokens = login("cashier@example.com", "Password123");
        AuthTokens managerTokens = login("manager@example.com", "Password123");
        AuthTokens inventoryTokens = login("inventory@example.com", "Password123");
        AuthTokens otherTokens = login("other.admin@example.com", "Password123");

        String createdCustomer = mockMvc.perform(post("/api/v1/customers")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminTokens.accessToken()))
                        .contentType(APPLICATION_JSON)
                        .content(
                                """
                        {
                          "type": "INDIVIDUAL",
                          "code": "cus-0001",
                          "name": " Jane Wanjiku ",
                          "email": "Jane@Example.com",
                          "phone": "+254712345678",
                          "taxNumber": "P051234567X",
                          "addressLine1": "Kimathi Street",
                          "city": "Nairobi",
                          "stateOrCounty": "Nairobi",
                          "postalCode": "00100",
                          "countryCode": "ke",
                          "notes": "Prefers SMS communication"
                        }
                        """))
                .andExpect(status().isCreated())
                .andExpect(header().exists(HttpHeaders.LOCATION))
                .andExpect(jsonPath("$.code").value("CUS-0001"))
                .andExpect(jsonPath("$.countryCode").value("KE"))
                .andExpect(jsonPath("$.active").value(true))
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);

        JsonNode createdBody = objectMapper.readTree(createdCustomer);
        UUID customerId = UUID.fromString(createdBody.get("id").asText());

        mockMvc.perform(post("/api/v1/customers")
                        .header(HttpHeaders.AUTHORIZATION, bearer(cashierTokens.accessToken()))
                        .contentType(APPLICATION_JSON)
                        .content(
                                """
                        {
                          "type": "BUSINESS",
                          "code": "cus-0002",
                          "name": "Sunrise Hotel Ltd"
                        }
                        """))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/customers")
                        .header(HttpHeaders.AUTHORIZATION, bearer(inventoryTokens.accessToken()))
                        .contentType(APPLICATION_JSON)
                        .content(
                                """
                        {
                          "type": "INDIVIDUAL",
                          "code": "cus-1000",
                          "name": "Blocked User"
                        }
                        """))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/customers")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminTokens.accessToken()))
                        .contentType(APPLICATION_JSON)
                        .content(
                                """
                        {
                          "code": "cus-1001",
                          "name": "Missing Type"
                        }
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.type").exists());

        mockMvc.perform(post("/api/v1/customers")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminTokens.accessToken()))
                        .contentType(APPLICATION_JSON)
                        .content(
                                """
                        {
                          "type": "INDIVIDUAL",
                          "code": "a",
                          "name": "Jane"
                        }
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.code").exists());

        mockMvc.perform(post("/api/v1/customers")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminTokens.accessToken()))
                        .contentType(APPLICATION_JSON)
                        .content(
                                """
                        {
                          "type": "INDIVIDUAL",
                          "code": "cus-0001",
                          "name": "Duplicate Code"
                        }
                        """))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/api/v1/customers")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminTokens.accessToken()))
                        .contentType(APPLICATION_JSON)
                        .content(
                                """
                        {
                          "type": "INDIVIDUAL",
                          "code": "cus-0001",
                          "name": "Other Tenant"
                        }
                        """))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/api/v1/customers"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/customers")
                        .header(HttpHeaders.AUTHORIZATION, bearer(cashierTokens.accessToken()))
                        .param("search", "cus")
                        .param("type", "INDIVIDUAL")
                        .param("active", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));

        mockMvc.perform(get("/api/v1/customers")
                        .header(HttpHeaders.AUTHORIZATION, bearer(managerTokens.accessToken()))
                        .param("search", "jane"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));

        mockMvc.perform(get("/api/v1/customers/{customerId}", customerId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(cashierTokens.accessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("CUS-0001"));

        mockMvc.perform(get("/api/v1/customers/{customerId}", UUID.randomUUID())
                        .header(HttpHeaders.AUTHORIZATION, bearer(cashierTokens.accessToken())))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/v1/customers/{customerId}", customerId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(otherTokens.accessToken())))
                .andExpect(status().isNotFound());

        mockMvc.perform(put("/api/v1/customers/{customerId}", customerId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminTokens.accessToken()))
                        .contentType(APPLICATION_JSON)
                        .content(
                                """
                        {
                          "type": "BUSINESS",
                          "name": "Sunrise Hotel Ltd",
                          "email": "accounts@example.com",
                          "phone": "+254700000000",
                          "taxNumber": "P051234567X",
                          "addressLine1": "Kimathi Street",
                          "addressLine2": "2nd Floor",
                          "city": "Nairobi",
                          "stateOrCounty": "Nairobi",
                          "postalCode": "00100",
                          "countryCode": "ke",
                          "notes": "Main account"
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("CUS-0001"))
                .andExpect(jsonPath("$.type").value("BUSINESS"));

        mockMvc.perform(put("/api/v1/customers/{customerId}", customerId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(otherTokens.accessToken()))
                        .contentType(APPLICATION_JSON)
                        .content(
                                """
                        {
                          "type": "BUSINESS",
                          "name": "Cross Tenant",
                          "email": "cross@example.com"
                        }
                        """))
                .andExpect(status().isNotFound());

        mockMvc.perform(patch("/api/v1/customers/{customerId}/status", customerId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(managerTokens.accessToken()))
                        .contentType(APPLICATION_JSON)
                        .content("{\"active\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        mockMvc.perform(patch("/api/v1/customers/{customerId}/status", customerId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(managerTokens.accessToken()))
                        .contentType(APPLICATION_JSON)
                        .content("{\"active\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true));

        mockMvc.perform(patch("/api/v1/customers/{customerId}/status", customerId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(cashierTokens.accessToken()))
                        .contentType(APPLICATION_JSON)
                        .content("{\"active\":false}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/v1/customers/{customerId}/status", customerId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(otherTokens.accessToken()))
                        .contentType(APPLICATION_JSON)
                        .content("{\"active\":false}"))
                .andExpect(status().isNotFound());
    }

    private AuthTokens login(String email, String password) throws Exception {
        String response = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(email, password))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);

        JsonNode body = objectMapper.readTree(response);
        return new AuthTokens(body.get("accessToken").asText(), body.get("refreshToken").asText());
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private Tenant defaultTenant() {
        return tenantRepository.findByCodeIgnoreCase("DEFAULT").orElseThrow();
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
        user.setFirstName(role.name());
        user.setLastName("User");
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setEnabled(true);
        user.setMustChangePassword(false);
        user.setRole(role);
        user.setTenant(tenant);
        return user;
    }

    private record AuthTokens(String accessToken, String refreshToken) {}
}
