package com.inlet.core.knowledgebase.controller;

import com.inlet.core.knowledgebase.model.KnowledgeDocument;
import com.inlet.core.knowledgebase.service.KnowledgeIngestionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/training")
public class KnowledgeController {

    private final KnowledgeIngestionService ingestionService;

    public KnowledgeController(KnowledgeIngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }


    @GetMapping("/history")
    public ResponseEntity<List<Map<String, String>>> getHistory(@RequestParam String uid) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy - hh:mm a");
        
        List<Map<String, String>> history = ingestionService.getUiHistory(uid).stream()
            .map(doc -> Map.of(
                "id", doc.getName(),
                "name", doc.getName(),
                "type", doc.getType(),
                "url", doc.getUrl() != null ? doc.getUrl() : "",
                "content", doc.getContent() != null ? doc.getContent() : "",
                "date", doc.getCreatedAt().format(formatter)
            ))
            .collect(Collectors.toList());
            
        return ResponseEntity.ok(history);
    }


    @PostMapping("/upload-pdf")
    public ResponseEntity<String> uploadTrainingDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam("uid") String uid) {
        return ResponseEntity.ok(ingestionService.ingestPdfDocument(file, uid));
    }


    @PostMapping("/chat")
    public ResponseEntity<Map<String, String>> developerChat(@RequestBody Map<String, String> payload) {
        String message = payload.get("message");
        String uid = payload.get("uid");
        
        String reply = ingestionService.chatWithDeveloper(message, uid);
        return ResponseEntity.ok(Map.of("reply", reply));
    }


    @DeleteMapping("/file/{fileName}")
    public ResponseEntity<String> deleteSpecificFile(
            @PathVariable String fileName,
            @RequestParam String uid) {
        ingestionService.deleteSpecificFile(fileName, uid);
        return ResponseEntity.ok("Deleted from history and global memory.");
    }


    @DeleteMapping("/clear")
    public ResponseEntity<String> clearMemory(@RequestParam String uid) {
        ingestionService.wipeMemory(uid);
        return ResponseEntity.ok("User history and associated global vectors wiped.");
    }
}