package com.chainguard.casehub.repository;

import com.chainguard.casehub.model.CaseStatus;
import com.chainguard.casehub.model.ComplianceCase;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ComplianceCaseRepository extends JpaRepository<ComplianceCase, UUID> {
    List<ComplianceCase> findByStatusOrderByUpdatedAtDesc(CaseStatus status);
    List<ComplianceCase> findByWalletAddressIgnoreCaseOrderByUpdatedAtDesc(String walletAddress);
}
