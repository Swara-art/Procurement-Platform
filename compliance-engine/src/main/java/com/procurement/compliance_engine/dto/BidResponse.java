package com.procurement.compliance_engine.dto;

import com.procurement.compliance_engine.entity.Bid;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record BidResponse(
        UUID id,
        UUID tenderId,
        UUID bidderProfileId,
        BigDecimal quotedAmount,
        String status,
        OffsetDateTime submittedAt
) {
    public static BidResponse from(Bid bid) {
        return new BidResponse(
                bid.getId(),
                bid.getTender().getId(),
                bid.getBidder().getId(),
                bid.getQuotedAmount(),
                bid.getStatus(),
                bid.getSubmittedAt()
        );
    }
}
