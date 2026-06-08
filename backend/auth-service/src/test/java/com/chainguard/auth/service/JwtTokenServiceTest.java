package com.chainguard.auth.service;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenServiceTest {

    @Test
    void issueTokenShouldIncludeSubjectAndRoles() {
        JwtTokenService service = new JwtTokenService(
                "chainguard-demo-secret-key-must-be-at-least-32-bytes",
                3600
        );

        String userId = "11111111-2222-3333-4444-555555555555";
        String token = service.issueToken(userId, "admin@chainguard.demo", List.of("ADMIN", "ANALYST"));
        Jwt jwt = service.jwtDecoder().decode(token);

        assertThat(jwt.getSubject()).isEqualTo("admin@chainguard.demo");
        assertThat(jwt.getClaimAsStringList("roles")).containsExactly("ADMIN", "ANALYST");
        assertThat(jwt.getClaimAsString("userId")).isEqualTo(userId);
    }
}
