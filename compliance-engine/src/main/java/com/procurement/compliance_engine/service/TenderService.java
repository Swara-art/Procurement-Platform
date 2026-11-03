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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import com.procurement.compliance_engine.security.CurrentUser;
import com.procurement.compliance_engine.security.UserPrincipal;

@Service
public class TenderService {

    @Autowired private JdbcTemplate jdbcTemplate;

    private final TenderRepository tenderRepository;
    private final OrganizationRepository organizationRepository;
    private final TenderRequirementRepository requirementRepository;
    private final TenderClassificationService classificationService;

    public TenderService(
            TenderRepository tenderRepository,
            OrganizationRepository organizationRepository,
            TenderRequirementRepository requirementRepository,
            TenderClassificationService classificationService
    ) {
        this.tenderRepository = tenderRepository;
        this.organizationRepository = organizationRepository;
        this.requirementRepository = requirementRepository;
        this.classificationService = classificationService;
    }

    @Transactional
    public TenderResponse createTender(CreateTenderRequest request) {
        String tenderNumber = request.tenderNumber().trim();
        if (tenderRepository.findByTenderNumber(tenderNumber).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Tender number already exists");
        }

        UserPrincipal actor=CurrentUser.get();
        if(actor==null || !("PROCUREMENT_OFFICER".equals(actor.role())||"SYSTEM_ADMIN".equals(actor.role()))) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Tender creation requires a procurement officer or system administrator");
        UUID authorityId=actor!=null && "PROCUREMENT_OFFICER".equals(actor.role()) ? actor.organizationId() : request.procurementAuthorityId();
        Organization authority = organizationRepository.findById(authorityId)
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
        if(actor!=null && "PROCUREMENT_OFFICER".equals(actor.role()) && !authority.getId().equals(actor.organizationId())) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"You may create tenders only for your authority");

        Tender tender = new Tender();
        tender.setTenderNumber(tenderNumber);
        tender.setTitle(request.title().trim());
        tender.setDescription(request.description() == null ? null : request.description().trim());
        tender.setAuthority(authority);
        tender.setEstimatedValue(request.estimatedValue());
        tender.setSubmissionDeadline(request.submissionDeadline());
        tender.setStatus("DRAFT");

        try {
            Tender saved=tenderRepository.saveAndFlush(tender);
            classificationService.enqueue(saved,false);
            return TenderResponse.from(saved);
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
        String normalizedStatus = null;
        if (status == null || status.isBlank()) {
        } else {
            normalizedStatus = status.trim().toUpperCase(Locale.ROOT);
            if ("OPEN".equals(normalizedStatus)) {
                normalizedStatus = "PUBLISHED";
            }
            if (!List.of("DRAFT", "PUBLISHED", "CLOSED", "CANCELLED").contains(normalizedStatus)) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Status must be DRAFT, OPEN, PUBLISHED, CLOSED, or CANCELLED"
                );
            }
        }
        UserPrincipal actor=CurrentUser.get();
        if(actor==null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Authentication required");
        List<Tender> tenders;
        if (actor != null && "BIDDER".equals(actor.role())) {
            tenders = normalizedStatus != null && !"PUBLISHED".equals(normalizedStatus)
                    ? List.of() : tenderRepository.findByStatus("PUBLISHED");
        } else if (actor != null && ("PROCUREMENT_OFFICER".equals(actor.role()) || "EVALUATION_OFFICER".equals(actor.role()))) {
            tenders = normalizedStatus == null
                    ? tenderRepository.findByAuthorityId(actor.organizationId())
                    : tenderRepository.findByStatusAndAuthorityId(normalizedStatus, actor.organizationId());
        } else {
            tenders = normalizedStatus == null ? tenderRepository.findAll() : tenderRepository.findByStatus(normalizedStatus);
        }
        return tenders.stream().map(TenderResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public TenderResponse getTenderById(UUID id) {
        UserPrincipal actor=CurrentUser.get();
        Tender tender = scopedTenderById(id, actor)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tender not found"));
        assertCanView(tender);
        return TenderResponse.from(tender);
    }

    @Transactional(readOnly = true)
    public Tender getTenderEntity(UUID id) {
        return scopedTenderById(id, CurrentUser.get()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Tender not found"));
    }

    @Transactional
    public TenderResponse publishTender(UUID tenderId) {
        UserPrincipal actor=CurrentUser.get();
        if(actor==null || !("PROCUREMENT_OFFICER".equals(actor.role())||"SYSTEM_ADMIN".equals(actor.role()))) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Tender publication requires a procurement officer or system administrator");
        Tender tender = scopedTenderById(tenderId, actor)
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
        jdbcTemplate.update("INSERT INTO audit_logs(actor_id,event_type,resource_id,outcome) VALUES (?,?,?,'SUCCESS')",actor==null?null:actor.id(),"TENDER_PUBLISHED",tenderId);
        return TenderResponse.from(tenderRepository.saveAndFlush(tender));
    }

    public void assertCanView(Tender tender) {
        UserPrincipal actor=CurrentUser.get(); if(actor==null)return;
        if("BIDDER".equals(actor.role()) && !"PUBLISHED".equals(tender.getStatus())) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Tender not found");
        if(("PROCUREMENT_OFFICER".equals(actor.role())||"EVALUATION_OFFICER".equals(actor.role())) && !tender.getAuthority().getId().equals(actor.organizationId())) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Tender belongs to another authority");
    }

    private java.util.Optional<Tender> scopedTenderById(UUID id, UserPrincipal actor) {
        if (actor == null) return java.util.Optional.empty();
        if ("SYSTEM_ADMIN".equals(actor.role())) return tenderRepository.findById(id);
        if ("BIDDER".equals(actor.role())) return tenderRepository.findByIdAndStatus(id, "PUBLISHED");
        if ("PROCUREMENT_OFFICER".equals(actor.role()) || "EVALUATION_OFFICER".equals(actor.role())) return tenderRepository.findByIdAndAuthorityId(id, actor.organizationId());
        return java.util.Optional.empty();
    }
}
