package com.chainguard.risk.dto;

import java.io.Serializable;

public record TriggeredRule(
        String code,
        String severity,
        String description,
        int scoreImpact
) implements Serializable {}
