package com.devrick.pos.security.principal;

import com.devrick.pos.tenant.entity.Tenant;
import com.devrick.pos.tenant.entity.TenantStatus;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public record AuthenticatedUser(
        UUID userId,
        UUID tenantId,
        String tenantName,
        String tenantCode,
        TenantStatus tenantStatus,
        String username,
        String password,
        boolean enabled,
        boolean mustChangePassword,
        Collection<? extends GrantedAuthority> authorities)
        implements UserDetails {

    public AuthenticatedUser {
        authorities = authorities == null ? List.of() : List.copyOf(authorities);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    public boolean isTenantActive() {
        return tenantStatus == TenantStatus.ACTIVE;
    }

    public Tenant toTenant() {
        Tenant tenant = new Tenant();
        tenant.setId(tenantId);
        tenant.setName(tenantName);
        tenant.setCode(tenantCode);
        tenant.setStatus(tenantStatus);
        return tenant;
    }
}
