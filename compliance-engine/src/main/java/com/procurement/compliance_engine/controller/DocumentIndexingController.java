package com.procurement.compliance_engine.controller;

import com.procurement.compliance_engine.service.DocumentIndexingService;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@Profile("dev")
@RequestMapping("/api/v1/documents")
public class DocumentIndexingController {

    private final DocumentIndexingService indexingService;

    public DocumentIndexingController(DocumentIndexingService indexingService) {
        this.indexingService = indexingService;
    }

    @PostMapping("/{documentId}/index")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','PROCUREMENT_OFFICER','EVALUATION_OFFICER')")
    public Map<String, Object> index(@PathVariable UUID documentId) throws IOException {
        int chunkCount = indexingService.indexDocument(documentId);
        return Map.of(
                "documentId", documentId,
                "chunksIndexed", chunkCount,
                "status", "INDEXED"
        );
    }
}
