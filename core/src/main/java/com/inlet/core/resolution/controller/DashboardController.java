package com.inlet.core.resolution.controller;

import com.inlet.core.resolution.model.SupportTicket;
import com.inlet.core.resolution.service.TicketWorkflowService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

    private final TicketWorkflowService workflowService;

    public DashboardController(TicketWorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    @GetMapping("/pending")
    public List<SupportTicket> getPendingTicketsForSplitScreen() {
        return workflowService.getPendingTickets();
    }

    @PostMapping("/tickets/{id}/regenerate")
    public SupportTicket requestAiRewrite(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        return workflowService.regenerateDraft(id, payload.get("instructions"));
    }

    @PostMapping("/tickets/{id}/send")
    public SupportTicket manualSend(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        return workflowService.approveAndSend(id, payload.get("finalDraft"));
    }
}