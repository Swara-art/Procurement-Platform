package com.procurement.compliance_engine.repository;

import com.procurement.compliance_engine.entity.Bid;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BidRepository extends JpaRepository<Bid, UUID> {

    boolean existsByTenderIdAndBidderId(UUID tenderId, UUID bidderId);

    List<Bid> findByTenderId(UUID tenderId);

    List<Bid> findByBidderId(UUID bidderId);

    Optional<Bid> findByTenderIdAndBidderId(UUID tenderId, UUID bidderId);

    @Query("select b from Bid b where b.bidder.organization.id = :organizationId")
    List<Bid> findAllByBidderOrganizationId(@Param("organizationId") UUID organizationId);

    @Query("select b from Bid b where b.tender.authority.id = :authorityId and b.tender.submissionDeadline <= CURRENT_TIMESTAMP")
    List<Bid> findAllVisibleToAuthority(@Param("authorityId") UUID authorityId);

    @Query("select b from Bid b where b.tender.id = :tenderId and b.bidder.organization.id = :organizationId")
    List<Bid> findAllByTenderAndBidderOrganization(@Param("tenderId") UUID tenderId, @Param("organizationId") UUID organizationId);

    @Query("select b from Bid b where b.tender.id = :tenderId and b.tender.authority.id = :authorityId and b.tender.submissionDeadline <= CURRENT_TIMESTAMP")
    List<Bid> findAllByTenderAndAuthorityAfterDeadline(@Param("tenderId") UUID tenderId, @Param("authorityId") UUID authorityId);

    @Query("select b from Bid b where b.id = :bidId and b.bidder.organization.id = :organizationId")
    Optional<Bid> findByIdAndBidderOrganization(@Param("bidId") UUID bidId, @Param("organizationId") UUID organizationId);

    @Query("select b from Bid b where b.id = :bidId and b.tender.authority.id = :authorityId and b.tender.submissionDeadline <= CURRENT_TIMESTAMP")
    Optional<Bid> findByIdAndAuthorityAfterDeadline(@Param("bidId") UUID bidId, @Param("authorityId") UUID authorityId);

    @Query("select b from Bid b where b.id = :bidId and b.bidder.organization.id = :organizationId and b.status = 'DRAFT'")
    Optional<Bid> findDraftByIdAndBidderOrganization(@Param("bidId") UUID bidId, @Param("organizationId") UUID organizationId);

    @Query("select case when count(b) > 0 then true else false end from Bid b where b.id = :bidId and b.bidder.organization.id = :organizationId")
    boolean existsByIdAndBidderOrganization(@Param("bidId") UUID bidId, @Param("organizationId") UUID organizationId);

    @Query("select case when count(b) > 0 then true else false end from Bid b where b.id = :bidId and b.tender.authority.id = :authorityId and b.tender.submissionDeadline <= CURRENT_TIMESTAMP")
    boolean existsByIdAndAuthorityAfterDeadline(@Param("bidId") UUID bidId, @Param("authorityId") UUID authorityId);
}
