package com.devrick.pos.branch.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Request body for changing a branch active status.")
public record UpdateBranchStatusRequest(
        @Schema(description = "Whether the branch is active", example = "false") @NotNull Boolean active) {}
