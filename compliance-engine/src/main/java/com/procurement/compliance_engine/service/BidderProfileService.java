package com.procurement.compliance_engine.service;

import com.procurement.compliance_engine.dto.BidderProfileResponse;
import com.procurement.compliance_engine.entity.BidderProfile;
import com.procurement.compliance_engine.entity.Organization;
import com.procurement.compliance_engine.repository.BidderProfileRepository;
import com.procurement.compliance_engine.repository.OrganizationRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
public class BidderProfileService {

    private final BidderProfileRepository bidderRepository;
    private final OrganizationRepository organizationRepository;

    public BidderProfileService(
            BidderProfileRepository bidderRepository,
            OrganizationRepository organizationRepository
    ) {
        this.bidderRepository = bidderRepository;
        this.organizationRepository = organizationRepository;
    }

    @Transactional
    public BidderProfileResponse createProfile(UUID organizationId) {
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Organization not found"
                ));

        if (!"BIDDER".equals(organization.getOrganizationType())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Only BIDDER organizations can have bidder profiles"
            );
        }

        if (bidderRepository.existsByOrganizationId(organizationId)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Bidder profile already exists"
            );
        }

        BidderProfile profile = new BidderProfile();
        profile.setOrganization(organization);

        try {
            BidderProfile saved = bidderRepository.saveAndFlush(profile);
            return new BidderProfileResponse(saved.getId(), organization.getId());
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Bidder profile already exists or violates a database constraint",
                    ex
            );
        }
    }
}
