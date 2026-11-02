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
@Table(name = "bids")
@Getter
@Setter
public class Bid {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tender_id", nullable = false)
    private Tender tender;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bidder_id", nullable = false)
    private BidderProfile bidder;

    @Column(name = "quoted_amount", precision = 18, scale = 2)
    private BigDecimal quotedAmount;

    @Column(name = "submitted_at")
    private OffsetDateTime submittedAt;

    @Column(nullable = false, length = 30)
    private String status = "DRAFT";

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;
}
