package com.procurement.compliance_engine.dto;

import com.procurement.compliance_engine.entity.TenderRequirement;

import java.time.OffsetDateTime;
import java.util.UUID;

public record TenderRequirementResponse(
        UUID id,
        UUID tenderId,
        String requirementCode,
        String description,
        String operator,
        String expectedValue,
        Boolean mandatory,
        OffsetDateTime createdAt
) {
    public static TenderRequirementResponse from(TenderRequirement requirement) {
        return new TenderRequirementResponse(
                requirement.getId(),
                requirement.getTender().getId(),
                requirement.getRequirementCode(),
                requirement.getDescription(),
                requirement.getOperator(),
                requirement.getExpectedValue(),
                requirement.getMandatory(),
                requirement.getCreatedAt()
        );
    }
}
