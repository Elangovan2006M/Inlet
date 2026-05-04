package com.inlet.core.knowledgebase.controller;

import org.springframework.web.bind.annotation.*;
import java.util.List;
import com.inlet.core.knowledgebase.model.SupportRule;
import com.inlet.core.knowledgebase.service.KnowledgeBaseService;

@RestController
@RequestMapping("/api/v1/rules")
public class RuleController {

    private final KnowledgeBaseService service;

    public RuleController(KnowledgeBaseService service) {
        this.service = service;
    }

    @PostMapping
    public SupportRule addRule(@RequestBody SupportRule rule) {
        return service.saveRule(rule);
    }

    @GetMapping
    public List<SupportRule> getAllRules() {
        return service.getAllRules();
    }
}