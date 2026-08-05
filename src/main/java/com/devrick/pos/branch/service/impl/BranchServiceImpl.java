package com.devrick.pos.branch.service.impl;

import com.devrick.pos.branch.dto.BranchResponse;
import com.devrick.pos.branch.dto.CreateBranchRequest;
import com.devrick.pos.branch.dto.UpdateBranchRequest;
import com.devrick.pos.branch.dto.UpdateBranchStatusRequest;
import com.devrick.pos.branch.entity.Branch;
import com.devrick.pos.branch.mapper.BranchMapper;
import com.devrick.pos.branch.repository.BranchRepository;
import com.devrick.pos.branch.repository.BranchSpecifications;
import com.devrick.pos.branch.service.BranchService;
import com.devrick.pos.common.dto.PageResponse;
import com.devrick.pos.exception.branch.BranchNotFoundException;
import com.devrick.pos.exception.branch.DuplicateBranchCodeException;
import com.devrick.pos.exception.branch.DuplicateBranchNameException;
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
public class BranchServiceImpl implements BranchService {

    private static final Logger log = LoggerFactory.getLogger(BranchServiceImpl.class);
    private static final List<String> ALLOWED_SORT_PROPERTIES = List.of("name", "code", "city", "active", "createdAt", "updatedAt");
    private static final Sort DEFAULT_SORT = Sort.by(Sort.Direction.DESC, "createdAt");

    private final BranchRepository branchRepository;
    private final BranchMapper branchMapper;
    private final TenantRepository tenantRepository;
    private final CurrentTenantProvider currentTenantProvider;

    public BranchServiceImpl(
            BranchRepository branchRepository,
            BranchMapper branchMapper,
            TenantRepository tenantRepository,
            CurrentTenantProvider currentTenantProvider) {
        this.branchRepository = branchRepository;
        this.branchMapper = branchMapper;
        this.tenantRepository = tenantRepository;
        this.currentTenantProvider = currentTenantProvider;
    }

