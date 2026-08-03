package com.devrick.pos.security.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @Schema(description = "User email address", example = "cashier@example.com") @NotBlank @Email @Size(max = 255)
                String email,
        @Schema(
                        description = "User password",
                        example = "Password123!",
                        format = "password",
                        accessMode = Schema.AccessMode.WRITE_ONLY)
                @NotBlank
                String password) {}
