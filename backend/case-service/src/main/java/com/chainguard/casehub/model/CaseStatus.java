package com.chainguard.casehub.model;

import java.util.Map;
import java.util.Set;

/**
 * Case lifecycle states and the allowed transitions between them, forming an
 * explicit state machine:
 *
 * <pre>
 *   OPEN ───▶ REVIEWING ───▶ ESCALATED ───▶ CLOSED
 *    │            │              │
 *    └────────────┴──────────────┴──────────▶ CLOSED
 *   REVIEWING ◀── ESCALATED   (de-escalate back to review)
 * </pre>
 *
 * Illegal jumps (e.g. CLOSED → OPEN, or OPEN → ESCALATED) are rejected.
 */
public enum CaseStatus {
    OPEN,
    REVIEWING,
    ESCALATED,
    CLOSED;

    private static final Map<CaseStatus, Set<CaseStatus>> ALLOWED = Map.of(
            OPEN, Set.of(REVIEWING, CLOSED),
            REVIEWING, Set.of(ESCALATED, CLOSED, OPEN),
            ESCALATED, Set.of(REVIEWING, CLOSED),
            CLOSED, Set.of()
    );

    public boolean canTransitionTo(CaseStatus target) {
        return target != null && ALLOWED.getOrDefault(this, Set.of()).contains(target);
    }

    public Set<CaseStatus> allowedTransitions() {
        return ALLOWED.getOrDefault(this, Set.of());
    }
}
