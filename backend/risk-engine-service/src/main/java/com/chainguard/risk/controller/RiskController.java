package com.chainguard.risk.controller;

import com.chainguard.risk.dto.WalletRiskResponse;
import com.chainguard.risk.service.RiskScoringService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/risk")
public class RiskController {
    private final RiskScoringService service;

    public RiskController(RiskScoringService service) {
        this.service = service;
    }

    @GetMapping("/wallets/{walletAddress}")
    @PreAuthorize("hasAnyRole('ANALYST', 'REVIEWER', 'ADMIN')")
    public WalletRiskResponse evaluateWallet(@PathVariable String walletAddress) {
        return service.evaluateWallet(walletAddress);
    }

    @GetMapping("/health")
    public String health() {
        return "risk-engine-service ok";
    }
}
