package com.procurement.compliance_engine.service;

import com.procurement.compliance_engine.dto.CreateOrganizationRequest;
import com.procurement.compliance_engine.dto.OrganizationResponse;
import com.procurement.compliance_engine.entity.Organization;
import com.procurement.compliance_engine.repository.OrganizationRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
public class OrganizationService {

    private final OrganizationRepository repository;

    public OrganizationService(OrganizationRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public OrganizationResponse createOrganization(CreateOrganizationRequest request) {
        String name = request.name().trim();
        String registrationNumber = request.registrationNumber().trim();

        if (repository.existsByRegistrationNumber(registrationNumber)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Registration number already exists"
            );
        }

        Organization organization = new Organization();
        organization.setName(name);
        organization.setOrganizationType(request.organizationType());
        organization.setRegistrationNumber(registrationNumber);

        try {
            Organization saved = repository.saveAndFlush(organization);
            return OrganizationResponse.from(saved);
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Organization name or registration number already exists",
                    ex
            );
        }
    }

    @Transactional(readOnly = true)
    public List<OrganizationResponse> getAllOrganizations() {
        return repository.findAll()
                .stream()
                .map(OrganizationResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrganizationResponse getOrganizationById(UUID id) {
        Organization organization = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Organization not found"
                ));

        return OrganizationResponse.from(organization);
    }
}
