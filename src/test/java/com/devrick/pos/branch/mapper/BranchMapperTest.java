package com.devrick.pos.branch.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.devrick.pos.branch.dto.BranchResponse;
import com.devrick.pos.branch.dto.CreateBranchRequest;
import com.devrick.pos.branch.dto.UpdateBranchRequest;
import com.devrick.pos.branch.entity.Branch;
import com.devrick.pos.tenant.entity.Tenant;
import com.devrick.pos.tenant.entity.TenantStatus;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class BranchMapperTest {

    private final BranchMapper branchMapper = Mappers.getMapper(BranchMapper.class);

    @Test
    void createRequestMapsEditableFieldsOnly() {
        CreateBranchRequest request = new CreateBranchRequest(
                "Nairobi CBD",
                "nrb-cbd",
                "nairobi@example.com",
                "+254712345678",
                "Kimathi Street",
                "2nd Floor",
                "Nairobi",
                "Nairobi",
                "00100",
                "ke");

        Branch branch = branchMapper.toEntity(request);

        assertEquals("Nairobi CBD", branch.getName());
        assertEquals("nrb-cbd", branch.getCode());
        assertEquals("nairobi@example.com", branch.getEmail());
        assertEquals("+254712345678", branch.getPhone());
        assertEquals("Kimathi Street", branch.getAddressLine1());
        assertEquals("2nd Floor", branch.getAddressLine2());
        assertEquals("Nairobi", branch.getCity());
        assertEquals("Nairobi", branch.getStateOrCounty());
        assertEquals("00100", branch.getPostalCode());
        assertEquals("ke", branch.getCountryCode());
        assertNull(branch.getTenant());
        assertTrue(branch.isActive());
        assertNull(branch.getId());
        assertNull(branch.getCreatedAt());
        assertNull(branch.getUpdatedAt());
    }

    @Test
    void entityMapsToResponse() {
        Branch branch = branch(UUID.randomUUID(), tenant(), true);

        BranchResponse response = branchMapper.toResponse(branch);

        assertEquals(branch.getId(), response.id());
        assertEquals("Nairobi CBD", response.name());
        assertEquals("NRB-CBD", response.code());
        assertEquals("nairobi@example.com", response.email());
        assertEquals("+254712345678", response.phone());
        assertEquals("Kimathi Street", response.addressLine1());
        assertEquals("2nd Floor", response.addressLine2());
        assertEquals("Nairobi", response.city());
        assertEquals("Nairobi", response.stateOrCounty());
        assertEquals("00100", response.postalCode());
        assertEquals("KE", response.countryCode());
        assertEquals(true, response.active());
        assertEquals(branch.getCreatedAt(), response.createdAt());
        assertEquals(branch.getUpdatedAt(), response.updatedAt());
    }

    @Test
    void updateMappingPreservesServerControlledFields() {
        Branch branch = branch(UUID.randomUUID(), tenant(), false);
        UUID originalId = branch.getId();
        UUID originalTenantId = branch.getTenant().getId();
        String originalCode = branch.getCode();
        Instant createdAt = branch.getCreatedAt();
        Instant updatedAt = branch.getUpdatedAt();

        branchMapper.updateEntity(
                new UpdateBranchRequest(
                        "Westlands",
                        "westlands@example.com",
                        "+254700000000",
                        "Waiyaki Way",
                        "Suite 4",
                        "Nairobi",
                        "Nairobi",
                        "00200",
                        "ke"),
                branch);

        assertEquals(originalId, branch.getId());
        assertEquals(originalTenantId, branch.getTenant().getId());
        assertEquals(originalCode, branch.getCode());
        assertEquals(false, branch.isActive());
        assertEquals(createdAt, branch.getCreatedAt());
        assertEquals(updatedAt, branch.getUpdatedAt());
        assertEquals("Westlands", branch.getName());
        assertEquals("westlands@example.com", branch.getEmail());
        assertEquals("ke", branch.getCountryCode());
    }

    private Branch branch(UUID id, Tenant tenant, boolean active) {
        Branch branch = new Branch();
        branch.setId(id);
        branch.setTenant(tenant);
        branch.setName("Nairobi CBD");
        branch.setCode("NRB-CBD");
        branch.setEmail("nairobi@example.com");
        branch.setPhone("+254712345678");
        branch.setAddressLine1("Kimathi Street");
        branch.setAddressLine2("2nd Floor");
        branch.setCity("Nairobi");
        branch.setStateOrCounty("Nairobi");
        branch.setPostalCode("00100");
        branch.setCountryCode("KE");
        branch.setActive(active);
        branch.setCreatedAt(Instant.parse("2026-08-01T10:00:00Z"));
        branch.setUpdatedAt(Instant.parse("2026-08-01T11:00:00Z"));
        return branch;
    }

    private Tenant tenant() {
        Tenant tenant = new Tenant();
        tenant.setId(UUID.fromString("55555555-5555-5555-5555-555555555555"));
        tenant.setName("Default Business");
        tenant.setCode("DEFAULT");
        tenant.setStatus(TenantStatus.ACTIVE);
        return tenant;
    }
}
