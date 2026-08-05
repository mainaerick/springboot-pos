package com.devrick.pos.customer.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.devrick.pos.customer.dto.CreateCustomerRequest;
import com.devrick.pos.customer.dto.CustomerResponse;
import com.devrick.pos.customer.dto.UpdateCustomerRequest;
import com.devrick.pos.customer.entity.Customer;
import com.devrick.pos.customer.entity.CustomerType;
import com.devrick.pos.tenant.entity.Tenant;
import com.devrick.pos.tenant.entity.TenantStatus;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class CustomerMapperTest {

    private final CustomerMapper customerMapper = Mappers.getMapper(CustomerMapper.class);

    @Test
    void createRequestMapsEditableFieldsAndIgnoresTenantAndAuditData() {
        CreateCustomerRequest request = new CreateCustomerRequest(
                CustomerType.INDIVIDUAL,
                "cus-0001",
                "Jane Wanjiku",
                "jane@example.com",
                "+254712345678",
                "P051234567X",
                "Kimathi Street",
                "2nd Floor",
                "Nairobi",
                "Nairobi",
                "00100",
                "ke",
                "Prefers SMS communication");

        Customer customer = customerMapper.toEntity(request);

        assertEquals(CustomerType.INDIVIDUAL, customer.getType());
        assertEquals("cus-0001", customer.getCode());
        assertEquals("Jane Wanjiku", customer.getName());
        assertEquals("jane@example.com", customer.getEmail());
        assertNull(customer.getTenant());
        assertNull(customer.getId());
    }

    @Test
    void responseMapsCustomerFields() {
        Customer customer = customer();

        CustomerResponse response = customerMapper.toResponse(customer);

        assertEquals(customer.getId(), response.id());
        assertEquals(CustomerType.BUSINESS, response.type());
        assertEquals("CUS-0002", response.code());
        assertEquals("Sunrise Hotel Ltd", response.name());
        assertEquals(Instant.parse("2026-08-01T10:00:00Z"), response.createdAt());
    }

    @Test
    void updateRequestPreservesIdTenantCodeActiveAndAuditData() {
        Customer customer = customer();
        Tenant originalTenant = customer.getTenant();
        UUID originalId = customer.getId();
        boolean originalActive = customer.isActive();
        Instant originalCreatedAt = customer.getCreatedAt();

        customerMapper.updateEntity(
                new UpdateCustomerRequest(
                        CustomerType.INDIVIDUAL,
                        "Jane Wanjiku",
                        "jane@example.com",
                        "+254712345678",
                        "P051234567X",
                        "Kimathi Street",
                        "2nd Floor",
                        "Nairobi",
                        "Nairobi",
                        "00100",
                        "ke",
                        "Updated notes"),
                customer);

        assertEquals(originalId, customer.getId());
        assertEquals(originalTenant, customer.getTenant());
        assertEquals("CUS-0002", customer.getCode());
        assertEquals(originalActive, customer.isActive());
        assertEquals(originalCreatedAt, customer.getCreatedAt());
        assertEquals("Updated notes", customer.getNotes());
    }

    private Customer customer() {
        Tenant tenant = new Tenant();
        tenant.setId(UUID.fromString("55555555-5555-5555-5555-555555555555"));
        tenant.setName("Default Business");
        tenant.setCode("DEFAULT");
        tenant.setStatus(TenantStatus.ACTIVE);

        Customer customer = new Customer();
        customer.setId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
        customer.setTenant(tenant);
        customer.setType(CustomerType.BUSINESS);
        customer.setCode("CUS-0002");
        customer.setName("Sunrise Hotel Ltd");
        customer.setEmail("accounts@example.com");
        customer.setPhone("+254700000000");
        customer.setTaxNumber("P051234567X");
        customer.setAddressLine1("Kimathi Street");
        customer.setAddressLine2("2nd Floor");
        customer.setCity("Nairobi");
        customer.setStateOrCounty("Nairobi");
        customer.setPostalCode("00100");
        customer.setCountryCode("KE");
        customer.setNotes("Main account");
        customer.setActive(false);
        customer.setCreatedAt(Instant.parse("2026-08-01T10:00:00Z"));
        customer.setUpdatedAt(Instant.parse("2026-08-01T10:05:00Z"));
        return customer;
    }
}
