package com.chainguard.ai.provider;

import com.chainguard.ai.dto.AiSummaryRequest;
import com.chainguard.ai.dto.AiSummaryResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@ConditionalOnProperty(name = "ai.provider", havingValue = "mock", matchIfMissing = true)
public class MockAiInvestigationProvider implements AiInvestigationProvider {

    @Override
    public AiSummaryResponse generateSummary(AiSummaryRequest request) {
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
