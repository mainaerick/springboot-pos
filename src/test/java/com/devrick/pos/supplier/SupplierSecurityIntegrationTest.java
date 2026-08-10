package com.devrick.pos.supplier;

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
import com.devrick.pos.supplier.dto.CreateSupplierRequest;
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
class SupplierSecurityIntegrationTest {

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
    void supplierApiEnforcesTenantIsolationRoleChecksAndValidation() throws Exception {
        Tenant defaultTenant = defaultTenant();
        Tenant otherTenant = tenantRepository.saveAndFlush(createTenant("Second Business", "SECOND"));

        User admin = user(defaultTenant, "supplier.admin@example.com", "Password123", Role.ADMIN);
        User manager = user(defaultTenant, "supplier.manager@example.com", "Password123", Role.MANAGER);
        User cashier = user(defaultTenant, "supplier.cashier@example.com", "Password123", Role.CASHIER);
        User otherAdmin = user(otherTenant, "other.admin@example.com", "Password123", Role.ADMIN);
        userRepository.saveAndFlush(admin);
        userRepository.saveAndFlush(manager);
        userRepository.saveAndFlush(cashier);
        userRepository.saveAndFlush(otherAdmin);

        AuthTokens adminTokens = login("supplier.admin@example.com", "Password123");
        AuthTokens managerTokens = login("supplier.manager@example.com", "Password123");
        AuthTokens cashierTokens = login("supplier.cashier@example.com", "Password123");
        AuthTokens otherTokens = login("other.admin@example.com", "Password123");

        String createdSupplier = mockMvc.perform(post("/api/v1/suppliers")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminTokens.accessToken()))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateSupplierRequest(
                                "sup-0001",
                                " Acme Supplies Ltd ",
                                " Mary Wanjiku ",
                                "Accounts@Acme.com",
                                " +254712345678 ",
                                " +254700000000 ",
                                " P051234567X ",
                                " Kimathi Street ",
                                " 2nd Floor ",
                                " Nairobi ",
                                " Nairobi ",
                                " 00100 ",
                                "ke",
                                " Preferred supplier "))))
                .andExpect(status().isCreated())
                .andExpect(header().exists(HttpHeaders.LOCATION))
                .andExpect(jsonPath("$.code").value("SUP-0001"))
                .andExpect(jsonPath("$.countryCode").value("KE"))
                .andExpect(jsonPath("$.active").value(true))
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);

        JsonNode createdBody = objectMapper.readTree(createdSupplier);
        UUID supplierId = UUID.fromString(createdBody.get("id").asText());

        mockMvc.perform(post("/api/v1/suppliers")
                        .header(HttpHeaders.AUTHORIZATION, bearer(cashierTokens.accessToken()))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateSupplierRequest(
                                "sup-0002",
                                "Blocked Supplier",
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null))))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/suppliers"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/suppliers").header(HttpHeaders.AUTHORIZATION, bearer(managerTokens.accessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));

        mockMvc.perform(get("/api/v1/suppliers/{supplierId}", supplierId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(managerTokens.accessToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUP-0001"));

        mockMvc.perform(get("/api/v1/suppliers/{supplierId}", UUID.randomUUID())
                        .header(HttpHeaders.AUTHORIZATION, bearer(managerTokens.accessToken())))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/v1/suppliers/{supplierId}", supplierId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(otherTokens.accessToken())))
                .andExpect(status().isNotFound());

        mockMvc.perform(put("/api/v1/suppliers/{supplierId}", supplierId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminTokens.accessToken()))
                        .contentType(APPLICATION_JSON)
                        .content("""
                        {
                          "name": "Office Mart",
                          "contactPerson": "John Doe",
                          "email": "billing@example.com",
                          "phone": "+254700000000",
                          "alternatePhone": "+254711111111",
                          "taxNumber": "P051234567X",
                          "addressLine1": "Waiyaki Way",
                          "addressLine2": "Suite 4",
                          "city": "Nairobi",
                          "stateOrCounty": "Nairobi",
                          "postalCode": "00200",
                          "countryCode": "ke",
                          "notes": "Main account"
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Office Mart"))
                .andExpect(jsonPath("$.code").value("SUP-0001"));

        mockMvc.perform(patch("/api/v1/suppliers/{supplierId}/status", supplierId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminTokens.accessToken()))
                        .contentType(APPLICATION_JSON)
                        .content("{\"active\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        mockMvc.perform(patch("/api/v1/suppliers/{supplierId}/status", supplierId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminTokens.accessToken()))
                        .contentType(APPLICATION_JSON)
                        .content("{\"active\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true));

        mockMvc.perform(patch("/api/v1/suppliers/{supplierId}/status", supplierId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(cashierTokens.accessToken()))
                        .contentType(APPLICATION_JSON)
                        .content("{\"active\":false}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/suppliers")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminTokens.accessToken()))
                        .param("search", "office")
                        .param("active", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));

        mockMvc.perform(post("/api/v1/suppliers")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminTokens.accessToken()))
                        .contentType(APPLICATION_JSON)
                        .content("""
                        {
                          "code": "SUP-0001",
                          "name": "Duplicate Code"
                        }
                        """))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/api/v1/suppliers")
                        .header(HttpHeaders.AUTHORIZATION, bearer(otherTokens.accessToken()))
                        .contentType(APPLICATION_JSON)
                        .content("""
                        {
                          "code": "SUP-0001",
                          "name": "Other Tenant"
                        }
                        """))
                .andExpect(status().isCreated());
    }

    @Test
    void supplierValidationReturnsBadRequestForInvalidPayloads() throws Exception {
        Tenant defaultTenant = defaultTenant();
        User admin = user(defaultTenant, "validator@example.com", "Password123", Role.ADMIN);
        userRepository.saveAndFlush(admin);
        AuthTokens tokens = login("validator@example.com", "Password123");

        mockMvc.perform(post("/api/v1/suppliers")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokens.accessToken()))
                        .contentType(APPLICATION_JSON)
                        .content("""
                        {
                          "code": "",
                          "name": ""
                        }
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"));

        mockMvc.perform(post("/api/v1/suppliers")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokens.accessToken()))
                        .contentType(APPLICATION_JSON)
                        .content("""
                        {
                          "code": "a",
                          "name": "Acme Supplies Ltd"
                        }
                        """))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/v1/suppliers")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokens.accessToken()))
                        .contentType(APPLICATION_JSON)
                        .content("""
                        {
                          "code": "sup-0001",
                          "name": "Acme Supplies Ltd",
                          "email": "bad-email",
                          "countryCode": "k"
                        }
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.email").exists())
                .andExpect(jsonPath("$.fieldErrors.countryCode").exists());
    }

    private AuthTokens login(String email, String password) throws Exception {
        String loginResponse = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(email, password))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);

        JsonNode responseBody = objectMapper.readTree(loginResponse);
        return new AuthTokens(responseBody.get("accessToken").asText(), responseBody.get("refreshToken").asText());
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private User user(Tenant tenant, String email, String rawPassword, Role role) {
        User user = new User();
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setEmail(email);
        user.setTenant(tenant);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRole(role);
        user.setEnabled(true);
        return user;
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

    private record AuthTokens(String accessToken, String refreshToken) {}
}
