package com.devrick.pos.tenant.security;

import com.devrick.pos.tenant.entity.Tenant;
import java.util.UUID;

public interface CurrentTenantProvider {

    UUID getCurrentTenantId();

    Tenant getCurrentTenant();
}
