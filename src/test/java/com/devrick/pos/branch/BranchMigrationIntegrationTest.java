package com.devrick.pos.branch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.devrick.pos.branch.entity.Branch;
import com.devrick.pos.branch.repository.BranchRepository;
import com.devrick.pos.tenant.entity.Tenant;
import com.devrick.pos.tenant.repository.TenantRepository;
import java.sql.ResultSet;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class BranchMigrationIntegrationTest {

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void branchTableAndIndexesArePresent() {
        Integer tableCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'branches'", Integer.class);
        assertEquals(1, tableCount);

        Integer tenantIdColumn = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns WHERE table_name = 'branches' AND column_name = 'tenant_id'",
                Integer.class);
        assertEquals(1, tenantIdColumn);

        Integer activeDefault = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns WHERE table_name = 'branches' AND column_name = 'active' AND column_default LIKE '%TRUE%'",
                Integer.class);
        assertEquals(1, activeDefault);

        Integer uniqueCodeConstraint = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.table_constraints WHERE table_name = 'branches' AND constraint_name = 'uk_branches_tenant_code'",
                Integer.class);
        assertEquals(1, uniqueCodeConstraint);

        Integer nameIndex = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.indexes WHERE table_name = 'branches' AND index_name = 'uk_branches_tenant_name_ci'",
                Integer.class);
        assertEquals(1, nameIndex);

        Integer tenantIndex = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.indexes WHERE table_name = 'branches' AND index_name = 'idx_branches_tenant_id'",
                Integer.class);
        assertEquals(1, tenantIndex);
    }

    @Test
    void branchDefaultsToActiveAndTenantForeignKeyWorks() {
        Tenant tenant = tenantRepository.findByCodeIgnoreCase("DEFAULT").orElseThrow();
        Branch branch = new Branch();
        branch.setTenant(tenant);
        branch.setName("Nairobi CBD");
        branch.setCode("NRB-CBD");
        branch.setCreatedAt(java.time.Instant.parse("2026-08-01T10:00:00Z"));
        branch.setUpdatedAt(java.time.Instant.parse("2026-08-01T10:00:00Z"));

        Branch saved = branchRepository.saveAndFlush(branch);

        assertTrue(saved.isActive());
        assertEquals(tenant.getId(), saved.getTenant().getId());
    }
}
