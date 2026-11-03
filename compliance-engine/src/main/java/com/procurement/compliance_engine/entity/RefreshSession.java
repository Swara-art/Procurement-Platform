package com.procurement.compliance_engine.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity @Table(name="refresh_sessions") @Getter @Setter
public class RefreshSession {
    @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="user_id", nullable=false) private AppUser user;
    @Column(name="token_hash", nullable=false, unique=true) private String tokenHash;
    @Column(name="expires_at", nullable=false) private OffsetDateTime expiresAt;
    @Column(name="revoked_at") private OffsetDateTime revokedAt;
    @Column(name="replaced_by_hash") private String replacedByHash;
}
