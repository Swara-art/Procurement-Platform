package com.procurement.compliance_engine.service;

import com.procurement.compliance_engine.dto.BidDocumentResponse;
import com.procurement.compliance_engine.entity.Bid;
import com.procurement.compliance_engine.entity.BidDocument;
import com.procurement.compliance_engine.repository.BidDocumentRepository;
import com.procurement.compliance_engine.repository.BidRepository;
import com.procurement.compliance_engine.security.CurrentUser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
public class BidDocumentService {
    private final BidDocumentRepository documentRepository; private final BidRepository bidRepository;
    private final DocumentStorageService storage; private final long maxFileSizeBytes; private final JdbcTemplate jdbc; private final boolean scanRequired;
    public BidDocumentService(BidDocumentRepository documents,BidRepository bids,DocumentStorageService storage,@Value("${app.storage.max-file-size-bytes:26214400}") long maxBytes,@Value("${app.storage.scan-required:true}") boolean scanRequired,JdbcTemplate jdbc){this.documentRepository=documents;this.bidRepository=bids;this.storage=storage;this.maxFileSizeBytes=maxBytes;this.scanRequired=scanRequired;this.jdbc=jdbc;}

    @Transactional
    public BidDocumentResponse uploadDocument(UUID bidId,MultipartFile file){return uploadDocument(bidId,"COMPLIANCE",file);}

    @Transactional
    public BidDocumentResponse uploadDocument(UUID bidId,String category,MultipartFile file){
        var actor=CurrentUser.get();if(actor==null||!"BIDDER".equals(actor.role()))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Bidder account required");
        Bid bid=bidRepository.findByIdAndBidderOrganization(bidId,actor.organizationId()).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Bid not found"));
        String type=normalizeCategory(category);byte[] bytes=readAndValidate(file);String checksum=sha256(bytes);String filename=safeFilename(file.getOriginalFilename());
        UUID docId=UUID.randomUUID(),versionId=UUID.randomUUID();String key="organizations/"+actor.organizationId()+"/bids/"+bidId+"/"+type.toLowerCase()+"/"+docId+"/"+versionId+".pdf";
        storage.put(key,bytes,"application/pdf",checksum);
        try {
            BidDocument d=new BidDocument();d.setId(docId);d.setBid(bid);d.setOriginalFilename(filename);d.setContentType("application/pdf");d.setFileSize((long)bytes.length);d.setStorageKey(key);d.setSha256Hash(checksum);d.setDocumentType(type);d.setStorageProvider(storage.provider());d.setBucketIdentifier(storage.bucketIdentifier());d.setUploadedBy(actor.id());d.setVerificationStatus("PENDING");d.setProcessingStatus(scanRequired?"QUARANTINED":"AVAILABLE");
            BidDocument saved=documentRepository.saveAndFlush(d);rollbackCleanup(key);audit(actor.id(),actor.organizationId(),docId,"DOCUMENT_UPLOADED","SUCCESS");return BidDocumentResponse.from(saved);
        } catch(DataIntegrityViolationException e){storage.delete(key);throw new ResponseStatusException(HttpStatus.CONFLICT,"Document metadata violates a database constraint",e);}
    }

