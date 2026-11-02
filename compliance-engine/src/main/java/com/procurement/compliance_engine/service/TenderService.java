package com.procurement.compliance_engine.service;

import com.procurement.compliance_engine.dto.CreateTenderRequest;
import com.procurement.compliance_engine.dto.TenderResponse;
import com.procurement.compliance_engine.entity.Organization;
import com.procurement.compliance_engine.entity.Tender;
import com.procurement.compliance_engine.repository.OrganizationRepository;
import com.procurement.compliance_engine.repository.TenderRepository;
import com.procurement.compliance_engine.repository.TenderRequirementRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;
import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class TenderService {

    private final TenderRepository tenderRepository;
    private final OrganizationRepository organizationRepository;
    private final TenderRequirementRepository requirementRepository;

    public TenderService(
            TenderRepository tenderRepository,
            OrganizationRepository organizationRepository,
            TenderRequirementRepository requirementRepository
    ) {
        this.tenderRepository = tenderRepository;
        this.organizationRepository = organizationRepository;
        this.requirementRepository = requirementRepository;
    }

    @Transactional
    public TenderResponse createTender(CreateTenderRequest request) {
        String tenderNumber = request.tenderNumber().trim();
        if (tenderRepository.findByTenderNumber(tenderNumber).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Tender number already exists");
        }

        Organization authority = organizationRepository.findById(request.procurementAuthorityId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Procurement authority not found"
                ));

        if (!"PROCUREMENT_AUTHORITY".equals(authority.getOrganizationType())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Organization must be a procurement authority"
            );
        }

        Tender tender = new Tender();
        tender.setTenderNumber(tenderNumber);
        tender.setTitle(request.title().trim());
        tender.setDescription(request.description() == null ? null : request.description().trim());
        tender.setAuthority(authority);
        tender.setEstimatedValue(request.estimatedValue());
        tender.setSubmissionDeadline(request.submissionDeadline());
        tender.setStatus("DRAFT");

        try {
            return TenderResponse.from(tenderRepository.saveAndFlush(tender));
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Tender number already exists",
                    ex
            );
        }
    }

    @Transactional(readOnly = true)
    public List<TenderResponse> getAllTenders(String status) {
        List<Tender> tenders;
        if (status == null || status.isBlank()) {
            tenders = tenderRepository.findAll();
        } else {
            String normalizedStatus = status.trim().toUpperCase(Locale.ROOT);
            if ("OPEN".equals(normalizedStatus)) {
                normalizedStatus = "PUBLISHED";
            }
            if (!List.of("DRAFT", "PUBLISHED", "CLOSED", "CANCELLED").contains(normalizedStatus)) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Status must be DRAFT, OPEN, PUBLISHED, CLOSED, or CANCELLED"
                );
            }
            tenders = tenderRepository.findByStatus(normalizedStatus);
        }
        return tenders.stream().map(TenderResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public TenderResponse getTenderById(UUID id) {
        Tender tender = tenderRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tender not found"));
        return TenderResponse.from(tender);
    }

    @Transactional
    public TenderResponse publishTender(UUID tenderId) {
        Tender tender = tenderRepository.findById(tenderId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Tender not found"
                ));

        if (!"DRAFT".equals(tender.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Only DRAFT tenders can be published"
            );
        }

        if (!tender.getSubmissionDeadline().isAfter(OffsetDateTime.now())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Submission deadline must be in the future"
            );
        }

        if (!requirementRepository.existsByTenderId(tenderId)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Add at least one requirement before publishing"
            );
        }

        tender.setStatus("PUBLISHED");
        return TenderResponse.from(tenderRepository.saveAndFlush(tender));
    }
}
