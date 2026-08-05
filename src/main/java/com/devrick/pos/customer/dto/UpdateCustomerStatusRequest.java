package com.devrick.pos.customer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Request body for changing a customer active status.")
public record UpdateCustomerStatusRequest(
        @Schema(description = "Whether the customer is active", example = "false") @NotNull Boolean active) {}
