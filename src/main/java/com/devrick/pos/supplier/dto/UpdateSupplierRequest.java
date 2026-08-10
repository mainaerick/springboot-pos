package com.devrick.pos.supplier.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Request body for updating editable supplier fields.")
public record UpdateSupplierRequest(
        @Schema(description = "Supplier name", example = "Acme Supplies Ltd")
                @NotBlank
                @Size(max = 150)
                String name,
        @Schema(description = "Primary contact person", example = "Mary Wanjiku") @Size(max = 150) String contactPerson,
        @Schema(description = "Supplier email", example = "accounts@acme.com") @Email @Size(max = 254) String email,
        @Schema(description = "Primary phone number", example = "+254712345678") @Size(max = 30) String phone,
        @Schema(description = "Alternate phone number", example = "+254700000000") @Size(max = 30) String alternatePhone,
        @Schema(description = "Tax number", example = "P051234567X") @Size(max = 50) String taxNumber,
        @Schema(description = "Primary address line", example = "Kimathi Street") @Size(max = 200) String addressLine1,
        @Schema(description = "Secondary address line", example = "2nd Floor") @Size(max = 200) String addressLine2,
        @Schema(description = "City", example = "Nairobi") @Size(max = 100) String city,
        @Schema(description = "State or county", example = "Nairobi") @Size(max = 100) String stateOrCounty,
        @Schema(description = "Postal code", example = "00100") @Size(max = 30) String postalCode,
        @Schema(description = "Two-letter country code", example = "ke")
                @Size(min = 2, max = 2)
                @Pattern(regexp = "^[A-Za-z]{2}$")
                String countryCode,
        @Schema(description = "Internal notes", example = "Preferred supplier for office stationery")
                @Size(max = 1000)
                String notes) {}
