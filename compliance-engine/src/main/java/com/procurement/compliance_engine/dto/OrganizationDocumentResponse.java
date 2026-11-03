package com.procurement.compliance_engine.dto;
import com.procurement.compliance_engine.entity.OrganizationDocument;
import java.time.OffsetDateTime;
import java.util.UUID;
public record OrganizationDocumentResponse(UUID id,String documentType,String financialYear,String originalFilename,Long fileSize,String contentType,Integer version,String verificationStatus,String processingStatus,OffsetDateTime uploadedAt){
 public static OrganizationDocumentResponse from(OrganizationDocument d){return new OrganizationDocumentResponse(d.getId(),d.getDocumentType(),d.getFinancialYear(),d.getOriginalFilename(),d.getFileSize(),d.getContentType(),d.getVersion(),d.getVerificationStatus(),d.getProcessingStatus(),d.getUploadedAt());}
}
