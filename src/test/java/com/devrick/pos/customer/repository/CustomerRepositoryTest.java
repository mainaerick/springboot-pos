package com.devrick.pos.customer.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.devrick.pos.customer.entity.Customer;
import com.devrick.pos.customer.entity.CustomerType;
import com.devrick.pos.tenant.entity.Tenant;
import com.devrick.pos.tenant.entity.TenantStatus;
import com.devrick.pos.tenant.repository.TenantRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CustomerRepositoryTest {

    private static final UUID DEFAULT_TENANT_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @Test
    void persistsValidCustomerAndFindsItByTenant() {
        Tenant tenant = defaultTenant();
        Customer customer = customer(tenant, "CUS-0001", "Jane Wanjiku", CustomerType.INDIVIDUAL, true);

        Customer saved = customerRepository.saveAndFlush(customer);

        assertTrue(customerRepository.findByIdAndTenantId(saved.getId(), tenant.getId()).isPresent());
        assertEquals(saved.getId(), customerRepository.findByIdAndTenantId(saved.getId(), tenant.getId()).orElseThrow().getId());
    }

    @Test
    void rejectsCustomerWithoutTenant() {
        Customer customer = customer(null, "CUS-0001", "Jane Wanjiku", CustomerType.INDIVIDUAL, true);

        assertThrows(DataIntegrityViolationException.class, () -> customerRepository.saveAndFlush(customer));
    }

    @Test
    void tenantScopedCodeUniquenessAllowsSameCodeAcrossTenants() {
        Tenant tenantA = defaultTenant();
        Tenant tenantB = tenant("Second Business", "SECOND");

        customerRepository.saveAndFlush(customer(tenantA, "CUS-0001", "Jane Wanjiku", CustomerType.INDIVIDUAL, true));
        customerRepository.saveAndFlush(customer(tenantB, "CUS-0001", "Jane Wanjiku", CustomerType.INDIVIDUAL, true));

        assertTrue(customerRepository.existsByTenantIdAndCodeIgnoreCase(tenantA.getId(), "cus-0001"));
        assertTrue(customerRepository.existsByTenantIdAndCodeIgnoreCase(tenantB.getId(), "cus-0001"));
    }

    @Test
    void duplicateCodeWithinTenantIsRejected() {
        Tenant tenant = defaultTenant();
        customerRepository.saveAndFlush(customer(tenant, "CUS-0001", "Jane Wanjiku", CustomerType.INDIVIDUAL, true));

        assertThrows(
                DataIntegrityViolationException.class,
                () -> customerRepository.saveAndFlush(customer(tenant, "CUS-0001", "Another", CustomerType.BUSINESS, false)));
    }

    @Test
    void sameNameEmailAndPhoneAreAllowed() {
        Tenant tenant = defaultTenant();
        customerRepository.saveAndFlush(customer(tenant, "CUS-0001", "Jane Wanjiku", CustomerType.INDIVIDUAL, true));
        Customer saved = customerRepository.saveAndFlush(customer(tenant, "CUS-0002", "Jane Wanjiku", CustomerType.BUSINESS, false));

        assertEquals(saved.getId(), customerRepository.findByIdAndTenantId(saved.getId(), tenant.getId()).orElseThrow().getId());
    }

    @Test
    void listsOnlyTenantOwnedCustomersAndAppliesFilters() {
        Tenant tenantA = defaultTenant();
        Tenant tenantB = tenant("Second Business", "SECOND");

        customerRepository.saveAndFlush(customer(tenantA, "CUS-0001", "Jane Wanjiku", CustomerType.INDIVIDUAL, true));
        customerRepository.saveAndFlush(customer(tenantA, "CUS-0002", "Sunrise Hotel Ltd", CustomerType.BUSINESS, false));
        customerRepository.saveAndFlush(customer(tenantB, "CUS-0003", "Other Tenant", CustomerType.INDIVIDUAL, true));

        assertEquals(2, customerRepository.findAll(
                        CustomerSpecifications.byTenantAndFilters(tenantA.getId(), null, null, null), PageRequest.of(0, 10))
                .getTotalElements());
        assertEquals(1, customerRepository.findAll(
                        CustomerSpecifications.byTenantAndFilters(tenantA.getId(), null, CustomerType.INDIVIDUAL, null),
                        PageRequest.of(0, 10))
                .getTotalElements());
        assertEquals(1, customerRepository.findAll(
                        CustomerSpecifications.byTenantAndFilters(tenantA.getId(), null, CustomerType.BUSINESS, null),
                        PageRequest.of(0, 10))
                .getTotalElements());
        assertEquals(1, customerRepository.findAll(
                        CustomerSpecifications.byTenantAndFilters(tenantA.getId(), null, null, true), PageRequest.of(0, 10))
                .getTotalElements());
        assertEquals(1, customerRepository.findAll(
                        CustomerSpecifications.byTenantAndFilters(tenantA.getId(), null, null, false), PageRequest.of(0, 10))
                .getTotalElements());
    }

    @Test
    void searchesByCodeNameEmailPhoneAndTaxNumber() {
        Tenant tenant = defaultTenant();
        customerRepository.saveAndFlush(customer(tenant, "CUS-0001", "Jane Wanjiku", CustomerType.INDIVIDUAL, true));
        customerRepository.saveAndFlush(customer(tenant, "SUN-0001", "Sunrise Hotel Ltd", CustomerType.BUSINESS, false, "info@sunrise.com", "+254700000000", "B000000"));

        assertEquals(1, customerRepository.findAll(
                        CustomerSpecifications.byTenantAndFilters(tenant.getId(), "cus", null, null), PageRequest.of(0, 10))
                .getTotalElements());
        assertEquals(1, customerRepository.findAll(
                        CustomerSpecifications.byTenantAndFilters(tenant.getId(), "wanjiku", null, null), PageRequest.of(0, 10))
                .getTotalElements());
        assertEquals(1, customerRepository.findAll(
                        CustomerSpecifications.byTenantAndFilters(tenant.getId(), "jane@", null, null), PageRequest.of(0, 10))
                .getTotalElements());
        assertEquals(1, customerRepository.findAll(
                        CustomerSpecifications.byTenantAndFilters(tenant.getId(), "712345678", null, null), PageRequest.of(0, 10))
                .getTotalElements());
        assertEquals(1, customerRepository.findAll(
                        CustomerSpecifications.byTenantAndFilters(tenant.getId(), "p051234567", null, null), PageRequest.of(0, 10))
                .getTotalElements());
    }

    @Test
    void appliesPaginationAndSorting() {
        Tenant tenant = defaultTenant();
        customerRepository.saveAndFlush(customer(tenant, "CUS-0002", "Sunrise Hotel Ltd", CustomerType.BUSINESS, false));
        customerRepository.saveAndFlush(customer(tenant, "CUS-0001", "Jane Wanjiku", CustomerType.INDIVIDUAL, true));

        var page = customerRepository.findAll(
                CustomerSpecifications.byTenantAndFilters(tenant.getId(), null, null, null),
                PageRequest.of(0, 1, Sort.by(Sort.Direction.ASC, "name")));

        assertEquals(2, page.getTotalElements());
        assertEquals(1, page.getContent().size());
        assertEquals("Jane Wanjiku", page.getContent().get(0).getName());
    }

    @Test
    void requiredFieldsAndTenantForeignKeyAreEnforced() {
        assertThrows(DataIntegrityViolationException.class, () -> customerRepository.saveAndFlush(new Customer()));

        Customer customer = customer(null, "CUS-0001", "Jane Wanjiku", CustomerType.INDIVIDUAL, true);
        Tenant tenant = new Tenant();
        tenant.setId(UUID.fromString("77777777-7777-7777-7777-777777777777"));
        customer.setTenant(tenant);

        assertThrows(DataIntegrityViolationException.class, () -> customerRepository.saveAndFlush(customer));
    }

    @Test
    void deletingCustomerDoesNotDeleteTenant() {
        Tenant tenant = defaultTenant();
        Customer saved = customerRepository.saveAndFlush(customer(tenant, "CUS-0001", "Jane Wanjiku", CustomerType.INDIVIDUAL, true));

        customerRepository.delete(saved);
        customerRepository.flush();

        assertTrue(tenantRepository.findById(tenant.getId()).isPresent());
    }

    private Tenant defaultTenant() {
        return tenantRepository.findById(DEFAULT_TENANT_ID).orElseThrow();
    }

    private Tenant tenant(String name, String code) {
        Tenant tenant = new Tenant();
        tenant.setName(name);
        tenant.setCode(code);
        tenant.setStatus(TenantStatus.ACTIVE);
        return tenantRepository.saveAndFlush(tenant);
    }

    private Customer customer(Tenant tenant, String code, String name, CustomerType type, boolean active) {
        return customer(tenant, code, name, type, active, "jane@example.com", "+254712345678", "P051234567X");
    }

    private Customer customer(
            Tenant tenant,
            String code,
            String name,
            CustomerType type,
            boolean active,
            String email,
            String phone,
            String taxNumber) {
        Customer customer = new Customer();
        customer.setTenant(tenant);
        customer.setType(type);
        customer.setCode(code);
        customer.setName(name);
        customer.setEmail(email);
        customer.setPhone(phone);
        customer.setTaxNumber(taxNumber);
        customer.setAddressLine1("Kimathi Street");
        customer.setAddressLine2("2nd Floor");
        customer.setCity("Nairobi");
        customer.setStateOrCounty("Nairobi");
        customer.setPostalCode("00100");
        customer.setCountryCode("KE");
        customer.setNotes("Prefers SMS communication");
        customer.setActive(active);
        return customer;
    }
}
