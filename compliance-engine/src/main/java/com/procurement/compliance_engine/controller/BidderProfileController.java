package com.procurement.compliance_engine.controller;

import com.procurement.compliance_engine.dto.BidderProfileResponse;
import com.procurement.compliance_engine.service.BidderProfileService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/v1/organizations")
public class BidderProfileController {

    private final BidderProfileService service;

    public BidderProfileController(BidderProfileService service) {
        this.service = service;
    }

    @PostMapping("/{organizationId}/bidder-profile")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public BidderProfileResponse create(@PathVariable UUID organizationId) {
        return service.createProfile(organizationId);
    }
}
