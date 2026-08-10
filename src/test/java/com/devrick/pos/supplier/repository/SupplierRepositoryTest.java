package com.devrick.pos.supplier.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.devrick.pos.supplier.entity.Supplier;
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
class SupplierRepositoryTest {

    private static final UUID DEFAULT_TENANT_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");

    @Autowired
    private SupplierRepository supplierRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @Test
    void persistsValidSupplierAndFindsItByTenant() {
        Tenant tenant = defaultTenant();
        Supplier supplier = supplier(tenant, "SUP-0001", "Acme Supplies Ltd", true);

        Supplier saved = supplierRepository.saveAndFlush(supplier);

        assertTrue(supplierRepository.findByIdAndTenantId(saved.getId(), tenant.getId()).isPresent());
        assertEquals(saved.getId(), supplierRepository.findByIdAndTenantId(saved.getId(), tenant.getId()).orElseThrow().getId());
    }

    @Test
    void rejectsSupplierWithoutTenant() {
        Supplier supplier = supplier(null, "SUP-0001", "Acme Supplies Ltd", true);

        assertThrows(DataIntegrityViolationException.class, () -> supplierRepository.saveAndFlush(supplier));
    }

    @Test
    void tenantScopedCodeUniquenessAllowsSameCodeAcrossTenants() {
        Tenant tenantA = defaultTenant();
        Tenant tenantB = tenant("Second Business", "SECOND");

        supplierRepository.saveAndFlush(supplier(tenantA, "SUP-0001", "Acme Supplies Ltd", true));
        supplierRepository.saveAndFlush(supplier(tenantB, "SUP-0001", "Other Supplies", true));

        assertTrue(supplierRepository.existsByTenantIdAndCodeIgnoreCase(tenantA.getId(), "sup-0001"));
        assertTrue(supplierRepository.existsByTenantIdAndCodeIgnoreCase(tenantB.getId(), "sup-0001"));
    }

    @Test
    void duplicateCodeWithinTenantIsRejected() {
        Tenant tenant = defaultTenant();
        supplierRepository.saveAndFlush(supplier(tenant, "SUP-0001", "Acme Supplies Ltd", true));

        assertThrows(DataIntegrityViolationException.class,
                () -> supplierRepository.saveAndFlush(supplier(tenant, "SUP-0001", "Another Supplier", false)));
    }

    @Test
    void sameNameEmailPhoneAndTaxNumberAreAllowed() {
        Tenant tenant = defaultTenant();
        supplierRepository.saveAndFlush(supplier(tenant, "SUP-0001", "Acme Supplies Ltd", true));
        Supplier saved = supplierRepository.saveAndFlush(supplier(tenant, "SUP-0002", "Acme Supplies Ltd", false));

        assertEquals(saved.getId(), supplierRepository.findByIdAndTenantId(saved.getId(), tenant.getId()).orElseThrow().getId());
    }

    @Test
    void listsOnlyTenantOwnedSuppliersAndAppliesFilters() {
        Tenant tenantA = defaultTenant();
        Tenant tenantB = tenant("Second Business", "SECOND");

        supplierRepository.saveAndFlush(supplier(tenantA, "SUP-0001", "Acme Supplies Ltd", true));
        supplierRepository.saveAndFlush(supplier(tenantA, "SUP-0002", "Office Mart", false));
        supplierRepository.saveAndFlush(supplier(tenantB, "SUP-0003", "Other Tenant", true));

        assertEquals(2, supplierRepository.findAll(
                        SupplierSpecifications.byTenantAndFilters(tenantA.getId(), null, null), PageRequest.of(0, 10))
                .getTotalElements());
        assertEquals(1, supplierRepository.findAll(
                        SupplierSpecifications.byTenantAndFilters(tenantA.getId(), null, true), PageRequest.of(0, 10))
                .getTotalElements());
        assertEquals(1, supplierRepository.findAll(
                        SupplierSpecifications.byTenantAndFilters(tenantA.getId(), null, false), PageRequest.of(0, 10))
                .getTotalElements());
    }

