package com.chainguard.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ai.external")
public record AiProviderProperties(
        String endpoint,
        String apiKey,
        String model
) {}
