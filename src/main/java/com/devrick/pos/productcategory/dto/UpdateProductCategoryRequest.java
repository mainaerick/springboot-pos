package com.devrick.pos.productcategory.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Request body for updating editable product category fields.")
public record UpdateProductCategoryRequest(
        @Schema(description = "Category name", example = "Beverages")
                @NotBlank
                @Size(max = 120)
                String name,
        @Schema(description = "Optional category description", example = "Soft drinks and juices")
                @Size(max = 500)
                String description) {}
