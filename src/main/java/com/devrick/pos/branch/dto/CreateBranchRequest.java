package com.devrick.pos.branch.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Request body for creating a branch.")
public record CreateBranchRequest(
        @Schema(description = "Branch name", example = "Nairobi CBD")
                @NotBlank
                @Size(max = 120)
                String name,
        @Schema(description = "Stable branch code", example = "NRB-CBD")
                @NotBlank
                @Size(min = 2, max = 30)
                @Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9_-]*$")
                String code,
        @Schema(description = "Branch email", example = "nairobi@example.com")
                @Email
                @Size(max = 254)
                String email,
        @Schema(description = "Branch phone number", example = "+254712345678") @Size(max = 30) String phone,
        @Schema(description = "Primary address line", example = "Kimathi Street") @Size(max = 200) String addressLine1,
        @Schema(description = "Secondary address line", example = "2nd Floor") @Size(max = 200) String addressLine2,
        @Schema(description = "City", example = "Nairobi") @Size(max = 100) String city,
        @Schema(description = "State or county", example = "Nairobi") @Size(max = 100) String stateOrCounty,
        @Schema(description = "Postal code", example = "00100") @Size(max = 30) String postalCode,
        @Schema(description = "Two-letter country code", example = "KE")
                @Size(min = 2, max = 2)
                @Pattern(regexp = "^[A-Za-z]{2}$")
                String countryCode) {}
