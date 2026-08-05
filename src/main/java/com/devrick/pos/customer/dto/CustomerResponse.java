package com.devrick.pos.customer.dto;

import com.devrick.pos.customer.entity.CustomerType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "Customer details returned by the API.")
public record CustomerResponse(
        @Schema(description = "Unique customer identifier") UUID id,
        @Schema(description = "Customer type", example = "INDIVIDUAL") CustomerType type,
        @Schema(description = "Stable customer code", example = "CUS-0001") String code,
        @Schema(description = "Customer name", example = "Jane Wanjiku") String name,
        @Schema(description = "Customer email", example = "jane@example.com") String email,
        @Schema(description = "Customer phone", example = "+254712345678") String phone,
        @Schema(description = "Tax number", example = "P051234567X") String taxNumber,
        @Schema(description = "Primary address line", example = "Kimathi Street") String addressLine1,
        @Schema(description = "Secondary address line", example = "2nd Floor") String addressLine2,
        @Schema(description = "City", example = "Nairobi") String city,
        @Schema(description = "State or county", example = "Nairobi") String stateOrCounty,
        @Schema(description = "Postal code", example = "00100") String postalCode,
        @Schema(description = "Two-letter country code", example = "KE") String countryCode,
        @Schema(description = "Internal notes", example = "Prefers SMS communication") String notes,
        @Schema(description = "Whether the customer is active", example = "true") boolean active,
        @Schema(description = "Creation timestamp", format = "date-time") Instant createdAt,
        @Schema(description = "Last update timestamp", format = "date-time") Instant updatedAt) {}
