package com.chainguard.casehub.rule;

import com.chainguard.casehub.rule.dto.AmlRuleRequest;
import com.chainguard.casehub.rule.dto.AmlRuleResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/rules")
public class RuleController {
    private final RuleManagementService service;

    public RuleController(RuleManagementService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ANALYST', 'REVIEWER', 'ADMIN')")
    public List<AmlRuleResponse> listRules() {
        return service.listRules();
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public AmlRuleResponse createRule(@Valid @RequestBody AmlRuleRequest request) {
        return service.createRule(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public AmlRuleResponse updateRule(@PathVariable UUID id, @Valid @RequestBody AmlRuleRequest request) {
        return service.updateRule(id, request);
    }

    @PatchMapping("/{id}/enabled")
    @PreAuthorize("hasRole('ADMIN')")
    public AmlRuleResponse setEnabled(@PathVariable UUID id, @RequestBody Map<String, Boolean> body) {
        return service.setEnabled(id, Boolean.TRUE.equals(body.get("enabled")));
    }
}