    @Override
    @Transactional
    public BranchResponse createBranch(CreateBranchRequest request) {
        UUID tenantId = currentTenantProvider.getCurrentTenantId();
        Tenant tenant = tenantRepository.getReferenceById(tenantId);
        String normalizedName = normalizeName(request.name());
        String normalizedCode = normalizeCode(request.code());
        log.info("Creating branch with code {} for tenant {}", normalizedCode, tenantId);

        ensureUniqueCode(tenantId, normalizedCode);
        ensureUniqueName(tenantId, normalizedName);

        Branch branch = branchMapper.toEntity(normalizeCreateRequest(request, normalizedName, normalizedCode));
        branch.setTenant(tenant);
        branch.setActive(true);

        Branch savedBranch = saveBranch(branch);
        return branchMapper.toResponse(savedBranch);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<BranchResponse> getBranches(String search, Boolean active, Pageable pageable) {
        UUID tenantId = currentTenantProvider.getCurrentTenantId();
        Pageable sanitizedPageable = sanitizePageable(pageable);
        String normalizedSearch = normalizeSearch(search);
        Specification<Branch> specification = BranchSpecifications.byTenantAndFilters(tenantId, normalizedSearch, active);
        Page<BranchResponse> responsePage = branchRepository.findAll(specification, sanitizedPageable)
                .map(branchMapper::toResponse);
        return toPageResponse(responsePage);
    }

    @Override
    @Transactional(readOnly = true)
    public BranchResponse getBranch(UUID branchId) {
        UUID tenantId = currentTenantProvider.getCurrentTenantId();
        return branchMapper.toResponse(findBranchById(branchId, tenantId));
    }

    @Override
    @Transactional
    public BranchResponse updateBranch(UUID branchId, UpdateBranchRequest request) {
        UUID tenantId = currentTenantProvider.getCurrentTenantId();
        Branch branch = findBranchById(branchId, tenantId);
        String normalizedName = normalizeName(request.name());
        log.info("Updating branch {}", branchId);

        if (branchRepository.existsByTenantIdAndNameIgnoreCaseAndIdNot(tenantId, normalizedName, branchId)) {
            throw new DuplicateBranchNameException(normalizedName);
        }

        branchMapper.updateEntity(normalizeUpdateRequest(request, normalizedName), branch);
        branch.setName(normalizedName);

        Branch savedBranch = saveBranch(branch);
        return branchMapper.toResponse(savedBranch);
    }

    @Override
    @Transactional
    public BranchResponse updateBranchStatus(UUID branchId, UpdateBranchStatusRequest request) {
        UUID tenantId = currentTenantProvider.getCurrentTenantId();
        Branch branch = findBranchById(branchId, tenantId);
        boolean desiredActive = Boolean.TRUE.equals(request.active());

        if (branch.isActive() == desiredActive) {
            return branchMapper.toResponse(branch);
        }

        branch.setActive(desiredActive);
        log.info("Updating branch status {} to {}", branchId, desiredActive);
        Branch savedBranch = saveBranch(branch);
        return branchMapper.toResponse(savedBranch);
    }

    private Branch findBranchById(UUID branchId, UUID tenantId) {
        return branchRepository.findByIdAndTenantId(branchId, tenantId)
                .orElseThrow(() -> new BranchNotFoundException(branchId));
    }

    private void ensureUniqueCode(UUID tenantId, String code) {
        if (branchRepository.existsByTenantIdAndCodeIgnoreCase(tenantId, code)) {
            throw new DuplicateBranchCodeException(code);
        }
    }

    private void ensureUniqueName(UUID tenantId, String name) {
        if (branchRepository.existsByTenantIdAndNameIgnoreCase(tenantId, name)) {
            throw new DuplicateBranchNameException(name);
        }
    }

    private Branch saveBranch(Branch branch) {
        try {
            return branchRepository.saveAndFlush(branch);
        } catch (DataIntegrityViolationException exception) {
            throw translateConflict(exception, branch);
        }
    }

    private RuntimeException translateConflict(DataIntegrityViolationException exception, Branch branch) {
        String message = extractMessage(exception);
        if (message.contains("uk_branches_tenant_code")) {
            return new DuplicateBranchCodeException(branch.getCode());
        }
        if (message.contains("uk_branches_tenant_name_ci")) {
            return new DuplicateBranchNameException(branch.getName());
        }
        if (message.contains("branches") && message.contains("code")) {
            return new DuplicateBranchCodeException(branch.getCode());
        }
        if (message.contains("branches") && message.contains("name")) {
            return new DuplicateBranchNameException(branch.getName());
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

    private String normalizeName(String name) {
        return name.trim();
    }

    private String normalizeCode(String code) {
        return code.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeCountryCode(String countryCode) {
        if (!StringUtils.hasText(countryCode)) {
            return null;
        }
        return countryCode.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeOptionalText(String text) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        return text.trim();
    }

    private CreateBranchRequest normalizeCreateRequest(
            CreateBranchRequest request, String normalizedName, String normalizedCode) {
        return new CreateBranchRequest(
                normalizedName,
                normalizedCode,
                normalizeOptionalText(request.email()),
                normalizeOptionalText(request.phone()),
                normalizeOptionalText(request.addressLine1()),
                normalizeOptionalText(request.addressLine2()),
                normalizeOptionalText(request.city()),
                normalizeOptionalText(request.stateOrCounty()),
                normalizeOptionalText(request.postalCode()),
                normalizeCountryCode(request.countryCode()));
    }

    private UpdateBranchRequest normalizeUpdateRequest(UpdateBranchRequest request, String normalizedName) {
        return new UpdateBranchRequest(
                normalizedName,
                normalizeOptionalText(request.email()),
                normalizeOptionalText(request.phone()),
                normalizeOptionalText(request.addressLine1()),
                normalizeOptionalText(request.addressLine2()),
                normalizeOptionalText(request.city()),
                normalizeOptionalText(request.stateOrCounty()),
                normalizeOptionalText(request.postalCode()),
                normalizeCountryCode(request.countryCode()));
    }

    private PageResponse<BranchResponse> toPageResponse(Page<BranchResponse> page) {
        return PageResponse.<BranchResponse>builder()
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
