package com.chainguard.ai.provider;

import com.chainguard.ai.dto.AiSummaryRequest;
import com.chainguard.ai.dto.AiSummaryResponse;

public interface AiInvestigationProvider {
    AiSummaryResponse generateSummary(AiSummaryRequest request);
}
