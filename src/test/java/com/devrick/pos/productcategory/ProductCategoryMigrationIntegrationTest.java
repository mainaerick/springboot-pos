package com.devrick.pos.productcategory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.devrick.pos.productcategory.entity.ProductCategory;
import com.devrick.pos.productcategory.repository.ProductCategoryRepository;
import com.devrick.pos.tenant.entity.Tenant;
import com.devrick.pos.tenant.repository.TenantRepository;
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
class ProductCategoryMigrationIntegrationTest {

    @Autowired
    private ProductCategoryRepository productCategoryRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void productCategoryTableAndIndexesArePresent() {
        assertEquals(1, query("SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'product_categories'"));
        assertEquals(1, query(
                "SELECT COUNT(*) FROM information_schema.columns WHERE table_name = 'product_categories' AND column_name = 'tenant_id'"));
        assertEquals(1, query(
                "SELECT COUNT(*) FROM information_schema.columns WHERE table_name = 'product_categories' AND column_name = 'name_ci'"));
        assertEquals(1, query(
                "SELECT COUNT(*) FROM information_schema.columns WHERE table_name = 'product_categories' AND column_name = 'active' AND column_default LIKE '%TRUE%'"));
        assertEquals(1, query(
                "SELECT COUNT(*) FROM information_schema.table_constraints WHERE table_name = 'product_categories' AND constraint_name = 'uk_product_categories_tenant_code'"));
        assertEquals(1, query(
                "SELECT COUNT(*) FROM information_schema.indexes WHERE table_name = 'product_categories' AND index_name = 'uk_product_categories_tenant_name_ci'"));
        assertEquals(1, query(
                "SELECT COUNT(*) FROM information_schema.indexes WHERE table_name = 'product_categories' AND index_name = 'idx_product_categories_tenant_id'"));
        assertEquals(1, query(
                "SELECT COUNT(*) FROM information_schema.indexes WHERE table_name = 'product_categories' AND index_name = 'idx_product_categories_tenant_active'"));
        assertEquals(1, query(
                "SELECT COUNT(*) FROM information_schema.indexes WHERE table_name = 'product_categories' AND index_name = 'idx_product_categories_tenant_name_ci'"));
    }

    @Test
    void productCategoryDefaultsToActiveAndTenantForeignKeyWorks() {
        Tenant tenant = tenantRepository.findByCodeIgnoreCase("DEFAULT").orElseThrow();
        ProductCategory category = new ProductCategory();
        category.setTenant(tenant);
        category.setCode("BEVERAGES");
        category.setName("Beverages");
        category.setNameCi("beverages");
        category.setCreatedAt(java.time.Instant.parse("2026-08-01T10:00:00Z"));
        category.setUpdatedAt(java.time.Instant.parse("2026-08-01T10:00:00Z"));

        ProductCategory saved = productCategoryRepository.saveAndFlush(category);

        assertTrue(saved.isActive());
        assertEquals(tenant.getId(), saved.getTenant().getId());
    }

    private Integer query(String sql) {
        return jdbcTemplate.queryForObject(sql, Integer.class);
    }
}
