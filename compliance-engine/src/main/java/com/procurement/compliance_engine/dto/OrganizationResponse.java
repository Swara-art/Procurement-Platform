package com.procurement.compliance_engine.dto;

import com.procurement.compliance_engine.entity.Organization;

import java.time.OffsetDateTime;
import java.util.UUID;

public record OrganizationResponse(
        UUID id,
        String name,
        String organizationType,
        String registrationNumber,
        OffsetDateTime createdAt
) {
    public static OrganizationResponse from(Organization organization) {
        return new OrganizationResponse(
                organization.getId(),
                organization.getName(),
                organization.getOrganizationType(),
                organization.getRegistrationNumber(),
                organization.getCreatedAt()
        );
    }
}
