package com.devrick.pos.productcategory.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import com.devrick.pos.productcategory.service.impl.ProductCategoryServiceImpl;
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
class ProductCategoryServiceImplTest {

    private static final UUID TENANT_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");

    @Mock
    private ProductCategoryRepository productCategoryRepository;

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private CurrentTenantProvider currentTenantProvider;

    private final ProductCategoryMapper productCategoryMapper = Mappers.getMapper(ProductCategoryMapper.class);
    private final Tenant tenant = tenant();

    private ProductCategoryServiceImpl productCategoryService;

    @BeforeEach
    void setUp() {
        Mockito.lenient().when(currentTenantProvider.getCurrentTenantId()).thenReturn(TENANT_ID);
        Mockito.lenient().when(tenantRepository.getReferenceById(TENANT_ID)).thenReturn(tenant);
        productCategoryService =
                new ProductCategoryServiceImpl(productCategoryRepository, productCategoryMapper, tenantRepository, currentTenantProvider);
    }

    @Test
    void createCategoryNormalizesFieldsAndUsesAuthenticatedTenant() {
        CreateProductCategoryRequest request = new CreateProductCategoryRequest(" beverages ", " Beverages ", " Soft drinks ");
        Instant now = Instant.parse("2026-08-01T10:00:00Z");

        when(productCategoryRepository.existsByTenantIdAndCodeIgnoreCase(TENANT_ID, "BEVERAGES")).thenReturn(false);
        when(productCategoryRepository.existsByTenantIdAndNameIgnoreCase(TENANT_ID, "Beverages")).thenReturn(false);
        when(productCategoryRepository.saveAndFlush(any(ProductCategory.class))).thenAnswer(invocation -> {
            ProductCategory category = invocation.getArgument(0);
            category.setId(UUID.randomUUID());
            category.setCreatedAt(now);
            category.setUpdatedAt(now);
            return category;
        });

        ProductCategoryResponse response = productCategoryService.createCategory(request);

        ArgumentCaptor<ProductCategory> captor = ArgumentCaptor.forClass(ProductCategory.class);
        verify(productCategoryRepository).saveAndFlush(captor.capture());
        assertEquals("BEVERAGES", captor.getValue().getCode());
        assertEquals("Beverages", captor.getValue().getName());
        assertEquals("Soft drinks", captor.getValue().getDescription());
        assertEquals(tenant, captor.getValue().getTenant());
        assertTrue(captor.getValue().isActive());
        assertEquals(now, response.createdAt());
    }

    @Test
    void createCategoryRejectsDuplicateCode() {
        when(productCategoryRepository.existsByTenantIdAndCodeIgnoreCase(TENANT_ID, "BEVERAGES")).thenReturn(true);

        assertThrows(DuplicateProductCategoryCodeException.class,
                () -> productCategoryService.createCategory(new CreateProductCategoryRequest("beverages", "Beverages", null)));

        verify(productCategoryRepository, never()).saveAndFlush(any());
    }

    @Test
    void createCategoryRejectsDuplicateName() {
        when(productCategoryRepository.existsByTenantIdAndCodeIgnoreCase(TENANT_ID, "BEVERAGES")).thenReturn(false);
        when(productCategoryRepository.existsByTenantIdAndNameIgnoreCase(TENANT_ID, "Beverages")).thenReturn(true);

        assertThrows(DuplicateProductCategoryNameException.class,
                () -> productCategoryService.createCategory(new CreateProductCategoryRequest("beverages", " Beverages ", null)));

        verify(productCategoryRepository, never()).saveAndFlush(any());
    }

    @Test
    void getCategoryReturnsTenantOwnedCategory() {
        ProductCategory category = category(UUID.randomUUID(), true);
        when(productCategoryRepository.findByIdAndTenantId(category.getId(), TENANT_ID)).thenReturn(Optional.of(category));

        ProductCategoryResponse response = productCategoryService.getCategory(category.getId());

        assertEquals(category.getId(), response.id());
        assertEquals(category.getCode(), response.code());
    }

    @Test
    void getCategoryThrowsWhenCategoryIsMissing() {
        UUID categoryId = UUID.randomUUID();
        when(productCategoryRepository.findByIdAndTenantId(categoryId, TENANT_ID)).thenReturn(Optional.empty());

        assertThrows(ProductCategoryNotFoundException.class, () -> productCategoryService.getCategory(categoryId));
    }

    @Test
    void getCategoryDoesNotCrossTenantBoundaries() {
        UUID categoryId = UUID.randomUUID();
        when(productCategoryRepository.findByIdAndTenantId(categoryId, TENANT_ID)).thenReturn(Optional.empty());

        assertThrows(ProductCategoryNotFoundException.class, () -> productCategoryService.getCategory(categoryId));
    }

