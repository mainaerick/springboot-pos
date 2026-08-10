package com.devrick.pos.branch.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.devrick.pos.branch.entity.Branch;
import com.devrick.pos.tenant.entity.Tenant;
import com.devrick.pos.tenant.entity.TenantStatus;
import com.devrick.pos.tenant.repository.TenantRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class BranchRepositoryTest {

    private static final UUID DEFAULT_TENANT_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @Test
    void persistsValidBranchAndFindsItByTenant() {
        Tenant tenant = defaultTenant();
        Branch branch = branch(tenant, "Nairobi CBD", "NRB-CBD", true);

        Branch saved = branchRepository.saveAndFlush(branch);

        assertTrue(branchRepository.findByIdAndTenantId(saved.getId(), tenant.getId()).isPresent());
        assertEquals(saved.getId(), branchRepository.findByIdAndTenantId(saved.getId(), tenant.getId()).orElseThrow().getId());
    }

    @Test
    void rejectsBranchWithoutTenant() {
        Branch branch = branch(null, "Nairobi CBD", "NRB-CBD", true);

        assertThrows(DataIntegrityViolationException.class, () -> branchRepository.saveAndFlush(branch));
    }

    @Test
    void tenantScopedCodeUniquenessAllowsSameCodeAcrossTenants() {
        Tenant tenantA = defaultTenant();
        Tenant tenantB = tenant("Second Business", "SECOND");

        branchRepository.saveAndFlush(branch(tenantA, "Nairobi CBD", "NRB-CBD", true));
        branchRepository.saveAndFlush(branch(tenantB, "Westlands", "NRB-CBD", true));

        assertTrue(branchRepository.existsByTenantIdAndCodeIgnoreCase(tenantA.getId(), "nrb-cbd"));
        assertTrue(branchRepository.existsByTenantIdAndCodeIgnoreCase(tenantB.getId(), "nrb-cbd"));
    }

    @Test
    void caseInsensitiveNameUniquenessIsTenantScoped() {
        Tenant tenant = defaultTenant();
        branchRepository.saveAndFlush(branch(tenant, "Nairobi CBD", "NRB-CBD", true));

        Branch duplicate = branch(tenant, "nairobi cbd", "NRB-01", true);

        assertThrows(DataIntegrityViolationException.class, () -> branchRepository.saveAndFlush(duplicate));
    }

    @Test
    void sameNameIsAllowedInAnotherTenant() {
        Tenant tenantA = defaultTenant();
        Tenant tenantB = tenant("Second Business", "SECOND");

        branchRepository.saveAndFlush(branch(tenantA, "Nairobi CBD", "NRB-CBD", true));
        Branch saved = branchRepository.saveAndFlush(branch(tenantB, "nairobi cbd", "NRB-01", true));

        assertEquals(saved.getId(), branchRepository.findByIdAndTenantId(saved.getId(), tenantB.getId()).orElseThrow().getId());
    }

    @Test
    void listsOnlyTenantOwnedBranchesAndAppliesFilters() {
        Tenant tenantA = defaultTenant();
        Tenant tenantB = tenant("Second Business", "SECOND");

        branchRepository.saveAndFlush(branch(tenantA, "Nairobi CBD", "NRB-CBD", true));
        branchRepository.saveAndFlush(branch(tenantA, "Westlands", "WST-01", false));
        branchRepository.saveAndFlush(branch(tenantB, "Other Tenant", "OT-01", true));

        assertEquals(2, branchRepository.findAll(
                        BranchSpecifications.byTenantAndFilters(tenantA.getId(), null, null), PageRequest.of(0, 10))
                .getTotalElements());
        assertEquals(1, branchRepository.findAll(
                        BranchSpecifications.byTenantAndFilters(tenantA.getId(), null, true), PageRequest.of(0, 10))
                .getTotalElements());
        assertEquals(1, branchRepository.findAll(
                        BranchSpecifications.byTenantAndFilters(tenantA.getId(), null, false), PageRequest.of(0, 10))
                .getTotalElements());
    }

    @Test
    void searchesNameAndCodeCaseInsensitively() {
        Tenant tenant = defaultTenant();
        branchRepository.saveAndFlush(branch(tenant, "Nairobi CBD", "NRB-CBD", true));
        branchRepository.saveAndFlush(branch(tenant, "Westlands", "WST-01", false));

        assertEquals(1, branchRepository.findAll(
                        BranchSpecifications.byTenantAndFilters(tenant.getId(), "cbd", null), PageRequest.of(0, 10))
                .getTotalElements());
        assertEquals(1, branchRepository.findAll(
                        BranchSpecifications.byTenantAndFilters(tenant.getId(), "wst", null), PageRequest.of(0, 10))
                .getTotalElements());
    }

    @Test
    void appliesPaginationAndSorting() {
        Tenant tenant = defaultTenant();
        branchRepository.saveAndFlush(branch(tenant, "Westlands", "WST-01", true));
        branchRepository.saveAndFlush(branch(tenant, "Nairobi CBD", "NRB-CBD", true));

        var page = branchRepository.findAll(
                BranchSpecifications.byTenantAndFilters(tenant.getId(), null, null),
                PageRequest.of(0, 1, Sort.by(Sort.Direction.ASC, "name")));

        assertEquals(2, page.getTotalElements());
        assertEquals(1, page.getContent().size());
        assertEquals("Nairobi CBD", page.getContent().get(0).getName());
    }

    @Test
    void deletingBranchDoesNotDeleteTenant() {
        Tenant tenant = defaultTenant();
        Branch saved = branchRepository.saveAndFlush(branch(tenant, "Nairobi CBD", "NRB-CBD", true));

        branchRepository.delete(saved);
        branchRepository.flush();

        assertTrue(tenantRepository.findById(tenant.getId()).isPresent());
    }

    @Test
    void tenantForeignKeyRejectsUnknownTenant() {
        Branch branch = branch(null, "Nairobi CBD", "NRB-CBD", true);
        Tenant tenant = new Tenant();
        tenant.setId(UUID.fromString("66666666-6666-6666-6666-666666666666"));
        branch.setTenant(tenant);

        assertThrows(DataIntegrityViolationException.class, () -> branchRepository.saveAndFlush(branch));
    }

    @Test
    void requiredFieldsAreEnforced() {
        Branch branch = new Branch();

        assertThrows(DataIntegrityViolationException.class, () -> branchRepository.saveAndFlush(branch));
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

    private Branch branch(Tenant tenant, String name, String code, boolean active) {
        Branch branch = new Branch();
        branch.setTenant(tenant);
        branch.setName(name);
        branch.setCode(code);
        branch.setEmail("branch@example.com");
        branch.setPhone("+254712345678");
        branch.setAddressLine1("Kimathi Street");
        branch.setCity("Nairobi");
        branch.setStateOrCounty("Nairobi");
        branch.setPostalCode("00100");
        branch.setCountryCode("KE");
        branch.setActive(active);
        return branch;
    }
}
