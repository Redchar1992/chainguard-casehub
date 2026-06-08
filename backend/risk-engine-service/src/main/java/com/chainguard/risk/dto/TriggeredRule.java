package com.chainguard.risk.dto;

public record TriggeredRule(
        String code,
        String severity,
        String description,
        int scoreImpact
) {}
