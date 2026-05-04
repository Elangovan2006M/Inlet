package com.inlet.core.knowledgebase.controller;

import com.inlet.core.knowledgebase.service.KnowledgeIngestionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/training")
public class KnowledgeController {

    private final KnowledgeIngestionService ingestionService;

    public KnowledgeController(KnowledgeIngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @PostMapping("/upload-pdf")
    public ResponseEntity<String> uploadTrainingDocument(@RequestParam("file") MultipartFile file) {
        String result = ingestionService.ingestPdfDocument(file);
        return ResponseEntity.ok(result);
    }
}