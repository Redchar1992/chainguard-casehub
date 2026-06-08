package com.chainguard.casehub.rule.dto;

import com.chainguard.casehub.model.RiskLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AmlRuleRequest(
        @NotBlank String code,
        @NotBlank String name,
        @NotNull RiskLevel severity,
        @NotBlank String threshold,
        boolean enabled
) {}
