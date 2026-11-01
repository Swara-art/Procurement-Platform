package com.procurement.compliance_engine.repository;

import com.procurement.compliance_engine.entity.Tender;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TenderRepository extends JpaRepository<Tender, UUID> {

    List<Tender> findByAuthorityId(UUID authorityId);

    List<Tender> findByStatus(String status);

    Optional<Tender> findByTenderNumber(String tenderNumber);
}
