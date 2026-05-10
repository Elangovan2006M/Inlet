package com.inlet.core.knowledgebase.repository;

import com.inlet.core.knowledgebase.model.KnowledgeDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface KnowledgeDocumentRepository extends JpaRepository<KnowledgeDocument, Long> {
    
    List<KnowledgeDocument> findByUidOrderByCreatedAtDesc(String uid);

    List<KnowledgeDocument> findByUid(String uid);

    @Transactional
    void deleteByUid(String uid);
}