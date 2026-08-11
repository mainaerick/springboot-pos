package com.devrick.pos.productcategory.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.devrick.pos.productcategory.entity.ProductCategory;
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
class ProductCategoryRepositoryTest {

    private static final UUID DEFAULT_TENANT_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");

    @Autowired
    private ProductCategoryRepository productCategoryRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @Test
    void persistsValidCategoryAndFindsItByTenant() {
        Tenant tenant = defaultTenant();
        ProductCategory category = category(tenant, "BEVERAGES", "Beverages", "Soft drinks", true);

        ProductCategory saved = productCategoryRepository.saveAndFlush(category);

        assertTrue(productCategoryRepository.findByIdAndTenantId(saved.getId(), tenant.getId()).isPresent());
        assertEquals(saved.getId(), productCategoryRepository.findByIdAndTenantId(saved.getId(), tenant.getId()).orElseThrow().getId());
    }

    @Test
    void rejectsCategoryWithoutTenant() {
        ProductCategory category = category(null, "BEVERAGES", "Beverages", "Soft drinks", true);

        assertThrows(DataIntegrityViolationException.class, () -> productCategoryRepository.saveAndFlush(category));
    }

    @Test
    void tenantScopedCodeUniquenessAllowsSameCodeAcrossTenants() {
        Tenant tenantA = defaultTenant();
        Tenant tenantB = tenant("Second Business", "SECOND");

        productCategoryRepository.saveAndFlush(category(tenantA, "BEVERAGES", "Beverages", "Soft drinks", true));
        productCategoryRepository.saveAndFlush(category(tenantB, "BEVERAGES", "Drinks", "Other tenant", true));

        assertTrue(productCategoryRepository.existsByTenantIdAndCodeIgnoreCase(tenantA.getId(), "beverages"));
        assertTrue(productCategoryRepository.existsByTenantIdAndCodeIgnoreCase(tenantB.getId(), "beverages"));
    }

    @Test
    void duplicateCodeWithinTenantIsRejected() {
        Tenant tenant = defaultTenant();
        productCategoryRepository.saveAndFlush(category(tenant, "BEVERAGES", "Beverages", "Soft drinks", true));

        assertThrows(DataIntegrityViolationException.class,
                () -> productCategoryRepository.saveAndFlush(category(tenant, "BEVERAGES", "Drinks", "Duplicate code", false)));
    }

    @Test
    void tenantScopedCaseInsensitiveNameUniquenessAllowsSameNameAcrossTenants() {
        Tenant tenantA = defaultTenant();
        Tenant tenantB = tenant("Second Business", "SECOND");

        productCategoryRepository.saveAndFlush(category(tenantA, "BEVERAGES", "Beverages", "Soft drinks", true));
        productCategoryRepository.saveAndFlush(category(tenantB, "BEV-01", "beverages", "Other tenant", true));

        assertTrue(productCategoryRepository.existsByTenantIdAndNameIgnoreCase(tenantA.getId(), "beverages"));
        assertTrue(productCategoryRepository.existsByTenantIdAndNameIgnoreCase(tenantB.getId(), "BEVERAGES"));
    }

    @Test
    void duplicateNameWithinTenantIsRejectedCaseInsensitively() {
        Tenant tenant = defaultTenant();
        productCategoryRepository.saveAndFlush(category(tenant, "BEVERAGES", "Beverages", "Soft drinks", true));

        assertThrows(DataIntegrityViolationException.class,
                () -> productCategoryRepository.saveAndFlush(category(tenant, "DRINKS", "beverages", "Duplicate name", false)));
    }

