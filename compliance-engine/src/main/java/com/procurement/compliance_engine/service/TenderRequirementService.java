package com.procurement.compliance_engine.service;

import com.procurement.compliance_engine.dto.CreateTenderRequirementRequest;
import com.procurement.compliance_engine.dto.TenderRequirementResponse;
import com.procurement.compliance_engine.entity.Tender;
import com.procurement.compliance_engine.entity.TenderRequirement;
import com.procurement.compliance_engine.repository.TenderRepository;
import com.procurement.compliance_engine.repository.TenderRequirementRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class TenderRequirementService {

    private final TenderRequirementRepository requirementRepository;
    private final TenderRepository tenderRepository;

    public TenderRequirementService(
            TenderRequirementRepository requirementRepository,
            TenderRepository tenderRepository
    ) {
        this.requirementRepository = requirementRepository;
        this.tenderRepository = tenderRepository;
    }

    @Transactional
    public TenderRequirementResponse createRequirement(
            UUID tenderId,
            CreateTenderRequirementRequest request
    ) {
        Tender tender = tenderRepository.findById(tenderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tender not found"));

        if (!"DRAFT".equals(tender.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Requirements can only be added to DRAFT tenders"
            );
        }

        String requirementCode = request.requirementCode().trim();
        if (requirementRepository.existsByTenderIdAndRequirementCode(tenderId, requirementCode)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Requirement code already exists for this tender"
            );
        }

        TenderRequirement requirement = new TenderRequirement();
        requirement.setTender(tender);
        requirement.setRequirementCode(requirementCode);
        requirement.setDescription(request.description().trim());
        requirement.setOperator(request.operator().trim().toUpperCase(Locale.ROOT));
        requirement.setExpectedValue(
                request.expectedValue() == null ? null : request.expectedValue().trim()
        );
        requirement.setMandatory(request.mandatory());

        try {
            TenderRequirement saved = requirementRepository.saveAndFlush(requirement);
            return TenderRequirementResponse.from(saved);
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Requirement code already exists for this tender",
                    ex
            );
        }
    }

    @Transactional(readOnly = true)
    public List<TenderRequirementResponse> getRequirements(UUID tenderId) {
        if (!tenderRepository.existsById(tenderId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tender not found");
        }

        return requirementRepository.findByTenderId(tenderId)
                .stream()
                .map(TenderRequirementResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public TenderRequirementResponse getRequirementById(UUID tenderId, UUID requirementId) {
        if (!tenderRepository.existsById(tenderId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tender not found");
        }

        TenderRequirement requirement = requirementRepository
                .findByIdAndTenderId(requirementId, tenderId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Requirement not found"
                ));

        return TenderRequirementResponse.from(requirement);
    }
}
