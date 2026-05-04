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
import com.google.api.services.gmail.model.MessagePartHeader;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
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
        if (in == null) {
            throw new RuntimeException("credentials.json not found in resources folder");
        }
        
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                httpTransport, JSON_FACTORY, clientSecrets, Collections.singletonList(GmailScopes.GMAIL_READONLY))
                .setAccessType("offline")
                .build();

        Credential credential = new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver()).authorize("user");

        gmailClient = new Gmail.Builder(httpTransport, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    public List<String> fetchUnreadCustomerEmails() {
        List<String> extractedEmails = new ArrayList<>();

        try {
            List<Message> messages = gmailClient.users().messages().list("me")
                    .setQ("is:unread")
                    .execute()
                    .getMessages();

            if (messages == null || messages.isEmpty()) {
                return extractedEmails;
            }

            for (Message msgReference : messages) {
                Message fullMessage = gmailClient.users().messages().get("me", msgReference.getId()).execute();
                
                String fromAddress = "";
                String subject = "";
                String receivedTime = "";
                
                for (MessagePartHeader header : fullMessage.getPayload().getHeaders()) {
                    if (header.getName().equalsIgnoreCase("From")) {
                        fromAddress = header.getValue();
                    } else if (header.getName().equalsIgnoreCase("Subject")) {
                        subject = header.getValue();
                    } else if (header.getName().equalsIgnoreCase("Date")) {
                        receivedTime = header.getValue();
                    }
                }

                // --- THE SMRATER EXTRACTION LOGIC ---
                String rawBodyBase64 = null;
                if (fullMessage.getPayload().getParts() != null && !fullMessage.getPayload().getParts().isEmpty()) {
                    // It's a Multipart email
                    rawBodyBase64 = fullMessage.getPayload().getParts().get(0).getBody().getData();
                } else if (fullMessage.getPayload().getBody() != null) {
                    // It's a Simple Plain Text email
                    rawBodyBase64 = fullMessage.getPayload().getBody().getData();
                }

                String decodedBody = "No text content found.";
                if (rawBodyBase64 != null) {
                    decodedBody = new String(Base64.getUrlDecoder().decode(rawBodyBase64), StandardCharsets.UTF_8);
                }

                String formattedEmailContext = String.format("Time: %s\nFrom: %s\nSubject: %s\nBody:\n%s", 
                        receivedTime, fromAddress, subject, decodedBody);
                
                extractedEmails.add(formattedEmailContext);
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return extractedEmails;
    }

    
}