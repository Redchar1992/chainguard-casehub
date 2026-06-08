package com.chainguard.risk.dto;

import java.io.Serializable;
import java.util.List;

public record WalletRiskResponse(
        String walletAddress,
        int riskScore,
        String riskLevel,
        List<TriggeredRule> triggeredRules,
        boolean cached
) implements Serializable {
    public WalletRiskResponse asCached() {
        return new WalletRiskResponse(walletAddress, riskScore, riskLevel, triggeredRules, true);
    }
}
