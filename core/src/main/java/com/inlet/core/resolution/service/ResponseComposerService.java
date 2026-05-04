package com.inlet.core.resolution.service;

import com.inlet.core.extraction.dto.ExtractedTicketDTO;
import com.inlet.core.resolution.dto.TicketResolutionDTO;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ResponseComposerService {

    private final ChatClient chatClient;
    private final VectorStore vectorStore;
    private final TicketWorkflowService ticketWorkflowService;

    public ResponseComposerService(
            ChatClient.Builder chatClientBuilder, 
            VectorStore vectorStore,
            TicketWorkflowService ticketWorkflowService) {
        this.chatClient = chatClientBuilder.build();
        this.vectorStore = vectorStore;
        this.ticketWorkflowService = ticketWorkflowService;
    }

    public TicketResolutionDTO draftReply(ExtractedTicketDTO ticket) {
        // 1. Search the Vector Database for rules related to the customer's exact issue
        List<Document> similarDocuments = vectorStore.similaritySearch(
                SearchRequest.query(ticket.contentSummary()).withTopK(2)
        );
        
        String companyContext = similarDocuments.stream()
                .map(Document::getContent)
                .collect(Collectors.joining("\n\n"));

        if (companyContext.isEmpty()) {
            companyContext = "No specific rules found. Escalate to a human agent gently.";
        }

        // --- THE FIX: Create a locked, final copy for the Lambda ---
        final String finalCompanyContext = companyContext;

        // 2. Feed the Vector Context to the NVIDIA AI
        String promptText = """
            You are an expert, professional customer support agent for Inlet.
            Draft a polite and empathetic email reply to the customer based STRICTLY on the provided Company Policy.
            
            Company Policy / Context:
            {context}
            
            Customer Context:
            Name: {name}
            Issue Summary: {issue}
            
            Constraints:
            1. Write ONLY the exact email body to be sent. No introductory text.
            2. Keep it concise, warm, and professional.
            3. Do not invent policies outside of the provided context.
            """;

        String draftedEmail = chatClient.prompt()
                .user(u -> u.text(promptText)
                        .param("context", finalCompanyContext) // USING THE FINAL COPY
                        .param("name", ticket.customerName())
                        .param("issue", ticket.contentSummary()))
                .call()
                .content();

        ticketWorkflowService.createPendingTicket(ticket, draftedEmail);

        return new TicketResolutionDTO(ticket, finalCompanyContext, draftedEmail); // USING THE FINAL COPY
    }
}