    @Transactional(readOnly=true) public byte[] download(UUID bidId,UUID documentId){var actor=CurrentUser.get();if(actor==null)throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Authentication required");BidDocument d=visibleDocument(documentId,actor);if(!d.getBid().getId().equals(bidId))throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Document not found");if(d.getSupersededAt()!=null)throw new ResponseStatusException(HttpStatus.GONE,"Document version has been superseded");if(!"AVAILABLE".equals(d.getProcessingStatus()))throw new ResponseStatusException(HttpStatus.LOCKED,"Document is quarantined pending malware scanning");byte[] content=storage.get(d.getStorageKey());if(!sha256(content).equalsIgnoreCase(d.getSha256Hash()))throw new ResponseStatusException(HttpStatus.CONFLICT,"Document checksum mismatch");return content;}

    @Transactional(readOnly=true) public String filename(UUID documentId){var actor=CurrentUser.get();if(actor==null)throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Authentication required");return visibleDocument(documentId,actor).getOriginalFilename();}
    private BidDocument visibleDocument(UUID id,com.procurement.compliance_engine.security.UserPrincipal actor){
        BidDocument d;
        if("SYSTEM_ADMIN".equals(actor.role()))d=documentRepository.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Document not found"));
        else if("BIDDER".equals(actor.role()))d=documentRepository.findByIdAndBidderOrganization(id,actor.organizationId()).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Document not found"));
        else if("PROCUREMENT_OFFICER".equals(actor.role())||"EVALUATION_OFFICER".equals(actor.role()))d=documentRepository.findByIdAndAuthorityAfterDeadline(id,actor.organizationId()).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Document not found"));
        else throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Access denied");
        if("FINANCIAL".equals(d.getDocumentType())&&("PROCUREMENT_OFFICER".equals(actor.role())||"EVALUATION_OFFICER".equals(actor.role()))&&d.getBid().getTender().getSubmissionDeadline().isAfter(OffsetDateTime.now()))throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Document not found");
        return d;
    }

    @Transactional(readOnly=true) public List<BidDocumentResponse> getDocuments(UUID bidId){var actor=CurrentUser.get();if(actor==null)throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Authentication required");List<BidDocument> docs;if("SYSTEM_ADMIN".equals(actor.role()))docs=documentRepository.findByBidIdAndSupersededAtIsNull(bidId);else if("BIDDER".equals(actor.role())){if(!bidRepository.existsByIdAndBidderOrganization(bidId,actor.organizationId()))throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Bid not found");docs=documentRepository.findByBidAndBidderOrganization(bidId,actor.organizationId());}else if("PROCUREMENT_OFFICER".equals(actor.role())||"EVALUATION_OFFICER".equals(actor.role())){if(!bidRepository.existsByIdAndAuthorityAfterDeadline(bidId,actor.organizationId()))throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Bid not found");docs=documentRepository.findByBidAndAuthorityAfterDeadline(bidId,actor.organizationId());}else throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Access denied");return docs.stream().filter(d->d.getSupersededAt()==null).filter(d->!"FINANCIAL".equals(d.getDocumentType())||"BIDDER".equals(actor.role())||"SYSTEM_ADMIN".equals(actor.role())||!d.getBid().getTender().getSubmissionDeadline().isAfter(OffsetDateTime.now())).map(BidDocumentResponse::from).toList();}

    private byte[] readAndValidate(MultipartFile file){if(file==null||file.isEmpty())throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"A non-empty PDF file is required");if(file.getSize()>maxFileSizeBytes)throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,"File exceeds upload size limit");try{byte[] bytes=file.getBytes();if(bytes.length==0||bytes.length>maxFileSizeBytes)throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,"File exceeds upload size limit");if(!pdfSignature(bytes))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Uploaded file is not a PDF");return bytes;}catch(IOException e){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Unable to read uploaded file",e);}}
    private static String normalizeCategory(String c){String t=c==null?"COMPLIANCE":c.trim().toUpperCase(java.util.Locale.ROOT);if(!List.of("TECHNICAL","FINANCIAL","COMPLIANCE").contains(t))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Unsupported bid document category");return t;}
    private static String safeFilename(String n){if(n==null||n.isBlank())throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Original filename is required");String b=n.replace('\\','/');b=b.substring(b.lastIndexOf('/')+1).replaceAll("[\\p{Cntrl}]","").trim();if(b.isBlank()||b.length()>255)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid filename");return b;}
    private static boolean pdfSignature(byte[] b){byte[] sig="%PDF-".getBytes(java.nio.charset.StandardCharsets.US_ASCII);for(int i=0;i<=Math.min(b.length-sig.length,1024);i++){boolean ok=true;for(int j=0;j<sig.length;j++)if(b[i+j]!=sig[j]){ok=false;break;}if(ok)return true;}return false;}
    private static String sha256(byte[] b){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(b));}catch(Exception e){throw new IllegalStateException(e);}}
    private void rollbackCleanup(String key){if(TransactionSynchronizationManager.isSynchronizationActive())TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization(){public void afterCompletion(int status){if(status!=STATUS_COMMITTED)storage.delete(key);}});}
    private void audit(UUID actor,UUID org,UUID doc,String event,String outcome){jdbc.update("INSERT INTO document_audit_events(actor_id,organization_id,document_id,event_type,outcome) VALUES (?,?,?,?,?)",actor,org,doc,event,outcome);}
}
