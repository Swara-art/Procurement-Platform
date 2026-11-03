package com.procurement.compliance_engine.controller;
import com.procurement.compliance_engine.service.TenderClassificationService;
import com.procurement.compliance_engine.service.TenderService;
import org.springframework.http.HttpStatus;import org.springframework.security.access.prepost.PreAuthorize;import org.springframework.web.bind.annotation.*;import java.util.*;
@RestController @RequestMapping("/api/v1/tenders/{tenderId}")
public class TenderClassificationController {
 private final TenderService tenders;private final TenderClassificationService classification;
 public TenderClassificationController(TenderService t,TenderClassificationService c){this.tenders=t;this.classification=c;}
 @PostMapping("/classify") @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','PROCUREMENT_OFFICER')") @ResponseStatus(HttpStatus.ACCEPTED) public Map<String,Object> classify(@PathVariable UUID tenderId){var tender=tenders.getTenderEntity(tenderId);return Map.of("jobId",classification.enqueue(tender,false),"status","PENDING");}
 @PostMapping("/reclassify") @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','PROCUREMENT_OFFICER')") @ResponseStatus(HttpStatus.ACCEPTED) public Map<String,Object> reclassify(@PathVariable UUID tenderId){var tender=tenders.getTenderEntity(tenderId);return Map.of("jobId",classification.enqueue(tender,true),"status","PENDING");}
 @GetMapping("/classification") public Map<String,Object> get(@PathVariable UUID tenderId){tenders.getTenderEntity(tenderId);return classification.get(tenderId);}
 @PutMapping("/classification/confirm") @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','PROCUREMENT_OFFICER')") public Map<String,Object> confirm(@PathVariable UUID tenderId,@RequestBody ConfirmRequest body){tenders.getTenderEntity(tenderId);return classification.confirm(tenderId,body.sectors());}
 public record ConfirmRequest(List<String> sectors){}
}
