package com.procurement.compliance_engine.controller;

import com.procurement.compliance_engine.dto.EvidenceSearchResponse;
import com.procurement.compliance_engine.service.EvidenceRetrievalService;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.ai.document.Document;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@Profile("dev & ai")
@RequestMapping("/api/v1/bids/{bidId}/evidence")
public class EvidenceRetrievalController {

    private final EvidenceRetrievalService service;

    public EvidenceRetrievalController(EvidenceRetrievalService service) {
        this.service = service;
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','PROCUREMENT_OFFICER','EVALUATION_OFFICER','BIDDER')")
    public List<EvidenceSearchResponse> search(
            @PathVariable UUID bidId,
            @RequestParam String query
    ) {
        return service.searchEvidence(bidId, query)
                .stream()
                .map(EvidenceRetrievalController::toResponse)
                .toList();
    }

    private static EvidenceSearchResponse toResponse(Document document) {
        Map<String, Object> metadata = document.getMetadata();
        return new EvidenceSearchResponse(
                UUID.fromString(metadata.get("documentId").toString()),
                integerMetadata(metadata, "pageNumber"),
                integerMetadata(metadata, "chunkIndex"),
                document.getText(),
                document.getScore()
        );
    }

    private static int integerMetadata(Map<String, Object> metadata, String key) {
        Object value = metadata.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.parseInt(value.toString());
    }
}
