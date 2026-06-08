package com.chainguard.casehub.rule.dto;

import com.chainguard.casehub.model.RiskLevel;

import java.time.Instant;
import java.util.UUID;

public record AmlRuleResponse(
        UUID id,
        String code,
        String name,
        RiskLevel severity,
        String threshold,
        boolean enabled,
        int version,
        Instant createdAt,
        Instant updatedAt
) {}
