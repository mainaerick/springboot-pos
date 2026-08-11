package com.devrick.pos.productcategory.service;

import com.devrick.pos.common.dto.PageResponse;
import com.devrick.pos.productcategory.dto.CreateProductCategoryRequest;
import com.devrick.pos.productcategory.dto.ProductCategoryResponse;
import com.devrick.pos.productcategory.dto.UpdateProductCategoryRequest;
import com.devrick.pos.productcategory.dto.UpdateProductCategoryStatusRequest;
import java.util.UUID;
import org.springframework.data.domain.Pageable;

public interface ProductCategoryService {

    ProductCategoryResponse createCategory(CreateProductCategoryRequest request);

    PageResponse<ProductCategoryResponse> getCategories(String search, Boolean active, Pageable pageable);

    ProductCategoryResponse getCategory(UUID categoryId);

    ProductCategoryResponse updateCategory(UUID categoryId, UpdateProductCategoryRequest request);

    ProductCategoryResponse updateCategoryStatus(UUID categoryId, UpdateProductCategoryStatusRequest request);
}
