package com.procurement.compliance_engine.controller;

import com.procurement.compliance_engine.dto.BusinessLegalTypeResponse;
import com.procurement.compliance_engine.dto.LegalDocumentRequirementResponse;
import com.procurement.compliance_engine.service.BusinessLegalTypeService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/company-types")
public class BusinessLegalTypeController {
    private final BusinessLegalTypeService service;
    public BusinessLegalTypeController(BusinessLegalTypeService service) { this.service = service; }
    @GetMapping public List<BusinessLegalTypeResponse> list() { return service.list(); }
    @GetMapping("/{type}/required-documents") public List<LegalDocumentRequirementResponse> requirements(@PathVariable String type) { return service.requirements(type); }
}
