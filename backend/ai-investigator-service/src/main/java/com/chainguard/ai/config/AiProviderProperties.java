package com.chainguard.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration for the external LLM provider (Anthropic Messages API).
 *
 * @param baseUrl       API base URL (default https://api.anthropic.com)
 * @param apiKey        API key, read from env (AI_EXTERNAL_API_KEY); never hardcode
 * @param model         model id, e.g. claude-3-5-haiku-latest
 * @param version       Anthropic API version header value
 * @param maxTokens     response token budget
 * @param timeoutMillis HTTP read timeout in milliseconds
 */
@ConfigurationProperties(prefix = "ai.external")
public record AiProviderProperties(
        String baseUrl,
        String apiKey,
        String model,
        String version,
        Integer maxTokens,
        Integer timeoutMillis
) {
    public String baseUrlOrDefault() {
        return baseUrl == null || baseUrl.isBlank() ? "https://api.anthropic.com" : baseUrl;
    }

    public String modelOrDefault() {
        return model == null || model.isBlank() ? "claude-3-5-haiku-latest" : model;
    }

    public String versionOrDefault() {
        return version == null || version.isBlank() ? "2023-06-01" : version;
    }

    public int maxTokensOrDefault() {
        return maxTokens == null || maxTokens <= 0 ? 1024 : maxTokens;
    }

    public boolean hasApiKey() {
        return apiKey != null && !apiKey.isBlank();
    }
}
