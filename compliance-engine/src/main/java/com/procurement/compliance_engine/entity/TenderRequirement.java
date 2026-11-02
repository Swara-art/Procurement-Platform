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
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "tender_requirements")
@Getter
@Setter
public class TenderRequirement {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tender_id", nullable = false)
    private Tender tender;

    @Column(name = "requirement_code", nullable = false, length = 100)
    private String requirementCode;

    @Column(nullable = false, columnDefinition = "text")
    private String description;

    @Column(nullable = false, length = 30)
    private String operator;

    @Column(name = "expected_value", columnDefinition = "text")
    private String expectedValue;

    @Column(name = "is_mandatory", nullable = false)
    private Boolean mandatory = true;

    @Column(name = "created_at", insertable = false, updatable = false)
    @Generated(event = EventType.INSERT)
    private OffsetDateTime createdAt;
}
