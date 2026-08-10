package com.devrick.pos.supplier.service.impl;

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
import com.devrick.pos.supplier.repository.SupplierSpecifications;
import com.devrick.pos.supplier.service.SupplierService;
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
public class SupplierServiceImpl implements SupplierService {

    private static final Logger log = LoggerFactory.getLogger(SupplierServiceImpl.class);
    private static final List<String> ALLOWED_SORT_PROPERTIES =
            List.of("code", "name", "contactPerson", "city", "active", "createdAt", "updatedAt");
    private static final Sort DEFAULT_SORT = Sort.by(Sort.Direction.DESC, "createdAt");

    private final SupplierRepository supplierRepository;
    private final SupplierMapper supplierMapper;
    private final TenantRepository tenantRepository;
    private final CurrentTenantProvider currentTenantProvider;

    public SupplierServiceImpl(
            SupplierRepository supplierRepository,
            SupplierMapper supplierMapper,
            TenantRepository tenantRepository,
            CurrentTenantProvider currentTenantProvider) {
        this.supplierRepository = supplierRepository;
        this.supplierMapper = supplierMapper;
        this.tenantRepository = tenantRepository;
        this.currentTenantProvider = currentTenantProvider;
    }

    @Override
    @Transactional
    public SupplierResponse createSupplier(CreateSupplierRequest request) {
        UUID tenantId = currentTenantProvider.getCurrentTenantId();
        Tenant tenant = tenantRepository.getReferenceById(tenantId);
        String normalizedCode = normalizeCode(request.code());
        log.info("Creating supplier with code {} for tenant {}", normalizedCode, tenantId);

        ensureUniqueCode(tenantId, normalizedCode);

        Supplier supplier = supplierMapper.toEntity(normalizeCreateRequest(request, normalizedCode));
        supplier.setTenant(tenant);
        supplier.setActive(true);

        Supplier savedSupplier = saveSupplier(supplier);
        return supplierMapper.toResponse(savedSupplier);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<SupplierResponse> getSuppliers(String search, Boolean active, Pageable pageable) {
        UUID tenantId = currentTenantProvider.getCurrentTenantId();
        Pageable sanitizedPageable = sanitizePageable(pageable);
        String normalizedSearch = normalizeSearch(search);
        Specification<Supplier> specification = SupplierSpecifications.byTenantAndFilters(
                tenantId, normalizedSearch, active);
        Page<SupplierResponse> responsePage = supplierRepository.findAll(specification, sanitizedPageable)
                .map(supplierMapper::toResponse);
        return toPageResponse(responsePage);
    }

    @Override
    @Transactional(readOnly = true)
    public SupplierResponse getSupplier(UUID supplierId) {
        return supplierMapper.toResponse(findSupplierById(supplierId, currentTenantProvider.getCurrentTenantId()));
    }

    @Override
    @Transactional
    public SupplierResponse updateSupplier(UUID supplierId, UpdateSupplierRequest request) {
        UUID tenantId = currentTenantProvider.getCurrentTenantId();
        Supplier supplier = findSupplierById(supplierId, tenantId);
        log.info("Updating supplier {}", supplierId);

        supplierMapper.updateEntity(normalizeUpdateRequest(request), supplier);
        supplier.setName(normalizeName(request.name()));

        Supplier savedSupplier = saveSupplier(supplier);
        return supplierMapper.toResponse(savedSupplier);
    }

    @Override
    @Transactional
    public SupplierResponse updateSupplierStatus(UUID supplierId, UpdateSupplierStatusRequest request) {
        UUID tenantId = currentTenantProvider.getCurrentTenantId();
        Supplier supplier = findSupplierById(supplierId, tenantId);
        boolean desiredActive = Boolean.TRUE.equals(request.active());

        if (supplier.isActive() == desiredActive) {
            return supplierMapper.toResponse(supplier);
        }

        supplier.setActive(desiredActive);
        log.info("Updating supplier status {} to {}", supplierId, desiredActive);
        Supplier savedSupplier = saveSupplier(supplier);
        return supplierMapper.toResponse(savedSupplier);
    }

    private Supplier findSupplierById(UUID supplierId, UUID tenantId) {
        return supplierRepository.findByIdAndTenantId(supplierId, tenantId)
                .orElseThrow(() -> new SupplierNotFoundException(supplierId));
    }

    private void ensureUniqueCode(UUID tenantId, String code) {
        if (supplierRepository.existsByTenantIdAndCodeIgnoreCase(tenantId, code)) {
            throw new DuplicateSupplierCodeException(code);
        }
    }

    private Supplier saveSupplier(Supplier supplier) {
        try {
            return supplierRepository.saveAndFlush(supplier);
        } catch (DataIntegrityViolationException exception) {
            throw translateConflict(exception, supplier);
        }
    }

    private RuntimeException translateConflict(DataIntegrityViolationException exception, Supplier supplier) {
        String message = extractMessage(exception);
        if (message.contains("uk_suppliers_tenant_code")) {
            return new DuplicateSupplierCodeException(supplier.getCode());
        }
        if (message.contains("suppliers") && message.contains("code")) {
            return new DuplicateSupplierCodeException(supplier.getCode());
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

    private CreateSupplierRequest normalizeCreateRequest(CreateSupplierRequest request, String normalizedCode) {
        return new CreateSupplierRequest(
                normalizedCode,
                normalizeName(request.name()),
                normalizeOptionalText(request.contactPerson()),
                normalizeEmail(request.email()),
                normalizeOptionalText(request.phone()),
                normalizeOptionalText(request.alternatePhone()),
                normalizeOptionalText(request.taxNumber()),
                normalizeOptionalText(request.addressLine1()),
                normalizeOptionalText(request.addressLine2()),
                normalizeOptionalText(request.city()),
                normalizeOptionalText(request.stateOrCounty()),
                normalizeOptionalText(request.postalCode()),
                normalizeCountryCode(request.countryCode()),
                normalizeOptionalText(request.notes()));
    }

    private UpdateSupplierRequest normalizeUpdateRequest(UpdateSupplierRequest request) {
        return new UpdateSupplierRequest(
                normalizeName(request.name()),
                normalizeOptionalText(request.contactPerson()),
                normalizeEmail(request.email()),
                normalizeOptionalText(request.phone()),
                normalizeOptionalText(request.alternatePhone()),
                normalizeOptionalText(request.taxNumber()),
                normalizeOptionalText(request.addressLine1()),
                normalizeOptionalText(request.addressLine2()),
                normalizeOptionalText(request.city()),
                normalizeOptionalText(request.stateOrCounty()),
                normalizeOptionalText(request.postalCode()),
                normalizeCountryCode(request.countryCode()),
                normalizeOptionalText(request.notes()));
    }

    private PageResponse<SupplierResponse> toPageResponse(Page<SupplierResponse> page) {
        return PageResponse.<SupplierResponse>builder()
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
