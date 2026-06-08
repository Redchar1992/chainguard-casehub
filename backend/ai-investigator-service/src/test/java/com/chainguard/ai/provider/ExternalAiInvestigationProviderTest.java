package com.chainguard.ai.provider;

import com.chainguard.ai.config.AiProviderProperties;
import com.chainguard.ai.dto.AiSummaryRequest;
import com.chainguard.ai.dto.AiSummaryResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ExternalAiInvestigationProviderTest {

    private static final AiSummaryRequest REQUEST = new AiSummaryRequest(
            "0x00new-blacklist-bad0",
            90,
            "CRITICAL",
            List.of("BLACKLIST_EXPOSURE", "NEW_ADDRESS_LARGE_WITHDRAWAL"),
            List.of("Analyst flagged rapid movement")
    );

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void parsesAnthropicMessagesResponseIntoDto() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        String modelJson = "{\"summary\":\"Wallet shows blacklist exposure and a large early withdrawal.\","
                + "\"riskFactors\":[\"Blacklist counterparty\",\"New-address large withdrawal\"],"
                + "\"recommendedActions\":[\"Escalate to reviewer\"],\"confidence\":\"high\"}";
        String anthropicBody = "{\"id\":\"msg_1\",\"type\":\"message\",\"role\":\"assistant\","
                + "\"content\":[{\"type\":\"text\",\"text\":" + objectMapper.valueToTree(modelJson) + "}]}";

        server.expect(requestTo("https://api.anthropic.com/v1/messages"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andExpect(header("x-api-key", "test-key"))
                .andExpect(header("anthropic-version", "2023-06-01"))
                .andRespond(withSuccess(anthropicBody, MediaType.APPLICATION_JSON));

        ExternalAiInvestigationProvider provider = provider(builder, "test-key");
        AiSummaryResponse response = provider.generateSummary(REQUEST);

        server.verify();
        assertThat(response.summary()).contains("blacklist exposure");
        assertThat(response.riskFactors()).contains("Blacklist counterparty");
        assertThat(response.recommendedActions()).contains("Escalate to reviewer");
        assertThat(response.confidence()).isEqualTo("HIGH");
    }

    @Test
    void fallsBackToMockWhenApiKeyMissing() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        // No request expected: the provider must short-circuit to the mock.

        ExternalAiInvestigationProvider provider = provider(builder, "");
        AiSummaryResponse response = provider.generateSummary(REQUEST);

        server.verify();
        assertThat(response.summary()).contains("CRITICAL", "90");
        assertThat(response.riskFactors()).contains("BLACKLIST_EXPOSURE");
    }

    @Test
    void fallsBackToMockOnHttpError() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://api.anthropic.com/v1/messages"))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        ExternalAiInvestigationProvider provider = provider(builder, "test-key");
        AiSummaryResponse response = provider.generateSummary(REQUEST);

        server.verify();
        // Mock fallback summary still references the risk context.
        assertThat(response.summary()).contains("CRITICAL");
    }

    private ExternalAiInvestigationProvider provider(RestClient.Builder builder, String apiKey) {
        AiProviderProperties properties = new AiProviderProperties(
                "https://api.anthropic.com", apiKey, "claude-3-5-haiku-latest", "2023-06-01", 1024, 20000);
        return new ExternalAiInvestigationProvider(
                builder.build(), properties, objectMapper, new MockAiInvestigationProvider());
    }
}