    @Test
    void searchesBySupportedFieldsCaseInsensitively() {
        Tenant tenant = defaultTenant();
        supplierRepository.saveAndFlush(supplier(
                tenant,
                "SUP-0001",
                "Acme Supplies Ltd",
                true,
                "John Doe",
                "accounts@acme.com",
                "+254712345678",
                "+254700000000",
                "P051234567X"));
        supplierRepository.saveAndFlush(supplier(tenant, "SUP-0002", "Office Mart", false, "Mary Wanjiku", "info@office.com", "+254700000000", "+254711111111", "P099999999X"));

        assertEquals(1, supplierRepository.findAll(
                        SupplierSpecifications.byTenantAndFilters(tenant.getId(), "0001", null), PageRequest.of(0, 10))
                .getTotalElements());
        assertEquals(1, supplierRepository.findAll(
                        SupplierSpecifications.byTenantAndFilters(tenant.getId(), "acme", null), PageRequest.of(0, 10))
                .getTotalElements());
        assertEquals(1, supplierRepository.findAll(
                        SupplierSpecifications.byTenantAndFilters(tenant.getId(), "mary", null), PageRequest.of(0, 10))
                .getTotalElements());
        assertEquals(1, supplierRepository.findAll(
                        SupplierSpecifications.byTenantAndFilters(tenant.getId(), "office.com", null), PageRequest.of(0, 10))
                .getTotalElements());
        assertEquals(1, supplierRepository.findAll(
                        SupplierSpecifications.byTenantAndFilters(tenant.getId(), "712345678", null), PageRequest.of(0, 10))
                .getTotalElements());
        assertEquals(1, supplierRepository.findAll(
                        SupplierSpecifications.byTenantAndFilters(tenant.getId(), "711111111", null), PageRequest.of(0, 10))
                .getTotalElements());
        assertEquals(1, supplierRepository.findAll(
                        SupplierSpecifications.byTenantAndFilters(tenant.getId(), "p051234567", null), PageRequest.of(0, 10))
                .getTotalElements());
    }

    @Test
    void appliesPaginationAndSorting() {
        Tenant tenant = defaultTenant();
        supplierRepository.saveAndFlush(supplier(tenant, "SUP-0002", "Office Mart", false));
        supplierRepository.saveAndFlush(supplier(tenant, "SUP-0001", "Acme Supplies Ltd", true));

        var page = supplierRepository.findAll(
                SupplierSpecifications.byTenantAndFilters(tenant.getId(), null, null),
                PageRequest.of(0, 1, Sort.by(Sort.Direction.ASC, "name")));

        assertEquals(2, page.getTotalElements());
        assertEquals(1, page.getContent().size());
        assertEquals("Acme Supplies Ltd", page.getContent().get(0).getName());
    }

    @Test
    void deletingSupplierDoesNotDeleteTenant() {
        Tenant tenant = defaultTenant();
        Supplier saved = supplierRepository.saveAndFlush(supplier(tenant, "SUP-0001", "Acme Supplies Ltd", true));

        supplierRepository.delete(saved);
        supplierRepository.flush();

        assertTrue(tenantRepository.findById(tenant.getId()).isPresent());
    }

    @Test
    void requiredFieldsAndTenantForeignKeyAreEnforced() {
        assertThrows(DataIntegrityViolationException.class, () -> supplierRepository.saveAndFlush(new Supplier()));

        Supplier supplier = supplier(null, "SUP-0001", "Acme Supplies Ltd", true);
        Tenant tenant = new Tenant();
        tenant.setId(UUID.fromString("77777777-7777-7777-7777-777777777777"));
        supplier.setTenant(tenant);

        assertThrows(DataIntegrityViolationException.class, () -> supplierRepository.saveAndFlush(supplier));
    }

    @Test
    void defaultActiveIsTrue() {
        Tenant tenant = defaultTenant();
        Supplier supplier = new Supplier();
        supplier.setTenant(tenant);
        supplier.setCode("SUP-0009");
        supplier.setName("Default Active");
        supplier.setCreatedAt(java.time.Instant.parse("2026-08-01T10:00:00Z"));
        supplier.setUpdatedAt(java.time.Instant.parse("2026-08-01T10:00:00Z"));

        Supplier saved = supplierRepository.saveAndFlush(supplier);

        assertTrue(saved.isActive());
        assertEquals(tenant.getId(), saved.getTenant().getId());
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

    private Supplier supplier(Tenant tenant, String code, String name, boolean active) {
        return supplier(tenant, code, name, active, "Mary Wanjiku", "accounts@acme.com", "+254712345678", "+254700000000", "P051234567X");
    }

    private Supplier supplier(
            Tenant tenant,
            String code,
            String name,
            boolean active,
            String contactPerson,
            String email,
            String phone,
            String alternatePhone,
            String taxNumber) {
        Supplier supplier = new Supplier();
        supplier.setTenant(tenant);
        supplier.setCode(code);
        supplier.setName(name);
        supplier.setContactPerson(contactPerson);
        supplier.setEmail(email);
        supplier.setPhone(phone);
        supplier.setAlternatePhone(alternatePhone);
        supplier.setTaxNumber(taxNumber);
        supplier.setAddressLine1("Kimathi Street");
        supplier.setAddressLine2("2nd Floor");
        supplier.setCity("Nairobi");
        supplier.setStateOrCounty("Nairobi");
        supplier.setPostalCode("00100");
        supplier.setCountryCode("KE");
        supplier.setNotes("Preferred supplier");
        supplier.setActive(active);
        return supplier;
    }
}
