package com.procurement.compliance_engine.service;

import com.procurement.compliance_engine.classification.SectorTaxonomy;
import com.procurement.compliance_engine.dto.*;
import com.procurement.compliance_engine.entity.*;
import com.procurement.compliance_engine.repository.*;
import com.procurement.compliance_engine.security.CurrentUser;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;

@Service
public class BidderOnboardingService {
    private final BidderProfileRepository profiles;
    private final OrganizationRepository organizations;
    private final JdbcTemplate jdbc;
    private final OrganizationDocumentService docs;

    public BidderOnboardingService(BidderProfileRepository profiles, OrganizationRepository organizations,
            JdbcTemplate jdbc, OrganizationDocumentService docs) {
        this.profiles=profiles; this.organizations=organizations; this.jdbc=jdbc; this.docs=docs;
    }

    @Transactional
    public BidderOnboardingResponse save(BidderOnboardingRequest r) {
        var actor=CurrentUser.get();
        if(actor==null||!"BIDDER".equals(actor.role())) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Bidder account required");
        String type=canonicalType(r.entityType());
        if(!Boolean.TRUE.equals(jdbc.query("SELECT EXISTS(SELECT 1 FROM business_legal_types WHERE code=? AND active)",rs->rs.next()&&rs.getBoolean(1),type))) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Unsupported business legal structure");
        String primary=validSector(r.primarySector()); Set<String> all=new LinkedHashSet<>(); all.add(primary);
        if(r.additionalSectors()!=null) for(String s:r.additionalSectors()) all.add(validSector(s));
        if(all.size()>11) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Too many sectors");
        Organization org=organizations.findById(actor.organizationId()).orElseThrow();
        if(!org.getRegistrationNumber().equals(r.registrationNumber())&& !"BIDDER".equals(org.getOrganizationType())) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Organization registration cannot be changed");
        org.setName(r.legalName().trim()); org.setRegistrationNumber(r.registrationNumber().trim());
        BidderProfile p=profiles.findByOrganizationId(org.getId()).orElseThrow(()->new ResponseStatusException(HttpStatus.CONFLICT,"Bidder profile is missing"));
        p.setEntityType(type); p.setBusinessAddress(r.businessAddress()); p.setContactNumber(r.contactNumber()); p.setPanNumber(r.panNumber()); p.setCin(r.cin()); p.setLlpin(r.llpin());
        p.setGstin(r.gstApplicable()?r.gstin():null); p.setGstApplicable(r.gstApplicable()); p.setEstablishmentDate(r.establishmentDate()); p.setExperienceYears(r.experienceYears()); p.setCompletedProjects(r.completedProjects());
        p.setAuthorizedSignatoryName(r.authorizedSignatoryName()); p.setAuthorizedSignatoryEmail(r.authorizedSignatoryEmail()); p.setGovernmentOwned(r.governmentOwned()||"GOVERNMENT_ENTITY".equals(type));
        p.setFirmRegistered(r.firmRegistered()); p.setRepresentativeIsSignatory(r.representativeIsSignatory()); p.setMsmeBenefitClaimed(r.msmeBenefitClaimed()); p.setShopRegistrationApplicable(r.shopRegistrationApplicable());
        p.setBusinessLicenseAvailable(r.businessLicenseAvailable()); p.setIncorporatedEntity(r.incorporatedEntity()); p.setPanApplicable(r.panApplicable()); p.setSection8TaxRegistrationApplicable(r.section8TaxRegistrationApplicable());
        if("PROFILE_INCOMPLETE".equals(p.getOnboardingStatus())) p.setOnboardingStatus("DOCUMENTS_PENDING");
        jdbc.update("DELETE FROM bidder_sectors WHERE organization_id=?",org.getId());
        for(String sector:all) jdbc.update("INSERT INTO bidder_sectors(organization_id,sector_code,is_primary) VALUES (?,?,?)",org.getId(),sector,sector.equals(primary));
        jdbc.update("DELETE FROM bidder_capabilities WHERE organization_id=?",org.getId());
        if(r.capabilities()!=null) for(String c:r.capabilities()){String v=c.trim();if(!v.isBlank())jdbc.update("INSERT INTO bidder_capabilities(organization_id,capability,normalized_capability) VALUES (?,?,?) ON CONFLICT (organization_id,normalized_capability) DO NOTHING",org.getId(),v,v.toLowerCase(Locale.ROOT));}
        if(r.turnovers()!=null) for(var t:r.turnovers()) jdbc.update("INSERT INTO bidder_financials(organization_id,financial_year,annual_turnover) VALUES (?,?,?) ON CONFLICT (organization_id,financial_year) DO UPDATE SET annual_turnover=excluded.annual_turnover",org.getId(),t.financialYear(),t.annualTurnover());
        return response(org,p);
    }

