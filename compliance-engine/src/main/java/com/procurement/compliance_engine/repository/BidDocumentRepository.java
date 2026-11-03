package com.procurement.compliance_engine.repository;

import com.procurement.compliance_engine.entity.BidDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

import java.util.List;
import java.util.UUID;

public interface BidDocumentRepository extends JpaRepository<BidDocument, UUID> {

    List<BidDocument> findByBidId(UUID bidId);
    List<BidDocument> findByBidIdAndSupersededAtIsNull(UUID bidId);

    @Query("select d from BidDocument d where d.bid.id = :bidId and d.bid.bidder.organization.id = :organizationId")
    List<BidDocument> findByBidAndBidderOrganization(@Param("bidId") UUID bidId, @Param("organizationId") UUID organizationId);

    @Query("select d from BidDocument d where d.bid.id = :bidId and d.bid.tender.authority.id = :authorityId and d.bid.tender.submissionDeadline <= CURRENT_TIMESTAMP")
    List<BidDocument> findByBidAndAuthorityAfterDeadline(@Param("bidId") UUID bidId, @Param("authorityId") UUID authorityId);

    @Query("select d from BidDocument d where d.id = :documentId and d.bid.bidder.organization.id = :organizationId")
    Optional<BidDocument> findByIdAndBidderOrganization(@Param("documentId") UUID documentId, @Param("organizationId") UUID organizationId);

    @Query("select d from BidDocument d where d.id = :documentId and d.bid.tender.authority.id = :authorityId and d.bid.tender.submissionDeadline <= CURRENT_TIMESTAMP")
    Optional<BidDocument> findByIdAndAuthorityAfterDeadline(@Param("documentId") UUID documentId, @Param("authorityId") UUID authorityId);
}
