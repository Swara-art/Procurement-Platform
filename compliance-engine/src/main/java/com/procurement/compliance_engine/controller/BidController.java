package com.procurement.compliance_engine.controller;

import com.procurement.compliance_engine.dto.BidResponse;
import com.procurement.compliance_engine.dto.SubmitBidRequest;
import com.procurement.compliance_engine.service.BidService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
public class BidController {

    private final BidService service;

    public BidController(BidService service) {
        this.service = service;
    }

    @PostMapping("/api/v1/tenders/{tenderId}/bids")
    @ResponseStatus(HttpStatus.CREATED)
    public BidResponse submit(
            @PathVariable UUID tenderId,
            @Valid @RequestBody SubmitBidRequest request
    ) {
        return service.submitBid(tenderId, request);
    }

    @GetMapping("/api/v1/tenders/{tenderId}/bids")
    public List<BidResponse> getBidsByTender(@PathVariable UUID tenderId) {
        return service.getBidsByTender(tenderId);
    }

    @GetMapping("/api/v1/bids/{bidId}")
    public BidResponse getBidById(@PathVariable UUID bidId) {
        return service.getBidById(bidId);
    }
}
