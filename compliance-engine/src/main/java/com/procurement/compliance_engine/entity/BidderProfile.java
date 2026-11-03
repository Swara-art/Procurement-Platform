package com.procurement.compliance_engine.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "bidder_profiles")
@Getter
@Setter
public class BidderProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false, unique = true)
    private Organization organization;

    @Column(name = "pan_number", length = 10)
    private String panNumber;

    @Column(length = 15)
    private String gstin;

    @Column(length = 21)
    private String cin;

    @Column(name = "business_address", columnDefinition = "text")
    private String businessAddress;

    @Column(name="entity_type") private String entityType;
    @Column(name="contact_number") private String contactNumber;
    @Column(name="establishment_date") private java.time.LocalDate establishmentDate;
    @Column(name="experience_years") private Integer experienceYears;
    @Column(name="completed_projects") private Integer completedProjects;
    @Column(name="gst_applicable",nullable=false) private boolean gstApplicable;
    @Column(name="authorized_signatory_name") private String authorizedSignatoryName;
    @Column(name="authorized_signatory_email") private String authorizedSignatoryEmail;
    @Column(name="onboarding_status",nullable=false) private String onboardingStatus="PROFILE_INCOMPLETE";
    @Column(name="llpin",length=8) private String llpin;
    @Column(name="government_owned",nullable=false) private boolean governmentOwned;
    @Column(name="firm_registered",nullable=false) private boolean firmRegistered;
    @Column(name="representative_is_signatory",nullable=false) private boolean representativeIsSignatory=true;
    @Column(name="msme_benefit_claimed",nullable=false) private boolean msmeBenefitClaimed;
    @Column(name="shop_registration_applicable",nullable=false) private boolean shopRegistrationApplicable;
    @Column(name="business_license_available",nullable=false) private boolean businessLicenseAvailable;
    @Column(name="incorporated_entity",nullable=false) private boolean incorporatedEntity=true;
    @Column(name="pan_applicable",nullable=false) private boolean panApplicable=true;
    @Column(name="section8_tax_registration_applicable",nullable=false) private boolean section8TaxRegistrationApplicable;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;
}
