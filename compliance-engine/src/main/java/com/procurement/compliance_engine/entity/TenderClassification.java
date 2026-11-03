package com.procurement.compliance_engine.entity;
import jakarta.persistence.*;import lombok.Getter;import lombok.Setter;import java.time.OffsetDateTime;import java.util.UUID;
@Entity @Table(name="tender_classifications") @Getter @Setter
public class TenderClassification {
 @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="job_id",nullable=false) private TenderClassificationJob job;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="tender_id",nullable=false) private Tender tender;
 @Column(name="sector_code",nullable=false) private String sectorCode;
 @Column(name="relevance_score",nullable=false,precision=5,scale=4) private java.math.BigDecimal relevanceScore;
 @Column(nullable=false) private Boolean confirmed=false;
 @Column(nullable=false) private String source;
 @Column(name="confirmed_by") private UUID confirmedBy;
 @Column(name="confirmed_at") private OffsetDateTime confirmedAt;
}
