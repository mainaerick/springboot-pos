package com.devrick.pos.supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.devrick.pos.supplier.entity.Supplier;
import com.devrick.pos.supplier.repository.SupplierRepository;
import com.devrick.pos.tenant.entity.Tenant;
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
class SupplierMigrationIntegrationTest {

    @Autowired
    private SupplierRepository supplierRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void supplierTableAndIndexesArePresent() {
        assertEquals(1, query("SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'suppliers'"));
        assertEquals(1, query(
                "SELECT COUNT(*) FROM information_schema.columns WHERE table_name = 'suppliers' AND column_name = 'tenant_id'"));
        assertEquals(1, query(
                "SELECT COUNT(*) FROM information_schema.columns WHERE table_name = 'suppliers' AND column_name = 'active' AND column_default LIKE '%TRUE%'"));
        assertEquals(1, query(
                "SELECT COUNT(*) FROM information_schema.table_constraints WHERE table_name = 'suppliers' AND constraint_name = 'uk_suppliers_tenant_code'"));
        assertEquals(1, query(
                "SELECT COUNT(*) FROM information_schema.indexes WHERE table_name = 'suppliers' AND index_name = 'idx_suppliers_tenant_id'"));
        assertEquals(1, query(
                "SELECT COUNT(*) FROM information_schema.indexes WHERE table_name = 'suppliers' AND index_name = 'idx_suppliers_tenant_active'"));
        assertEquals(1, query(
                "SELECT COUNT(*) FROM information_schema.indexes WHERE table_name = 'suppliers' AND index_name = 'idx_suppliers_tenant_name_ci'"));
    }

    @Test
    void supplierDefaultsToActiveAndTenantForeignKeyWorks() {
        Tenant tenant = tenantRepository.findByCodeIgnoreCase("DEFAULT").orElseThrow();
        Supplier supplier = new Supplier();
        supplier.setTenant(tenant);
        supplier.setCode("SUP-0001");
        supplier.setName("Acme Supplies Ltd");
        supplier.setCreatedAt(java.time.Instant.parse("2026-08-01T10:00:00Z"));
        supplier.setUpdatedAt(java.time.Instant.parse("2026-08-01T10:00:00Z"));

        Supplier saved = supplierRepository.saveAndFlush(supplier);

        assertTrue(saved.isActive());
        assertEquals(tenant.getId(), saved.getTenant().getId());
    }

    private Integer query(String sql) {
        return jdbcTemplate.queryForObject(sql, Integer.class);
    }
}
