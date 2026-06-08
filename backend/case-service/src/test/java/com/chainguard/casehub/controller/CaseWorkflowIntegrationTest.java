package com.chainguard.casehub.controller;

import com.chainguard.casehub.audit.AuditLogRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration test for the case pipeline: a JWT (issued with the shared HS256
 * secret, mirroring the auth-service) drives the gateway-style security filter,
 * @PreAuthorize role checks, JPA persistence, the status state machine and
 * audit logging — all against an in-memory H2 database.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CaseWorkflowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${security.jwt.secret}")
    private String secret;

    @Test
    void analystCanCreateCaseAndDriveLegalTransitionsButNotIllegalOnes() throws Exception {
        String token = analystToken();

        // Create a case.
        MvcResult created = mockMvc.perform(post("/api/cases")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("{\"walletAddress\":\"0x00new-blacklist-bad0\","
                                + "\"title\":\"Critical wallet\",\"riskScore\":90,\"riskLevel\":\"CRITICAL\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andReturn();

        JsonNode body = objectMapper.readTree(created.getResponse().getContentAsString());
        String caseId = body.get("id").asText();
        long auditBefore = auditLogRepository.count();

        // Legal: OPEN -> REVIEWING.
        mockMvc.perform(patch("/api/cases/{id}/status", caseId)
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("{\"status\":\"REVIEWING\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REVIEWING"));

        // Audit row written for the status change.
        assertThat(auditLogRepository.count()).isEqualTo(auditBefore + 1);

        // Illegal: REVIEWING -> CLOSED is legal, but jump REVIEWING -> (re)OPEN
        // after closing is not. First close, then attempt to reopen.
        mockMvc.perform(patch("/api/cases/{id}/status", caseId)
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("{\"status\":\"CLOSED\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/cases/{id}/status", caseId)
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("{\"status\":\"OPEN\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ILLEGAL_STATUS_TRANSITION"));
    }

    @Test
    void unknownStatusValueReturnsBadRequest() throws Exception {
        String token = analystToken();
        MvcResult created = mockMvc.perform(post("/api/cases")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("{\"walletAddress\":\"0xabc\",\"title\":\"t\",\"riskScore\":10,\"riskLevel\":\"LOW\"}"))
                .andExpect(status().isOk())
                .andReturn();
        String caseId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(patch("/api/cases/{id}/status", caseId)
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("{\"status\":\"BOGUS\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void requestWithoutTokenIsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/cases")
                        .contentType("application/json")
                        .content("{\"walletAddress\":\"0xabc\",\"title\":\"t\",\"riskScore\":10,\"riskLevel\":\"LOW\"}"))
                .andExpect(status().isUnauthorized());
    }

    private String analystToken() {
        var encoder = new NimbusJwtEncoder(new ImmutableSecret<>(secret.getBytes(StandardCharsets.UTF_8)));
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("chainguard-auth-service")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(3600))
                .subject("analyst@chainguard.demo")
                .claim("userId", "22222222-2222-2222-2222-222222222222")
                .claim("roles", List.of("ANALYST"))
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
