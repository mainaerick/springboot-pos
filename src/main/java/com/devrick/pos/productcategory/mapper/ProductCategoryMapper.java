package com.devrick.pos.productcategory.mapper;

import com.devrick.pos.productcategory.dto.CreateProductCategoryRequest;
import com.devrick.pos.productcategory.dto.ProductCategoryResponse;
import com.devrick.pos.productcategory.dto.UpdateProductCategoryRequest;
import com.devrick.pos.productcategory.entity.ProductCategory;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface ProductCategoryMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "tenant", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "nameCi", ignore = true)
    ProductCategory toEntity(CreateProductCategoryRequest request);

    ProductCategoryResponse toResponse(ProductCategory productCategory);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "tenant", ignore = true)
    @Mapping(target = "code", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "nameCi", ignore = true)
    void updateEntity(UpdateProductCategoryRequest request, @MappingTarget ProductCategory productCategory);
}
