package com.procurement.compliance_engine.controller;

import com.procurement.compliance_engine.dto.CreateTenderRequirementRequest;
import com.procurement.compliance_engine.dto.TenderRequirementResponse;
import com.procurement.compliance_engine.service.TenderRequirementService;
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
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/v1/tenders/{tenderId}/requirements")
public class TenderRequirementController {

    private final TenderRequirementService service;
    private final com.procurement.compliance_engine.service.TenderService tenderService;

    public TenderRequirementController(TenderRequirementService service, com.procurement.compliance_engine.service.TenderService tenderService) {
        this.service = service;
        this.tenderService = tenderService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','PROCUREMENT_OFFICER')")
    @ResponseStatus(HttpStatus.CREATED)
    public TenderRequirementResponse create(
            @PathVariable UUID tenderId,
            @Valid @RequestBody CreateTenderRequirementRequest request
    ) {
        tenderService.assertCanView(tenderService.getTenderEntity(tenderId));
        return service.createRequirement(tenderId, request);
    }

    @GetMapping
    public List<TenderRequirementResponse> getAll(@PathVariable UUID tenderId) {
        tenderService.assertCanView(tenderService.getTenderEntity(tenderId));
        return service.getRequirements(tenderId);
    }

    @GetMapping("/{requirementId}")
    public TenderRequirementResponse getById(
            @PathVariable UUID tenderId,
            @PathVariable UUID requirementId
    ) {
        tenderService.assertCanView(tenderService.getTenderEntity(tenderId));
        return service.getRequirementById(tenderId, requirementId);
    }
}
