package com.procurement.compliance_engine.repository;

import com.procurement.compliance_engine.entity.BidDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BidDocumentRepository extends JpaRepository<BidDocument, UUID> {

    List<BidDocument> findByBidId(UUID bidId);
}