    @Test
    void updateCategoryPreservesCodeTenantAndActiveStatus() {
        ProductCategory category = category(UUID.randomUUID(), false);
        when(productCategoryRepository.findByIdAndTenantId(category.getId(), TENANT_ID)).thenReturn(Optional.of(category));
        when(productCategoryRepository.existsByTenantIdAndNameIgnoreCaseAndIdNot(TENANT_ID, "Cleaning Supplies", category.getId()))
                .thenReturn(false);
        when(productCategoryRepository.saveAndFlush(any(ProductCategory.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductCategoryResponse response = productCategoryService.updateCategory(
                category.getId(),
                new UpdateProductCategoryRequest(" Cleaning Supplies ", " Warehouse and floor care "));

        assertEquals("Cleaning Supplies", response.name());
        assertEquals("BEVERAGES", response.code());
        assertFalse(response.active());
        assertEquals("Warehouse and floor care", response.description());
    }

    @Test
    void updateCategoryRejectsDuplicateName() {
        ProductCategory category = category(UUID.randomUUID(), true);
        when(productCategoryRepository.findByIdAndTenantId(category.getId(), TENANT_ID)).thenReturn(Optional.of(category));
        when(productCategoryRepository.existsByTenantIdAndNameIgnoreCaseAndIdNot(TENANT_ID, "Cleaning Supplies", category.getId()))
                .thenReturn(true);

        assertThrows(DuplicateProductCategoryNameException.class,
                () -> productCategoryService.updateCategory(
                        category.getId(), new UpdateProductCategoryRequest(" Cleaning Supplies ", null)));

        verify(productCategoryRepository, never()).saveAndFlush(any());
    }

    @Test
    void updateCategoryStatusIsIdempotentWhenStatusDoesNotChange() {
        ProductCategory category = category(UUID.randomUUID(), false);
        when(productCategoryRepository.findByIdAndTenantId(category.getId(), TENANT_ID)).thenReturn(Optional.of(category));

        ProductCategoryResponse response =
                productCategoryService.updateCategoryStatus(category.getId(), new UpdateProductCategoryStatusRequest(false));

        assertFalse(response.active());
        verify(productCategoryRepository, never()).saveAndFlush(any());
    }

    @Test
    void updateCategoryStatusDeactivatesAndReactivatesCategory() {
        ProductCategory category = category(UUID.randomUUID(), true);
        when(productCategoryRepository.findByIdAndTenantId(category.getId(), TENANT_ID)).thenReturn(Optional.of(category));
        when(productCategoryRepository.saveAndFlush(any(ProductCategory.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductCategoryResponse deactivated =
                productCategoryService.updateCategoryStatus(category.getId(), new UpdateProductCategoryStatusRequest(false));
        assertFalse(deactivated.active());
        assertFalse(category.isActive());

        ProductCategoryResponse reactivated =
                productCategoryService.updateCategoryStatus(category.getId(), new UpdateProductCategoryStatusRequest(true));
        assertTrue(reactivated.active());
        assertTrue(category.isActive());
        verify(productCategoryRepository, Mockito.times(2)).saveAndFlush(any(ProductCategory.class));
    }

    @Test
    void getCategoriesAppliesSearchActiveFilterAndSafeSorting() {
        ProductCategory first = category(UUID.randomUUID(), true);
        ProductCategory second = category(UUID.randomUUID(), false);
        when(productCategoryRepository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(first, second), PageRequest.of(0, 20), 2));

        PageResponse<ProductCategoryResponse> response = productCategoryService.getCategories(
                " bev ",
                true,
                PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "unsupported")));

        ArgumentCaptor<PageRequest> captor = ArgumentCaptor.forClass(PageRequest.class);
        verify(productCategoryRepository).findAll(any(Specification.class), captor.capture());
        assertEquals("createdAt", captor.getValue().getSort().iterator().next().getProperty());
        assertEquals(2, response.getTotalElements());
        assertEquals(first.getId(), response.getContent().get(0).id());
    }

    @Test
    void getCategoriesHandlesBlankSearchAndInactiveFilter() {
        ProductCategory category = category(UUID.randomUUID(), false);
        when(productCategoryRepository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(category)));

        PageResponse<ProductCategoryResponse> response =
                productCategoryService.getCategories("   ", false, PageRequest.of(0, 10));

        assertEquals(1, response.getTotalElements());
        assertFalse(response.getContent().get(0).active());
    }

    private ProductCategory category(UUID id, boolean active) {
        ProductCategory category = new ProductCategory();
        category.setId(id);
        category.setTenant(tenant);
        category.setCode("BEVERAGES");
        category.setName("Beverages");
        category.setDescription("Soft drinks");
        category.setActive(active);
        return category;
    }

    private Tenant tenant() {
        Tenant tenant = new Tenant();
        tenant.setId(TENANT_ID);
        tenant.setName("Default Business");
        tenant.setCode("DEFAULT");
        tenant.setStatus(TenantStatus.ACTIVE);
        return tenant;
    }
}
