package com.inlet.core.resolution.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class SupportTicket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String customerEmail;
    private String customerName;
    private String issueCategory;
    
    @Column(columnDefinition = "TEXT")
    private String originalMessage;
    
    @Column(columnDefinition = "TEXT")
    private String aiDraftedReply;
    
    private String status; // e.g., PENDING_REVIEW, SENT
    private LocalDateTime createdAt;

    public SupportTicket() {
        this.createdAt = LocalDateTime.now();
        this.status = "PENDING_REVIEW";
    }

    // Getters and Setters
    public Long getId() { return id; }
    public String getCustomerEmail() { return customerEmail; }
    public void setCustomerEmail(String customerEmail) { this.customerEmail = customerEmail; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public String getIssueCategory() { return issueCategory; }
    public void setIssueCategory(String issueCategory) { this.issueCategory = issueCategory; }
    public String getOriginalMessage() { return originalMessage; }
    public void setOriginalMessage(String originalMessage) { this.originalMessage = originalMessage; }
    public String getAiDraftedReply() { return aiDraftedReply; }
    public void setAiDraftedReply(String aiDraftedReply) { this.aiDraftedReply = aiDraftedReply; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}