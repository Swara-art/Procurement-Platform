package com.procurement.compliance_engine.controller;

import com.procurement.compliance_engine.dto.DocumentChecklistItem;
import com.procurement.compliance_engine.dto.OrganizationDocumentResponse;
import com.procurement.compliance_engine.service.OrganizationDocumentService;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController @RequestMapping("/api/v1/bidder/documents")
public class OrganizationDocumentController {
 private final OrganizationDocumentService service;
 public OrganizationDocumentController(OrganizationDocumentService service){this.service=service;}
 @GetMapping @PreAuthorize("hasRole('BIDDER')") public List<OrganizationDocumentResponse> list(){return service.list();}
 @GetMapping("/checklist") @PreAuthorize("hasRole('BIDDER')") public List<DocumentChecklistItem> checklist(){return service.checklist();}
 @PostMapping(value="/registration",consumes=MediaType.MULTIPART_FORM_DATA_VALUE) @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasRole('BIDDER')") public OrganizationDocumentResponse registration(@RequestParam("file") MultipartFile f){return service.upload("BUSINESS_REGISTRATION",null,f);}
 @PostMapping(value="/pan",consumes=MediaType.MULTIPART_FORM_DATA_VALUE) @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasRole('BIDDER')") public OrganizationDocumentResponse pan(@RequestParam("file") MultipartFile f){return service.upload("PAN",null,f);}
 @PostMapping(value="/gst",consumes=MediaType.MULTIPART_FORM_DATA_VALUE) @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasRole('BIDDER')") public OrganizationDocumentResponse gst(@RequestParam("file") MultipartFile f){return service.upload("GST_CERTIFICATE",null,f);}
 @PostMapping(value="/authorization",consumes=MediaType.MULTIPART_FORM_DATA_VALUE) @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasRole('BIDDER')") public OrganizationDocumentResponse authorization(@RequestParam("file") MultipartFile f){return service.upload("SIGNATORY_AUTHORIZATION",null,f);}
 @PostMapping(value="/address-proof",consumes=MediaType.MULTIPART_FORM_DATA_VALUE) @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasRole('BIDDER')") public OrganizationDocumentResponse address(@RequestParam("file") MultipartFile f){return service.upload("ADDRESS_PROOF",null,f);}
 @PostMapping(value="/financials/{financialYear}",consumes=MediaType.MULTIPART_FORM_DATA_VALUE) @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasRole('BIDDER')") public OrganizationDocumentResponse financial(@PathVariable String financialYear,@RequestParam("file") MultipartFile f){return service.upload("FINANCIAL_STATEMENT",financialYear,f);}
 @PostMapping(value="/experience",consumes=MediaType.MULTIPART_FORM_DATA_VALUE) @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasRole('BIDDER')") public OrganizationDocumentResponse experience(@RequestParam("file") MultipartFile f){return service.upload("EXPERIENCE_PROOF",null,f);}
  @PostMapping(value="/certifications",consumes=MediaType.MULTIPART_FORM_DATA_VALUE) @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasRole('BIDDER')") public OrganizationDocumentResponse certification(@RequestParam(value="type",defaultValue="ISO_CERTIFICATE") String type,@RequestParam("file") MultipartFile f){return service.upload(type,null,f);}
 @PostMapping(value="/type/{type}",consumes=MediaType.MULTIPART_FORM_DATA_VALUE) @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasRole('BIDDER')") public OrganizationDocumentResponse typed(@PathVariable String type,@RequestParam(value="financialYear",required=false) String financialYear,@RequestParam("file") MultipartFile f){return service.upload(type,financialYear,f);}
 @PostMapping(value="/{type}",consumes=MediaType.MULTIPART_FORM_DATA_VALUE) @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasRole('BIDDER')") public OrganizationDocumentResponse conditional(@PathVariable String type,@RequestParam("file") MultipartFile f){return service.upload(type,null,f);}
 @GetMapping("/{id}/content") @PreAuthorize("hasAnyRole('BIDDER','SYSTEM_ADMIN')") public ResponseEntity<byte[]> download(@PathVariable UUID id){return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF).header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=\"document.pdf\"").body(service.download(id));}
 @PatchMapping("/{id}/review") @PreAuthorize("hasRole('SYSTEM_ADMIN')") public ResponseEntity<Void> review(@PathVariable UUID id,@RequestBody Map<String,String> body){service.review(id,body.get("status"),body.get("reason"));return ResponseEntity.noContent().build();}
}
