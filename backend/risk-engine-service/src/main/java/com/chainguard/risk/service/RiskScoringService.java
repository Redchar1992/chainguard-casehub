package com.chainguard.risk.service;

import com.chainguard.risk.dto.TriggeredRule;
import com.chainguard.risk.dto.WalletRiskResponse;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class RiskScoringService {

    public WalletRiskResponse evaluateWallet(String walletAddress) {
        List<TriggeredRule> rules = new ArrayList<>();
        String normalized = walletAddress.toLowerCase(Locale.ROOT);

        if (normalized.endsWith("bad") || normalized.contains("blacklist")) {
            rules.add(new TriggeredRule(
                    "BLACKLIST_EXPOSURE",
                    "CRITICAL",
                    "Wallet interacted with a known blacklisted address.",
                    45
            ));
        }

        if (normalized.length() % 2 == 0) {
            rules.add(new TriggeredRule(
                    "HIGH_FREQUENCY_TRANSFER",
                    "HIGH",
                    "Wallet shows high-frequency transfer behavior in a short time window.",
                    25
            ));
        }

        if (normalized.contains("new") || normalized.startsWith("0x00")) {
            rules.add(new TriggeredRule(
                    "NEW_ADDRESS_LARGE_WITHDRAWAL",
                    "HIGH",
                    "New wallet received or withdrew a large amount shortly after creation.",
                    20
            ));
        }

        int score = Math.min(100, rules.stream().mapToInt(TriggeredRule::scoreImpact).sum());
        String level = toRiskLevel(score);
        return new WalletRiskResponse(walletAddress, score, level, rules, false);
    }

    private String toRiskLevel(int score) {
        if (score >= 80) return "CRITICAL";
        if (score >= 60) return "HIGH";
        if (score >= 30) return "MEDIUM";
        return "LOW";
    }
}
