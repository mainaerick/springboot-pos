package com.devrick.pos.tenant.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.devrick.pos.security.principal.AuthenticatedUser;
import com.devrick.pos.tenant.entity.Tenant;
import com.devrick.pos.tenant.entity.TenantStatus;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

class SecurityCurrentTenantProviderTest {

    private static final UUID TENANT_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void returnsCurrentTenantFromAuthenticatedPrincipal() {
        AuthenticatedUser authenticatedUser = new AuthenticatedUser(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                TENANT_ID,
                "Default Business",
                "DEFAULT",
                TenantStatus.ACTIVE,
                "john.doe@example.com",
                "encoded-password",
                true,
                false,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        SecurityContextHolder.getContext()
                .setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                        authenticatedUser, null, authenticatedUser.getAuthorities()));

        CurrentTenantProvider provider = new SecurityCurrentTenantProvider();
        Tenant tenant = provider.getCurrentTenant();

        assertEquals(TENANT_ID, tenant.getId());
        assertEquals("DEFAULT", tenant.getCode());
        assertEquals(TenantStatus.ACTIVE, tenant.getStatus());
    }

    @Test
    void throwsWhenNoAuthenticatedUserExists() {
        CurrentTenantProvider provider = new SecurityCurrentTenantProvider();

        assertThrows(AuthenticationCredentialsNotFoundException.class, provider::getCurrentTenant);
    }

    @Test
    void throwsWhenTenantIsNotActive() {
        AuthenticatedUser authenticatedUser = new AuthenticatedUser(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                TENANT_ID,
                "Default Business",
                "DEFAULT",
                TenantStatus.SUSPENDED,
                "john.doe@example.com",
                "encoded-password",
                true,
                false,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        SecurityContextHolder.getContext()
                .setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                        authenticatedUser, null, authenticatedUser.getAuthorities()));

        CurrentTenantProvider provider = new SecurityCurrentTenantProvider();

        assertThrows(AccessDeniedException.class, provider::getCurrentTenant);
    }
}
