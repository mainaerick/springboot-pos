package com.devrick.pos.tenant.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.devrick.pos.tenant.entity.Tenant;
import com.devrick.pos.tenant.entity.TenantStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TenantRepositoryTest {

    @Autowired
    private TenantRepository tenantRepository;

    @Test
    void findByCodeIgnoreCaseIsCaseInsensitive() {
        Tenant tenant = new Tenant();
        tenant.setName("Acme Pharmacy Ltd");
        tenant.setCode("ACME");
        tenant.setStatus(TenantStatus.ACTIVE);
        tenantRepository.saveAndFlush(tenant);

        assertTrue(tenantRepository.findByCodeIgnoreCase("acme").isPresent());
        assertTrue(tenantRepository.existsByCodeIgnoreCase("AcMe"));
        assertEquals(
                "ACME",
                tenantRepository.findByCodeIgnoreCase("acme").orElseThrow().getCode());
    }
}
