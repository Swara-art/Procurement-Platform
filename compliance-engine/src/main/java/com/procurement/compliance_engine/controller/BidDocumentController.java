package com.procurement.compliance_engine.controller;

import com.procurement.compliance_engine.dto.BidDocumentResponse;
import com.procurement.compliance_engine.service.BidDocumentService;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@Profile("dev")
@RequestMapping("/api/v1/bids")
public class BidDocumentController {

    private final BidDocumentService documentService;

    public BidDocumentController(BidDocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping(value = "/{bidId}/documents", consumes = "multipart/form-data")
    @ResponseStatus(HttpStatus.CREATED)
    public BidDocumentResponse uploadDocument(
            @PathVariable UUID bidId,
            @RequestParam("file") MultipartFile file
    ) {
        return documentService.uploadDocument(bidId, file);
    }
}
