package com.procurement.compliance_engine.dto;

import java.util.UUID;

public record EvidenceSearchResponse(
        UUID documentId,
        int pageNumber,
        int chunkIndex,
        String content,
        Double similarityScore
) {
}
