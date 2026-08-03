package com.devrick.pos.tenant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.devrick.pos.tenant.repository.TenantRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TenantMigrationIntegrationTest {

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void defaultTenantAndTenantForeignKeyArePresent() {
        assertTrue(tenantRepository.findByCodeIgnoreCase("DEFAULT").isPresent());

        Integer nullTenantCount =
                jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users WHERE tenant_id IS NULL", Integer.class);
        assertEquals(0, nullTenantCount);

        Integer tenantColumnCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns WHERE table_name = 'users' AND column_name = 'tenant_id'",
                Integer.class);
        assertEquals(1, tenantColumnCount);
    }
}
