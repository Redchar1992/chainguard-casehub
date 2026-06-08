package com.chainguard.casehub.controller;

import com.chainguard.casehub.dto.CaseResponse;
import com.chainguard.casehub.dto.CreateCaseRequest;
import com.chainguard.casehub.model.CaseStatus;
import com.chainguard.casehub.service.CaseWorkflowService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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
    public CaseResponse createCase(@Valid @RequestBody CreateCaseRequest request) {
        return service.createCase(request);
    }

    @GetMapping
    public List<CaseResponse> listCases() {
        return service.listCases();
    }

    @GetMapping("/{id}")
    public CaseResponse getCase(@PathVariable UUID id) {
        return service.getCase(id);
    }

    @PatchMapping("/{id}/status")
    public CaseResponse updateStatus(@PathVariable UUID id, @RequestBody Map<String, String> body) {
        return service.updateStatus(id, CaseStatus.valueOf(body.get("status")));
    }

    @GetMapping("/health")
    public String health() {
        return "case-service ok";
    }
}
