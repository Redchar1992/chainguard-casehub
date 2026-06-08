package com.chainguard.ai.dto;

import java.util.List;

public record AiSummaryResponse(
        String summary,
        List<String> riskFactors,
        List<String> recommendedActions,
        String confidence
) {}
