package com.inlet.core.extraction.dto;

import java.util.List;

public record ExtractedTicketDTO(
    String id,
    String sender,
    String senderEmail,
    String subject,
    String time,
    boolean isUnread,
    String preview,
    String fullBody,
    List<AttachmentDTO> attachments
) {}