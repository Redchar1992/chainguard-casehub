package com.chainguard.risk.dto;

import java.util.List;

public record WalletRiskResponse(
        String walletAddress,
        int riskScore,
        String riskLevel,
        List<TriggeredRule> triggeredRules,
        boolean cached
) {}
