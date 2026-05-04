package com.inlet.core.knowledgebase.service;

import org.springframework.stereotype.Service;

import java.util.List;

import com.inlet.core.knowledgebase.model.SupportRule;
import com.inlet.core.knowledgebase.repository.SupportRuleRepository;

@Service
public class KnowledgeBaseService {

    private final SupportRuleRepository repository;

    public KnowledgeBaseService(SupportRuleRepository repository) {
        this.repository = repository;
    }

    public SupportRule saveRule(SupportRule rule) {
        return repository.save(rule);
    }

    public List<SupportRule> getAllRules() {
        return repository.findAll();
    }

    public String getResolutionForCategory(String category) {
        List<SupportRule> rules = repository.findByIssueCategoryIgnoreCase(category);
        if (rules.isEmpty()) {
            return "No specific rules found in the knowledge base. Escalate to human agent.";
        }
        return rules.get(0).getResolutionSteps();
    }
}