package com.inlet.core.knowledgebase.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "knowledge_documents")
public class KnowledgeDocument {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String type; // "PDF" or "TEXT"
    private String url;
    
    // NEW: Associate every document with a specific User ID
    private String uid;

    @Column(columnDefinition = "TEXT")
    private String content;

    private LocalDateTime createdAt = LocalDateTime.now();

    // --- GETTERS & SETTERS ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    // NEW Getter/Setter for UID
    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}