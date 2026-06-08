package com.chainguard.casehub.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CaseStatusTest {

    @Test
    void allowedTransitionsMatchTheLifecycle() {
        assertThat(CaseStatus.OPEN.canTransitionTo(CaseStatus.REVIEWING)).isTrue();
        assertThat(CaseStatus.OPEN.canTransitionTo(CaseStatus.CLOSED)).isTrue();
        assertThat(CaseStatus.REVIEWING.canTransitionTo(CaseStatus.ESCALATED)).isTrue();
        assertThat(CaseStatus.ESCALATED.canTransitionTo(CaseStatus.REVIEWING)).isTrue();
        assertThat(CaseStatus.ESCALATED.canTransitionTo(CaseStatus.CLOSED)).isTrue();
    }

    @Test
    void illegalJumpsAreRejected() {
        assertThat(CaseStatus.OPEN.canTransitionTo(CaseStatus.ESCALATED)).isFalse();
        assertThat(CaseStatus.CLOSED.canTransitionTo(CaseStatus.OPEN)).isFalse();
        assertThat(CaseStatus.CLOSED.allowedTransitions()).isEmpty();
        assertThat(CaseStatus.REVIEWING.canTransitionTo(null)).isFalse();
    }
}
