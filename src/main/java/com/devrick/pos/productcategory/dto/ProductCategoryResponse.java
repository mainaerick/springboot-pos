package com.devrick.pos.productcategory.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "Product category details returned by the API.")
public record ProductCategoryResponse(
        @Schema(description = "Unique category identifier") UUID id,
        @Schema(description = "Stable category code", example = "BEVERAGES") String code,
        @Schema(description = "Category name", example = "Beverages") String name,
        @Schema(description = "Optional category description", example = "Soft drinks and juices") String description,
        @Schema(description = "Whether the category is active", example = "true") boolean active,
        @Schema(description = "Creation timestamp", format = "date-time") Instant createdAt,
        @Schema(description = "Last update timestamp", format = "date-time") Instant updatedAt) {}
