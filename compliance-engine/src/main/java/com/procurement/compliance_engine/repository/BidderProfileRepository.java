package com.procurement.compliance_engine.repository;

import com.procurement.compliance_engine.entity.BidderProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface BidderProfileRepository extends JpaRepository<BidderProfile, UUID> {

    Optional<BidderProfile> findByOrganizationId(UUID organizationId);
}
