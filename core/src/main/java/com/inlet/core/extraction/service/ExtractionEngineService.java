package com.inlet.core.extraction.service;

import org.springframework.stereotype.Service;
import java.util.List;
import com.inlet.core.extraction.dto.ExtractedTicketDTO;

@Service
public class ExtractionEngineService {

    private final GmailListenerService gmailListenerService;

    // We no longer inject the ChatClient here because extraction is now 
    // handled instantly by standard Java in the GmailListenerService!
    public ExtractionEngineService(GmailListenerService gmailListenerService) {
        this.gmailListenerService = gmailListenerService;
    }

    public List<ExtractedTicketDTO> processUnreadEmails() {
        // Just return the fast-parsed emails directly
        return gmailListenerService.fetchUnreadCustomerEmails();
    }
}