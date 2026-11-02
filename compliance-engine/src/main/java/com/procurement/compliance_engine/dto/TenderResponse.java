package com.procurement.compliance_engine.dto;

import com.procurement.compliance_engine.entity.Tender;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record TenderResponse(
        UUID id,
        String tenderNumber,
        String title,
        String description,
        UUID procurementAuthorityId,
        BigDecimal estimatedValue,
        String status,
        OffsetDateTime submissionDeadline,
        OffsetDateTime createdAt
) {
    public static TenderResponse from(Tender tender) {
        return new TenderResponse(
                tender.getId(),
                tender.getTenderNumber(),
                tender.getTitle(),
                tender.getDescription(),
                tender.getAuthority().getId(),
                tender.getEstimatedValue(),
                tender.getStatus(),
                tender.getSubmissionDeadline(),
                tender.getCreatedAt()
        );
    }
}
