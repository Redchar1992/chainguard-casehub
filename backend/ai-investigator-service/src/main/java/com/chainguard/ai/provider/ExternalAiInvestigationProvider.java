package com.chainguard.ai.provider;

import com.chainguard.ai.config.AiProviderProperties;
import com.chainguard.ai.dto.AiSummaryRequest;
import com.chainguard.ai.dto.AiSummaryResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Real LLM-backed investigation provider using the Anthropic Messages API
 * (POST {baseUrl}/v1/messages). Builds a structured prompt from the wallet's
 * risk signals, calls the model, and parses the JSON content into
 * {@link AiSummaryResponse}. Any failure (missing key, network, malformed
 * output) falls back to {@link MockAiInvestigationProvider} so the compliance
 * workflow is never blocked by the AI layer.
 *
 * <p>Activated with {@code ai.provider=external}. The API key is read from the
 * {@code AI_EXTERNAL_API_KEY} environment variable; it is never hardcoded.
 */
@Component
@Primary
@ConditionalOnProperty(name = "ai.provider", havingValue = "external")
public class ExternalAiInvestigationProvider implements AiInvestigationProvider {
    private static final Logger log = LoggerFactory.getLogger(ExternalAiInvestigationProvider.class);

    private final RestClient restClient;
    private final AiProviderProperties properties;
    private final ObjectMapper objectMapper;
    private final MockAiInvestigationProvider fallbackProvider;

    public ExternalAiInvestigationProvider(
            RestClient restClient,
            AiProviderProperties properties,
            ObjectMapper objectMapper,
            MockAiInvestigationProvider fallbackProvider
    ) {
        this.restClient = restClient;
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.fallbackProvider = fallbackProvider;
    }

    @Override
    public AiSummaryResponse generateSummary(AiSummaryRequest request) {
        if (!properties.hasApiKey()) {
            log.warn("AI_EXTERNAL_API_KEY is not set; falling back to the offline mock provider.");
            return fallbackProvider.generateSummary(request);
        }

        try {
            JsonNode body = restClient.post()
                    .uri(properties.baseUrlOrDefault() + "/v1/messages")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("x-api-key", properties.apiKey())
                    .header("anthropic-version", properties.versionOrDefault())
                    .body(buildPayload(request))
                    .retrieve()
                    .body(JsonNode.class);

            AiSummaryResponse parsed = parseResponse(body);
            return parsed != null ? parsed : fallbackProvider.generateSummary(request);
        } catch (Exception ex) {
            // AI provider failures must not block the compliance workflow.
            log.warn("External AI provider call failed ({}); using mock fallback.", ex.getMessage());
            return fallbackProvider.generateSummary(request);
        }
    }

    private Map<String, Object> buildPayload(AiSummaryRequest request) {
        return Map.of(
                "model", properties.modelOrDefault(),
                "max_tokens", properties.maxTokensOrDefault(),
                "system", InvestigationPromptBuilder.systemPrompt(),
                "messages", List.of(Map.of(
                        "role", "user",
                        "content", InvestigationPromptBuilder.userPrompt(request)
                ))
        );
    }

    /**
     * Parses the Anthropic Messages response: the model's text lives in
     * {@code content[].text}; that text is itself the JSON object we asked for.
     */
    private AiSummaryResponse parseResponse(JsonNode body) {
        if (body == null) {
            return null;
        }
        JsonNode content = body.path("content");
        if (!content.isArray() || content.isEmpty()) {
            return null;
        }
        StringBuilder text = new StringBuilder();
        for (JsonNode block : content) {
            if ("text".equals(block.path("type").asText())) {
                text.append(block.path("text").asText());
            }
        }
        String json = extractJsonObject(text.toString());
        if (json == null) {
            return null;
        }
        try {
            JsonNode parsed = objectMapper.readTree(json);
            String summary = parsed.path("summary").asText("");
            if (summary.isBlank()) {
                return null;
            }
            return new AiSummaryResponse(
                    summary,
                    toList(parsed.path("riskFactors")),
                    toList(parsed.path("recommendedActions")),
                    normalizeConfidence(parsed.path("confidence").asText("MEDIUM"))
            );
        } catch (Exception ex) {
            log.warn("Failed to parse model JSON output: {}", ex.getMessage());
            return null;
        }
    }

    /** Defensively extract the first JSON object even if the model wraps it. */
    private String extractJsonObject(String text) {
        if (text == null) {
            return null;
        }
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start < 0 || end <= start) {
            return null;
        }
        return text.substring(start, end + 1);
    }

    private List<String> toList(JsonNode node) {
        List<String> values = new ArrayList<>();
        if (node != null && node.isArray()) {
            node.forEach(item -> values.add(item.asText()));
        }
        return values;
    }

    private String normalizeConfidence(String confidence) {
        String upper = confidence == null ? "MEDIUM" : confidence.trim().toUpperCase();
        return switch (upper) {
            case "LOW", "MEDIUM", "HIGH" -> upper;
            default -> "MEDIUM";
        };
    }
}
