package com.inlet.core.knowledgebase.service;

import com.inlet.core.knowledgebase.model.KnowledgeDocument;
import com.inlet.core.knowledgebase.repository.KnowledgeDocumentRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class KnowledgeIngestionService {

    public static final ThreadLocal<String> CURRENT_UID = new ThreadLocal<>();

    private final VectorStore vectorStore;
    private final ChatClient chatClient;
    private final KnowledgeDocumentRepository repository;
    private final JdbcTemplate jdbcTemplate;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${supabase.url}") String supabaseUrl;
    @Value("${supabase.key}") String supabaseKey;
    private final String BUCKET_NAME = "knowledge-base";

    public KnowledgeIngestionService(VectorStore vectorStore, ChatClient.Builder chatClientBuilder, 
                                     KnowledgeDocumentRepository repository, JdbcTemplate jdbcTemplate) {
        this.vectorStore = vectorStore;
        this.chatClient = chatClientBuilder.build();
        this.repository = repository;
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<KnowledgeDocument> getUiHistory(String uid) {
        return repository.findByUidOrderByCreatedAtDesc(uid);
    }

    public String ingestPdfDocument(MultipartFile file, String uid) {
        try {
            String fileName = file.getOriginalFilename();
            String storagePath = uid + "/" + fileName;
            String storageUrl = supabaseUrl + "/storage/v1/object/" + BUCKET_NAME + "/" + storagePath;
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + supabaseKey);
            headers.set("apikey", supabaseKey);
            headers.set("x-upsert", "true"); 
            headers.setContentType(MediaType.valueOf(file.getContentType()));

            HttpEntity<byte[]> entity = new HttpEntity<>(file.getBytes(), headers);
            restTemplate.exchange(storageUrl, HttpMethod.POST, entity, String.class);

            String publicUrl = supabaseUrl + "/storage/v1/object/public/" + BUCKET_NAME + "/" + storagePath;

            KnowledgeDocument doc = repository.findByUid(uid).stream()
                    .filter(d -> d.getName().equals(fileName))
                    .findFirst()
                    .orElse(new KnowledgeDocument());

            doc.setName(fileName);
            doc.setType("PDF");
            doc.setUrl(publicUrl);
            doc.setUid(uid); 
            repository.save(doc);

            jdbcTemplate.update("DELETE FROM vector_store WHERE metadata->>'file_name' = ?", fileName);

            Resource pdfResource = file.getResource();
            PagePdfDocumentReader pdfReader = new PagePdfDocumentReader(pdfResource);
            TokenTextSplitter textSplitter = new TokenTextSplitter();
            
            List<Document> chunks = textSplitter.apply(pdfReader.get());
            for (Document chunk : chunks) {
                chunk.getMetadata().put("file_name", fileName);
                chunk.getMetadata().put("updated_by", uid); 
            }
            
            vectorStore.accept(chunks);
            return "Central Memory Synchronized: " + fileName;
        } catch (Exception e) {
            throw new RuntimeException("Global ingestion failed: " + e.getMessage(), e);
        }
    }

    public void ingestText(String text, String uid, String identifier) {
        String safeIdentifier = (identifier != null && !identifier.isBlank()) ? identifier : "manual-rule";
        String safeUid = (uid != null && !uid.isBlank()) ? uid : "system";

        jdbcTemplate.update("DELETE FROM vector_store WHERE metadata->>'file_name' = ?", safeIdentifier);

        Document doc = new Document(text);
        doc.getMetadata().put("file_name", safeIdentifier);
        doc.getMetadata().put("updated_by", safeUid);

        vectorStore.add(List.of(doc));

        KnowledgeDocument dbDoc = repository.findByUid(safeUid).stream()
                .filter(d -> d.getName().equals(safeIdentifier))
                .findFirst()
                .orElse(new KnowledgeDocument());

        dbDoc.setName(safeIdentifier);
        dbDoc.setType("TEXT");
        dbDoc.setContent(text);
        dbDoc.setUid(safeUid);
        repository.save(dbDoc);
    }

    public void deleteSpecificFile(String fileName, String uid) {
        repository.findByUid(uid).stream()
            .filter(d -> d.getName().equals(fileName))
            .findFirst()
            .ifPresent(doc -> {
                repository.delete(doc);
                jdbcTemplate.update("DELETE FROM vector_store WHERE metadata->>'file_name' = ?", fileName);
                try {
                    String storageUrl = supabaseUrl + "/storage/v1/object/" + BUCKET_NAME + "/" + uid + "/" + fileName;
                    HttpHeaders headers = new HttpHeaders();
                    headers.set("Authorization", "Bearer " + supabaseKey);
                    headers.set("apikey", supabaseKey);
                    restTemplate.exchange(storageUrl, HttpMethod.DELETE, new HttpEntity<>(headers), String.class);
                } catch(Exception e) { System.out.println("Storage cleanup skipped."); }
            });
    }

    public void wipeMemory(String uid) {
        List<KnowledgeDocument> userDocs = repository.findByUid(uid);
        for (KnowledgeDocument doc : userDocs) {
            jdbcTemplate.update("DELETE FROM vector_store WHERE metadata->>'file_name' = ?", doc.getName());
        }
        repository.deleteByUid(uid);
    }

    public String chatWithDeveloper(String message, String uid) {
        CURRENT_UID.set(uid);
        
        try {
            List<Document> docs = vectorStore.similaritySearch(
                SearchRequest.query(message).withTopK(10)
            );

            String context = docs.stream()
                    .map(d -> {
                        String lastUpdater = (String) d.getMetadata().getOrDefault("updated_by", "System/Original PDF");
                        return "--- DOCUMENT ---\n" +
                               "Rule Text: " + d.getContent() + "\n" +
                               "Metadata_LastUpdater: " + lastUpdater + "\n" +
                               "----------------";
                    })
                    .collect(Collectors.joining("\n\n"));

            return chatClient.prompt()
                    .system(s -> s.text("""
                        You are a Central Knowledge Manager. Answer based on the global context provided.
                        Each piece of context includes the UID of the developer who last updated it.
                        If the user asks who changed a rule, you can provide that UID.
                        
                        Context:
                        {context}
                        """)
                        .param("context", context))
                    .user(message)
                    .functions("updateRuleDatabaseTool")
                    .call()
                    .content();
                    
        } finally {
            CURRENT_UID.remove();
        }
    }
    
    public String getResolutionForCategory(String category, String uid) {
        List<Document> docs = vectorStore.similaritySearch(
            SearchRequest.query("Company policy regarding: " + category).withTopK(5)
        );
        if (docs.isEmpty()) return "No specific policy found in central memory.";
        return docs.stream().map(Document::getContent).collect(Collectors.joining("\n\n"));
    }
}