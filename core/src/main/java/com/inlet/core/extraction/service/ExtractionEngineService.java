package com.inlet.core.extraction.service;

import org.springframework.stereotype.Service;
import java.util.List;
import com.inlet.core.extraction.dto.ExtractedTicketDTO;

@Service
public class ExtractionEngineService {

    private final GmailListenerService gmailListenerService;

    public ExtractionEngineService(GmailListenerService gmailListenerService) {
        this.gmailListenerService = gmailListenerService;
    }

    public List<ExtractedTicketDTO> processUnreadEmails() {
        return gmailListenerService.fetchUnreadCustomerEmails();
    }
}