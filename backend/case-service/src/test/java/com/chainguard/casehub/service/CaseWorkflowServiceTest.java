package com.chainguard.casehub.service;

import com.chainguard.casehub.audit.AuditService;
import com.chainguard.casehub.dto.CaseResponse;
import com.chainguard.casehub.dto.CreateCaseRequest;
import com.chainguard.casehub.model.CaseStatus;
import com.chainguard.casehub.model.ComplianceCase;
import com.chainguard.casehub.model.RiskLevel;
import com.chainguard.casehub.repository.ComplianceCaseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CaseWorkflowServiceTest {

    private ComplianceCaseRepository repository;
    private AuditService auditService;
    private CaseWorkflowService service;

    @BeforeEach
    void setUp() {
        repository = mock(ComplianceCaseRepository.class);
        auditService = mock(AuditService.class);
        service = new CaseWorkflowService(repository, auditService);
    }

    @Test
    void createCaseShouldPersistOpenCase() {
        when(repository.save(any(ComplianceCase.class))).thenAnswer(invocation -> {
            ComplianceCase saved = invocation.getArgument(0);
            setField(saved, "createdAt", Instant.now());
            setField(saved, "updatedAt", Instant.now());
            return saved;
        });

        CaseResponse response = service.createCase(new CreateCaseRequest(
                "0x00new-blacklist-bad0",
                "Critical wallet investigation",
                90,
                RiskLevel.CRITICAL
        ));

        assertThat(response.status()).isEqualTo(CaseStatus.OPEN);
        assertThat(response.riskLevel()).isEqualTo(RiskLevel.CRITICAL);
        assertThat(response.assignee()).isEqualTo("unassigned");
    }

    @Test
    void updateStatusShouldApplyLegalTransitionAndAudit() {
        ComplianceCase complianceCase = persistedCase(CaseStatus.OPEN);
        when(repository.findById(complianceCase.getId())).thenReturn(Optional.of(complianceCase));
        when(repository.save(any(ComplianceCase.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CaseResponse response = service.updateStatus(complianceCase.getId(), CaseStatus.REVIEWING);

        assertThat(response.status()).isEqualTo(CaseStatus.REVIEWING);
        verify(auditService).record(anyString(), anyString(), anyString(), anyMap());
    }

    @Test
    void updateStatusShouldRejectIllegalTransition() {
        // OPEN -> ESCALATED is not allowed (must go through REVIEWING first).
        ComplianceCase complianceCase = persistedCase(CaseStatus.OPEN);
        when(repository.findById(complianceCase.getId())).thenReturn(Optional.of(complianceCase));

        assertThatThrownBy(() -> service.updateStatus(complianceCase.getId(), CaseStatus.ESCALATED))
                .isInstanceOf(IllegalStatusTransitionException.class);

        assertThat(complianceCase.getStatus()).isEqualTo(CaseStatus.OPEN);
        verify(repository, never()).save(any());
        verify(auditService, never()).record(anyString(), anyString(), anyString(), anyMap());
    }

    @Test
    void updateStatusShouldRejectReopeningClosedCase() {
        ComplianceCase complianceCase = persistedCase(CaseStatus.OPEN);
        // Drive it to CLOSED legally, then attempt to reopen.
        complianceCase.transitionTo(CaseStatus.CLOSED);
        when(repository.findById(complianceCase.getId())).thenReturn(Optional.of(complianceCase));

        assertThatThrownBy(() -> service.updateStatus(complianceCase.getId(), CaseStatus.OPEN))
                .isInstanceOf(IllegalStatusTransitionException.class);
    }

    @Test
    void getMissingCaseShouldThrowNotFound() {
        var id = persistedCase(CaseStatus.OPEN).getId();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getCase(id)).isInstanceOf(ResourceNotFoundException.class);
    }

    private ComplianceCase persistedCase(CaseStatus status) {
        ComplianceCase complianceCase = new ComplianceCase("0xabc", "Case", 60, RiskLevel.HIGH);
        setField(complianceCase, "createdAt", Instant.now());
        setField(complianceCase, "updatedAt", Instant.now());
        if (status != CaseStatus.OPEN) {
            setField(complianceCase, "status", status);
        }
        return complianceCase;
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
