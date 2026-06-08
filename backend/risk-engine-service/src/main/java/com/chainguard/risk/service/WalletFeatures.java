package com.chainguard.risk.service;

import com.chainguard.risk.transaction.WalletTransactions;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;

/**
 * Aggregated, deterministic signals derived from a wallet's transaction history.
 * Computing these once keeps {@link RiskScoringService} rule evaluation pure and
 * unit-testable without touching MongoDB.
 *
 * @param transactionCount      total number of transactions on record
 * @param totalAmountUsd        sum of all transaction amounts (USD)
 * @param maxOutboundAmountUsd  largest single outbound (withdrawal) amount
 * @param maxBurstCount         maximum number of transactions observed within
 *                              {@code windowMinutes} of each other (sliding window)
 * @param blacklistHits         number of transactions whose counterparty is tagged
 *                              blacklist/sanctioned, or carries a blacklist tx tag
 * @param walletAgeDays         days since the wallet was first seen (or since its
 *                              earliest transaction when firstSeen is absent)
 * @param hasTransactions       whether any transaction data exists for the wallet
 */
public record WalletFeatures(
        int transactionCount,
        double totalAmountUsd,
        double maxOutboundAmountUsd,
        int maxBurstCount,
        int blacklistHits,
        long walletAgeDays,
        boolean hasTransactions
) {
    private static final Set<String> BLACKLIST_COUNTERPARTY_TAGS = Set.of("blacklist", "sanctioned");
    private static final Set<String> BLACKLIST_TX_TAGS = Set.of("blacklist_exposure", "sanctioned_exposure");

    /**
     * Builds features from a wallet document, evaluating the burst window against
     * {@code now}. Burst detection uses a sliding window of {@code windowMinutes}.
     */
    public static WalletFeatures from(WalletTransactions wallet, int windowMinutes, Instant now) {
        if (wallet == null || wallet.getTransactions().isEmpty()) {
            return new WalletFeatures(0, 0.0, 0.0, 0, 0, Long.MAX_VALUE, false);
        }

        List<WalletTransactions.Transaction> txs = wallet.getTransactions();
        int count = txs.size();
        double total = 0.0;
        double maxOutbound = 0.0;
        int blacklistHits = 0;
        Instant earliestTx = null;

        for (WalletTransactions.Transaction tx : txs) {
            double amount = tx.getAmountUsd();
            total += amount;
            if ("OUT".equalsIgnoreCase(tx.getDirection()) && amount > maxOutbound) {
                maxOutbound = amount;
            }
            if (isBlacklistExposure(tx)) {
                blacklistHits++;
            }
            Instant ts = tx.timestampInstant();
            if (ts != null && (earliestTx == null || ts.isBefore(earliestTx))) {
                earliestTx = ts;
            }
        }

        int maxBurst = maxBurst(txs, windowMinutes);
        long ageDays = walletAgeDays(wallet.firstSeenInstant(), earliestTx, now);

        return new WalletFeatures(count, total, maxOutbound, maxBurst, blacklistHits, ageDays, true);
    }

    private static boolean isBlacklistExposure(WalletTransactions.Transaction tx) {
        for (String tag : tx.getCounterpartyTags()) {
            if (tag != null && BLACKLIST_COUNTERPARTY_TAGS.contains(tag.toLowerCase())) {
                return true;
            }
        }
        for (String tag : tx.getTags()) {
            if (tag != null && BLACKLIST_TX_TAGS.contains(tag.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Largest count of transactions falling inside any {@code windowMinutes}-wide
     * window. Implemented as a two-pointer sweep over timestamp-sorted events.
     * Transactions without a parseable timestamp are ignored for burst purposes.
     */
    private static int maxBurst(List<WalletTransactions.Transaction> txs, int windowMinutes) {
        List<Instant> times = txs.stream()
                .map(WalletTransactions.Transaction::timestampInstant)
                .filter(t -> t != null)
                .sorted()
                .toList();
        if (times.isEmpty()) {
            return 0;
        }
        Duration window = Duration.ofMinutes(Math.max(1, windowMinutes));
        int best = 1;
        int start = 0;
        for (int end = 0; end < times.size(); end++) {
            while (Duration.between(times.get(start), times.get(end)).compareTo(window) > 0) {
                start++;
            }
            best = Math.max(best, end - start + 1);
        }
        return best;
    }

    private static long walletAgeDays(Instant firstSeen, Instant earliestTx, Instant now) {
        Instant origin = firstSeen != null ? firstSeen : earliestTx;
        if (origin == null) {
            return Long.MAX_VALUE;
        }
        long days = Duration.between(origin, now).toDays();
        return Math.max(0, days);
    }
}
