package com.inlet.core.knowledgebase.service;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;
import java.util.function.Function;

@Configuration
public class AiKnowledgeTools {

    private final KnowledgeIngestionService ingestionService;

    public AiKnowledgeTools(KnowledgeIngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    public record RuleUpdateRequest(
            @Description("A short, unique identifier for the rule being updated (e.g., 'Rule 10 - International Support').")
            String ruleIdentifier,
            @Description("The full, final text of the rule to be saved in central memory. " +
                         "CRITICAL: DO NOT include any 'Last Updated By' or 'UID' tags in this string. " +
                         "Provide ONLY the pure rule text.") 
            String completeNewRule
        ) {}

    @Bean
    @Description("Update or add a policy to the central knowledge base. " +
                 "This update will be reflected for all developers in the system. " +
                 "Pass the FULL new rule text into 'completeNewRule'.")
    public Function<RuleUpdateRequest, String> updateRuleDatabaseTool() {
        return request -> {

            String uid = KnowledgeIngestionService.CURRENT_UID.get();
            
            if (uid != null) {

                ingestionService.ingestText("CRITICAL OVERRIDE: " + request.completeNewRule(), uid, request.ruleIdentifier());
                

                return "Database successfully updated. Inform the user that the rule was updated globally.";
            }
            
            return "Failed: No user context found. Could not update database.";
        };
    }
}