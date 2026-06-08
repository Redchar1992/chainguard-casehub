package com.chainguard.risk.service;

import com.chainguard.risk.dto.TriggeredRule;
import com.chainguard.risk.dto.WalletRiskResponse;
import com.chainguard.risk.rule.AmlRuleEntity;
import com.chainguard.risk.rule.AmlRuleRepository;
import com.chainguard.risk.transaction.WalletTransactionRepository;
import com.chainguard.risk.transaction.WalletTransactions;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Evaluates wallet AML risk from real data:
 * <ul>
 *   <li>enabled rules + thresholds are loaded from PostgreSQL ({@code aml_rules}),</li>
 *   <li>the wallet's transaction history is loaded from MongoDB,</li>
 *   <li>each rule is evaluated against deterministic {@link WalletFeatures},</li>
 *   <li>results are cached in Redis (optional; failures degrade gracefully).</li>
 * </ul>
 * The score is the capped sum of triggered-rule impacts, which are derived from
 * each rule's severity — not from any property of the address string.
 */
@Service
public class RiskScoringService {
    private static final Logger log = LoggerFactory.getLogger(RiskScoringService.class);
    private static final Duration CACHE_TTL = Duration.ofMinutes(10);
    private static final int DEFAULT_FREQUENCY_WINDOW_MINUTES = 30;

    private final AmlRuleRepository ruleRepository;
    private final WalletTransactionRepository transactionRepository;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public RiskScoringService(
            AmlRuleRepository ruleRepository,
            WalletTransactionRepository transactionRepository,
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper
    ) {
        this(ruleRepository, transactionRepository, redisTemplate, objectMapper, Clock.systemUTC());
    }

    RiskScoringService(
            AmlRuleRepository ruleRepository,
            WalletTransactionRepository transactionRepository,
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            Clock clock
    ) {
        this.ruleRepository = ruleRepository;
        this.transactionRepository = transactionRepository;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.clock = clock;
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
        List<AmlRuleEntity> rules = ruleRepository.findByEnabledTrue();
        WalletTransactions wallet = transactionRepository.findByWalletAddress(walletAddress).orElse(null);

        // Use the longest configured frequency window so a single feature snapshot
        // covers every frequency rule deterministically.
        int frequencyWindow = resolveFrequencyWindow(rules);
        WalletFeatures features = WalletFeatures.from(wallet, frequencyWindow, clock.instant());

        List<TriggeredRule> triggered = new ArrayList<>();
        for (AmlRuleEntity rule : rules) {
            evaluateRule(rule, features).ifPresent(triggered::add);
        }

        int score = Math.min(100, triggered.stream().mapToInt(TriggeredRule::scoreImpact).sum());
        return new WalletRiskResponse(walletAddress, score, toRiskLevel(score), triggered, false);
    }

    private int resolveFrequencyWindow(List<AmlRuleEntity> rules) {
        int window = DEFAULT_FREQUENCY_WINDOW_MINUTES;
        for (AmlRuleEntity rule : rules) {
            if ("HIGH_FREQUENCY_TRANSFER".equals(rule.getCode())) {
                JsonNode threshold = parseThreshold(rule);
                window = Math.max(window, threshold.path("windowMinutes").asInt(DEFAULT_FREQUENCY_WINDOW_MINUTES));
            }
        }
        return window;
    }

    private Optional<TriggeredRule> evaluateRule(AmlRuleEntity rule, WalletFeatures features) {
        if (!features.hasTransactions()) {
            return Optional.empty();
        }
        JsonNode threshold = parseThreshold(rule);
        return switch (rule.getCode()) {
            case "BLACKLIST_EXPOSURE" -> evaluateBlacklist(rule, features);
            case "HIGH_FREQUENCY_TRANSFER" -> evaluateHighFrequency(rule, features, threshold);
            case "NEW_ADDRESS_LARGE_WITHDRAWAL" -> evaluateNewAddressLargeWithdrawal(rule, features, threshold);
            case "LARGE_AGGREGATE_VOLUME" -> evaluateLargeAggregateVolume(rule, features, threshold);
            default -> Optional.empty();
        };
    }

    private Optional<TriggeredRule> evaluateBlacklist(AmlRuleEntity rule, WalletFeatures features) {
        if (features.blacklistHits() <= 0) {
            return Optional.empty();
        }
        return Optional.of(triggered(rule, String.format(
                "Wallet interacted with %d blacklisted/sanctioned counterparty transaction(s).",
                features.blacklistHits())));
    }

    private Optional<TriggeredRule> evaluateHighFrequency(AmlRuleEntity rule, WalletFeatures features, JsonNode threshold) {
        int count = threshold.path("count").asInt(20);
        int windowMinutes = threshold.path("windowMinutes").asInt(DEFAULT_FREQUENCY_WINDOW_MINUTES);
        if (features.maxBurstCount() < count) {
            return Optional.empty();
        }
        return Optional.of(triggered(rule, String.format(
                "Detected %d transfers within a %d-minute window (threshold %d).",
                features.maxBurstCount(), windowMinutes, count)));
    }

    private Optional<TriggeredRule> evaluateNewAddressLargeWithdrawal(AmlRuleEntity rule, WalletFeatures features, JsonNode threshold) {
        int maxAgeDays = threshold.path("addressAgeDays").asInt(7);
        double amountUsd = threshold.path("amountUsd").asDouble(10_000);
        if (features.walletAgeDays() > maxAgeDays || features.maxOutboundAmountUsd() < amountUsd) {
            return Optional.empty();
        }
        return Optional.of(triggered(rule, String.format(
                "New wallet (age %d day(s)) moved %.2f USD outbound, exceeding %.2f threshold.",
                features.walletAgeDays(), features.maxOutboundAmountUsd(), amountUsd)));
    }

    private Optional<TriggeredRule> evaluateLargeAggregateVolume(AmlRuleEntity rule, WalletFeatures features, JsonNode threshold) {
        double totalUsd = threshold.path("totalUsd").asDouble(100_000);
        if (features.totalAmountUsd() < totalUsd) {
            return Optional.empty();
        }
        return Optional.of(triggered(rule, String.format(
                "Aggregate transaction volume %.2f USD exceeds %.2f threshold.",
                features.totalAmountUsd(), totalUsd)));
    }

    private TriggeredRule triggered(AmlRuleEntity rule, String description) {
        return new TriggeredRule(rule.getCode(), rule.getSeverity(), description, scoreImpact(rule.getSeverity()));
    }

    private JsonNode parseThreshold(AmlRuleEntity rule) {
        try {
            String json = rule.getThreshold();
            if (json == null || json.isBlank()) {
                return objectMapper.createObjectNode();
            }
            return objectMapper.readTree(json);
        } catch (JsonProcessingException ex) {
            log.warn("Invalid threshold JSON for rule {}: {}", rule.getCode(), ex.getOriginalMessage());
            return objectMapper.createObjectNode();
        }
    }

    private int scoreImpact(String severity) {
        return switch (severity == null ? "" : severity.toUpperCase(Locale.ROOT)) {
            case "CRITICAL" -> 45;
            case "HIGH" -> 25;
            case "MEDIUM" -> 15;
            case "LOW" -> 5;
            default -> 10;
        };
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
