package com.chainguard.risk.rule;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

/**
 * Read-only view of the {@code aml_rules} table owned (and migrated via Flyway)
 * by the case-service. The risk engine loads enabled rules and their threshold
 * configuration to evaluate wallets.
 */
@Entity
@Table(name = "aml_rules")
public class AmlRuleEntity {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 128)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, length = 32)
    private String severity;

    @Column(nullable = false, columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String threshold;

    @Column(nullable = false)
    private boolean enabled;

    @Column(nullable = false)
    private int version;

    protected AmlRuleEntity() {
    }

    /** Test-only factory; production instances are hydrated by JPA. */
    public static AmlRuleEntity forTest(String code, String severity, String threshold, boolean enabled) {
        AmlRuleEntity entity = new AmlRuleEntity();
        entity.id = UUID.randomUUID();
        entity.code = code;
        entity.name = code;
        entity.severity = severity;
        entity.threshold = threshold;
        entity.enabled = enabled;
        entity.version = 1;
        return entity;
    }

    public UUID getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getSeverity() {
        return severity;
    }

    public String getThreshold() {
        return threshold;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getVersion() {
        return version;
    }
}
