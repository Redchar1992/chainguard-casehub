package com.chainguard.risk.controller;

import com.chainguard.risk.config.SecurityConfig;
import com.chainguard.risk.dto.WalletRiskResponse;
import com.chainguard.risk.service.RiskScoringService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies method-level RBAC on the risk-engine wallet scoring endpoint:
 * a request with no token is unauthorized, a token whose role is not one of
 * ANALYST/REVIEWER/ADMIN is forbidden, and an ANALYST token is allowed.
 */
@WebMvcTest(RiskController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
class RiskControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RiskScoringService service;

    @Value("${security.jwt.secret}")
    private String secret;

    @Test
    void noTokenIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/risk/wallets/0xabc"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unprivilegedRoleIsForbidden() throws Exception {
        mockMvc.perform(get("/api/risk/wallets/0xabc")
                        .header("Authorization", "Bearer " + tokenWithRoles("VIEWER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void analystRoleIsAllowed() throws Exception {
        when(service.evaluateWallet(anyString()))
                .thenReturn(new WalletRiskResponse("0xabc", 0, "LOW", List.of(), false));

        mockMvc.perform(get("/api/risk/wallets/0xabc")
                        .header("Authorization", "Bearer " + tokenWithRoles("ANALYST")))
                .andExpect(status().isOk());
    }

    private String tokenWithRoles(String... roles) {
        var encoder = new NimbusJwtEncoder(new ImmutableSecret<>(secret.getBytes(StandardCharsets.UTF_8)));
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("chainguard-auth-service")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(3600))
                .subject("user@chainguard.demo")
                .claim("roles", List.of(roles))
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
