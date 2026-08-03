package com.devrick.pos.security.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.devrick.pos.common.enums.Role;
import com.devrick.pos.security.principal.AuthenticatedUser;
import com.devrick.pos.tenant.entity.Tenant;
import com.devrick.pos.tenant.entity.TenantStatus;
import com.devrick.pos.user.entity.User;
import com.devrick.pos.user.repository.UserRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    private static final UUID TENANT_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");

    @Mock
    private UserRepository userRepository;

    @Test
    void loadUserByUsernameMapsUserToUserDetails() {
        User user = new User();
        user.setId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
        user.setEmail("john.doe@example.com");
        user.setPassword("encoded-password");
        user.setRole(Role.ADMIN);
        user.setEnabled(true);
        user.setMustChangePassword(false);
        user.setTenant(tenant());

        org.mockito.Mockito.when(userRepository.findByEmailIgnoreCase("john.doe@example.com"))
                .thenReturn(Optional.of(user));

        CustomUserDetailsService service = new CustomUserDetailsService(userRepository);
        UserDetails userDetails = service.loadUserByUsername(" john.doe@example.com ");

        assertEquals("john.doe@example.com", userDetails.getUsername());
        assertEquals("encoded-password", userDetails.getPassword());
        assertFalse(userDetails.getAuthorities().isEmpty());
        assertEquals(
                "ROLE_ADMIN", userDetails.getAuthorities().iterator().next().getAuthority());

        AuthenticatedUser authenticatedUser = (AuthenticatedUser) userDetails;
        assertEquals(TENANT_ID, authenticatedUser.tenantId());
        assertEquals("Default Business", authenticatedUser.tenantName());
        assertEquals("DEFAULT", authenticatedUser.tenantCode());
        assertEquals(TenantStatus.ACTIVE, authenticatedUser.tenantStatus());
        assertFalse(authenticatedUser.mustChangePassword());
    }

    @Test
    void loadUserByUsernameThrowsWhenMissing() {
        org.mockito.Mockito.when(userRepository.findByEmailIgnoreCase("missing@example.com"))
                .thenReturn(Optional.empty());

        CustomUserDetailsService service = new CustomUserDetailsService(userRepository);

        UsernameNotFoundException exception =
                assertThrows(UsernameNotFoundException.class, () -> service.loadUserByUsername("missing@example.com"));

        assertEquals("User not found with email: missing@example.com", exception.getMessage());
    }

    @Test
    void loadUserByUsernameRejectsInactiveTenant() {
        User user = new User();
        user.setEmail("john.doe@example.com");
        user.setPassword("encoded-password");
        user.setRole(Role.ADMIN);
        user.setEnabled(true);
        user.setTenant(tenant(TenantStatus.SUSPENDED));

        org.mockito.Mockito.when(userRepository.findByEmailIgnoreCase("john.doe@example.com"))
                .thenReturn(Optional.of(user));

        CustomUserDetailsService service = new CustomUserDetailsService(userRepository);

        assertThrows(DisabledException.class, () -> service.loadUserByUsername("john.doe@example.com"));
    }

    private Tenant tenant() {
        return tenant(TenantStatus.ACTIVE);
    }

    private Tenant tenant(TenantStatus status) {
        Tenant tenant = new Tenant();
        tenant.setId(TENANT_ID);
        tenant.setName("Default Business");
        tenant.setCode("DEFAULT");
        tenant.setStatus(status);
        return tenant;
    }
}
