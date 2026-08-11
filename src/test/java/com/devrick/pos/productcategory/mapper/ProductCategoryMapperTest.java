package com.devrick.pos.productcategory.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.devrick.pos.productcategory.dto.CreateProductCategoryRequest;
import com.devrick.pos.productcategory.dto.ProductCategoryResponse;
import com.devrick.pos.productcategory.dto.UpdateProductCategoryRequest;
import com.devrick.pos.productcategory.entity.ProductCategory;
import com.devrick.pos.tenant.entity.Tenant;
import com.devrick.pos.tenant.entity.TenantStatus;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class ProductCategoryMapperTest {

    private final ProductCategoryMapper productCategoryMapper = Mappers.getMapper(ProductCategoryMapper.class);

    @Test
    void createRequestMapsEditableFieldsAndIgnoresTenantAndAuditData() {
        CreateProductCategoryRequest request = new CreateProductCategoryRequest(
                "beverages",
                "Beverages",
                "Soft drinks and juices");

        ProductCategory category = productCategoryMapper.toEntity(request);

        assertEquals("beverages", category.getCode());
        assertEquals("Beverages", category.getName());
        assertNull(category.getNameCi());
        assertEquals("Soft drinks and juices", category.getDescription());
        assertNull(category.getTenant());
        assertNull(category.getId());
        assertTrue(category.isActive());
    }

    @Test
    void responseMapsProductCategoryFields() {
        ProductCategory category = productCategory();

        ProductCategoryResponse response = productCategoryMapper.toResponse(category);

        assertEquals(category.getId(), response.id());
        assertEquals("BEVERAGES", response.code());
        assertEquals("Beverages", response.name());
        assertEquals("Soft drinks and juices", response.description());
        assertTrue(response.active());
        assertEquals(Instant.parse("2026-08-01T10:00:00Z"), response.createdAt());
    }

    @Test
    void updateRequestPreservesIdTenantCodeActiveAndAuditData() {
        ProductCategory category = productCategory();
        UUID originalId = category.getId();
        Tenant originalTenant = category.getTenant();
        String originalCode = category.getCode();
        boolean originalActive = category.isActive();
        Instant originalCreatedAt = category.getCreatedAt();

        productCategoryMapper.updateEntity(
                new UpdateProductCategoryRequest("Cleaning Supplies", "Warehouse and floor care"),
                category);

        assertEquals(originalId, category.getId());
        assertEquals(originalTenant, category.getTenant());
        assertEquals(originalCode, category.getCode());
        assertEquals(originalActive, category.isActive());
        assertEquals(originalCreatedAt, category.getCreatedAt());
        assertEquals("Cleaning Supplies", category.getName());
        assertEquals("beverages", category.getNameCi());
        assertEquals("Warehouse and floor care", category.getDescription());
    }

    private ProductCategory productCategory() {
        Tenant tenant = new Tenant();
        tenant.setId(UUID.fromString("55555555-5555-5555-5555-555555555555"));
        tenant.setName("Default Business");
        tenant.setCode("DEFAULT");
        tenant.setStatus(TenantStatus.ACTIVE);

        ProductCategory category = new ProductCategory();
        category.setId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
        category.setTenant(tenant);
        category.setCode("BEVERAGES");
        category.setName("Beverages");
        category.setNameCi("beverages");
        category.setDescription("Soft drinks and juices");
        category.setActive(true);
        category.setCreatedAt(Instant.parse("2026-08-01T10:00:00Z"));
        category.setUpdatedAt(Instant.parse("2026-08-01T11:00:00Z"));
        return category;
    }
}
