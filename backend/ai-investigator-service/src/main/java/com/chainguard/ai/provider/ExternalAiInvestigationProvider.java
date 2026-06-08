package com.chainguard.ai.provider;

import com.chainguard.ai.config.AiProviderProperties;
import com.chainguard.ai.dto.AiSummaryRequest;
import com.chainguard.ai.dto.AiSummaryResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "ai.provider", havingValue = "external")
public class ExternalAiInvestigationProvider implements AiInvestigationProvider {
    private final RestClient restClient;
    private final AiProviderProperties properties;
    private final MockAiInvestigationProvider fallbackProvider = new MockAiInvestigationProvider();

    public ExternalAiInvestigationProvider(RestClient restClient, AiProviderProperties properties) {
        this.restClient = restClient;
        this.properties = properties;
    }

    @Override
    public AiSummaryResponse generateSummary(AiSummaryRequest request) {
        if (properties.endpoint() == null || properties.endpoint().isBlank()) {
            return fallbackProvider.generateSummary(request);
        }

        try {
            ExternalAiResponse response = restClient.post()
                    .uri(properties.endpoint())
                    .contentType(MediaType.APPLICATION_JSON)
                    .headers(headers -> {
                        if (properties.apiKey() != null && !properties.apiKey().isBlank()) {
                            headers.setBearerAuth(properties.apiKey());
                        }
                    })
                    .body(buildPayload(request))
                    .retrieve()
                    .body(ExternalAiResponse.class);

            if (response == null || response.summary() == null || response.summary().isBlank()) {
                return fallbackProvider.generateSummary(request);
            }

            return new AiSummaryResponse(
                    response.summary(),
                    emptyIfNull(response.riskFactors()),
                    emptyIfNull(response.recommendedActions()),
                    response.confidence() == null ? "MEDIUM" : response.confidence()
            );
        } catch (Exception ignored) {
            // AI provider failures must not block compliance workflow.
            return fallbackProvider.generateSummary(request);
        }
    }

    private Map<String, Object> buildPayload(AiSummaryRequest request) {
        return Map.of(
                "model", properties.model() == null ? "default" : properties.model(),
                "task", "crypto_compliance_investigation_summary",
                "walletAddress", request.walletAddress(),
                "riskScore", request.riskScore(),
                "riskLevel", request.riskLevel(),
                "triggeredRules", request.triggeredRules() == null ? List.of() : request.triggeredRules(),
                "analystNotes", request.analystNotes() == null ? List.of() : request.analystNotes(),
                "responseSchema", Map.of(
                        "summary", "string",
                        "riskFactors", "string[]",
                        "recommendedActions", "string[]",
                        "confidence", "LOW|MEDIUM|HIGH"
                )
        );
    }

    private List<String> emptyIfNull(List<String> values) {
        return values == null ? List.of() : values;
    }

    private record ExternalAiResponse(
            String summary,
            List<String> riskFactors,
            List<String> recommendedActions,
            String confidence
    ) {}
}
