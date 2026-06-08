package com.chainguard.casehub.dto;

import com.chainguard.casehub.model.CaseStatus;
import com.chainguard.casehub.model.RiskLevel;

import java.time.Instant;
import java.util.UUID;

public record CaseResponse(
        UUID id,
        String walletAddress,
        String title,
        CaseStatus status,
        int riskScore,
        RiskLevel riskLevel,
        String assignee,
        Instant createdAt,
        Instant updatedAt
) {}
