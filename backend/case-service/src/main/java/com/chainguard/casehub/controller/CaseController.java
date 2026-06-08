package com.chainguard.casehub.controller;

import com.chainguard.casehub.dto.CaseResponse;
import com.chainguard.casehub.dto.CreateCaseRequest;
import com.chainguard.casehub.model.CaseStatus;
import com.chainguard.casehub.service.CaseWorkflowService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/cases")
public class CaseController {
    private final CaseWorkflowService service;

    public CaseController(CaseWorkflowService service) {
        this.service = service;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ANALYST', 'ADMIN')")
    public CaseResponse createCase(@Valid @RequestBody CreateCaseRequest request) {
        return service.createCase(request);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ANALYST', 'REVIEWER', 'ADMIN')")
    public List<CaseResponse> listCases(
            @RequestParam(required = false) CaseStatus status,
            @RequestParam(required = false) String walletAddress
    ) {
        return service.listCases(status, walletAddress);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ANALYST', 'REVIEWER', 'ADMIN')")
    public CaseResponse getCase(@PathVariable UUID id) {
        return service.getCase(id);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ANALYST', 'REVIEWER', 'ADMIN')")
    public CaseResponse updateStatus(@PathVariable UUID id, @RequestBody Map<String, String> body) {
        String status = body == null ? null : body.get("status");
        if (status == null || status.isBlank()) {
            throw new IllegalArgumentException("Field 'status' is required");
        }
        return service.updateStatus(id, parseStatus(status));
    }

    private CaseStatus parseStatus(String status) {
        try {
            return CaseStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Unknown case status: " + status);
        }
    }

    @GetMapping("/health")
    public String health() {
        return "case-service ok";
    }
}
