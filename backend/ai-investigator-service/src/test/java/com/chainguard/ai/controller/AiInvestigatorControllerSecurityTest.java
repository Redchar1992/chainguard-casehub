package com.chainguard.ai.controller;

import com.chainguard.ai.config.SecurityConfig;
import com.chainguard.ai.dto.AiSummaryResponse;
import com.chainguard.ai.service.AiInvestigationService;
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
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies method-level RBAC on the AI case-summary endpoint: no token is
 * unauthorized, an unprivileged role is forbidden, and an ANALYST token is
 * allowed (and the caseId from the path is passed through to the service).
 */
@WebMvcTest(AiInvestigatorController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
class AiInvestigatorControllerSecurityTest {

    private static final String BODY =
            "{\"walletAddress\":\"0xabc\",\"riskScore\":90,\"riskLevel\":\"CRITICAL\"}";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AiInvestigationService service;

    @Value("${security.jwt.secret}")
    private String secret;

    @Test
    void noTokenIsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/ai/cases/{caseId}/summary", UUID.randomUUID())
                        .contentType("application/json")
                        .content(BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unprivilegedRoleIsForbidden() throws Exception {
        mockMvc.perform(post("/api/ai/cases/{caseId}/summary", UUID.randomUUID())
                        .header("Authorization", "Bearer " + tokenWithRoles("VIEWER"))
                        .contentType("application/json")
                        .content(BODY))
                .andExpect(status().isForbidden());
    }

    @Test
    void analystRoleIsAllowedAndCaseIdIsUsed() throws Exception {
        UUID caseId = UUID.randomUUID();
        when(service.generateSummary(eq(caseId), any()))
                .thenReturn(new AiSummaryResponse("ok", List.of(), List.of(), "HIGH"));

        mockMvc.perform(post("/api/ai/cases/{caseId}/summary", caseId)
                        .header("Authorization", "Bearer " + tokenWithRoles("ANALYST"))
                        .contentType("application/json")
                        .content(BODY))
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
