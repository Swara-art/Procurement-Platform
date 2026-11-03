package com.procurement.compliance_engine.repository;

import com.procurement.compliance_engine.entity.Tender;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TenderRepository extends JpaRepository<Tender, UUID> {

    List<Tender> findByAuthorityId(UUID authorityId);

    List<Tender> findByStatus(String status);
    List<Tender> findByStatusAndAuthorityId(String status, UUID authorityId);
    Optional<Tender> findByIdAndStatus(UUID id, String status);
    Optional<Tender> findByIdAndAuthorityId(UUID id, UUID authorityId);

    Optional<Tender> findByTenderNumber(String tenderNumber);
}
