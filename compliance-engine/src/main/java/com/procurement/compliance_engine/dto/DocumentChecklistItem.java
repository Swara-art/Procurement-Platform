package com.procurement.compliance_engine.dto;
public record DocumentChecklistItem(String documentType,boolean required,boolean uploaded,String status,String policyCondition,String displayName,String requirementLevel,String alternativeGroup) {}
