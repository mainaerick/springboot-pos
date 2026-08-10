package com.devrick.pos.supplier.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.devrick.pos.supplier.dto.CreateSupplierRequest;
import com.devrick.pos.supplier.dto.SupplierResponse;
import com.devrick.pos.supplier.dto.UpdateSupplierRequest;
import com.devrick.pos.supplier.entity.Supplier;
import com.devrick.pos.tenant.entity.Tenant;
import com.devrick.pos.tenant.entity.TenantStatus;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class SupplierMapperTest {

    private final SupplierMapper supplierMapper = Mappers.getMapper(SupplierMapper.class);

    @Test
    void createRequestMapsEditableFieldsAndIgnoresTenantAndAuditData() {
        CreateSupplierRequest request = new CreateSupplierRequest(
                "sup-0001",
                "Acme Supplies Ltd",
                "Mary Wanjiku",
                "accounts@acme.com",
                "+254712345678",
                "+254700000000",
                "P051234567X",
                "Kimathi Street",
                "2nd Floor",
                "Nairobi",
                "Nairobi",
                "00100",
                "ke",
                "Preferred supplier");

        Supplier supplier = supplierMapper.toEntity(request);

        assertEquals("sup-0001", supplier.getCode());
        assertEquals("Acme Supplies Ltd", supplier.getName());
        assertEquals("Mary Wanjiku", supplier.getContactPerson());
        assertNull(supplier.getTenant());
        assertNull(supplier.getId());
        assertTrue(supplier.isActive());
    }

    @Test
    void responseMapsSupplierFields() {
        Supplier supplier = supplier();

        SupplierResponse response = supplierMapper.toResponse(supplier);

        assertEquals(supplier.getId(), response.id());
        assertEquals("SUP-0001", response.code());
        assertEquals("Acme Supplies Ltd", response.name());
        assertEquals("Mary Wanjiku", response.contactPerson());
        assertEquals(Instant.parse("2026-08-01T10:00:00Z"), response.createdAt());
    }

    @Test
    void updateRequestPreservesIdTenantCodeActiveAndAuditData() {
        Supplier supplier = supplier();
        UUID originalId = supplier.getId();
        Tenant originalTenant = supplier.getTenant();
        String originalCode = supplier.getCode();
        boolean originalActive = supplier.isActive();
        Instant originalCreatedAt = supplier.getCreatedAt();

        supplierMapper.updateEntity(
                new UpdateSupplierRequest(
                        "Acme Supplies Ltd",
                        "Mary Wanjiku",
                        "accounts@acme.com",
                        "+254712345678",
                        "+254700000000",
                        "P051234567X",
                        "Kimathi Street",
                        "2nd Floor",
                        "Nairobi",
                        "Nairobi",
                        "00100",
                        "ke",
                        "Updated notes"),
                supplier);

        assertEquals(originalId, supplier.getId());
        assertEquals(originalTenant, supplier.getTenant());
        assertEquals(originalCode, supplier.getCode());
        assertEquals(originalActive, supplier.isActive());
        assertEquals(originalCreatedAt, supplier.getCreatedAt());
        assertEquals("Updated notes", supplier.getNotes());
    }

    private Supplier supplier() {
        Tenant tenant = new Tenant();
        tenant.setId(UUID.fromString("55555555-5555-5555-5555-555555555555"));
        tenant.setName("Default Business");
        tenant.setCode("DEFAULT");
        tenant.setStatus(TenantStatus.ACTIVE);

        Supplier supplier = new Supplier();
        supplier.setId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
        supplier.setTenant(tenant);
        supplier.setCode("SUP-0001");
        supplier.setName("Acme Supplies Ltd");
        supplier.setContactPerson("Mary Wanjiku");
        supplier.setEmail("accounts@acme.com");
        supplier.setPhone("+254712345678");
        supplier.setAlternatePhone("+254700000000");
        supplier.setTaxNumber("P051234567X");
        supplier.setAddressLine1("Kimathi Street");
        supplier.setAddressLine2("2nd Floor");
        supplier.setCity("Nairobi");
        supplier.setStateOrCounty("Nairobi");
        supplier.setPostalCode("00100");
        supplier.setCountryCode("KE");
        supplier.setNotes("Preferred supplier");
        supplier.setActive(false);
        supplier.setCreatedAt(Instant.parse("2026-08-01T10:00:00Z"));
        supplier.setUpdatedAt(Instant.parse("2026-08-01T11:00:00Z"));
        return supplier;
    }
}
