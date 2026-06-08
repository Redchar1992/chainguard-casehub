package com.chainguard.ai.service;

import com.chainguard.ai.dto.AiSummaryRequest;
import com.chainguard.ai.dto.AiSummaryResponse;
import com.chainguard.ai.provider.MockAiInvestigationProvider;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AiInvestigationServiceTest {

    @Test
    void generateSummaryShouldIncludeRiskContext() {
        AiInvestigationService service = new AiInvestigationService(new MockAiInvestigationProvider());

        AiSummaryResponse response = service.generateSummary(new AiSummaryRequest(
                "0x00new-blacklist-bad0",
                90,
                "CRITICAL",
                List.of("BLACKLIST_EXPOSURE"),
                List.of("Analyst note")
        ));

        assertThat(response.summary()).contains("CRITICAL", "90");
        assertThat(response.riskFactors()).contains("BLACKLIST_EXPOSURE");
        assertThat(response.confidence()).isEqualTo("HIGH");
    }
}
