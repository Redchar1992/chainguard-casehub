package com.chainguard.casehub.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "compliance_cases")
public class ComplianceCase {
    @Id
    private UUID id;

    @Column(name = "wallet_address", nullable = false, length = 128)
    private String walletAddress;

    @Column(nullable = false)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private CaseStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false, length = 32)
    private RiskLevel riskLevel;

    @Column(name = "risk_score", nullable = false)
    private int riskScore;

    @Column(name = "assignee_id")
    private UUID assigneeId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ComplianceCase() {
    }

    public ComplianceCase(String walletAddress, String title, int riskScore, RiskLevel riskLevel) {
        this.id = UUID.randomUUID();
        this.walletAddress = walletAddress;
        this.title = title;
        this.riskScore = riskScore;
        this.riskLevel = riskLevel;
        this.status = CaseStatus.OPEN;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (id == null) {
            id = UUID.randomUUID();
        }
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getWalletAddress() {
        return walletAddress;
    }

    public String getTitle() {
        return title;
    }

    public CaseStatus getStatus() {
        return status;
    }

    public RiskLevel getRiskLevel() {
        return riskLevel;
    }

    public int getRiskScore() {
        return riskScore;
    }

    public UUID getAssigneeId() {
        return assigneeId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void updateStatus(CaseStatus status) {
        this.status = status;
    }

    public void assignTo(UUID assigneeId) {
        this.assigneeId = assigneeId;
    }
}
