package com.chainguard.casehub.dto;

import com.chainguard.casehub.model.RiskLevel;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateCaseRequest(
        @NotBlank String walletAddress,
        @NotBlank String title,
        @Min(0) @Max(100) int riskScore,
        @NotNull RiskLevel riskLevel
) {}
