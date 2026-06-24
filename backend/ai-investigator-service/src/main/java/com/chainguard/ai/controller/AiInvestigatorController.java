package com.chainguard.ai.controller;

import com.chainguard.ai.dto.AiSummaryRequest;
import com.chainguard.ai.dto.AiSummaryResponse;
import com.chainguard.ai.service.AiInvestigationService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/ai")
public class AiInvestigatorController {
    private final AiInvestigationService service;

    public AiInvestigatorController(AiInvestigationService service) {
        this.service = service;
    }

    @PostMapping("/cases/{caseId}/summary")
    @PreAuthorize("hasAnyRole('ANALYST', 'REVIEWER', 'ADMIN')")
    public AiSummaryResponse generateCaseSummary(
            @PathVariable UUID caseId,
            @Valid @RequestBody AiSummaryRequest request
    ) {
        return service.generateSummary(caseId, request);
    }

    @GetMapping("/health")
    public String health() {
        return "ai-investigator-service ok";
    }
}
