package com.inlet.core.knowledgebase.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class SupportRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String issueCategory;
    private String resolutionSteps;

    public SupportRule() {}

    public SupportRule(String issueCategory, String resolutionSteps) {
        this.issueCategory = issueCategory;
        this.resolutionSteps = resolutionSteps;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getIssueCategory() { return issueCategory; }
    public void setIssueCategory(String issueCategory) { this.issueCategory = issueCategory; }
    public String getResolutionSteps() { return resolutionSteps; }
    public void setResolutionSteps(String resolutionSteps) { this.resolutionSteps = resolutionSteps; }
}