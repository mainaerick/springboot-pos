package com.devrick.pos.security.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Request body for refreshing an access token.")
public record RefreshTokenRequest(
        @Schema(
                        description = "Valid refresh token issued during login",
                        format = "password",
                        accessMode = Schema.AccessMode.WRITE_ONLY)
                @NotBlank
                String refreshToken) {}
