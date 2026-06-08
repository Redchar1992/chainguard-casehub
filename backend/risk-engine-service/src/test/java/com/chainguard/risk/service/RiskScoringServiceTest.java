package com.chainguard.risk.service;

import com.chainguard.risk.dto.TriggeredRule;
import com.chainguard.risk.dto.WalletRiskResponse;
import com.chainguard.risk.rule.AmlRuleEntity;
import com.chainguard.risk.rule.AmlRuleRepository;
import com.chainguard.risk.transaction.WalletTransactionRepository;
import com.chainguard.risk.transaction.WalletTransactions;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.lang.reflect.Field;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RiskScoringServiceTest {

    private static final Instant NOW = Instant.parse("2026-06-08T12:00:00Z");

    private AmlRuleRepository ruleRepository;
    private WalletTransactionRepository transactionRepository;
    private StringRedisTemplate redisTemplate;
    private RiskScoringService service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        ruleRepository = mock(AmlRuleRepository.class);
        transactionRepository = mock(WalletTransactionRepository.class);
        redisTemplate = mock(StringRedisTemplate.class);
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);

        when(ruleRepository.findByEnabledTrue()).thenReturn(List.of(
                rule("BLACKLIST_EXPOSURE", "CRITICAL", "{\"counterpartyTag\":\"blacklist\"}"),
                rule("HIGH_FREQUENCY_TRANSFER", "HIGH", "{\"count\":20,\"windowMinutes\":30}"),
                rule("NEW_ADDRESS_LARGE_WITHDRAWAL", "HIGH", "{\"addressAgeDays\":7,\"amountUsd\":10000}"),
                rule("LARGE_AGGREGATE_VOLUME", "MEDIUM", "{\"totalUsd\":100000}")
        ));

        service = new RiskScoringService(
                ruleRepository,
                transactionRepository,
                redisTemplate,
                new ObjectMapper(),
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void blacklistExposureAndNewAddressWithdrawalTriggerForHighRiskWallet() {
        WalletTransactions wallet = wallet("0xbad", "2026-06-05T09:00:00Z", List.of(
                tx("IN", 12500.50, "2026-06-08T10:00:00Z", List.of("blacklist"), List.of()),
                tx("OUT", 12400.00, "2026-06-08T10:08:00Z", List.of(), List.of())
        ));
        when(transactionRepository.findByWalletAddress("0xbad")).thenReturn(Optional.of(wallet));

        WalletRiskResponse response = service.evaluateWallet("0xbad");

        List<String> codes = response.triggeredRules().stream().map(TriggeredRule::code).toList();
        assertThat(codes).contains("BLACKLIST_EXPOSURE", "NEW_ADDRESS_LARGE_WITHDRAWAL");
        // CRITICAL (45) + HIGH (25) = 70 -> HIGH band.
        assertThat(response.riskScore()).isEqualTo(70);
        assertThat(response.riskLevel()).isEqualTo("HIGH");
        assertThat(response.cached()).isFalse();
    }

    @Test
    void highFrequencyTriggersWhenBurstExceedsThreshold() {
        List<WalletTransactions.Transaction> txs = new ArrayList<>();
        for (int i = 0; i < 24; i++) {
            String minute = (i < 10 ? "0" : "") + i;
            txs.add(tx("OUT", 500.0, "2026-06-08T11:" + minute + ":00Z", List.of(), List.of()));
        }
        // Old wallet so the new-address rule does not fire.
        WalletTransactions wallet = wallet("0xhot", "2024-01-01T00:00:00Z", txs);
        when(transactionRepository.findByWalletAddress("0xhot")).thenReturn(Optional.of(wallet));

        WalletRiskResponse response = service.evaluateWallet("0xhot");

        List<String> codes = response.triggeredRules().stream().map(TriggeredRule::code).toList();
        assertThat(codes).contains("HIGH_FREQUENCY_TRANSFER");
        assertThat(codes).doesNotContain("BLACKLIST_EXPOSURE", "NEW_ADDRESS_LARGE_WITHDRAWAL");
    }

    @Test
    void largeAggregateVolumeTriggersWhenTotalExceedsThreshold() {
        WalletTransactions wallet = wallet("0xwhale", "2024-01-01T00:00:00Z", List.of(
                tx("IN", 60000.0, "2026-05-01T08:00:00Z", List.of(), List.of()),
                tx("OUT", 55000.0, "2026-05-10T08:00:00Z", List.of(), List.of())
        ));
        when(transactionRepository.findByWalletAddress("0xwhale")).thenReturn(Optional.of(wallet));

        WalletRiskResponse response = service.evaluateWallet("0xwhale");

        List<String> codes = response.triggeredRules().stream().map(TriggeredRule::code).toList();
        assertThat(codes).containsExactly("LARGE_AGGREGATE_VOLUME");
    }

    @Test
    void cleanWalletScoresLowWithNoTriggeredRules() {
        WalletTransactions wallet = wallet("0xclean", "2024-03-01T00:00:00Z", List.of(
                tx("IN", 250.0, "2026-05-20T08:00:00Z", List.of("exchange"), List.of()),
                tx("OUT", 120.0, "2026-06-01T16:30:00Z", List.of("merchant"), List.of())
        ));
        when(transactionRepository.findByWalletAddress("0xclean")).thenReturn(Optional.of(wallet));

        WalletRiskResponse response = service.evaluateWallet("0xclean");

        assertThat(response.triggeredRules()).isEmpty();
        assertThat(response.riskScore()).isZero();
        assertThat(response.riskLevel()).isEqualTo("LOW");
    }

    @Test
    void unknownWalletWithNoTransactionsScoresLow() {
        when(transactionRepository.findByWalletAddress("0xunknown")).thenReturn(Optional.empty());

        WalletRiskResponse response = service.evaluateWallet("0xunknown");

        assertThat(response.triggeredRules()).isEmpty();
        assertThat(response.riskScore()).isZero();
        assertThat(response.riskLevel()).isEqualTo("LOW");
    }

    @Test
    void disabledRulesAreNotEvaluated() {
        // Only the blacklist rule is enabled; the wallet would also match volume,
        // but that rule is not returned by findByEnabledTrue().
        when(ruleRepository.findByEnabledTrue()).thenReturn(List.of(
                rule("BLACKLIST_EXPOSURE", "CRITICAL", "{\"counterpartyTag\":\"blacklist\"}")
        ));
        WalletTransactions wallet = wallet("0xpartial", "2024-01-01T00:00:00Z", List.of(
                tx("IN", 250000.0, "2026-06-08T10:00:00Z", List.of("sanctioned"), List.of())
        ));
        when(transactionRepository.findByWalletAddress("0xpartial")).thenReturn(Optional.of(wallet));

        WalletRiskResponse response = service.evaluateWallet("0xpartial");

        List<String> codes = response.triggeredRules().stream().map(TriggeredRule::code).toList();
        assertThat(codes).containsExactly("BLACKLIST_EXPOSURE");
        assertThat(codes).doesNotContain("LARGE_AGGREGATE_VOLUME");
    }

    // --- helpers -------------------------------------------------------------

    private AmlRuleEntity rule(String code, String severity, String threshold) {
        return AmlRuleEntity.forTest(code, severity, threshold, true);
    }

    private WalletTransactions wallet(String address, String firstSeen, List<WalletTransactions.Transaction> txs) {
        WalletTransactions wallet = new WalletTransactions();
        setField(wallet, "walletAddress", address);
        setField(wallet, "firstSeen", firstSeen);
        setField(wallet, "transactions", txs);
        return wallet;
    }

    private WalletTransactions.Transaction tx(
            String direction, double amount, String timestamp,
            List<String> counterpartyTags, List<String> tags
    ) {
        WalletTransactions.Transaction tx = new WalletTransactions.Transaction();
        setField(tx, "direction", direction);
        setField(tx, "amountUsd", amount);
        setField(tx, "timestamp", timestamp);
        setField(tx, "counterpartyTags", counterpartyTags);
        setField(tx, "tags", tags);
        return tx;
    }

    private static void setField(Object target, String name, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(name);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
