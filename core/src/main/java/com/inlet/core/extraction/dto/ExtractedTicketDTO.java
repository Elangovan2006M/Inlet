package com.inlet.core.extraction.dto;

public record ExtractedTicketDTO(
        String emailAddress,
        String receivedTime,
        String issueCategory,
        String urgencyLevel,
        String contentSummary,
        String customerName
) {}