package com.inlet.core.knowledgebase.repository;

import com.inlet.core.knowledgebase.model.KnowledgeDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface KnowledgeDocumentRepository extends JpaRepository<KnowledgeDocument, Long> {
    
    // Fetch history only for a specific user
    List<KnowledgeDocument> findByUidOrderByCreatedAtDesc(String uid);

    // Find all documents for a user (used for filtering during specific deletes)
    List<KnowledgeDocument> findByUid(String uid);

    // Used for the "Wipe Memory" feature to clear a specific user's SQL history
    @Transactional
    void deleteByUid(String uid);
}