package com.chainguard.casehub.service;

import com.chainguard.casehub.dto.CaseResponse;
import com.chainguard.casehub.dto.CreateCaseRequest;
import com.chainguard.casehub.model.CaseStatus;
import com.chainguard.casehub.model.ComplianceCase;
import com.chainguard.casehub.model.RiskLevel;
import com.chainguard.casehub.repository.ComplianceCaseRepository;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CaseWorkflowServiceTest {

    @Test
    void createCaseShouldPersistOpenCase() {
        ComplianceCaseRepository repository = mock(ComplianceCaseRepository.class);
        when(repository.save(any(ComplianceCase.class))).thenAnswer(invocation -> {
            ComplianceCase saved = invocation.getArgument(0);
            setField(saved, "createdAt", Instant.now());
            setField(saved, "updatedAt", Instant.now());
            return saved;
        });

        CaseWorkflowService service = new CaseWorkflowService(repository);
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
    void updateStatusShouldPersistNewStatus() {
        ComplianceCaseRepository repository = mock(ComplianceCaseRepository.class);
        ComplianceCase complianceCase = new ComplianceCase("0xabc", "Case", 60, RiskLevel.HIGH);
        setField(complianceCase, "createdAt", Instant.now());
        setField(complianceCase, "updatedAt", Instant.now());
        when(repository.findById(complianceCase.getId())).thenReturn(Optional.of(complianceCase));
        when(repository.save(any(ComplianceCase.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CaseWorkflowService service = new CaseWorkflowService(repository);
        CaseResponse response = service.updateStatus(complianceCase.getId(), CaseStatus.ESCALATED);

        assertThat(response.status()).isEqualTo(CaseStatus.ESCALATED);
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
