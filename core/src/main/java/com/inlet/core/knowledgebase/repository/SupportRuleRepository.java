package com.inlet.core.knowledgebase.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import com.inlet.core.knowledgebase.model.SupportRule;

@Repository
public interface SupportRuleRepository extends JpaRepository<SupportRule, Long> {
    List<SupportRule> findByIssueCategoryIgnoreCase(String issueCategory);
}