    @Transactional(readOnly=true)
    public BidderOnboardingResponse get() {
        var actor=CurrentUser.get(); if(actor==null||!"BIDDER".equals(actor.role())) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Bidder account required");
        Organization o=organizations.findById(actor.organizationId()).orElseThrow(); BidderProfile p=profiles.findByOrganizationId(o.getId()).orElseThrow(); return response(o,p);
    }

    @Transactional
    public BidderOnboardingResponse submit() {
        var actor=CurrentUser.get(); if(actor==null||!"BIDDER".equals(actor.role())) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Bidder account required");
        Organization o=organizations.findById(actor.organizationId()).orElseThrow(); BidderProfile p=profiles.findByOrganizationId(o.getId()).orElseThrow();
        if(p.getEntityType()==null) throw new ResponseStatusException(HttpStatus.CONFLICT,"Complete company details before submitting");
        List<DocumentChecklistItem> checklist=docs.checklist(); List<String> missing=checklist.stream().filter(i->i.required()&&!i.uploaded()&&i.alternativeGroup()==null).map(DocumentChecklistItem::displayName).toList();
        Map<String,List<DocumentChecklistItem>> alternatives=new HashMap<>(); checklist.stream().filter(i->i.alternativeGroup()!=null).forEach(i->alternatives.computeIfAbsent(i.alternativeGroup(),k->new ArrayList<>()).add(i));
        for(var group:alternatives.entrySet()) if(group.getValue().stream().anyMatch(DocumentChecklistItem::required)&&group.getValue().stream().noneMatch(DocumentChecklistItem::uploaded)) missing.add("One of: "+group.getValue().stream().map(DocumentChecklistItem::displayName).reduce((a,b)->a+", "+b).orElse(group.getKey()));
        if(!missing.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Required documents are missing: "+String.join("; ",missing));
        p.setOnboardingStatus("VERIFICATION_PENDING");
        jdbc.update("INSERT INTO document_audit_events(actor_id,organization_id,event_type,outcome) VALUES (?,?, 'BIDDER_ONBOARDING_SUBMITTED','SUCCESS')",actor.id(),o.getId());
        return response(o,p);
    }

    private BidderOnboardingResponse response(Organization o,BidderProfile p) {
        List<String> sectors=jdbc.queryForList("SELECT sector_code FROM bidder_sectors WHERE organization_id=? AND is_primary=false ORDER BY sector_code",String.class,o.getId());
        String primary=jdbc.query("SELECT sector_code FROM bidder_sectors WHERE organization_id=? AND is_primary=true",rs->rs.next()?rs.getString(1):null,o.getId());
        List<String> caps=jdbc.queryForList("SELECT capability FROM bidder_capabilities WHERE organization_id=? ORDER BY capability",String.class,o.getId());
        List<BidderOnboardingResponse.Turnover> turnovers=jdbc.query("SELECT financial_year,annual_turnover FROM bidder_financials WHERE organization_id=? ORDER BY financial_year DESC",(rs,n)->new BidderOnboardingResponse.Turnover(rs.getString(1),rs.getBigDecimal(2)),o.getId());
        return new BidderOnboardingResponse(o.getName(),p.getEntityType(),o.getRegistrationNumber(),p.getBusinessAddress(),p.getContactNumber(),p.getPanNumber(),p.getGstin(),p.isGstApplicable(),primary,sectors,caps,p.getEstablishmentDate(),p.getExperienceYears(),p.getCompletedProjects(),turnovers,p.getAuthorizedSignatoryName(),p.getAuthorizedSignatoryEmail(),p.getOnboardingStatus(),docs.checklist(),p.getCin(),p.getLlpin(),p.isGovernmentOwned(),p.isFirmRegistered(),p.isRepresentativeIsSignatory(),p.isMsmeBenefitClaimed(),p.isShopRegistrationApplicable(),p.isBusinessLicenseAvailable(),p.isIncorporatedEntity(),p.isPanApplicable(),p.isSection8TaxRegistrationApplicable());
    }
    private static String canonicalType(String t){return switch(t==null?"":t){case "PROPRIETORSHIP"->"SOLE_PROPRIETORSHIP";case "PRIVATE_LIMITED","PUBLIC_LIMITED","OPC","LLP","PARTNERSHIP","SOLE_PROPRIETORSHIP","SECTION_8","GOVERNMENT_ENTITY"->t;default->t;};}
    private static String validSector(String s){if(s==null||!SectorTaxonomy.contains(s))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid supplier sector");return s;}
}
