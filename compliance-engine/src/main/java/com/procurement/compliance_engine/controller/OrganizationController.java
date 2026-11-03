package com.procurement.compliance_engine.controller;

import com.procurement.compliance_engine.dto.CreateOrganizationRequest;
import com.procurement.compliance_engine.dto.OrganizationResponse;
import com.procurement.compliance_engine.service.OrganizationService;
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
@RequestMapping("/api/v1/organizations")
public class OrganizationController {

    private final OrganizationService service;

    public OrganizationController(OrganizationService service) {
        this.service = service;
    }

    @PostMapping
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public OrganizationResponse create(@Valid @RequestBody CreateOrganizationRequest request) {
        return service.createOrganization(request);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','PROCUREMENT_OFFICER')")
    public List<OrganizationResponse> getAll() {
        return service.getAllOrganizations();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','PROCUREMENT_OFFICER')")
    public OrganizationResponse getById(@PathVariable UUID id) {
        return service.getOrganizationById(id);
    }
}
