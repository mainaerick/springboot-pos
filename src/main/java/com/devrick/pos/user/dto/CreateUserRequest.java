package com.devrick.pos.user.dto;

import com.devrick.pos.common.enums.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
        @Schema(description = "First name", example = "Jane") @NotBlank @Size(min = 2, max = 100) String firstName,
        @Schema(description = "Last name", example = "Doe") @NotBlank @Size(min = 2, max = 100) String lastName,
        @Schema(description = "Employee email address", example = "jane.doe@example.com")
                @NotBlank
                @Email
                @Size(max = 255)
                String email,
        @Schema(
                        description = "Initial password",
                        example = "Password123!",
                        format = "password",
                        accessMode = Schema.AccessMode.WRITE_ONLY)
                @NotBlank
                @Size(min = 8, max = 100)
                String password,
        @Schema(description = "Assigned user role", example = "MANAGER") Role role) {}
