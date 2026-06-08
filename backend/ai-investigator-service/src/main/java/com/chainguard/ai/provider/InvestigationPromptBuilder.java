package com.chainguard.ai.provider;

import com.chainguard.ai.dto.AiSummaryRequest;

import java.util.List;

/**
 * Builds the system and user prompts for the AML investigation summary. The
 * prompts embed the wallet's real risk signals and constrain the model to emit
 * a strict JSON object that maps 1:1 onto {@code AiSummaryResponse}.
 */
public final class InvestigationPromptBuilder {

    private InvestigationPromptBuilder() {
    }

    public static String systemPrompt() {
        return """
                You are a crypto AML compliance investigation assistant for an exchange.
                You receive a wallet's risk signals and produce a concise investigation draft.
                You never make a final compliance decision; you support a human analyst.
                Respond with ONLY a single JSON object, no markdown, no prose, matching:
                {
                  "summary": string,
                  "riskFactors": string[],
                  "recommendedActions": string[],
                  "confidence": "LOW" | "MEDIUM" | "HIGH"
                }
                Base confidence on the strength and number of triggered rules.
                """;
    }

    public static String userPrompt(AiSummaryRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append("Wallet address: ").append(request.walletAddress()).append('\n');
        sb.append("Risk score (0-100): ").append(request.riskScore()).append('\n');
        sb.append("Risk level: ").append(request.riskLevel()).append('\n');
        sb.append("Triggered AML rules:\n").append(bullets(request.triggeredRules(), "none"));
        sb.append("Analyst notes:\n").append(bullets(request.analystNotes(), "none"));
        sb.append("\nWrite the investigation summary as JSON only.");
        return sb.toString();
    }

    private static String bullets(List<String> values, String emptyText) {
        if (values == null || values.isEmpty()) {
            return "  - " + emptyText + "\n";
        }
        StringBuilder sb = new StringBuilder();
        for (String value : values) {
            sb.append("  - ").append(value).append('\n');
        }
        return sb.toString();
    }
}
