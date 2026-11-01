package com.procurement.compliance_engine.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "tenders")
@Getter
@Setter
public class Tender {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "authority_id", nullable = false)
    private Organization authority;

    @Column(name = "tender_number", nullable = false, unique = true, length = 100)
    private String tenderNumber;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "estimated_value", precision = 18, scale = 2)
    private BigDecimal estimatedValue;

    @Column(name = "submission_deadline", nullable = false)
    private OffsetDateTime submissionDeadline;

    @Column(nullable = false, length = 30)
    private String status = "DRAFT";

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;
}
