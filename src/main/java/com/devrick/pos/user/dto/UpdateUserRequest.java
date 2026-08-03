package com.devrick.pos.user.dto;

import com.devrick.pos.common.enums.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
        @Schema(description = "First name", example = "Jane") @NotBlank @Size(min = 2, max = 100) String firstName,
        @Schema(description = "Last name", example = "Doe") @NotBlank @Size(min = 2, max = 100) String lastName,
        @Schema(description = "Employee email address", example = "jane.doe@example.com")
                @NotBlank
                @Email
                @Size(max = 255)
                String email,
        @Schema(description = "Whether the user account is enabled", example = "true") @NotNull Boolean enabled,
        @Schema(description = "Assigned user role", example = "ACCOUNTANT") Role role) {}
