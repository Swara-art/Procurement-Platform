package com.procurement.compliance_engine.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record BidderOnboardingRequest(
        @NotBlank @Size(max=255) String legalName,
        @NotBlank @Pattern(regexp="PRIVATE_LIMITED|PUBLIC_LIMITED|OPC|LLP|PARTNERSHIP|SOLE_PROPRIETORSHIP|SECTION_8|GOVERNMENT_ENTITY|PROPRIETORSHIP|SOCIETY|OTHER") String entityType,
        @NotBlank @Size(max=255) String registrationNumber,
        @NotBlank String businessAddress,
        @NotBlank @Pattern(regexp="[+0-9() -]{8,30}") String contactNumber,
        @NotBlank String primarySector,
        @Size(max=10) List<String> additionalSectors,
        @Size(max=30) List<@Size(max=120) String> capabilities,
        @Pattern(regexp="[A-Z]{5}[0-9]{4}[A-Z]",message="PAN format is invalid") String panNumber,
        String gstin,
        boolean gstApplicable,
        LocalDate establishmentDate,
        @Min(0) @Max(150) Integer experienceYears,
        @Min(0) Integer completedProjects,
        String authorizedSignatoryName,
        @Email String authorizedSignatoryEmail,
        List<Turnover> turnovers,
        @Pattern(regexp="[A-Z0-9]{21}",message="CIN must be 21 alphanumeric characters") String cin,
        @Pattern(regexp="[A-Z0-9]{8}",message="LLPIN must be 8 alphanumeric characters") String llpin,
        boolean governmentOwned,
        boolean firmRegistered,
        boolean representativeIsSignatory,
        boolean msmeBenefitClaimed,
        boolean shopRegistrationApplicable,
        boolean businessLicenseAvailable,
        boolean incorporatedEntity,
        boolean panApplicable,
        boolean section8TaxRegistrationApplicable) {
    public record Turnover(@NotBlank @Pattern(regexp="20\\d{2}-20\\d{2}") String financialYear,
                          @NotNull @DecimalMin("0") BigDecimal annualTurnover) {}
}
