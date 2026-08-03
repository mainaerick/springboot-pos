package com.devrick.pos.tenant.security;

import com.devrick.pos.security.principal.AuthenticatedUser;
import com.devrick.pos.tenant.entity.Tenant;
import com.devrick.pos.tenant.entity.TenantStatus;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class SecurityCurrentTenantProvider implements CurrentTenantProvider {

    @Override
    public UUID getCurrentTenantId() {
        return getCurrentTenant().getId();
    }

    @Override
    public Tenant getCurrentTenant() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AuthenticationCredentialsNotFoundException("No authenticated user is available");
        }

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof AuthenticatedUser authenticatedUser)) {
            throw new AuthenticationCredentialsNotFoundException("Authenticated user is unavailable");
        }

        if (authenticatedUser.tenantId() == null) {
            throw new AccessDeniedException("Authenticated user does not belong to a tenant");
        }

        if (authenticatedUser.tenantStatus() != TenantStatus.ACTIVE) {
            throw new AccessDeniedException("Tenant is not active");
        }

        if (!StringUtils.hasText(authenticatedUser.tenantName())
                || !StringUtils.hasText(authenticatedUser.tenantCode())) {
            throw new AccessDeniedException("Authenticated tenant details are incomplete");
        }

        return authenticatedUser.toTenant();
    }
}
