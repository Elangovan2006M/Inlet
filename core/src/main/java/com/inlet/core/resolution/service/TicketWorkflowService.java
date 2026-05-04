package com.inlet.core.resolution.service;

import com.inlet.core.extraction.dto.ExtractedTicketDTO;
import com.inlet.core.knowledgebase.service.KnowledgeBaseService;
import com.inlet.core.resolution.model.SupportTicket;
import com.inlet.core.resolution.repository.SupportTicketRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TicketWorkflowService {

    private final SupportTicketRepository ticketRepository;
    private final ChatClient chatClient;
    private final KnowledgeBaseService knowledgeBaseService;

    public TicketWorkflowService(SupportTicketRepository ticketRepository, ChatClient.Builder chatClientBuilder, KnowledgeBaseService knowledgeBaseService) {
        this.ticketRepository = ticketRepository;
        this.chatClient = chatClientBuilder.build();
        this.knowledgeBaseService = knowledgeBaseService;
    }

    // 1. Save new inbox items to the database
    public SupportTicket createPendingTicket(ExtractedTicketDTO dto, String aiDraft) {
        SupportTicket ticket = new SupportTicket();
        ticket.setCustomerEmail(dto.emailAddress());
        ticket.setCustomerName(dto.customerName());
        ticket.setIssueCategory(dto.issueCategory());
        ticket.setOriginalMessage(dto.contentSummary());
        ticket.setAiDraftedReply(aiDraft);
        return ticketRepository.save(ticket);
    }

    // 2. Fetch for Frontend Dashboard
    public List<SupportTicket> getPendingTickets() {
        return ticketRepository.findByStatus("PENDING_REVIEW");
    }

    // 3. The "Regenerate" Button Logic
    public SupportTicket regenerateDraft(Long ticketId, String agentInstructions) {
        SupportTicket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket not found"));

        String rule = knowledgeBaseService.getResolutionForCategory(ticket.getIssueCategory());

        String promptText = """
            You are an expert customer support agent. 
            Rewrite the email draft below based strictly on the Agent's new instructions.
            
            Original Customer Issue: {issue}
            Company Rule: {rule}
            Previous Draft: {oldDraft}
            
            Agent Instructions for Rewrite: {instructions}
            
            Return ONLY the new email body.
            """;

        String newDraft = chatClient.prompt()
                .user(u -> u.text(promptText)
                        .param("issue", ticket.getOriginalMessage())
                        .param("rule", rule)
                        .param("oldDraft", ticket.getAiDraftedReply())
                        .param("instructions", agentInstructions))
                .call()
                .content();

        ticket.setAiDraftedReply(newDraft);
        return ticketRepository.save(ticket);
    }

    // 4. The "Send" Button Logic
    public SupportTicket approveAndSend(Long ticketId, String finalEditedDraft) {
        SupportTicket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket not found"));
        
        ticket.setAiDraftedReply(finalEditedDraft);
        ticket.setStatus("SENT");
        
        // TODO: In Phase 5, we will trigger Gmail API to actually send the email here.
        
        return ticketRepository.save(ticket);
    }
}