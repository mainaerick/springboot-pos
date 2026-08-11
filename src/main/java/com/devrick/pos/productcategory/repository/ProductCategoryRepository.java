package com.devrick.pos.productcategory.repository;

import com.devrick.pos.productcategory.entity.ProductCategory;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ProductCategoryRepository
        extends JpaRepository<ProductCategory, UUID>, JpaSpecificationExecutor<ProductCategory> {

    Optional<ProductCategory> findByIdAndTenantId(UUID categoryId, UUID tenantId);

    boolean existsByTenantIdAndCodeIgnoreCase(UUID tenantId, String code);

    boolean existsByTenantIdAndNameIgnoreCase(UUID tenantId, String name);

    boolean existsByTenantIdAndNameIgnoreCaseAndIdNot(UUID tenantId, String name, UUID categoryId);
}
