package com.chainguard.risk.rule;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AmlRuleRepository extends JpaRepository<AmlRuleEntity, UUID> {
    List<AmlRuleEntity> findByEnabledTrue();
}
