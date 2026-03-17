package com.halleyx.workflow_engine.controller;
import com.halleyx.workflow_engine.dto.Request.ExecutionRequest;
import com.halleyx.workflow_engine.dto.Response.ExecutionResponse;
import com.halleyx.workflow_engine.service.ExecutionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RequestMapping("/api")
@RestController
public class ExecutionController {

    @Autowired
    private  ExecutionService executionService;

    @PostMapping("/workflows/{workflowId}/execute")
    public ResponseEntity<ExecutionResponse> startExecution(
            @PathVariable UUID workflowId,
            @Valid @RequestBody ExecutionRequest request) {

        ExecutionResponse response = executionService.startExecution(workflowId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/executions/{id}")
    public ResponseEntity<ExecutionResponse> getExecution(
            @PathVariable UUID id) {

        return ResponseEntity.ok(executionService.getExecution(id));
    }

    @GetMapping("/executions")
    public ResponseEntity<List<ExecutionResponse>> getAllExecutions() {
        return ResponseEntity.ok(executionService.getAllExecutions());
    }

    @PostMapping("/executions/{id}/approve")
    public ResponseEntity<ExecutionResponse> approveStep(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body) {

        String approverId = body.get("approverId");
        return ResponseEntity.ok(executionService.approveStep(id, approverId));
    }

    @PostMapping("/executions/{id}/cancel")
    public ResponseEntity<ExecutionResponse> cancelExecution(
            @PathVariable UUID id) {

        return ResponseEntity.ok(executionService.cancelExecution(id));
    }

    @PostMapping("/executions/{id}/retry")
    public ResponseEntity<ExecutionResponse> retryExecution(
            @PathVariable UUID id) {

        return ResponseEntity.ok(executionService.retryExecution(id));
    }
    @GetMapping("/executions/pending")
    public ResponseEntity<List<ExecutionResponse>> getPendingApprovals(
            @RequestParam String email) {
        return ResponseEntity.ok(executionService.getPendingApprovals(email));
    }
    @PostMapping("/executions/{id}/reject")
    public ResponseEntity<ExecutionResponse> rejectStep(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body) {

        String rejectorId = body.get("rejectorId");
        String comment    = body.get("comment");
        return ResponseEntity.ok(executionService.rejectStep(id, rejectorId, comment));
    }
}