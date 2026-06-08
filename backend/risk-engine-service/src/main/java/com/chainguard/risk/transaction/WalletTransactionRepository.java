package com.chainguard.risk.transaction;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface WalletTransactionRepository extends MongoRepository<WalletTransactions, String> {
    Optional<WalletTransactions> findByWalletAddress(String walletAddress);
}
