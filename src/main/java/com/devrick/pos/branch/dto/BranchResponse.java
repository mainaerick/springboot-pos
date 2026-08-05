package com.devrick.pos.branch.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "Branch details returned by the API.")
public record BranchResponse(
        @Schema(description = "Unique branch identifier") UUID id,
        @Schema(description = "Branch name", example = "Nairobi CBD") String name,
        @Schema(description = "Stable branch code", example = "NRB-CBD") String code,
        @Schema(description = "Branch email", example = "nairobi@example.com") String email,
        @Schema(description = "Branch phone number", example = "+254712345678") String phone,
        @Schema(description = "Primary address line", example = "Kimathi Street") String addressLine1,
        @Schema(description = "Secondary address line", example = "2nd Floor") String addressLine2,
        @Schema(description = "City", example = "Nairobi") String city,
        @Schema(description = "State or county", example = "Nairobi") String stateOrCounty,
        @Schema(description = "Postal code", example = "00100") String postalCode,
        @Schema(description = "Two-letter country code", example = "KE") String countryCode,
        @Schema(description = "Whether the branch is active", example = "true") boolean active,
        @Schema(description = "Creation timestamp", format = "date-time") Instant createdAt,
        @Schema(description = "Last update timestamp", format = "date-time") Instant updatedAt) {}
