package com.procurement.compliance_engine.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity @Table(name="organization_documents") @Getter @Setter
public class OrganizationDocument {
 @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="organization_id",nullable=false) private Organization organization;
 @Column(name="uploaded_by",nullable=false) private UUID uploadedBy;
 @Column(name="document_type",nullable=false) private String documentType;
 @Column(name="financial_year") private String financialYear;
 @Column(name="object_key",nullable=false,unique=true,length=700) private String objectKey;
 @Column(name="storage_provider",nullable=false) private String storageProvider;
 @Column(name="bucket_identifier") private String bucketIdentifier;
 @Column(name="original_filename",nullable=false) private String originalFilename;
 @Column(name="content_type",nullable=false) private String contentType;
 @Column(name="file_size",nullable=false) private Long fileSize;
 @Column(name="sha256_checksum",nullable=false) private String sha256Checksum;
 @Column(nullable=false) private Integer version=1;
 @Column(name="verification_status",nullable=false) private String verificationStatus="PENDING";
 @Column(name="processing_status",nullable=false) private String processingStatus="PENDING";
 @Column(name="verified_by") private UUID verifiedBy;
 @Column(name="verified_at") private OffsetDateTime verifiedAt;
 @Column(name="rejection_reason") private String rejectionReason;
 @Column(name="uploaded_at",insertable=false,updatable=false) private OffsetDateTime uploadedAt;
 @Column(name="superseded_at") private OffsetDateTime supersededAt;
}
