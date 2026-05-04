package com.inlet.core.resolution.dto;


import com.inlet.core.extraction.dto.ExtractedTicketDTO;

public record TicketResolutionDTO(
        ExtractedTicketDTO originalTicket,
        String appliedRule,
        String suggestedEmailReply
) {}