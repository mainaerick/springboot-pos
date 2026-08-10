package com.devrick.pos.supplier.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.devrick.pos.common.dto.PageResponse;
import com.devrick.pos.exception.supplier.DuplicateSupplierCodeException;
import com.devrick.pos.exception.supplier.SupplierNotFoundException;
import com.devrick.pos.supplier.dto.CreateSupplierRequest;
import com.devrick.pos.supplier.dto.SupplierResponse;
import com.devrick.pos.supplier.dto.UpdateSupplierRequest;
import com.devrick.pos.supplier.dto.UpdateSupplierStatusRequest;
import com.devrick.pos.supplier.entity.Supplier;
import com.devrick.pos.supplier.mapper.SupplierMapper;
import com.devrick.pos.supplier.repository.SupplierRepository;
import com.devrick.pos.supplier.service.impl.SupplierServiceImpl;
import com.devrick.pos.tenant.entity.Tenant;
import com.devrick.pos.tenant.entity.TenantStatus;
import com.devrick.pos.tenant.repository.TenantRepository;
import com.devrick.pos.tenant.security.CurrentTenantProvider;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class SupplierServiceImplTest {

    private static final UUID TENANT_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");
    private static final UUID OTHER_TENANT_ID = UUID.fromString("66666666-6666-6666-6666-666666666666");

    @Mock
    private SupplierRepository supplierRepository;

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private CurrentTenantProvider currentTenantProvider;

    private final SupplierMapper supplierMapper = Mappers.getMapper(SupplierMapper.class);
    private final Tenant tenant = tenant(TENANT_ID, "Default Business", "DEFAULT");

    private SupplierServiceImpl supplierService;

    @BeforeEach
    void setUp() {
        org.mockito.Mockito.lenient().when(currentTenantProvider.getCurrentTenantId()).thenReturn(TENANT_ID);
        org.mockito.Mockito.lenient().when(tenantRepository.getReferenceById(TENANT_ID)).thenReturn(tenant);
        supplierService = new SupplierServiceImpl(supplierRepository, supplierMapper, tenantRepository, currentTenantProvider);
    }

    @Test
    void createSupplierNormalizesFieldsAndUsesAuthenticatedTenant() {
        CreateSupplierRequest request = new CreateSupplierRequest(
                " sup-0001 ",
                " Acme Supplies Ltd ",
                " Mary Wanjiku ",
                " Accounts@Acme.com ",
                " +254712345678 ",
                " +254700000000 ",
                " P051234567X ",
                " Kimathi Street ",
                " 2nd Floor ",
                " Nairobi ",
                " Nairobi ",
                " 00100 ",
                "ke",
                " Preferred supplier ");

        Instant now = Instant.parse("2026-08-01T10:00:00Z");
        when(supplierRepository.existsByTenantIdAndCodeIgnoreCase(TENANT_ID, "SUP-0001")).thenReturn(false);
        when(supplierRepository.saveAndFlush(any(Supplier.class))).thenAnswer(invocation -> {
            Supplier supplier = invocation.getArgument(0);
            supplier.setId(UUID.randomUUID());
            supplier.setCreatedAt(now);
            supplier.setUpdatedAt(now);
            return supplier;
        });

        SupplierResponse response = supplierService.createSupplier(request);

        ArgumentCaptor<Supplier> captor = ArgumentCaptor.forClass(Supplier.class);
        verify(supplierRepository).saveAndFlush(captor.capture());
        assertEquals("SUP-0001", captor.getValue().getCode());
        assertEquals("Acme Supplies Ltd", captor.getValue().getName());
        assertEquals("KE", captor.getValue().getCountryCode());
        assertEquals(tenant, captor.getValue().getTenant());
        assertTrue(captor.getValue().isActive());
        assertEquals(now, response.createdAt());
    }

    @Test
    void createSupplierRejectsDuplicateCodeWithinTenant() {
        when(supplierRepository.existsByTenantIdAndCodeIgnoreCase(TENANT_ID, "SUP-0001")).thenReturn(true);

        assertThrows(
                DuplicateSupplierCodeException.class,
                () -> supplierService.createSupplier(new CreateSupplierRequest(
                        "sup-0001",
                        "Acme Supplies Ltd",
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null)));

        verify(supplierRepository, never()).saveAndFlush(any());
    }

    @Test
    void getSupplierReturnsTenantOwnedSupplier() {
        Supplier supplier = supplier(UUID.randomUUID(), "SUP-0001", "Acme Supplies Ltd", true);
        when(supplierRepository.findByIdAndTenantId(supplier.getId(), TENANT_ID)).thenReturn(Optional.of(supplier));

        SupplierResponse response = supplierService.getSupplier(supplier.getId());

        assertEquals(supplier.getId(), response.id());
        assertEquals(supplier.getCode(), response.code());
    }

    @Test
    void getSupplierThrowsWhenSupplierIsMissing() {
        UUID supplierId = UUID.randomUUID();
        when(supplierRepository.findByIdAndTenantId(supplierId, TENANT_ID)).thenReturn(Optional.empty());

        assertThrows(SupplierNotFoundException.class, () -> supplierService.getSupplier(supplierId));
    }

    @Test
    void getSupplierDoesNotCrossTenantBoundaries() {
        UUID supplierId = UUID.randomUUID();
        when(supplierRepository.findByIdAndTenantId(supplierId, TENANT_ID)).thenReturn(Optional.empty());

        assertThrows(SupplierNotFoundException.class, () -> supplierService.getSupplier(supplierId));
    }

    @Test
    void updateSupplierPreservesCodeActiveStatusAndTenant() {
        Supplier supplier = supplier(UUID.randomUUID(), "SUP-0001", "Acme Supplies Ltd", false);
        when(supplierRepository.findByIdAndTenantId(supplier.getId(), TENANT_ID)).thenReturn(Optional.of(supplier));
        when(supplierRepository.saveAndFlush(any(Supplier.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SupplierResponse response = supplierService.updateSupplier(
                supplier.getId(),
                new UpdateSupplierRequest(
                        " Sunrise Hotel Ltd ",
                        " Mary Wanjiku ",
                        " ACCOUNTS@EXAMPLE.COM ",
                        " +254700000000 ",
                        " +254711111111 ",
                        " P051234567X ",
                        " Kimathi Street ",
                        " 2nd Floor ",
                        " Nairobi ",
                        " Nairobi ",
                        " 00100 ",
                        "ke",
                        " Main account "));

        assertEquals("SUP-0001", response.code());
        assertFalse(response.active());
        assertEquals("Sunrise Hotel Ltd", response.name());
        assertEquals("KE", response.countryCode());
    }

    @Test
    void updateSupplierStatusIsIdempotentWhenStatusDoesNotChange() {
        Supplier supplier = supplier(UUID.randomUUID(), "SUP-0001", "Acme Supplies Ltd", false);
        when(supplierRepository.findByIdAndTenantId(supplier.getId(), TENANT_ID)).thenReturn(Optional.of(supplier));

        SupplierResponse response = supplierService.updateSupplierStatus(supplier.getId(), new UpdateSupplierStatusRequest(false));

        assertFalse(response.active());
        verify(supplierRepository, never()).saveAndFlush(any());
    }

    @Test
    void updateSupplierStatusChangesStatus() {
        Supplier supplier = supplier(UUID.randomUUID(), "SUP-0001", "Acme Supplies Ltd", false);
        when(supplierRepository.findByIdAndTenantId(supplier.getId(), TENANT_ID)).thenReturn(Optional.of(supplier));
        when(supplierRepository.saveAndFlush(any(Supplier.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SupplierResponse response = supplierService.updateSupplierStatus(supplier.getId(), new UpdateSupplierStatusRequest(true));

        assertTrue(response.active());
        verify(supplierRepository).saveAndFlush(any(Supplier.class));
    }

    @Test
    void getSuppliersSanitizesUnsupportedSortAndReturnsPageResponse() {
        Supplier supplier = supplier(UUID.randomUUID(), "SUP-0001", "Acme Supplies Ltd", true);
        when(supplierRepository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(java.util.List.of(supplier)));

        PageResponse<SupplierResponse> response = supplierService.getSuppliers(
                " acme ",
                true,
                PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "unsupported")));

        assertEquals(1, response.getTotalElements());
        assertEquals("SUP-0001", response.getContent().get(0).code());
    }

    @Test
    void getSuppliersHandlesBlankSearchAndInactiveFilter() {
        Supplier supplier = supplier(UUID.randomUUID(), "SUP-0002", "Office Mart", false);
        when(supplierRepository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(java.util.List.of(supplier)));

        PageResponse<SupplierResponse> response = supplierService.getSuppliers("   ", false, PageRequest.of(0, 10));

        assertEquals(1, response.getTotalElements());
        assertFalse(response.getContent().get(0).active());
    }

    private Tenant tenant(UUID id, String name, String code) {
        Tenant tenant = new Tenant();
        tenant.setId(id);
        tenant.setName(name);
        tenant.setCode(code);
        tenant.setStatus(TenantStatus.ACTIVE);
        return tenant;
    }

    private Supplier supplier(UUID id, String code, String name, boolean active) {
        Supplier supplier = new Supplier();
        supplier.setId(id);
        supplier.setTenant(tenant);
        supplier.setCode(code);
        supplier.setName(name);
        supplier.setContactPerson("Mary Wanjiku");
        supplier.setEmail("accounts@example.com");
        supplier.setPhone("+254712345678");
        supplier.setAlternatePhone("+254700000000");
        supplier.setTaxNumber("P051234567X");
        supplier.setAddressLine1("Kimathi Street");
        supplier.setAddressLine2("2nd Floor");
        supplier.setCity("Nairobi");
        supplier.setStateOrCounty("Nairobi");
        supplier.setPostalCode("00100");
        supplier.setCountryCode("KE");
        supplier.setNotes("Main account");
        supplier.setActive(active);
        supplier.setCreatedAt(Instant.parse("2026-08-01T10:00:00Z"));
        supplier.setUpdatedAt(Instant.parse("2026-08-01T11:00:00Z"));
        return supplier;
    }
}
