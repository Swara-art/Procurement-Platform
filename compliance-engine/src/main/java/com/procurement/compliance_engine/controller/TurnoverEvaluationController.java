package com.procurement.compliance_engine.controller;

import com.procurement.compliance_engine.dto.TurnoverEvaluationResponse;
import com.procurement.compliance_engine.service.TurnoverEvaluationService;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@Profile("dev & ai")
@RequestMapping("/api/v1/bids")
public class TurnoverEvaluationController {

    private final TurnoverEvaluationService evaluationService;

    public TurnoverEvaluationController(TurnoverEvaluationService evaluationService) {
        this.evaluationService = evaluationService;
    }

    @PostMapping("/{bidId}/evaluate/turnover")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','PROCUREMENT_OFFICER','EVALUATION_OFFICER')")
    public TurnoverEvaluationResponse evaluateTurnover(@PathVariable UUID bidId) {
        return evaluationService.evaluate(bidId);
    }
}
