package com.devrick.pos.branch.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.devrick.pos.branch.dto.BranchResponse;
import com.devrick.pos.branch.dto.CreateBranchRequest;
import com.devrick.pos.branch.dto.UpdateBranchRequest;
import com.devrick.pos.branch.dto.UpdateBranchStatusRequest;
import com.devrick.pos.branch.entity.Branch;
import com.devrick.pos.branch.mapper.BranchMapper;
import com.devrick.pos.branch.repository.BranchRepository;
import com.devrick.pos.branch.service.impl.BranchServiceImpl;
import com.devrick.pos.common.dto.PageResponse;
import com.devrick.pos.exception.branch.BranchNotFoundException;
import com.devrick.pos.exception.branch.DuplicateBranchCodeException;
import com.devrick.pos.exception.branch.DuplicateBranchNameException;
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
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class BranchServiceImplTest {

    private static final UUID TENANT_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");

    @Mock
    private BranchRepository branchRepository;

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private CurrentTenantProvider currentTenantProvider;

    private final BranchMapper branchMapper = Mappers.getMapper(BranchMapper.class);
    private final Tenant tenant = tenant();

    private BranchServiceImpl branchService;

    @BeforeEach
    void setUp() {
        org.mockito.Mockito.lenient().when(currentTenantProvider.getCurrentTenantId()).thenReturn(TENANT_ID);
        org.mockito.Mockito.lenient().when(tenantRepository.getReferenceById(TENANT_ID)).thenReturn(tenant);
        branchService = new BranchServiceImpl(branchRepository, branchMapper, tenantRepository, currentTenantProvider);
    }

    @Test
    void createBranchNormalizesFieldsAndUsesAuthenticatedTenant() {
        CreateBranchRequest request = new CreateBranchRequest(
                " Nairobi CBD ",
                "nrb-cbd",
                "nairobi@example.com",
                " +254712345678 ",
                " Kimathi Street ",
                " 2nd Floor ",
                " Nairobi ",
                " Nairobi ",
                " 00100 ",
                "ke");
        Branch savedBranch = branch(UUID.randomUUID(), true);
        when(branchRepository.existsByTenantIdAndCodeIgnoreCase(TENANT_ID, "NRB-CBD")).thenReturn(false);
        when(branchRepository.existsByTenantIdAndNameIgnoreCase(TENANT_ID, "Nairobi CBD")).thenReturn(false);
        when(branchRepository.saveAndFlush(any(Branch.class))).thenReturn(savedBranch);

        BranchResponse response = branchService.createBranch(request);

        ArgumentCaptor<Branch> captor = ArgumentCaptor.forClass(Branch.class);
        verify(branchRepository).saveAndFlush(captor.capture());
        assertEquals("Nairobi CBD", captor.getValue().getName());
        assertEquals("NRB-CBD", captor.getValue().getCode());
        assertEquals("ke", request.countryCode());
        assertEquals("KE", captor.getValue().getCountryCode());
        assertEquals(tenant, captor.getValue().getTenant());
        assertTrue(captor.getValue().isActive());
        assertEquals(savedBranch.getId(), response.id());
    }

    @Test
    void createBranchRejectsDuplicateCode() {
        when(branchRepository.existsByTenantIdAndCodeIgnoreCase(TENANT_ID, "NRB-CBD")).thenReturn(true);

        assertThrows(
                DuplicateBranchCodeException.class,
                () -> branchService.createBranch(new CreateBranchRequest(
                        "Nairobi CBD",
                        "nrb-cbd",
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null)));
        verify(branchRepository, never()).saveAndFlush(any());
    }

    @Test
    void createBranchRejectsDuplicateName() {
        when(branchRepository.existsByTenantIdAndCodeIgnoreCase(TENANT_ID, "NRB-CBD")).thenReturn(false);
        when(branchRepository.existsByTenantIdAndNameIgnoreCase(TENANT_ID, "Nairobi CBD")).thenReturn(true);

        assertThrows(
                DuplicateBranchNameException.class,
                () -> branchService.createBranch(new CreateBranchRequest(
                        " Nairobi CBD ",
                        "nrb-cbd",
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null)));
        verify(branchRepository, never()).saveAndFlush(any());
    }

    @Test
    void getBranchReturnsTenantOwnedBranch() {
        Branch branch = branch(UUID.randomUUID(), true);
        when(branchRepository.findByIdAndTenantId(branch.getId(), TENANT_ID)).thenReturn(Optional.of(branch));

        BranchResponse response = branchService.getBranch(branch.getId());

        assertEquals(branch.getId(), response.id());
        assertEquals(branch.getCode(), response.code());
    }

    @Test
    void getBranchThrowsWhenBranchIsMissing() {
        UUID branchId = UUID.randomUUID();
        when(branchRepository.findByIdAndTenantId(branchId, TENANT_ID)).thenReturn(Optional.empty());

        assertThrows(BranchNotFoundException.class, () -> branchService.getBranch(branchId));
    }

    @Test
    void updateBranchPreservesTenantCodeAndActiveStatus() {
        Branch branch = branch(UUID.randomUUID(), false);
        when(branchRepository.findByIdAndTenantId(branch.getId(), TENANT_ID)).thenReturn(Optional.of(branch));
        when(branchRepository.existsByTenantIdAndNameIgnoreCaseAndIdNot(TENANT_ID, "Westlands", branch.getId()))
                .thenReturn(false);
        when(branchRepository.saveAndFlush(any(Branch.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BranchResponse response = branchService.updateBranch(
                branch.getId(),
                new UpdateBranchRequest(
                        " Westlands ",
                        " westlands@example.com ",
                        " +254700000000 ",
                        " Waiyaki Way ",
                        " Suite 4 ",
                        " Nairobi ",
                        " Nairobi ",
                        " 00200 ",
                        "ke"));

        assertEquals("Westlands", response.name());
        assertEquals("NRB-CBD", response.code());
        assertFalse(response.active());
        assertEquals(tenant, branch.getTenant());
        assertEquals("KE", branch.getCountryCode());
    }

    @Test
    void updateBranchRejectsDuplicateName() {
        Branch branch = branch(UUID.randomUUID(), true);
        when(branchRepository.findByIdAndTenantId(branch.getId(), TENANT_ID)).thenReturn(Optional.of(branch));
        when(branchRepository.existsByTenantIdAndNameIgnoreCaseAndIdNot(TENANT_ID, "Westlands", branch.getId()))
                .thenReturn(true);

        assertThrows(
                DuplicateBranchNameException.class,
                () -> branchService.updateBranch(
                        branch.getId(),
                        new UpdateBranchRequest(
                                " Westlands ",
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null)));
        verify(branchRepository, never()).saveAndFlush(any());
    }

    @Test
    void updateBranchStatusIsIdempotentWhenStatusDoesNotChange() {
        Branch branch = branch(UUID.randomUUID(), false);
        when(branchRepository.findByIdAndTenantId(branch.getId(), TENANT_ID)).thenReturn(Optional.of(branch));

        BranchResponse response = branchService.updateBranchStatus(branch.getId(), new UpdateBranchStatusRequest(false));

        assertFalse(response.active());
        verify(branchRepository, never()).saveAndFlush(any());
    }

    @Test
    void updateBranchStatusChangesStatus() {
        Branch branch = branch(UUID.randomUUID(), false);
        when(branchRepository.findByIdAndTenantId(branch.getId(), TENANT_ID)).thenReturn(Optional.of(branch));
        when(branchRepository.saveAndFlush(any(Branch.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BranchResponse response = branchService.updateBranchStatus(branch.getId(), new UpdateBranchStatusRequest(true));

        assertTrue(response.active());
        verify(branchRepository).saveAndFlush(any(Branch.class));
    }

    @Test
    void getBranchesSanitizesUnsupportedSortAndReturnsPageResponse() {
        Branch branch = branch(UUID.randomUUID(), true);
        when(branchRepository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(branch), PageRequest.of(0, 20), 1));

        PageResponse<BranchResponse> response = branchService.getBranches(
                " cbd ",
                true,
                PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "unsupported")));

        ArgumentCaptor<PageRequest> captor = ArgumentCaptor.forClass(PageRequest.class);
        verify(branchRepository).findAll(any(Specification.class), captor.capture());
        assertEquals("createdAt", captor.getValue().getSort().iterator().next().getProperty());
        assertEquals(1, response.getTotalElements());
        assertEquals(branch.getId(), response.getContent().get(0).id());
    }

    private Branch branch(UUID id, boolean active) {
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

    private static Tenant tenant() {
        Tenant tenant = new Tenant();
        tenant.setId(TENANT_ID);
        tenant.setName("Default Business");
        tenant.setCode("DEFAULT");
        tenant.setStatus(TenantStatus.ACTIVE);
        return tenant;
    }
}
