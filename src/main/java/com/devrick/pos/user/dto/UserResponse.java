package com.devrick.pos.user.dto;

import com.devrick.pos.common.enums.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "User details returned by the API.")
public record UserResponse(
        @Schema(description = "Unique user identifier") UUID id,
        @Schema(description = "First name", example = "Jane") String firstName,
        @Schema(description = "Last name", example = "Doe") String lastName,
        @Schema(description = "Email address", example = "jane.doe@example.com") String email,
        @Schema(description = "User role", example = "ADMIN") Role role,
        @Schema(description = "Whether the user is enabled", example = "true") boolean enabled,
        @Schema(description = "Creation timestamp", format = "date-time") Instant createdAt,
        @Schema(description = "Last update timestamp", format = "date-time") Instant updatedAt) {}
