package com.inlet.core.extraction.controller;

import org.springframework.web.bind.annotation.*;
import com.inlet.core.extraction.dto.ExtractedTicketDTO;
import com.inlet.core.extraction.service.GmailListenerService;
import com.inlet.core.knowledgebase.service.KnowledgeIngestionService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/tickets")
public class TicketController {

    private final GmailListenerService gmailListenerService;
    private final KnowledgeIngestionService knowledgeService;
    private final VectorStore vectorStore;
    private final ChatClient chatClient;

    public TicketController(GmailListenerService gmailListenerService, 
                            KnowledgeIngestionService knowledgeService,
                            VectorStore vectorStore,
                            ChatClient.Builder chatClientBuilder) {
        this.gmailListenerService = gmailListenerService;
        this.knowledgeService = knowledgeService;
        this.vectorStore = vectorStore;
        this.chatClient = chatClientBuilder.build();
    }


    @GetMapping("/scan")
    public List<ExtractedTicketDTO> triggerInboxScanOnly() {
        return gmailListenerService.fetchUnreadCustomerEmails();
    }


    @GetMapping("/{id}/details")
    public ExtractedTicketDTO getTicketDetails(@PathVariable String id) {
        return gmailListenerService.fetchEmailDetails(id);
    }


    @PostMapping("/generate-reply")
    public Map<String, String> generateAiReply(@RequestBody Map<String, String> payload) {
        String customerMessage = payload.get("customerMessage");
        String customerName = payload.get("customerName");
        
        List<Document> docs = vectorStore.similaritySearch(
            SearchRequest.query("How to resolve: " + customerMessage).withTopK(3)
        );
        
        String centralPolicies = docs.stream()
                .map(Document::getContent)
                .collect(Collectors.joining("\n\n"));

        String draft = chatClient.prompt()
                .system(s -> s.text("""
                    You are an expert customer support agent for Inlet.
                    Write a professional, empathetic reply to the customer based ONLY on the company policies provided below.
                    Address the customer by name.
                    Do not make up policies.
                    
                    Company Policies:
                    {policies}
                    """)
                    .param("policies", centralPolicies))
                .user("Customer Name: " + customerName + "\n\nCustomer Message: " + customerMessage)
                .call()
                .content();

        return Map.of("draft", draft);
    }
    

    @PostMapping("/mark-read/{id}")
    public void markAsRead(@PathVariable String id) {
        gmailListenerService.markEmailAsRead(id);
    }
    
    @PostMapping("/send-reply")
    public void sendReply(@RequestBody Map<String, String> payload) {
        String to = payload.get("to");
        String subject = payload.get("subject");
        String body = payload.get("body");
        String messageId = payload.get("messageId");
        
        gmailListenerService.sendReplyEmail(to, subject, body, messageId);
    }
}