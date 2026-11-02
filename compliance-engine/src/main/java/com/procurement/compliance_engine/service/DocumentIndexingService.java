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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Profile("ai")
public class DocumentIndexingService {

    private static final Logger log = LoggerFactory.getLogger(DocumentIndexingService.class);

    private final BidDocumentRepository documentRepository;
    private final PdfChunkingService pdfChunkingService;
    private final VectorStore vectorStore;
    private final Path uploadRoot;

    public DocumentIndexingService(
            BidDocumentRepository documentRepository,
            PdfChunkingService pdfChunkingService,
            VectorStore vectorStore,
            @Value("${app.storage.upload-dir:./private-uploads}") String uploadDirectory
    ) {
        this.documentRepository = documentRepository;
        this.pdfChunkingService = pdfChunkingService;
        this.vectorStore = vectorStore;
        this.uploadRoot = Path.of(uploadDirectory).toAbsolutePath().normalize();
    }

    @Transactional
    public int indexDocument(UUID documentId) throws IOException {
        String stage = "loading document metadata";
        log.info("Starting indexing for document {}", documentId);
        try {
            BidDocument bidDocument = documentRepository.findById(documentId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));
            log.info("Document found; storage key={}", bidDocument.getStorageKey());

            stage = "resolving stored PDF";
            Path filePath = uploadRoot.resolve(bidDocument.getStorageKey()).normalize();
            if (!filePath.startsWith(uploadRoot)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid storage path");
            }
            boolean pdfExists = Files.isRegularFile(filePath);
            log.info("Resolved PDF path; file exists={}", pdfExists);
            if (!pdfExists) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Stored PDF not found");
            }

            stage = "verifying stored PDF hash";
            byte[] fileBytes = Files.readAllBytes(filePath);
            if (!sha256(fileBytes).equalsIgnoreCase(bidDocument.getSha256Hash())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Stored PDF hash does not match its metadata");
            }
            log.info("Stored PDF hash verified for document {}", documentId);

            stage = "extracting PDF text and creating chunks";
            List<PdfChunkingService.PdfChunk> chunks = pdfChunkingService.extractAndChunk(filePath);
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
