package com.chainguard.ai.service;

import com.chainguard.ai.dto.AiSummaryRequest;
import com.chainguard.ai.dto.AiSummaryResponse;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class AiInvestigationService {

    public AiSummaryResponse generateSummary(AiSummaryRequest request) {
        // MVP deterministic mock. Replace with provider-specific AI model API call.
        List<String> factors = new ArrayList<>();
        if (request.triggeredRules() != null && !request.triggeredRules().isEmpty()) {
            factors.addAll(request.triggeredRules());
        } else {
            factors.add("No high-severity rule was triggered, but analyst review is still recommended.");
        }

        String summary = "Wallet " + request.walletAddress()
                + " is classified as " + request.riskLevel()
                + " with risk score " + request.riskScore()
                + ". The case should be reviewed based on triggered AML rules and available transaction evidence.";

        return new AiSummaryResponse(
                summary,
                factors,
                List.of(
                        "Review wallet transaction timeline and counterparties.",
                        "Check exposure to blacklisted or high-risk entities.",
                        "Escalate to reviewer if funds source remains unclear."
                ),
                request.riskScore() >= 70 ? "HIGH" : "MEDIUM"
        );
    }
}
