package com.chainguard.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record AiSummaryRequest(
        @NotBlank String walletAddress,
        @NotNull Integer riskScore,
        @NotBlank String riskLevel,
        List<String> triggeredRules,
        List<String> analystNotes
) {}
