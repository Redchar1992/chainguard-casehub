package com.chainguard.auth.controller;

import com.chainguard.auth.audit.AuditLogRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end auth slice: seeds a user (H2), then drives the real login endpoint
 * through Spring Security, BCrypt verification, JWT issuance and audit logging.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtDecoder jwtDecoder;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Test
    void loginWithValidCredentialsReturnsTokenWithDbRolesAndWritesAudit() throws Exception {
        long auditBefore = auditLogRepository.count();

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content("{\"username\":\"analyst@chainguard.demo\",\"password\":\"Analyst123!\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.roles[0]").value("ANALYST"))
                .andReturn();

        String body = result.getResponse().getContentAsString();
        String token = com.jayway.jsonpath.JsonPath.read(body, "$.accessToken");
        Jwt jwt = jwtDecoder.decode(token);
        assertThat(jwt.getSubject()).isEqualTo("analyst@chainguard.demo");
        assertThat(jwt.getClaimAsStringList("roles")).containsExactly("ANALYST");
        assertThat(jwt.getClaimAsString("userId")).isEqualTo("22222222-2222-2222-2222-222222222222");

        assertThat(auditLogRepository.count()).isEqualTo(auditBefore + 1);
    }

    @Test
    void loginWithWrongPasswordReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content("{\"username\":\"analyst@chainguard.demo\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void loginWithUnknownUserReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content("{\"username\":\"ghost@chainguard.demo\",\"password\":\"Analyst123!\"}"))
                .andExpect(status().isUnauthorized());
    }
}
