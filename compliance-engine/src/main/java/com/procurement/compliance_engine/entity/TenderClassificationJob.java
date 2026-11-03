package com.procurement.compliance_engine.entity;
import jakarta.persistence.*;import lombok.Getter;import lombok.Setter;import java.time.OffsetDateTime;import java.util.UUID;
@Entity @Table(name="tender_classification_jobs") @Getter @Setter
public class TenderClassificationJob {
 @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="tender_id",nullable=false) private Tender tender;
 @Column(name="content_hash",nullable=false) private String contentHash;
 @Column(nullable=false) private String status="PENDING";
 @Column(nullable=false) private Integer attempts=0;
 @Column(name="next_attempt_at",nullable=false) private OffsetDateTime nextAttemptAt;
 @Column(name="model_version") private String modelVersion;
 @Column(name="last_error") private String lastError;
 @Column(name="created_at",insertable=false,updatable=false) private OffsetDateTime createdAt;
 @Column(name="updated_at") private OffsetDateTime updatedAt;
}
