package com.chainguard.casehub.rule;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AmlRuleRepository extends JpaRepository<AmlRule, UUID> {
    Optional<AmlRule> findByCode(String code);
    boolean existsByCode(String code);
}
