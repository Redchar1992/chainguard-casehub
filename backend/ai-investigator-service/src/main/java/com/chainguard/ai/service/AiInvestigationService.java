package com.chainguard.ai.service;

import com.chainguard.ai.dto.AiSummaryRequest;
import com.chainguard.ai.dto.AiSummaryResponse;
import com.chainguard.ai.provider.AiInvestigationProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AiInvestigationService {
    private static final Logger log = LoggerFactory.getLogger(AiInvestigationService.class);

    private final AiInvestigationProvider provider;

    public AiInvestigationService(AiInvestigationProvider provider) {
        this.provider = provider;
    }

    /**
     * Generates a summary for a specific case. The {@code caseId} is validated
     * and bound to the request context so the generated summary is correlated to
     * the case it was requested for (and never silently ignored).
     */
    public AiSummaryResponse generateSummary(UUID caseId, AiSummaryRequest request) {
        if (caseId == null) {
            throw new IllegalArgumentException("caseId is required");
        }
        log.info("Generating AI investigation summary for case {} (wallet {}, level {})",
                caseId, request.walletAddress(), request.riskLevel());
        return provider.generateSummary(request);
    }

    public AiSummaryResponse generateSummary(AiSummaryRequest request) {
        return provider.generateSummary(request);
    }
}
