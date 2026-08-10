package com.devrick.pos.customer.dto;

import com.devrick.pos.customer.entity.CustomerType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Request body for updating a customer.")
public record UpdateCustomerRequest(
        @Schema(description = "Customer type", example = "BUSINESS") @NotNull CustomerType type,
        @Schema(description = "Customer name", example = "Sunrise Hotel Ltd")
                @NotBlank
                @Size(max = 150)
                String name,
        @Schema(description = "Customer email", example = "accounts@example.com")
                @Email
                @Size(max = 254)
                String email,
        @Schema(description = "Customer phone", example = "+254712345678") @Size(max = 30) String phone,
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
        @Schema(description = "Internal notes", example = "Main account holder")
                @Size(max = 1000)
                String notes) {}
