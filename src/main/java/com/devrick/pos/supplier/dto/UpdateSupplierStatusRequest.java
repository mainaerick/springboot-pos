package com.devrick.pos.supplier.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Request body for changing a supplier active status.")
public record UpdateSupplierStatusRequest(
        @Schema(description = "Whether the supplier is active", example = "false") @NotNull Boolean active) {}
