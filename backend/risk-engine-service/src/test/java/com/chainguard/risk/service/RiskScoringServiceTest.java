package com.chainguard.risk.service;

import com.chainguard.risk.dto.WalletRiskResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RiskScoringServiceTest {

    @Test
    void evaluateWalletShouldReturnExplainableRiskRules() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);

        RiskScoringService service = new RiskScoringService(redisTemplate, new ObjectMapper());

        WalletRiskResponse response = service.evaluateWallet("0x00new-blacklist-bad0");

        assertThat(response.riskScore()).isEqualTo(90);
        assertThat(response.riskLevel()).isEqualTo("CRITICAL");
        assertThat(response.triggeredRules().stream().map(rule -> rule.code()).toList())
                .contains("BLACKLIST_EXPOSURE", "NEW_ADDRESS_LARGE_WITHDRAWAL");
        assertThat(response.cached()).isFalse();
        verify(valueOperations).set(anyString(), anyString(), any());
    }
}
