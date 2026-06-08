package com.chainguard.casehub.rule;

import com.chainguard.casehub.audit.AuditService;
import com.chainguard.casehub.rule.dto.AmlRuleRequest;
import com.chainguard.casehub.rule.dto.AmlRuleResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class RuleManagementService {
    private final AmlRuleRepository repository;
    private final ObjectMapper objectMapper;
    private final AuditService auditService;

    public RuleManagementService(AmlRuleRepository repository, ObjectMapper objectMapper, AuditService auditService) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<AmlRuleResponse> listRules() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional
    public AmlRuleResponse createRule(AmlRuleRequest request) {
        if (repository.existsByCode(request.code())) {
            throw new IllegalArgumentException("AML rule already exists: " + request.code());
        }
        AmlRule rule = new AmlRule(
                request.code(),
                request.name(),
                request.severity(),
                normalizeJson(request.threshold()),
                request.enabled()
        );
        return toResponse(repository.save(rule));
    }

    @Transactional
    public AmlRuleResponse updateRule(UUID id, AmlRuleRequest request) {
        AmlRule rule = findRule(id);
        rule.update(request.name(), request.severity(), normalizeJson(request.threshold()), request.enabled());
        return toResponse(repository.save(rule));
    }

    @Transactional
    public AmlRuleResponse setEnabled(UUID id, boolean enabled) {
        AmlRule rule = findRule(id);
        rule.setEnabled(enabled);
        AmlRuleResponse response = toResponse(repository.save(rule));
        auditService.record(
                "AML_RULE_TOGGLE",
                "AML_RULE",
                id.toString(),
                Map.of("code", rule.getCode(), "enabled", enabled, "version", rule.getVersion())
        );
        return response;
    }

    private AmlRule findRule(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new com.chainguard.casehub.service.ResourceNotFoundException("AML rule not found: " + id));
    }

    private String normalizeJson(String json) {
        try {
            Map<?, ?> parsed = objectMapper.readValue(json, Map.class);
            return objectMapper.writeValueAsString(parsed);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Rule threshold must be a valid JSON object");
        }
    }

    private AmlRuleResponse toResponse(AmlRule rule) {
        return new AmlRuleResponse(
                rule.getId(),
                rule.getCode(),
                rule.getName(),
                rule.getSeverity(),
                rule.getThreshold(),
                rule.isEnabled(),
                rule.getVersion(),
                rule.getCreatedAt(),
                rule.getUpdatedAt()
        );
    }
}
