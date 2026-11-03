package com.procurement.compliance_engine.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter @Setter
public class AppUser {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(name="full_name", nullable=false) private String fullName;
    @Column(nullable=false, unique=true) private String email;
    @Column(name="password_hash", nullable=false) private String passwordHash;
    @Column(nullable=false) private String role;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="organization_id") private Organization organization;
    @Column(name="account_status", nullable=false) private String accountStatus="ACTIVE";
    @Column(name="created_at", insertable=false, updatable=false) private OffsetDateTime createdAt;
    @UpdateTimestamp @Column(name="updated_at") private OffsetDateTime updatedAt;
    @Column(name="last_login_at") private OffsetDateTime lastLoginAt;
}
