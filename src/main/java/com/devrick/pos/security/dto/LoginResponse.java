package com.devrick.pos.security.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Tokens returned after a successful login.")
public record LoginResponse(
        @Schema(description = "JWT access token") String accessToken,
        @Schema(description = "JWT refresh token") String refreshToken,
        @Schema(description = "Token type", example = "Bearer") String tokenType,
        @Schema(description = "Access token lifetime in seconds", example = "900") long expiresIn,
        @Schema(description = "Whether the user must change their password after login", example = "false")
                boolean mustChangePassword) {}
