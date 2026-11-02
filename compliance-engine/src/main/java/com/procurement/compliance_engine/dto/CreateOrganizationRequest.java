package com.procurement.compliance_engine.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateOrganizationRequest(

        @NotBlank(message = "Organization name is required")
        @Size(max = 255)
        String name,

        @NotBlank(message = "Organization type is required")
        @Pattern(
                regexp = "BIDDER|PROCUREMENT_AUTHORITY",
                message = "Type must be BIDDER or PROCUREMENT_AUTHORITY"
        )
        String organizationType,

        @NotBlank(message = "Registration number is required")
        @Size(max = 255)
        String registrationNumber
) {
}
