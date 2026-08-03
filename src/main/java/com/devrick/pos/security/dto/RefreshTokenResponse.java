package com.devrick.pos.security.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Access token returned after refreshing a valid refresh token.")
public record RefreshTokenResponse(
        @Schema(description = "New JWT access token") String accessToken,
        @Schema(description = "Access token lifetime in seconds", example = "900") long expiresIn) {}
