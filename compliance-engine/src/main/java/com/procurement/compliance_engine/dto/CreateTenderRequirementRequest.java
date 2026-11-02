package com.procurement.compliance_engine.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateTenderRequirementRequest(

        @NotBlank(message = "Requirement code is required")
        @Size(max = 100)
        String requirementCode,

        @NotBlank(message = "Description is required")
        String description,

        @NotBlank(message = "Operator is required")
        @Size(max = 30)
        @Pattern(
                regexp = "EQUALS|GREATER_THAN|GREATER_THAN_OR_EQUAL|LESS_THAN|LESS_THAN_OR_EQUAL|EXISTS|IS_VALID",
                message = "Operator must be one of the supported comparison operators"
        )
        String operator,

        String expectedValue,

        @NotNull(message = "Mandatory flag is required")
        Boolean mandatory
) {
}
