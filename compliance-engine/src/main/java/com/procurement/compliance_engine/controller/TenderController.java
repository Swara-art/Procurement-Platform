package com.procurement.compliance_engine.controller;

import com.procurement.compliance_engine.dto.CreateTenderRequest;
import com.procurement.compliance_engine.dto.TenderResponse;
import com.procurement.compliance_engine.service.TenderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tenders")
public class TenderController {

    private final TenderService service;

    public TenderController(TenderService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TenderResponse create(@Valid @RequestBody CreateTenderRequest request) {
        return service.createTender(request);
    }

    @GetMapping
    public List<TenderResponse> getAll(@RequestParam(required = false) String status) {
        return service.getAllTenders(status);
    }

    @GetMapping("/{id}")
    public TenderResponse getById(@PathVariable UUID id) {
        return service.getTenderById(id);
    }

    @PatchMapping("/{id}/publish")
    public TenderResponse publishTender(@PathVariable UUID id) {
        return service.publishTender(id);
    }
}
