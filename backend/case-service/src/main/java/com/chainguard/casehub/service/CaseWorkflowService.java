package com.chainguard.casehub.service;

import com.chainguard.casehub.dto.CaseResponse;
import com.chainguard.casehub.dto.CreateCaseRequest;
import com.chainguard.casehub.model.CaseStatus;
import com.chainguard.casehub.model.ComplianceCase;
import com.chainguard.casehub.repository.ComplianceCaseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class CaseWorkflowService {
    private final ComplianceCaseRepository repository;

    public CaseWorkflowService(ComplianceCaseRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public CaseResponse createCase(CreateCaseRequest request) {
        ComplianceCase complianceCase = new ComplianceCase(
                request.walletAddress(),
                request.title(),
                request.riskScore(),
                request.riskLevel()
        );
        return toResponse(repository.save(complianceCase));
    }

    @Transactional(readOnly = true)
    public List<CaseResponse> listCases(CaseStatus status, String walletAddress) {
        List<ComplianceCase> results;
        if (status != null) {
            results = repository.findByStatusOrderByUpdatedAtDesc(status);
        } else if (walletAddress != null && !walletAddress.isBlank()) {
            results = repository.findByWalletAddressIgnoreCaseOrderByUpdatedAtDesc(walletAddress);
        } else {
            results = repository.findAll();
        }
        return results.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public CaseResponse getCase(UUID id) {
        return toResponse(findCase(id));
    }

    @Transactional
    public CaseResponse updateStatus(UUID id, CaseStatus status) {
        ComplianceCase complianceCase = findCase(id);
        complianceCase.updateStatus(status);
        return toResponse(repository.save(complianceCase));
    }

    private ComplianceCase findCase(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Case not found: " + id));
    }

    private CaseResponse toResponse(ComplianceCase complianceCase) {
        return new CaseResponse(
                complianceCase.getId(),
                complianceCase.getWalletAddress(),
                complianceCase.getTitle(),
                complianceCase.getStatus(),
                complianceCase.getRiskScore(),
                complianceCase.getRiskLevel(),
                complianceCase.getAssigneeId() == null ? "unassigned" : complianceCase.getAssigneeId().toString(),
                complianceCase.getCreatedAt(),
                complianceCase.getUpdatedAt()
        );
    }
}
