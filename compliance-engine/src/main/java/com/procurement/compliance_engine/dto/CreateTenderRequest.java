package com.procurement.compliance_engine.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record CreateTenderRequest(

        @NotBlank(message = "Tender number is required")
        @Size(max = 100)
        String tenderNumber,

        @NotBlank(message = "Tender title is required")
        @Size(max = 255)
        String title,

        String description,

        @NotNull(message = "Procurement authority ID is required")
        UUID procurementAuthorityId,

        @DecimalMin(value = "0.00", message = "Estimated value cannot be negative")
        @Digits(integer = 16, fraction = 2, message = "Estimated value must have up to 16 integer digits and 2 decimal places")
        BigDecimal estimatedValue,

        @NotNull(message = "Submission deadline is required")
        @Future(message = "Deadline must be in the future")
        OffsetDateTime submissionDeadline
) {
}
