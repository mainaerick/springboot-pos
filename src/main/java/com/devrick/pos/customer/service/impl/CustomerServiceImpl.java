package com.devrick.pos.customer.service.impl;

import com.devrick.pos.common.dto.PageResponse;
import com.devrick.pos.customer.dto.CreateCustomerRequest;
import com.devrick.pos.customer.dto.CustomerResponse;
import com.devrick.pos.customer.dto.UpdateCustomerRequest;
import com.devrick.pos.customer.dto.UpdateCustomerStatusRequest;
import com.devrick.pos.customer.entity.Customer;
import com.devrick.pos.customer.entity.CustomerType;
import com.devrick.pos.customer.mapper.CustomerMapper;
import com.devrick.pos.customer.repository.CustomerRepository;
import com.devrick.pos.customer.repository.CustomerSpecifications;
import com.devrick.pos.customer.service.CustomerService;
import com.devrick.pos.exception.customer.CustomerNotFoundException;
import com.devrick.pos.exception.customer.DuplicateCustomerCodeException;
import com.devrick.pos.tenant.entity.Tenant;
import com.devrick.pos.tenant.repository.TenantRepository;
import com.devrick.pos.tenant.security.CurrentTenantProvider;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class CustomerServiceImpl implements CustomerService {

    private static final Logger log = LoggerFactory.getLogger(CustomerServiceImpl.class);
    private static final List<String> ALLOWED_SORT_PROPERTIES =
            List.of("code", "name", "type", "city", "active", "createdAt", "updatedAt");
    private static final Sort DEFAULT_SORT = Sort.by(Sort.Direction.DESC, "createdAt");

    private final CustomerRepository customerRepository;
    private final CustomerMapper customerMapper;
    private final TenantRepository tenantRepository;
    private final CurrentTenantProvider currentTenantProvider;

    public CustomerServiceImpl(
            CustomerRepository customerRepository,
            CustomerMapper customerMapper,
            TenantRepository tenantRepository,
            CurrentTenantProvider currentTenantProvider) {
        this.customerRepository = customerRepository;
        this.customerMapper = customerMapper;
        this.tenantRepository = tenantRepository;
        this.currentTenantProvider = currentTenantProvider;
    }

    @Override
    @Transactional
    public CustomerResponse createCustomer(CreateCustomerRequest request) {
        UUID tenantId = currentTenantProvider.getCurrentTenantId();
        String normalizedCode = normalizeCode(request.code());
        log.info("Creating customer with code {} for tenant {}", normalizedCode, tenantId);

        ensureUniqueCode(tenantId, normalizedCode);

        Customer customer = customerMapper.toEntity(normalizeCreateRequest(request, normalizedCode));
        customer.setTenant(tenantRepository.getReferenceById(tenantId));
        customer.setActive(true);

        Customer savedCustomer = saveCustomer(customer);
        return customerMapper.toResponse(savedCustomer);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CustomerResponse> getCustomers(String search, CustomerType type, Boolean active, Pageable pageable) {
        UUID tenantId = currentTenantProvider.getCurrentTenantId();
        Pageable sanitizedPageable = sanitizePageable(pageable);
        String normalizedSearch = normalizeSearch(search);
        Specification<Customer> specification = CustomerSpecifications.byTenantAndFilters(
                tenantId, normalizedSearch, type, active);
        Page<CustomerResponse> responsePage = customerRepository.findAll(specification, sanitizedPageable)
                .map(customerMapper::toResponse);
        return toPageResponse(responsePage);
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerResponse getCustomer(UUID customerId) {
        return customerMapper.toResponse(findCustomerById(customerId, currentTenantProvider.getCurrentTenantId()));
    }

    @Override
    @Transactional
    public CustomerResponse updateCustomer(UUID customerId, UpdateCustomerRequest request) {
        UUID tenantId = currentTenantProvider.getCurrentTenantId();
        Customer customer = findCustomerById(customerId, tenantId);
        log.info("Updating customer {}", customerId);

        customerMapper.updateEntity(normalizeUpdateRequest(request), customer);
        customer.setType(request.type());
        customer.setName(normalizeName(request.name()));

        Customer savedCustomer = saveCustomer(customer);
        return customerMapper.toResponse(savedCustomer);
    }

    @Override
    @Transactional
    public CustomerResponse updateCustomerStatus(UUID customerId, UpdateCustomerStatusRequest request) {
        UUID tenantId = currentTenantProvider.getCurrentTenantId();
        Customer customer = findCustomerById(customerId, tenantId);
        boolean desiredActive = Boolean.TRUE.equals(request.active());

        if (customer.isActive() == desiredActive) {
            return customerMapper.toResponse(customer);
        }

        customer.setActive(desiredActive);
        log.info("Updating customer status {} to {}", customerId, desiredActive);
        Customer savedCustomer = saveCustomer(customer);
        return customerMapper.toResponse(savedCustomer);
    }

    private Customer findCustomerById(UUID customerId, UUID tenantId) {
        return customerRepository.findByIdAndTenantId(customerId, tenantId)
                .orElseThrow(() -> new CustomerNotFoundException(customerId));
    }

    private void ensureUniqueCode(UUID tenantId, String code) {
        if (customerRepository.existsByTenantIdAndCodeIgnoreCase(tenantId, code)) {
            throw new DuplicateCustomerCodeException(code);
        }
    }

    private Customer saveCustomer(Customer customer) {
        try {
            return customerRepository.saveAndFlush(customer);
        } catch (DataIntegrityViolationException exception) {
            throw translateConflict(exception, customer);
        }
    }

    private RuntimeException translateConflict(DataIntegrityViolationException exception, Customer customer) {
        String message = extractMessage(exception);
        if (message.contains("uk_customers_tenant_code")) {
            return new DuplicateCustomerCodeException(customer.getCode());
        }
        if (message.contains("customers") && message.contains("code")) {
            return new DuplicateCustomerCodeException(customer.getCode());
        }
        return exception;
    }

    private String extractMessage(Throwable exception) {
        Throwable current = exception;
        while (current != null) {
            String message = current.getMessage();
            if (StringUtils.hasText(message)) {
                return message.toLowerCase(Locale.ROOT);
            }
            current = current.getCause();
        }
        return "";
    }

    private Pageable sanitizePageable(Pageable pageable) {
        if (pageable == null || pageable.isUnpaged()) {
            return Pageable.unpaged();
        }

        Sort sanitizedSort = sanitizeSort(pageable.getSort());
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sanitizedSort);
    }

    private Sort sanitizeSort(Sort sort) {
        if (sort == null || sort.isUnsorted()) {
            return DEFAULT_SORT;
        }

        List<Sort.Order> orders = sort.stream()
                .filter(order -> ALLOWED_SORT_PROPERTIES.contains(order.getProperty()))
                .map(order -> new Sort.Order(order.getDirection(), order.getProperty()))
                .toList();

        return orders.isEmpty() ? DEFAULT_SORT : Sort.by(orders);
    }

    private String normalizeSearch(String search) {
        if (!StringUtils.hasText(search)) {
            return null;
        }
        return search.trim();
    }

    private String normalizeCode(String code) {
        return code.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeName(String name) {
        return name.trim();
    }

    private String normalizeOptionalText(String text) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        return text.trim();
    }

    private String normalizeEmail(String email) {
        if (!StringUtils.hasText(email)) {
            return null;
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeCountryCode(String countryCode) {
        if (!StringUtils.hasText(countryCode)) {
            return null;
        }
        return countryCode.trim().toUpperCase(Locale.ROOT);
    }

    private CreateCustomerRequest normalizeCreateRequest(CreateCustomerRequest request, String normalizedCode) {
        return new CreateCustomerRequest(
                request.type(),
                normalizedCode,
                normalizeName(request.name()),
                normalizeEmail(request.email()),
                normalizeOptionalText(request.phone()),
                normalizeOptionalText(request.taxNumber()),
                normalizeOptionalText(request.addressLine1()),
                normalizeOptionalText(request.addressLine2()),
                normalizeOptionalText(request.city()),
                normalizeOptionalText(request.stateOrCounty()),
                normalizeOptionalText(request.postalCode()),
                normalizeCountryCode(request.countryCode()),
                normalizeOptionalText(request.notes()));
    }

    private UpdateCustomerRequest normalizeUpdateRequest(UpdateCustomerRequest request) {
        return new UpdateCustomerRequest(
                request.type(),
                normalizeName(request.name()),
                normalizeEmail(request.email()),
                normalizeOptionalText(request.phone()),
                normalizeOptionalText(request.taxNumber()),
                normalizeOptionalText(request.addressLine1()),
                normalizeOptionalText(request.addressLine2()),
                normalizeOptionalText(request.city()),
                normalizeOptionalText(request.stateOrCounty()),
                normalizeOptionalText(request.postalCode()),
                normalizeCountryCode(request.countryCode()),
                normalizeOptionalText(request.notes()));
    }

    private PageResponse<CustomerResponse> toPageResponse(Page<CustomerResponse> page) {
        return PageResponse.<CustomerResponse>builder()
                .content(page.getContent())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }
}
