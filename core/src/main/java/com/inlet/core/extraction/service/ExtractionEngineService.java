package com.inlet.core.extraction.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.List;

import com.inlet.core.extraction.dto.ExtractedTicketDTO;

@Service
public class ExtractionEngineService {

    private final ChatClient chatClient;
    private final GmailListenerService gmailListenerService;

    public ExtractionEngineService(ChatClient.Builder chatClientBuilder, GmailListenerService gmailListenerService) {
        this.chatClient = chatClientBuilder.build();
        this.gmailListenerService = gmailListenerService;
    }

    public List<ExtractedTicketDTO> processUnreadEmails() {
        List<String> rawEmails = gmailListenerService.fetchUnreadCustomerEmails();
        
        return rawEmails.stream()
                .map(this::extractTicketData)
                .toList();
    }

    private ExtractedTicketDTO extractTicketData(String emailContent) {
        String promptText = """
            Analyze this customer support email and extract the exact details into the required JSON format.
            
            Strict Rules:
            1. Extract the sender's exact email address.
            2. Extract the exact time it was received.
            3. If the customer's name is not explicitly stated in the body or easily parsed from the email address, return "Unknown". Do not guess.
            4. Summarize the core issue.
            
            Email Content:
            {email}
            """;

        return chatClient.prompt()
                .user(u -> u.text(promptText).param("email", emailContent))
                .call()
                .entity(ExtractedTicketDTO.class);
    }
}