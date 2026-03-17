package com.halleyx.workflow_engine.controller;

import com.halleyx.workflow_engine.dto.Request.RuleRequest;
import com.halleyx.workflow_engine.entity.Rule;
import com.halleyx.workflow_engine.service.RuleService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RequestMapping("/api")
@RestController
public class RuleController {

    @Autowired
    private RuleService ruleService;

    @PostMapping("/steps/{stepId}/rules")
    public ResponseEntity<Rule> addRule(
            @PathVariable UUID stepId,
            @Valid @RequestBody RuleRequest request) {

        Rule rule = ruleService.addRule(stepId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(rule);
    }
    @GetMapping("/rules/{id}")
    public ResponseEntity<Rule> getRuleById(@PathVariable UUID id) {
        return ResponseEntity.ok(ruleService.getRuleById(id));
    }
    @GetMapping("/steps/{stepId}/rules")
    public ResponseEntity<List<Rule>> getAllRuleForStep(@PathVariable UUID stepId){
        return ResponseEntity.ok(ruleService.getRulesByStep(stepId));
    }
    @PutMapping("/rules/{id}")
    public ResponseEntity<Rule> updateRule(
            @PathVariable UUID id,
            @Valid @RequestBody RuleRequest request) {

        return ResponseEntity.ok(ruleService.updateRule(id, request));
    }
    @DeleteMapping("/rules/{id}")
    public ResponseEntity<Void> deleteRule(@PathVariable UUID id) {
        ruleService.deleteRule(id);
        return ResponseEntity.noContent().build();
    }
}
