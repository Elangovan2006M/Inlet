package com.inlet.core.extraction.service;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.MessagePart;
import com.google.api.services.gmail.model.MessagePartBody;
import com.google.api.services.gmail.model.MessagePartHeader;
import com.google.api.services.gmail.model.ModifyMessageRequest;
import com.inlet.core.extraction.dto.AttachmentDTO;
import com.inlet.core.extraction.dto.ExtractedTicketDTO;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

@Service
public class GmailListenerService {

    private Gmail gmailClient;
    private static final String APPLICATION_NAME = "Inlet Engine";
    private static final GsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

    @PostConstruct
    public void initGmailClient() throws Exception {
        InputStream in = GmailListenerService.class.getResourceAsStream("/credentials.json");
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                httpTransport, JSON_FACTORY, clientSecrets, Collections.singletonList(GmailScopes.MAIL_GOOGLE_COM))
                .setAccessType("offline")
                .build();

        Credential credential = new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver()).authorize("user");
        gmailClient = new Gmail.Builder(httpTransport, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME).build();
    }

    // --- STEP 1: THE FAST SCANNER (For the Inbox List) ---
    public List<ExtractedTicketDTO> fetchUnreadCustomerEmails() {
        List<ExtractedTicketDTO> extractedEmails = new ArrayList<>();

        try {
            List<Message> messages = gmailClient.users().messages().list("me")
                    .setQ("in:inbox").setMaxResults(30L).execute().getMessages();

            if (messages == null || messages.isEmpty()) return extractedEmails;

            for (Message msgReference : messages) {
                // LAZY LOADING FIX: We only ask for "metadata" (Headers) instead of "full" (Body/Files)
                Message metadataMsg = gmailClient.users().messages().get("me", msgReference.getId())
                        .setFormat("metadata")
                        .setMetadataHeaders(Arrays.asList("From", "Subject", "Date"))
                        .execute();
                
                String senderName = "Unknown Sender";
                String senderEmail = "unknown@email.com";
                String subject = "No Subject";
                String receivedTime = "";
                
                for (MessagePartHeader header : metadataMsg.getPayload().getHeaders()) {
                    if (header.getName().equalsIgnoreCase("From")) {
                        String headerValue = header.getValue();
                        
                        // EMAIL EXTRACTION FIX: Properly separate the Name and the Email Address
                        senderName = headerValue.replaceAll("<.*>", "").replace("\"", "").trim();
                        if (headerValue.contains("<") && headerValue.contains(">")) {
                            senderEmail = headerValue.substring(headerValue.indexOf("<") + 1, headerValue.indexOf(">")).trim();
                        } else {
                            senderEmail = headerValue.trim();
                        }
                        if (senderName.isEmpty()) senderName = senderEmail; // Fallback

                    } else if (header.getName().equalsIgnoreCase("Subject")) {
                        subject = header.getValue();
                    } else if (header.getName().equalsIgnoreCase("Date")) {
                        receivedTime = header.getValue().split("[-+]")[0].trim(); 
                    }
                }

                String previewSnippet = metadataMsg.getSnippet() != null ? metadataMsg.getSnippet() : "No preview...";
                boolean isUnread = metadataMsg.getLabelIds() != null && metadataMsg.getLabelIds().contains("UNREAD");

                extractedEmails.add(new ExtractedTicketDTO(
                    msgReference.getId(),
                    senderName,
                    senderEmail, // Passed to DTO
                    subject,
                    receivedTime,
                    isUnread,
                    previewSnippet,
                    null, // Body is null initially! (Lazy Loading)
                    new ArrayList<>() // Attachments are empty initially! (Lazy Loading)
                ));
            }

        } catch (Exception e) {
            System.err.println("Error fetching emails: " + e.getMessage());
        }

        return extractedEmails;
    }

    // --- STEP 2: THE DEEP FETCHER (For clicking a specific email) ---
    public ExtractedTicketDTO fetchEmailDetails(String messageId) {
        try {
            // Here we use "full" format to actually download the data and files
            Message fullMessage = gmailClient.users().messages().get("me", messageId)
                    .setFormat("full").execute();

            StringBuilder emailBody = new StringBuilder();
            List<AttachmentDTO> attachments = new ArrayList<>();
            extractContent(messageId, fullMessage.getPayload(), emailBody, attachments);

            String decodedBody = emailBody.toString().trim();
            if (decodedBody.isEmpty()) {
                decodedBody = "No plain text content found.";
            }

            // Return a partial DTO just containing the heavy data. 
            // The frontend will merge this with the data it already has.
            return new ExtractedTicketDTO(
                    messageId, "", "", "", "", false, "", 
                    decodedBody, attachments
            );

        } catch (Exception e) {
            System.err.println("Error fetching full email details: " + e.getMessage());
            return null;
        }
    }

    private void extractContent(String messageId, MessagePart part, StringBuilder body, List<AttachmentDTO> attachments) throws Exception {
        if (part.getFilename() != null && !part.getFilename().isEmpty()) {
            String attachmentId = part.getBody().getAttachmentId();
            if (attachmentId != null) {
                MessagePartBody attachBody = gmailClient.users().messages().attachments()
                        .get("me", messageId, attachmentId).execute();
                
                byte[] fileBytes = Base64.getUrlDecoder().decode(attachBody.getData());
                String standardBase64 = Base64.getEncoder().encodeToString(fileBytes);
                
                String dataUri = "data:" + part.getMimeType() + ";base64," + standardBase64;
                attachments.add(new AttachmentDTO(part.getFilename(), dataUri));
            }
        } else if (part.getMimeType().equalsIgnoreCase("text/plain") && part.getBody().getData() != null) {
            byte[] textBytes = Base64.getUrlDecoder().decode(part.getBody().getData());
            body.append(new String(textBytes, StandardCharsets.UTF_8)).append("\n");
        } else if (part.getParts() != null) {
            for (MessagePart subPart : part.getParts()) {
                extractContent(messageId, subPart, body, attachments);
            }
        }
    }

    public void markEmailAsRead(String messageId) {
        try {
            ModifyMessageRequest mods = new ModifyMessageRequest()
                    .setRemoveLabelIds(Collections.singletonList("UNREAD"));
            gmailClient.users().messages().modify("me", messageId, mods).execute();
        } catch (Exception e) {
            System.err.println("Error marking email as read: " + e.getMessage());
        }
    }
    
    
    public void sendReplyEmail(String toEmail, String subject, String bodyText, String originalMessageId) {
        try {
            Message originalMessage = gmailClient.users().messages().get("me", originalMessageId).setFormat("metadata").execute();
            String threadId = originalMessage.getThreadId();

            String messageIdHeader = "";
            for (MessagePartHeader header : originalMessage.getPayload().getHeaders()) {
                if (header.getName().equalsIgnoreCase("Message-ID")) {
                    messageIdHeader = header.getValue();
                    break;
                }
            }

            String cleanSubject = subject.toLowerCase().startsWith("re:") ? subject : "Re: " + subject;
            
            // --- THE FIX: Convert AI's Markdown to beautiful HTML ---
            String htmlBody = bodyText
                    .replace("\n", "<br>") // Convert line breaks to HTML breaks
                    .replaceAll("\\*\\*(.*?)\\*\\*", "<strong>$1</strong>") // Convert **bold** to <strong>
                    .replaceAll("\\*(.*?)\\*", "<em>$1</em>"); // Convert *italics* to <em>
            
            // --- THE FIX: Added Content-Type: text/html to the headers ---
            String rawEmailStr = "To: " + toEmail + "\r\n" +
                                 "Subject: " + cleanSubject + "\r\n" +
                                 "In-Reply-To: " + messageIdHeader + "\r\n" +
                                 "References: " + messageIdHeader + "\r\n" +
                                 "Content-Type: text/html; charset=\"UTF-8\"\r\n" +
                                 "\r\n" +
                                 htmlBody;

            String encodedEmail = Base64.getUrlEncoder().encodeToString(rawEmailStr.getBytes(StandardCharsets.UTF_8));

            Message message = new Message();
            message.setRaw(encodedEmail);
            message.setThreadId(threadId);

            gmailClient.users().messages().send("me", message).execute();
        } catch (Exception e) {
            System.err.println("Error sending email: " + e.getMessage());
            throw new RuntimeException("Failed to send email");
        }
    }
}