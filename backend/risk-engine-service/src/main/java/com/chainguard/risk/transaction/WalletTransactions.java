package com.chainguard.risk.transaction;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;
import java.util.List;

/**
 * Wallet transaction document seeded into MongoDB (see infra/mongo/init.js).
 * One document per wallet with an embedded list of transactions; the risk
 * engine reads these to evaluate AML rules.
 */
@Document(collection = "wallet_transactions")
public class WalletTransactions {

    @Id
    private String id;

    private String walletAddress;

    private String firstSeen;

    private List<Transaction> transactions;

    public String getId() {
        return id;
    }

    public String getWalletAddress() {
        return walletAddress;
    }

    public String getFirstSeen() {
        return firstSeen;
    }

    public List<Transaction> getTransactions() {
        return transactions == null ? List.of() : transactions;
    }

    public Instant firstSeenInstant() {
        return parseInstant(firstSeen);
    }

    static Instant parseInstant(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(value);
        } catch (RuntimeException ex) {
            return null;
        }
    }

    public static class Transaction {
        private String txHash;
        private String direction;
        private String counterparty;
        private List<String> counterpartyTags;
        private Double amountUsd;
        private String timestamp;
        private List<String> tags;

        @Field("amountUsd")
        public Double getAmountUsd() {
            return amountUsd == null ? 0.0 : amountUsd;
        }

        public String getTxHash() {
            return txHash;
        }

        public String getDirection() {
            return direction == null ? "" : direction;
        }

        public String getCounterparty() {
            return counterparty;
        }

        public List<String> getCounterpartyTags() {
            return counterpartyTags == null ? List.of() : counterpartyTags;
        }

        public String getTimestamp() {
            return timestamp;
        }

        public List<String> getTags() {
            return tags == null ? List.of() : tags;
        }

        public Instant timestampInstant() {
            return parseInstant(timestamp);
        }
    }
}
