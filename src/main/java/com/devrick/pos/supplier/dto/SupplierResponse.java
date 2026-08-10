package com.devrick.pos.supplier.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "Supplier details returned by the API.")
public record SupplierResponse(
        @Schema(description = "Unique supplier identifier") UUID id,
        @Schema(description = "Stable supplier code", example = "SUP-0001") String code,
        @Schema(description = "Supplier name", example = "Acme Supplies Ltd") String name,
        @Schema(description = "Primary contact person", example = "Mary Wanjiku") String contactPerson,
        @Schema(description = "Supplier email", example = "accounts@acme.com") String email,
        @Schema(description = "Primary phone number", example = "+254712345678") String phone,
        @Schema(description = "Alternate phone number", example = "+254700000000") String alternatePhone,
        @Schema(description = "Tax number", example = "P051234567X") String taxNumber,
        @Schema(description = "Primary address line", example = "Kimathi Street") String addressLine1,
        @Schema(description = "Secondary address line", example = "2nd Floor") String addressLine2,
        @Schema(description = "City", example = "Nairobi") String city,
        @Schema(description = "State or county", example = "Nairobi") String stateOrCounty,
        @Schema(description = "Postal code", example = "00100") String postalCode,
        @Schema(description = "Two-letter country code", example = "KE") String countryCode,
        @Schema(description = "Internal notes", example = "Preferred supplier for office stationery") String notes,
        @Schema(description = "Whether the supplier is active", example = "true") boolean active,
        @Schema(description = "Creation timestamp", format = "date-time") Instant createdAt,
        @Schema(description = "Last update timestamp", format = "date-time") Instant updatedAt) {}
