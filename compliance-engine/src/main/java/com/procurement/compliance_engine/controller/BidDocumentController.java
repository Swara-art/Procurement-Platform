package com.procurement.compliance_engine.controller;

import com.procurement.compliance_engine.dto.BidDocumentResponse;
import com.procurement.compliance_engine.service.BidDocumentService;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@Profile("dev")
@RequestMapping("/api/v1/bids")
public class BidDocumentController {

    private final BidDocumentService documentService;

    public BidDocumentController(BidDocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping(value = "/{bidId}/documents", consumes = "multipart/form-data")
    @PreAuthorize("hasRole('BIDDER')")
    @ResponseStatus(HttpStatus.CREATED)
    public BidDocumentResponse uploadDocument(
            @PathVariable UUID bidId,
            @RequestParam("file") MultipartFile file
    ) {
        return documentService.uploadDocument(bidId, file);
    }

    @PostMapping(value="/{bidId}/documents/{category}",consumes="multipart/form-data")
    @PreAuthorize("hasRole('BIDDER')")
    @ResponseStatus(HttpStatus.CREATED)
    public BidDocumentResponse uploadCategorized(@PathVariable UUID bidId,@PathVariable String category,@RequestParam("file") MultipartFile file){return documentService.uploadDocument(bidId,category,file);}

    @GetMapping("/{bidId}/documents")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','PROCUREMENT_OFFICER','EVALUATION_OFFICER','BIDDER')")
    public java.util.List<BidDocumentResponse> getDocuments(@PathVariable UUID bidId){return documentService.getDocuments(bidId);}

    @GetMapping("/{bidId}/documents/{documentId}/content")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','PROCUREMENT_OFFICER','EVALUATION_OFFICER','BIDDER')")
    public ResponseEntity<byte[]> download(@PathVariable UUID bidId,@PathVariable UUID documentId){byte[] body=documentService.download(bidId,documentId);return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF).header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=\"document.pdf\"").body(body);}
}
