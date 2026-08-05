package com.devrick.pos.customer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.devrick.pos.customer.entity.Customer;
import com.devrick.pos.customer.entity.CustomerType;
import com.devrick.pos.customer.repository.CustomerRepository;
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
class CustomerMigrationIntegrationTest {

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void customerTableAndIndexesArePresent() {
        assertEquals(1, query(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'customers'"));
        assertEquals(1, query(
                "SELECT COUNT(*) FROM information_schema.columns WHERE table_name = 'customers' AND column_name = 'tenant_id'"));
        assertEquals(1, query(
                "SELECT COUNT(*) FROM information_schema.columns WHERE table_name = 'customers' AND column_name = 'active' AND column_default LIKE '%TRUE%'"));
        assertEquals(1, query(
                "SELECT COUNT(*) FROM information_schema.table_constraints WHERE table_name = 'customers' AND constraint_name = 'uk_customers_tenant_code'"));
    }

    @Test
    void customerDefaultsToActiveAndTenantForeignKeyWorks() {
        Tenant tenant = tenantRepository.findByCodeIgnoreCase("DEFAULT").orElseThrow();
        Customer customer = new Customer();
        customer.setTenant(tenant);
        customer.setType(CustomerType.INDIVIDUAL);
        customer.setCode("CUS-0001");
        customer.setName("Jane Wanjiku");
        customer.setCreatedAt(java.time.Instant.parse("2026-08-01T10:00:00Z"));
        customer.setUpdatedAt(java.time.Instant.parse("2026-08-01T10:00:00Z"));

        Customer saved = customerRepository.saveAndFlush(customer);

        assertTrue(saved.isActive());
        assertEquals(tenant.getId(), saved.getTenant().getId());
    }

    @Test
    void flywayValidationSucceeds() {
        assertTrue(jdbcTemplate.queryForObject("SELECT 1", Integer.class) == 1);
    }

    private Integer query(String sql) {
        return jdbcTemplate.queryForObject(sql, Integer.class);
    }
}
