package com.devrick.pos.productcategory.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Request body for changing a product category active status.")
public record UpdateProductCategoryStatusRequest(
        @Schema(description = "Whether the category is active", example = "false") @NotNull Boolean active) {}
