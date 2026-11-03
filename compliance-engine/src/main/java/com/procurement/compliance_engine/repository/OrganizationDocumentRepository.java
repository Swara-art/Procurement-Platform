package com.procurement.compliance_engine.repository;
import com.procurement.compliance_engine.entity.OrganizationDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
public interface OrganizationDocumentRepository extends JpaRepository<OrganizationDocument,UUID> {
 List<OrganizationDocument> findByOrganizationIdAndSupersededAtIsNullOrderByUploadedAtDesc(UUID organizationId);
 List<OrganizationDocument> findByOrganizationIdAndDocumentTypeAndSupersededAtIsNull(UUID organizationId,String documentType);
 List<OrganizationDocument> findByOrganizationIdAndDocumentTypeAndFinancialYearAndSupersededAtIsNull(UUID organizationId,String documentType,String financialYear);
 Optional<OrganizationDocument> findByIdAndOrganizationId(UUID id,UUID organizationId);
 Integer countByOrganizationIdAndDocumentType(UUID organizationId,String documentType);
}
