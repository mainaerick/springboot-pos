package com.devrick.pos.productcategory.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Request body for creating a product category.")
public record CreateProductCategoryRequest(
        @Schema(description = "Stable category code", example = "BEVERAGES")
                @NotBlank
                @Size(min = 2, max = 40)
                @Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9_-]*$")
                String code,
        @Schema(description = "Category name", example = "Beverages")
                @NotBlank
                @Size(max = 120)
                String name,
        @Schema(description = "Optional category description", example = "Soft drinks and juices")
                @Size(max = 500)
                String description) {}
