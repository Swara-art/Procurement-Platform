package com.procurement.compliance_engine.repository;

import com.procurement.compliance_engine.entity.Bid;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BidRepository extends JpaRepository<Bid, UUID> {

    boolean existsByTenderIdAndBidderId(UUID tenderId, UUID bidderId);

    List<Bid> findByTenderId(UUID tenderId);

    List<Bid> findByBidderId(UUID bidderId);

    Optional<Bid> findByTenderIdAndBidderId(UUID tenderId, UUID bidderId);
}
