package com.chainguard.risk.service;

import com.chainguard.risk.dto.TriggeredRule;
import com.chainguard.risk.dto.WalletRiskResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class RiskScoringService {
    private static final Duration CACHE_TTL = Duration.ofMinutes(10);
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public RiskScoringService(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public WalletRiskResponse evaluateWallet(String walletAddress) {
        String cacheKey = cacheKey(walletAddress);
        WalletRiskResponse cached = readCache(cacheKey);
        if (cached != null) {
            return cached.asCached();
        }

        WalletRiskResponse response = calculateRisk(walletAddress);
        writeCache(cacheKey, response);
        return response;
    }

    private WalletRiskResponse calculateRisk(String walletAddress) {
        List<TriggeredRule> rules = new ArrayList<>();
        String normalized = walletAddress.toLowerCase(Locale.ROOT);

        if (normalized.endsWith("bad") || normalized.contains("blacklist")) {
            rules.add(new TriggeredRule(
                    "BLACKLIST_EXPOSURE",
                    "CRITICAL",
                    "Wallet interacted with a known blacklisted address.",
                    45
            ));
        }

        if (normalized.length() % 2 == 0) {
            rules.add(new TriggeredRule(
                    "HIGH_FREQUENCY_TRANSFER",
                    "HIGH",
                    "Wallet shows high-frequency transfer behavior in a short time window.",
                    25
            ));
        }

        if (normalized.contains("new") || normalized.startsWith("0x00")) {
            rules.add(new TriggeredRule(
                    "NEW_ADDRESS_LARGE_WITHDRAWAL",
                    "HIGH",
                    "New wallet received or withdrew a large amount shortly after creation.",
                    20
            ));
        }

        int score = Math.min(100, rules.stream().mapToInt(TriggeredRule::scoreImpact).sum());
        String level = toRiskLevel(score);
        return new WalletRiskResponse(walletAddress, score, level, rules, false);
    }

    private WalletRiskResponse readCache(String cacheKey) {
        try {
            String value = redisTemplate.opsForValue().get(cacheKey);
            if (value == null) {
                return null;
            }
            return objectMapper.readValue(value, WalletRiskResponse.class);
        } catch (RedisConnectionFailureException | JsonProcessingException ignored) {
            // Redis is optional for local demo. Fall back to direct calculation.
            return null;
        }
    }

    private void writeCache(String cacheKey, WalletRiskResponse response) {
        try {
            redisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(response), CACHE_TTL);
        } catch (RedisConnectionFailureException | JsonProcessingException ignored) {
            // Redis is optional for local demo. Do not fail risk evaluation.
        }
    }

    private String cacheKey(String walletAddress) {
        return "wallet-risk:" + walletAddress.toLowerCase(Locale.ROOT);
    }

    private String toRiskLevel(int score) {
        if (score >= 80) return "CRITICAL";
        if (score >= 60) return "HIGH";
        if (score >= 30) return "MEDIUM";
        return "LOW";
    }
}
