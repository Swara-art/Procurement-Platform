package com.procurement.compliance_engine.service;

import com.procurement.compliance_engine.dto.BidDocumentResponse;
import com.procurement.compliance_engine.entity.Bid;
import com.procurement.compliance_engine.entity.BidDocument;
import com.procurement.compliance_engine.repository.BidDocumentRepository;
import com.procurement.compliance_engine.repository.BidRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;
import com.procurement.compliance_engine.security.CurrentUser;

@Service
@Profile("dev")
public class BidDocumentService {

    private static final String PDF_CONTENT_TYPE = "application/pdf";
    private static final int PDF_HEADER_SCAN_BYTES = 1024;

    private final BidDocumentRepository documentRepository;
    private final BidRepository bidRepository;
    private final Path storageRoot;
    private final long maxFileSizeBytes;

    public BidDocumentService(
            BidDocumentRepository documentRepository,
            BidRepository bidRepository,
            @Value("${app.storage.upload-dir:./private-uploads}") String uploadDirectory,
            @Value("${app.storage.max-file-size-bytes:10485760}") long maxFileSizeBytes
    ) {
        this.documentRepository = documentRepository;
        this.bidRepository = bidRepository;
        this.storageRoot = Path.of(uploadDirectory).toAbsolutePath().normalize();
        this.maxFileSizeBytes = maxFileSizeBytes;
    }

    @Transactional
    public BidDocumentResponse uploadDocument(UUID bidId, MultipartFile file) {
        var actor=CurrentUser.get();
        if(actor==null || !"BIDDER".equals(actor.role())) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Bidder account required");
        Bid bid = bidRepository.findByIdAndBidderOrganization(bidId, actor.organizationId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bid not found"));

        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A non-empty PDF file is required");
        }
        if (file.getSize() > maxFileSizeBytes) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "PDF exceeds the 10 MB upload limit");
        }

        String originalFilename = safeOriginalFilename(file.getOriginalFilename());
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unable to read uploaded file", ex);
        }
        if (bytes.length == 0 || bytes.length > maxFileSizeBytes) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "PDF exceeds the 10 MB upload limit");
        }
        if (!hasPdfSignature(bytes)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Uploaded file is not a PDF");
        }

        String storageKey = UUID.randomUUID() + ".pdf";
        Path storedFile = storageRoot.resolve(storageKey).normalize();
        if (!storedFile.startsWith(storageRoot)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid storage path");
        }

        boolean metadataSaved = false;
        try {
            Files.createDirectories(storageRoot);
            Files.write(storedFile, bytes, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);

            BidDocument document = new BidDocument();
            document.setBid(bid);
            document.setOriginalFilename(originalFilename);
            document.setContentType(PDF_CONTENT_TYPE);
            document.setFileSize((long) bytes.length);
            document.setStorageKey(storageKey);
            document.setSha256Hash(sha256(bytes));

            BidDocument saved = documentRepository.saveAndFlush(document);
            if (TransactionSynchronizationManager.isSynchronizationActive()) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCompletion(int status) {
                        if (status != STATUS_COMMITTED) {
                            try {
                                Files.deleteIfExists(storedFile);
                            } catch (IOException ignored) {
                                // Keep the transaction failure; a later cleanup job can remove orphaned files.
                            }
                        }
                    }
                });
            }
            metadataSaved = true;
            return BidDocumentResponse.from(saved);
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Document metadata violates a database constraint", ex);
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to store uploaded PDF", ex);
        } finally {
            if (!metadataSaved) {
                try {
                    Files.deleteIfExists(storedFile);
                } catch (IOException ignored) {
                    // Keep the original failure; a later cleanup job can remove orphaned files.
                }
            }
        }
    }

    @Transactional(readOnly = true)
    public java.util.List<BidDocumentResponse> getDocuments(UUID bidId) {
        var actor=CurrentUser.get();
        if(actor==null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Authentication required");
        java.util.List<BidDocument> documents;
        if("SYSTEM_ADMIN".equals(actor.role())) documents=documentRepository.findByBidId(bidId);
        else if("BIDDER".equals(actor.role())) {
            if(!bidRepository.existsByIdAndBidderOrganization(bidId,actor.organizationId())) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Bid not found");
            documents=documentRepository.findByBidAndBidderOrganization(bidId,actor.organizationId());
        } else if("PROCUREMENT_OFFICER".equals(actor.role())||"EVALUATION_OFFICER".equals(actor.role())) {
            if(!bidRepository.existsByIdAndAuthorityAfterDeadline(bidId,actor.organizationId())) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Bid not found");
            documents=documentRepository.findByBidAndAuthorityAfterDeadline(bidId,actor.organizationId());
        } else throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Access denied");
        return documents.stream().map(BidDocumentResponse::from).toList();
    }

    private static String safeOriginalFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Original filename is required");
        }
        String basename = filename.replace('\\', '/');
        basename = basename.substring(basename.lastIndexOf('/') + 1)
                .replaceAll("[\\p{Cntrl}]", "")
                .trim();
        if (basename.isBlank() || basename.length() > 255) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Filename must be between 1 and 255 characters");
        }
        return basename;
    }

    private static boolean hasPdfSignature(byte[] bytes) {
        byte[] signature = "%PDF-".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        int limit = Math.min(bytes.length - signature.length, PDF_HEADER_SCAN_BYTES);
        for (int offset = 0; offset <= limit; offset++) {
            boolean matches = true;
            for (int index = 0; index < signature.length; index++) {
                if (bytes[offset + index] != signature[index]) {
                    matches = false;
                    break;
                }
            }
            if (matches) {
                return true;
            }
        }
        return false;
    }

    private static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is unavailable", ex);
        }
    }
}