    @Test
    void listsOnlyTenantOwnedCategoriesAndAppliesFilters() {
        Tenant tenantA = defaultTenant();
        Tenant tenantB = tenant("Second Business", "SECOND");

        productCategoryRepository.saveAndFlush(category(tenantA, "BEVERAGES", "Beverages", "Soft drinks", true));
        productCategoryRepository.saveAndFlush(category(tenantA, "CLEANING", "Cleaning Supplies", "Household", false));
        productCategoryRepository.saveAndFlush(category(tenantB, "OTHER", "Other Tenant", "Other", true));

        assertEquals(2, productCategoryRepository.findAll(
                        ProductCategorySpecifications.byTenantAndFilters(tenantA.getId(), null, null), PageRequest.of(0, 10))
                .getTotalElements());
        assertEquals(1, productCategoryRepository.findAll(
                        ProductCategorySpecifications.byTenantAndFilters(tenantA.getId(), null, true), PageRequest.of(0, 10))
                .getTotalElements());
        assertEquals(1, productCategoryRepository.findAll(
                        ProductCategorySpecifications.byTenantAndFilters(tenantA.getId(), null, false), PageRequest.of(0, 10))
                .getTotalElements());
    }

    @Test
    void searchesByCodeNameAndDescriptionCaseInsensitively() {
        Tenant tenant = defaultTenant();
        productCategoryRepository.saveAndFlush(category(tenant, "BEVERAGES", "Beverages", "Soft drinks and juices", true));
        productCategoryRepository.saveAndFlush(category(tenant, "CLEANING", "Cleaning Supplies", "Household cleaning", false));

        assertEquals(1, productCategoryRepository.findAll(
                        ProductCategorySpecifications.byTenantAndFilters(tenant.getId(), "bev", null), PageRequest.of(0, 10))
                .getTotalElements());
        assertEquals(1, productCategoryRepository.findAll(
                        ProductCategorySpecifications.byTenantAndFilters(tenant.getId(), "supplies", null), PageRequest.of(0, 10))
                .getTotalElements());
        assertEquals(1, productCategoryRepository.findAll(
                        ProductCategorySpecifications.byTenantAndFilters(tenant.getId(), "household", null), PageRequest.of(0, 10))
                .getTotalElements());
    }

    @Test
    void appliesPaginationAndSorting() {
        Tenant tenant = defaultTenant();
        productCategoryRepository.saveAndFlush(category(tenant, "CLEANING", "Cleaning Supplies", "Household", false));
        productCategoryRepository.saveAndFlush(category(tenant, "BEVERAGES", "Beverages", "Soft drinks", true));

        var page = productCategoryRepository.findAll(
                ProductCategorySpecifications.byTenantAndFilters(tenant.getId(), null, null),
                PageRequest.of(0, 1, Sort.by(Sort.Direction.ASC, "name")));

        assertEquals(2, page.getTotalElements());
        assertEquals(1, page.getContent().size());
        assertEquals("Beverages", page.getContent().get(0).getName());
    }

    @Test
    void deletingCategoryDoesNotDeleteTenant() {
        Tenant tenant = defaultTenant();
        ProductCategory saved = productCategoryRepository.saveAndFlush(category(tenant, "BEVERAGES", "Beverages", "Soft drinks", true));

        productCategoryRepository.delete(saved);
        productCategoryRepository.flush();

        assertTrue(tenantRepository.findById(tenant.getId()).isPresent());
    }

    @Test
    void tenantForeignKeyRejectsUnknownTenant() {
        ProductCategory category = category(null, "BEVERAGES", "Beverages", "Soft drinks", true);
        Tenant tenant = new Tenant();
        tenant.setId(UUID.fromString("77777777-7777-7777-7777-777777777777"));
        category.setTenant(tenant);

        assertThrows(DataIntegrityViolationException.class, () -> productCategoryRepository.saveAndFlush(category));
    }

    @Test
    void requiredFieldsAreEnforced() {
        assertThrows(DataIntegrityViolationException.class, () -> productCategoryRepository.saveAndFlush(new ProductCategory()));
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

    private ProductCategory category(Tenant tenant, String code, String name, String description, boolean active) {
        ProductCategory category = new ProductCategory();
        category.setTenant(tenant);
        category.setCode(code);
        category.setName(name);
        category.setNameCi(name == null ? null : name.trim().toLowerCase(java.util.Locale.ROOT));
        category.setDescription(description);
        category.setActive(active);
        return category;
    }
}
