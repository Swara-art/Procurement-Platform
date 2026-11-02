package com.procurement.compliance_engine.dto;

import java.util.UUID;

public record BidderProfileResponse(
        UUID id,
        UUID organizationId
) {
}
