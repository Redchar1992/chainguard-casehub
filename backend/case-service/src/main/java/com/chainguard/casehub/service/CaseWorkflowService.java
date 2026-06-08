package com.chainguard.casehub.service;

import com.chainguard.casehub.dto.CaseResponse;
import com.chainguard.casehub.dto.CreateCaseRequest;
import com.chainguard.casehub.model.CaseStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CaseWorkflowService {
    private final ConcurrentHashMap<UUID, CaseResponse> cases = new ConcurrentHashMap<>();

    public CaseResponse createCase(CreateCaseRequest request) {
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        CaseResponse response = new CaseResponse(
                id,
                request.walletAddress(),
                request.title(),
                CaseStatus.OPEN,
                request.riskScore(),
                request.riskLevel(),
                "unassigned",
                now,
                now
        );
        cases.put(id, response);
        return response;
    }

    public List<CaseResponse> listCases() {
        return new ArrayList<>(cases.values());
    }

    public CaseResponse getCase(UUID id) {
        CaseResponse response = cases.get(id);
        if (response == null) {
            throw new IllegalArgumentException("Case not found: " + id);
        }
        return response;
    }

    public CaseResponse updateStatus(UUID id, CaseStatus status) {
        CaseResponse current = getCase(id);
        CaseResponse updated = new CaseResponse(
                current.id(),
                current.walletAddress(),
                current.title(),
                status,
                current.riskScore(),
                current.riskLevel(),
                current.assignee(),
                current.createdAt(),
                Instant.now()
        );
        cases.put(id, updated);
        return updated;
    }
}
