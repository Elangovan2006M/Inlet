package com.inlet.core.knowledgebase.service;

import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class KnowledgeIngestionService {

    private final VectorStore vectorStore;

    public KnowledgeIngestionService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    public String ingestPdfDocument(MultipartFile file) {
        try {
            // 1. Convert the uploaded file to a Spring Resource
            Resource pdfResource = file.getResource();

            // 2. Read the PDF
            PagePdfDocumentReader pdfReader = new PagePdfDocumentReader(pdfResource);
            
            // 3. Split the document into small chunks so the AI doesn't get overwhelmed
            TokenTextSplitter textSplitter = new TokenTextSplitter();
            
            // 4. Convert chunks to embeddings and save to the Vector Database
            vectorStore.accept(textSplitter.apply(pdfReader.get()));

            return "Successfully trained the AI with: " + file.getOriginalFilename();
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to process PDF", e);
        }
    }
}