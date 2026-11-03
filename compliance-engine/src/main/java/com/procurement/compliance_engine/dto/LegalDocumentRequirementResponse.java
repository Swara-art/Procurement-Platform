package com.procurement.compliance_engine.dto;
public record LegalDocumentRequirementResponse(String documentType,String displayName,String description,String requirementLevel,String conditionExpression,String alternativeGroup,int sortOrder) {}
