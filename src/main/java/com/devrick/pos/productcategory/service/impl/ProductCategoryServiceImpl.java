package com.devrick.pos.productcategory.service.impl;

import com.devrick.pos.common.dto.PageResponse;
import com.devrick.pos.exception.productcategory.DuplicateProductCategoryCodeException;
import com.devrick.pos.exception.productcategory.DuplicateProductCategoryNameException;
import com.devrick.pos.exception.productcategory.ProductCategoryNotFoundException;
import com.devrick.pos.productcategory.dto.CreateProductCategoryRequest;
import com.devrick.pos.productcategory.dto.ProductCategoryResponse;
import com.devrick.pos.productcategory.dto.UpdateProductCategoryRequest;
import com.devrick.pos.productcategory.dto.UpdateProductCategoryStatusRequest;
import com.devrick.pos.productcategory.entity.ProductCategory;
import com.devrick.pos.productcategory.mapper.ProductCategoryMapper;
import com.devrick.pos.productcategory.repository.ProductCategoryRepository;
import com.devrick.pos.productcategory.repository.ProductCategorySpecifications;
import com.devrick.pos.productcategory.service.ProductCategoryService;
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
public class ProductCategoryServiceImpl implements ProductCategoryService {

    private static final Logger log = LoggerFactory.getLogger(ProductCategoryServiceImpl.class);
    private static final List<String> ALLOWED_SORT_PROPERTIES =
            List.of("code", "name", "active", "createdAt", "updatedAt");
    private static final Sort DEFAULT_SORT = Sort.by(Sort.Direction.DESC, "createdAt");

    private final ProductCategoryRepository productCategoryRepository;
    private final ProductCategoryMapper productCategoryMapper;
    private final TenantRepository tenantRepository;
    private final CurrentTenantProvider currentTenantProvider;

    public ProductCategoryServiceImpl(
            ProductCategoryRepository productCategoryRepository,
            ProductCategoryMapper productCategoryMapper,
            TenantRepository tenantRepository,
            CurrentTenantProvider currentTenantProvider) {
        this.productCategoryRepository = productCategoryRepository;
        this.productCategoryMapper = productCategoryMapper;
        this.tenantRepository = tenantRepository;
        this.currentTenantProvider = currentTenantProvider;
    }

