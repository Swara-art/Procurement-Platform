package com.procurement.compliance_engine.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record TurnoverEvaluationResponse(
        UUID bidId,
        String requirementCode,
        BigDecimal requiredAmount,
        BigDecimal declaredAmount,
        Boolean numericConditionMet,
        String status,
        String explanation,
        UUID sourceDocumentId,
        Integer sourcePage
) {
}
