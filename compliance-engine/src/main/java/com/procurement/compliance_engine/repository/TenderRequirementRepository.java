package com.procurement.compliance_engine.repository;

import com.procurement.compliance_engine.entity.TenderRequirement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TenderRequirementRepository extends JpaRepository<TenderRequirement, UUID> {

    List<TenderRequirement> findByTenderId(UUID tenderId);
}
