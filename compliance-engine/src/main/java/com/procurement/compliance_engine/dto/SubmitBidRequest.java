package com.procurement.compliance_engine.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record SubmitBidRequest(

        UUID bidderProfileId,

        @NotNull(message = "Quoted amount is required")
        @DecimalMin(
                value = "0.0",
                inclusive = false,
                message = "Quoted amount must be greater than zero"
        )
        @Digits(integer = 16, fraction = 2)
        BigDecimal quotedAmount
) {
}
