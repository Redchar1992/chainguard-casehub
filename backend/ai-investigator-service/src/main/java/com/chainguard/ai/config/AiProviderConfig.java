package com.chainguard.ai.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
@EnableConfigurationProperties(AiProviderProperties.class)
public class AiProviderConfig {

    @Bean
    public RestClient restClient(RestClient.Builder builder, AiProviderProperties properties) {
        int timeoutMillis = properties.timeoutMillis() == null || properties.timeoutMillis() <= 0
                ? 20_000
                : properties.timeoutMillis();
        ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.DEFAULTS
                .withConnectTimeout(Duration.ofSeconds(10))
                .withReadTimeout(Duration.ofMillis(timeoutMillis));
        return builder
                .requestFactory(ClientHttpRequestFactories.get(settings))
                .build();
    }
}
