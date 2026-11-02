package com.procurement.compliance_engine.dto;

import com.procurement.compliance_engine.entity.BidDocument;

import java.time.OffsetDateTime;
import java.util.UUID;

public record BidDocumentResponse(
        UUID id,
        UUID bidId,
        String originalFilename,
        Long fileSize,
        String contentType,
        OffsetDateTime uploadedAt
) {
    public static BidDocumentResponse from(BidDocument document) {
        return new BidDocumentResponse(
                document.getId(),
                document.getBid().getId(),
                document.getOriginalFilename(),
                document.getFileSize(),
                document.getContentType(),
                document.getUploadedAt()
        );
    }
}
