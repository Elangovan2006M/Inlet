package com.inlet.core.extraction.controller;

import com.inlet.core.extraction.dto.ExtractedTicketDTO;
import com.inlet.core.extraction.service.ExtractionEngineService;
import com.inlet.core.resolution.dto.TicketResolutionDTO;
import com.inlet.core.resolution.service.ResponseComposerService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tickets")
public class TicketController {

    private final ExtractionEngineService extractionEngineService;
    private final ResponseComposerService responseComposerService;

    public TicketController(ExtractionEngineService extractionEngineService, ResponseComposerService responseComposerService) {
        this.extractionEngineService = extractionEngineService;
        this.responseComposerService = responseComposerService;
    }

    @GetMapping("/scan")
    public List<ExtractedTicketDTO> triggerInboxScanOnly() {
        return extractionEngineService.processUnreadEmails();
    }

    @GetMapping("/process")
    public List<TicketResolutionDTO> triggerFullEndToEndPipeline() {
        List<ExtractedTicketDTO> rawTickets = extractionEngineService.processUnreadEmails();
        
        return rawTickets.stream()
                .map(responseComposerService::draftReply)
                .toList();
    }
}