package com.procurement.compliance_engine.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name="account_registration_requests")
@Getter @Setter
public class AccountRegistrationRequest {
    @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id;
    @Column(name="full_name",nullable=false) private String fullName;
    @Column(nullable=false) private String email;
    @Column(name="password_hash",nullable=false) private String passwordHash;
    @Column(name="actor_type",nullable=false) private String actorType;
    @Column(name="organization_name") private String organizationName;
    @Column(name="registration_number") private String registrationNumber;
    @Column(nullable=false) private String status="PENDING";
    @Column(name="submitted_at",insertable=false,updatable=false) private OffsetDateTime submittedAt;
    @Column(name="reviewed_by") private UUID reviewedBy;
    @Column(name="reviewed_at") private OffsetDateTime reviewedAt;
    @Column(name="provisioned_user_id") private UUID provisionedUserId;
}
