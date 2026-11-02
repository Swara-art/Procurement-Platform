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

@RestController
@RequestMapping("/api/v1/organizations")
public class OrganizationController {

    private final OrganizationService service;

    public OrganizationController(OrganizationService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrganizationResponse create(@Valid @RequestBody CreateOrganizationRequest request) {
        return service.createOrganization(request);
    }

    @GetMapping
    public List<OrganizationResponse> getAll() {
        return service.getAllOrganizations();
    }

    @GetMapping("/{id}")
    public OrganizationResponse getById(@PathVariable UUID id) {
        return service.getOrganizationById(id);
    }
}
