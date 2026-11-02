package com.procurement.compliance_engine.service;

import com.procurement.compliance_engine.dto.BidResponse;
import com.procurement.compliance_engine.dto.SubmitBidRequest;
import com.procurement.compliance_engine.entity.Bid;
import com.procurement.compliance_engine.entity.BidderProfile;
import com.procurement.compliance_engine.entity.Tender;
import com.procurement.compliance_engine.repository.BidRepository;
import com.procurement.compliance_engine.repository.BidderProfileRepository;
import com.procurement.compliance_engine.repository.TenderRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class BidService {

    private final BidRepository bidRepository;
    private final TenderRepository tenderRepository;
    private final BidderProfileRepository bidderProfileRepository;

    public BidService(
            BidRepository bidRepository,
            TenderRepository tenderRepository,
            BidderProfileRepository bidderProfileRepository
    ) {
        this.bidRepository = bidRepository;
        this.tenderRepository = tenderRepository;
        this.bidderProfileRepository = bidderProfileRepository;
    }

    @Transactional
    public BidResponse submitBid(UUID tenderId, SubmitBidRequest request) {
        Bid bid = createBid(tenderId, request, "SUBMITTED");
        bid.setSubmittedAt(OffsetDateTime.now());
        return saveBid(bid);
    }

    @Transactional
    public BidResponse createDraftBid(UUID tenderId, SubmitBidRequest request) {
        return saveBid(createBid(tenderId, request, "DRAFT"));
    }

    @Transactional
    public BidResponse submitDraftBid(UUID bidId) {
        Bid bid = bidRepository.findById(bidId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bid not found"));

        if (!"DRAFT".equals(bid.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only DRAFT bids can be submitted");
        }

        ensureTenderAcceptsBids(bid.getTender());
        bid.setStatus("SUBMITTED");
        bid.setSubmittedAt(OffsetDateTime.now());
        return saveBid(bid);
    }

    private Bid createBid(UUID tenderId, SubmitBidRequest request, String status) {
        Tender tender = tenderRepository.findById(tenderId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Tender not found"
                ));

        ensureTenderAcceptsBids(tender);

        BidderProfile bidderProfile = bidderProfileRepository
                .findById(request.bidderProfileId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Bidder profile not found"
                ));

        if (!"BIDDER".equals(bidderProfile.getOrganization().getOrganizationType())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Only BIDDER organizations can submit bids"
            );
        }

        if (bidRepository.existsByTenderIdAndBidderId(tenderId, bidderProfile.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Bid already exists for this tender and bidder"
            );
        }

        Bid bid = new Bid();
        bid.setTender(tender);
        bid.setBidder(bidderProfile);
        bid.setQuotedAmount(request.quotedAmount());
        bid.setStatus(status);
        return bid;
    }

    private void ensureTenderAcceptsBids(Tender tender) {
        if (!"PUBLISHED".equals(tender.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Bids can only be submitted to published tenders"
            );
        }
        if (!tender.getSubmissionDeadline().isAfter(OffsetDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Tender submission deadline has passed");
        }
    }

    private BidResponse saveBid(Bid bid) {
        try {
            return BidResponse.from(bidRepository.saveAndFlush(bid));
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Bid could not be saved due to a database constraint",
                    ex
            );
        }
    }

    @Transactional(readOnly = true)
    public List<BidResponse> getBidsByTender(UUID tenderId) {
        if (!tenderRepository.existsById(tenderId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tender not found");
        }

        return bidRepository.findByTenderId(tenderId)
                .stream()
                .map(BidResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public BidResponse getBidById(UUID bidId) {
        Bid bid = bidRepository.findById(bidId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bid not found"));
        return BidResponse.from(bid);
    }
}
