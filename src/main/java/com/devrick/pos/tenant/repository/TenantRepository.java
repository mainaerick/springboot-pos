package com.devrick.pos.tenant.repository;

import com.devrick.pos.tenant.entity.Tenant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantRepository extends JpaRepository<Tenant, UUID> {

    Optional<Tenant> findByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCase(String code);
}