    @Override
    @Transactional
    public ProductCategoryResponse createCategory(CreateProductCategoryRequest request) {
        UUID tenantId = currentTenantProvider.getCurrentTenantId();
        String normalizedCode = normalizeCode(request.code());
        String normalizedName = normalizeName(request.name());
        log.info("Creating product category with code {} for tenant {}", normalizedCode, tenantId);

        ensureUniqueCode(tenantId, normalizedCode);
        ensureUniqueName(tenantId, normalizedName);

        ProductCategory category = productCategoryMapper.toEntity(normalizeCreateRequest(
                request, normalizedCode, normalizedName));
        category.setTenant(tenantRepository.getReferenceById(tenantId));
        category.setActive(true);

        ProductCategory savedCategory = saveCategory(category);
        return productCategoryMapper.toResponse(savedCategory);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProductCategoryResponse> getCategories(String search, Boolean active, Pageable pageable) {
        UUID tenantId = currentTenantProvider.getCurrentTenantId();
        Pageable sanitizedPageable = sanitizePageable(pageable);
        String normalizedSearch = normalizeSearch(search);
        Specification<ProductCategory> specification =
                ProductCategorySpecifications.byTenantAndFilters(tenantId, normalizedSearch, active);
        Page<ProductCategoryResponse> responsePage = productCategoryRepository.findAll(specification, sanitizedPageable)
                .map(productCategoryMapper::toResponse);
        return toPageResponse(responsePage);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductCategoryResponse getCategory(UUID categoryId) {
        UUID tenantId = currentTenantProvider.getCurrentTenantId();
        return productCategoryMapper.toResponse(findCategoryById(categoryId, tenantId));
    }

    @Override
    @Transactional
    public ProductCategoryResponse updateCategory(UUID categoryId, UpdateProductCategoryRequest request) {
        UUID tenantId = currentTenantProvider.getCurrentTenantId();
        ProductCategory category = findCategoryById(categoryId, tenantId);
        String normalizedName = normalizeName(request.name());
        log.info("Updating product category {}", categoryId);

        if (productCategoryRepository.existsByTenantIdAndNameIgnoreCaseAndIdNot(tenantId, normalizedName, categoryId)) {
            throw new DuplicateProductCategoryNameException(normalizedName);
        }

        productCategoryMapper.updateEntity(normalizeUpdateRequest(request, normalizedName), category);
        category.setName(normalizedName);

        ProductCategory savedCategory = saveCategory(category);
        return productCategoryMapper.toResponse(savedCategory);
    }

    @Override
    @Transactional
    public ProductCategoryResponse updateCategoryStatus(UUID categoryId, UpdateProductCategoryStatusRequest request) {
        UUID tenantId = currentTenantProvider.getCurrentTenantId();
        ProductCategory category = findCategoryById(categoryId, tenantId);
        boolean desiredActive = Boolean.TRUE.equals(request.active());

        if (category.isActive() == desiredActive) {
            return productCategoryMapper.toResponse(category);
        }

        category.setActive(desiredActive);
        log.info("Updating product category status {} to {}", categoryId, desiredActive);
        ProductCategory savedCategory = saveCategory(category);
        return productCategoryMapper.toResponse(savedCategory);
    }

    private ProductCategory findCategoryById(UUID categoryId, UUID tenantId) {
        return productCategoryRepository.findByIdAndTenantId(categoryId, tenantId)
                .orElseThrow(() -> new ProductCategoryNotFoundException(categoryId));
    }

    private void ensureUniqueCode(UUID tenantId, String code) {
        if (productCategoryRepository.existsByTenantIdAndCodeIgnoreCase(tenantId, code)) {
            throw new DuplicateProductCategoryCodeException(code);
        }
    }

    private void ensureUniqueName(UUID tenantId, String name) {
        if (productCategoryRepository.existsByTenantIdAndNameIgnoreCase(tenantId, name)) {
            throw new DuplicateProductCategoryNameException(name);
        }
    }

    private ProductCategory saveCategory(ProductCategory category) {
        try {
            return productCategoryRepository.saveAndFlush(category);
        } catch (DataIntegrityViolationException exception) {
            throw translateConflict(exception, category);
        }
    }

    private RuntimeException translateConflict(DataIntegrityViolationException exception, ProductCategory category) {
        String message = extractMessage(exception);
        if (message.contains("uk_product_categories_tenant_code")) {
            return new DuplicateProductCategoryCodeException(category.getCode());
        }
        if (message.contains("uk_product_categories_tenant_name_ci")) {
            return new DuplicateProductCategoryNameException(category.getName());
        }
        if (message.contains("product_categories") && message.contains("code")) {
            return new DuplicateProductCategoryCodeException(category.getCode());
        }
        if (message.contains("product_categories") && message.contains("name")) {
            return new DuplicateProductCategoryNameException(category.getName());
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

    private String normalizeDescription(String description) {
        if (!StringUtils.hasText(description)) {
            return null;
        }
        return description.trim();
    }

    private CreateProductCategoryRequest normalizeCreateRequest(
            CreateProductCategoryRequest request, String normalizedCode, String normalizedName) {
        return new CreateProductCategoryRequest(
                normalizedCode,
                normalizedName,
                normalizeDescription(request.description()));
    }

    private UpdateProductCategoryRequest normalizeUpdateRequest(
            UpdateProductCategoryRequest request, String normalizedName) {
        return new UpdateProductCategoryRequest(normalizedName, normalizeDescription(request.description()));
    }

    private PageResponse<ProductCategoryResponse> toPageResponse(Page<ProductCategoryResponse> page) {
        return PageResponse.<ProductCategoryResponse>builder()
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
