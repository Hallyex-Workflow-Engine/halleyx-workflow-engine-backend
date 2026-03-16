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

@RestController
@CrossOrigin(origins = "*")
public class ExecutionController {

    @Autowired
    private  ExecutionService executionService;

    // POST /workflows/{workflowId}/execute — start execution
    @PostMapping("/workflows/{workflowId}/execute")
    public ResponseEntity<ExecutionResponse> startExecution(
            @PathVariable UUID workflowId,
            @Valid @RequestBody ExecutionRequest request) {

        ExecutionResponse response = executionService.startExecution(workflowId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // GET /executions/{id} — get execution status and logs
    @GetMapping("/executions/{id}")
    public ResponseEntity<ExecutionResponse> getExecution(
            @PathVariable UUID id) {

        return ResponseEntity.ok(executionService.getExecution(id));
    }

    // GET /executions — get all executions (audit log)
    @GetMapping("/executions")
    public ResponseEntity<List<ExecutionResponse>> getAllExecutions() {
        return ResponseEntity.ok(executionService.getAllExecutions());
    }

    // POST /executions/{id}/approve — human approves an approval step
    @PostMapping("/executions/{id}/approve")
    public ResponseEntity<ExecutionResponse> approveStep(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body) {

        String approverId = body.get("approverId");
        return ResponseEntity.ok(executionService.approveStep(id, approverId));
    }

    // POST /executions/{id}/cancel — cancel a running execution
    @PostMapping("/executions/{id}/cancel")
    public ResponseEntity<ExecutionResponse> cancelExecution(
            @PathVariable UUID id) {

        return ResponseEntity.ok(executionService.cancelExecution(id));
    }

    // POST /executions/{id}/retry — retry a failed execution
    @PostMapping("/executions/{id}/retry")
    public ResponseEntity<ExecutionResponse> retryExecution(
            @PathVariable UUID id) {

        return ResponseEntity.ok(executionService.retryExecution(id));
    }
}