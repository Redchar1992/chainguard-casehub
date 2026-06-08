package com.chainguard.ai.service;

import com.chainguard.ai.dto.AiSummaryRequest;
import com.chainguard.ai.dto.AiSummaryResponse;
import com.chainguard.ai.provider.AiInvestigationProvider;
import org.springframework.stereotype.Service;

@Service
public class AiInvestigationService {
    private final AiInvestigationProvider provider;

    public AiInvestigationService(AiInvestigationProvider provider) {
        this.provider = provider;
    }

    public AiSummaryResponse generateSummary(AiSummaryRequest request) {
        return provider.generateSummary(request);
    }
}
