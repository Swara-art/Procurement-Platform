package com.procurement.compliance_engine.service;

import com.procurement.compliance_engine.entity.BidDocument;
import com.procurement.compliance_engine.repository.BidDocumentRepository;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.procurement.compliance_engine.security.CurrentUser;

@Service
@Profile("ai")
public class DocumentIndexingService {

    private static final Logger log = LoggerFactory.getLogger(DocumentIndexingService.class);

    private final BidDocumentRepository documentRepository;
    private final PdfChunkingService pdfChunkingService;
    private final VectorStore vectorStore;
    private final DocumentStorageService storage;

    public DocumentIndexingService(
            BidDocumentRepository documentRepository,
            PdfChunkingService pdfChunkingService,
            VectorStore vectorStore,
            DocumentStorageService storage
    ) {
        this.documentRepository = documentRepository;
        this.pdfChunkingService = pdfChunkingService;
        this.vectorStore = vectorStore;
        this.storage = storage;
    }

    @Transactional
    public int indexDocument(UUID documentId) throws IOException {
        String stage = "loading document metadata";
        log.info("Starting indexing for document {}", documentId);
        try {
            var actor=CurrentUser.get();
            if(actor==null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Authentication required");
            var scopedDocument="SYSTEM_ADMIN".equals(actor.role())?documentRepository.findById(documentId)
                    : ("PROCUREMENT_OFFICER".equals(actor.role())||"EVALUATION_OFFICER".equals(actor.role()))?documentRepository.findByIdAndAuthorityAfterDeadline(documentId,actor.organizationId())
                    : java.util.Optional.<BidDocument>empty();
            BidDocument bidDocument = scopedDocument
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));
            if(!"AVAILABLE".equals(bidDocument.getProcessingStatus())) throw new ResponseStatusException(HttpStatus.LOCKED,"Document has not cleared malware scanning");
            log.info("Document found; storage key={}", bidDocument.getStorageKey());

            stage = "resolving stored PDF";
            stage = "verifying stored PDF hash";
            byte[] fileBytes = storage.get(bidDocument.getStorageKey());
            if (!sha256(fileBytes).equalsIgnoreCase(bidDocument.getSha256Hash())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Stored PDF hash does not match its metadata");
            }
            log.info("Stored PDF hash verified for document {}", documentId);

            stage = "extracting PDF text and creating chunks";
            Path filePath=Files.createTempFile("procurement-index-", ".pdf");
            List<PdfChunkingService.PdfChunk> chunks;
            try { Files.write(filePath,fileBytes); chunks=pdfChunkingService.extractAndChunk(filePath); }
            finally { Files.deleteIfExists(filePath); }
            log.info("PDF extraction completed for document {}; chunks={}", documentId, chunks.size());
            if (chunks.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "No extractable text found in PDF");
            }

            UUID bidId = bidDocument.getBid().getId();
            List<Document> vectorDocuments = new ArrayList<>(chunks.size());
            for (PdfChunkingService.PdfChunk chunk : chunks) {
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("documentId", documentId.toString());
                metadata.put("bidId", bidId.toString());
                metadata.put("organizationId",bidDocument.getBid().getBidder().getOrganization().getId().toString());
                metadata.put("documentType",bidDocument.getDocumentType());
                metadata.put("extractionVersion","pdf-chunk-v1");
                metadata.put("pageNumber", chunk.pageNumber());
                metadata.put("chunkIndex", chunk.chunkIndex());
                metadata.put("sha256", bidDocument.getSha256Hash());

                String chunkKey = documentId + ":" + chunk.chunkIndex();
                String stableId = UUID.nameUUIDFromBytes(
                        chunkKey.getBytes(StandardCharsets.UTF_8)
                ).toString();

                Document document = Document.builder()
                        .id(stableId)
                        .text(chunk.content())
                        .metadata(metadata)
                        .build();
                vectorDocuments.add(document);
            }

            stage = "generating embeddings and storing vectors";
            log.info("Sending {} chunks to VectorStore for document {}", vectorDocuments.size(), documentId);
            vectorStore.add(vectorDocuments);
            log.info("VectorStore indexing completed for document {}", documentId);
            return vectorDocuments.size();
        } catch (IOException | RuntimeException ex) {
            log.error("Indexing failed for document {} while {}", documentId, stage, ex);
            throw ex;
        }
    }

    private static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is unavailable", ex);
        }
    }
}
