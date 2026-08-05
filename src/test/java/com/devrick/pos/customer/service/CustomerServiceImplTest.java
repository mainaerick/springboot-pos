package com.devrick.pos.customer.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.devrick.pos.common.dto.PageResponse;
import com.devrick.pos.customer.dto.CreateCustomerRequest;
import com.devrick.pos.customer.dto.CustomerResponse;
import com.devrick.pos.customer.dto.UpdateCustomerRequest;
import com.devrick.pos.customer.dto.UpdateCustomerStatusRequest;
import com.devrick.pos.customer.entity.Customer;
import com.devrick.pos.customer.entity.CustomerType;
import com.devrick.pos.customer.mapper.CustomerMapper;
import com.devrick.pos.customer.repository.CustomerRepository;
import com.devrick.pos.customer.service.impl.CustomerServiceImpl;
import com.devrick.pos.exception.customer.CustomerNotFoundException;
import com.devrick.pos.exception.customer.DuplicateCustomerCodeException;
import com.devrick.pos.tenant.entity.Tenant;
import com.devrick.pos.tenant.entity.TenantStatus;
import com.devrick.pos.tenant.repository.TenantRepository;
import com.devrick.pos.tenant.security.CurrentTenantProvider;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class CustomerServiceImplTest {

    private static final UUID TENANT_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");
    private static final UUID OTHER_TENANT_ID = UUID.fromString("66666666-6666-6666-6666-666666666666");

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private CurrentTenantProvider currentTenantProvider;

    private final CustomerMapper customerMapper = Mappers.getMapper(CustomerMapper.class);
    private final Tenant tenant = tenant(TENANT_ID, "Default Business", "DEFAULT");

    private CustomerServiceImpl customerService;

    @BeforeEach
    void setUp() {
        Mockito.lenient().when(currentTenantProvider.getCurrentTenantId()).thenReturn(TENANT_ID);
        Mockito.lenient().when(tenantRepository.getReferenceById(TENANT_ID)).thenReturn(tenant);
        customerService = new CustomerServiceImpl(customerRepository, customerMapper, tenantRepository, currentTenantProvider);
    }

    @Test
    void createCustomerNormalizesFieldsAndUsesAuthenticatedTenant() {
        Instant now = Instant.parse("2026-08-01T10:00:00Z");
        CreateCustomerRequest request = new CreateCustomerRequest(
                CustomerType.INDIVIDUAL,
                " cus-0001 ",
                " Jane Wanjiku ",
                " Jane@Example.com ",
                " +254712345678 ",
                " P051234567X ",
                " Kimathi Street ",
                " 2nd Floor ",
                " Nairobi ",
                " Nairobi ",
                " 00100 ",
                "ke",
                " Prefers SMS communication ");

        when(customerRepository.existsByTenantIdAndCodeIgnoreCase(TENANT_ID, "CUS-0001")).thenReturn(false);
        when(customerRepository.saveAndFlush(any(Customer.class))).thenAnswer(invocation -> {
            Customer customer = invocation.getArgument(0);
            customer.setId(UUID.randomUUID());
            customer.setCreatedAt(now);
            customer.setUpdatedAt(now);
            return customer;
        });

        CustomerResponse response = customerService.createCustomer(request);

        ArgumentCaptor<Customer> captor = ArgumentCaptor.forClass(Customer.class);
        verify(customerRepository).saveAndFlush(captor.capture());
        assertEquals(CustomerType.INDIVIDUAL, captor.getValue().getType());
        assertEquals("CUS-0001", captor.getValue().getCode());
        assertEquals("Jane Wanjiku", captor.getValue().getName());
        assertEquals("jane@example.com", captor.getValue().getEmail());
        assertEquals("KE", captor.getValue().getCountryCode());
        assertEquals(tenant, captor.getValue().getTenant());
        assertTrue(captor.getValue().isActive());
        assertEquals("CUS-0001", response.code());
        assertEquals(now, response.createdAt());
    }

    @Test
    void createBusinessCustomerAllowsDifferentTypeAndNormalizesOptionalFields() {
        CreateCustomerRequest request = new CreateCustomerRequest(
                CustomerType.BUSINESS,
                "sunrise-hotel",
                "Sunrise Hotel Ltd",
                null,
                null,
                "P051234567X",
                null,
                null,
                null,
                null,
                null,
                null,
                null);

        when(customerRepository.existsByTenantIdAndCodeIgnoreCase(TENANT_ID, "SUNRISE-HOTEL")).thenReturn(false);
        when(customerRepository.saveAndFlush(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CustomerResponse response = customerService.createCustomer(request);

        assertEquals(CustomerType.BUSINESS, response.type());
        verify(customerRepository).saveAndFlush(any(Customer.class));
    }

    @Test
    void createCustomerRejectsDuplicateCodeWithinTenant() {
        when(customerRepository.existsByTenantIdAndCodeIgnoreCase(TENANT_ID, "CUS-0001")).thenReturn(true);

        assertThrows(
                DuplicateCustomerCodeException.class,
                () -> customerService.createCustomer(new CreateCustomerRequest(
                        CustomerType.INDIVIDUAL,
                        "cus-0001",
                        "Jane Wanjiku",
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

        verify(customerRepository, never()).saveAndFlush(any());
    }

    @Test
    void getCustomerReturnsTenantOwnedCustomer() {
        Customer customer = customer(UUID.randomUUID(), "CUS-0001", true, CustomerType.INDIVIDUAL);
        when(customerRepository.findByIdAndTenantId(customer.getId(), TENANT_ID)).thenReturn(Optional.of(customer));

        CustomerResponse response = customerService.getCustomer(customer.getId());

        assertEquals(customer.getId(), response.id());
        assertEquals(customer.getCode(), response.code());
    }

    @Test
    void getCustomerThrowsWhenCustomerIsMissing() {
        UUID customerId = UUID.randomUUID();
        when(customerRepository.findByIdAndTenantId(customerId, TENANT_ID)).thenReturn(Optional.empty());

        assertThrows(CustomerNotFoundException.class, () -> customerService.getCustomer(customerId));
    }

    @Test
    void getCustomerDoesNotCrossTenantBoundaries() {
        UUID customerId = UUID.randomUUID();
        when(customerRepository.findByIdAndTenantId(customerId, TENANT_ID)).thenReturn(Optional.empty());

        assertThrows(CustomerNotFoundException.class, () -> customerService.getCustomer(customerId));
    }

    @Test
    void updateCustomerPreservesCodeActiveStatusAndTenant() {
        Customer customer = customer(UUID.randomUUID(), "CUS-0001", false, CustomerType.INDIVIDUAL);
        when(customerRepository.findByIdAndTenantId(customer.getId(), TENANT_ID)).thenReturn(Optional.of(customer));
        when(customerRepository.saveAndFlush(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CustomerResponse response = customerService.updateCustomer(
                customer.getId(),
                new UpdateCustomerRequest(
                        CustomerType.BUSINESS,
                        " Sunrise Hotel Ltd ",
                        " accounts@example.com ",
                        " +254700000000 ",
                        " P051234567X ",
                        " Kimathi Street ",
                        " 2nd Floor ",
                        " Nairobi ",
                        " Nairobi ",
                        " 00100 ",
                        "ke",
                        " Main account "));

        assertEquals("CUS-0001", response.code());
        assertFalse(response.active());
        assertEquals(CustomerType.BUSINESS, response.type());
        assertEquals("Sunrise Hotel Ltd", response.name());
    }

    @Test
    void updateCustomerStatusIsIdempotentWhenStatusDoesNotChange() {
        Customer customer = customer(UUID.randomUUID(), "CUS-0001", false, CustomerType.INDIVIDUAL);
        when(customerRepository.findByIdAndTenantId(customer.getId(), TENANT_ID)).thenReturn(Optional.of(customer));

        CustomerResponse response = customerService.updateCustomerStatus(customer.getId(), new UpdateCustomerStatusRequest(false));

        assertFalse(response.active());
        verify(customerRepository, never()).saveAndFlush(any());
    }

    @Test
    void updateCustomerStatusDeactivatesAndReactivatesCustomer() {
        Customer customer = customer(UUID.randomUUID(), "CUS-0001", true, CustomerType.INDIVIDUAL);
        when(customerRepository.findByIdAndTenantId(customer.getId(), TENANT_ID)).thenReturn(Optional.of(customer));
        when(customerRepository.saveAndFlush(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CustomerResponse deactivated = customerService.updateCustomerStatus(customer.getId(), new UpdateCustomerStatusRequest(false));

        assertFalse(deactivated.active());
        assertFalse(customer.isActive());

        CustomerResponse reactivated = customerService.updateCustomerStatus(customer.getId(), new UpdateCustomerStatusRequest(true));

        assertTrue(reactivated.active());
        assertTrue(customer.isActive());
        verify(customerRepository, Mockito.times(2)).saveAndFlush(any(Customer.class));
    }

    @Test
    void getCustomersAppliesSearchTypeActiveFilterAndSafeSorting() {
        Customer first = customer(UUID.randomUUID(), "CUS-0001", true, CustomerType.INDIVIDUAL);
        Customer second = customer(UUID.randomUUID(), "CUS-0002", false, CustomerType.BUSINESS);
        when(customerRepository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(first, second), PageRequest.of(0, 20), 2));

        PageResponse<CustomerResponse> response = customerService.getCustomers(
                " cus ",
                CustomerType.INDIVIDUAL,
                true,
                PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "unsupported")));

        ArgumentCaptor<PageRequest> captor = ArgumentCaptor.forClass(PageRequest.class);
        verify(customerRepository).findAll(any(Specification.class), captor.capture());
        assertEquals("createdAt", captor.getValue().getSort().iterator().next().getProperty());
        assertEquals(1, response.getTotalPages());
        assertEquals(2, response.getTotalElements());
        assertEquals("CUS-0001", response.getContent().get(0).code());
    }

    @Test
    void getCustomersTreatsBlankSearchAsNoSearchFilter() {
        Customer customer = customer(UUID.randomUUID(), "CUS-0001", true, CustomerType.INDIVIDUAL);
        when(customerRepository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(customer), PageRequest.of(0, 10), 1));

        customerService.getCustomers("   ", null, null, PageRequest.of(0, 10));

        verify(customerRepository).findAll(any(Specification.class), any(PageRequest.class));
    }

    @Test
    void allowsSameCodeInAnotherTenantThroughTenantScopedLookup() {
        when(currentTenantProvider.getCurrentTenantId()).thenReturn(OTHER_TENANT_ID);
        when(tenantRepository.getReferenceById(OTHER_TENANT_ID)).thenReturn(tenant(OTHER_TENANT_ID, "Second Business", "SECOND"));
        when(customerRepository.existsByTenantIdAndCodeIgnoreCase(OTHER_TENANT_ID, "CUS-0001")).thenReturn(false);
        when(customerRepository.saveAndFlush(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CustomerResponse response = customerService.createCustomer(new CreateCustomerRequest(
                CustomerType.INDIVIDUAL,
                "cus-0001",
                "Jane Wanjiku",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null));

        assertEquals("CUS-0001", response.code());
    }

    private Customer customer(UUID id, String code, boolean active, CustomerType type) {
        Customer customer = new Customer();
        customer.setId(id);
        customer.setTenant(tenant);
        customer.setType(type);
        customer.setCode(code);
        customer.setName("Jane Wanjiku");
        customer.setEmail("jane@example.com");
        customer.setPhone("+254712345678");
        customer.setTaxNumber("P051234567X");
        customer.setAddressLine1("Kimathi Street");
        customer.setAddressLine2("2nd Floor");
        customer.setCity("Nairobi");
        customer.setStateOrCounty("Nairobi");
        customer.setPostalCode("00100");
        customer.setCountryCode("KE");
        customer.setNotes("Prefers SMS communication");
        customer.setActive(active);
        customer.setCreatedAt(Instant.parse("2026-08-01T10:00:00Z"));
        customer.setUpdatedAt(Instant.parse("2026-08-01T10:05:00Z"));
        return customer;
    }

    private Tenant tenant(UUID id, String name, String code) {
        Tenant tenant = new Tenant();
        tenant.setId(id);
        tenant.setName(name);
        tenant.setCode(code);
        tenant.setStatus(TenantStatus.ACTIVE);
        return tenant;
    }
}
