package com.halleyx.workflow_engine.controller;



import com.halleyx.workflow_engine.dto.Request.WorkflowRequest;
import com.halleyx.workflow_engine.dto.Response.WorkflowResponse;
import com.halleyx.workflow_engine.service.WorkflowService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/workflows")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class WorkflowController {

    private final WorkflowService workflowService;
    @PostMapping
    public ResponseEntity<WorkflowResponse> createWorkflow(
            @Valid @RequestBody WorkflowRequest request) {

        WorkflowResponse response = workflowService.createWorkflow(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    @GetMapping
    public ResponseEntity<List<WorkflowResponse>> getAllWorkflows() {
        return ResponseEntity.ok(workflowService.getAllWorkflows());
    }
    @GetMapping("/search")
    public ResponseEntity<List<WorkflowResponse>> searchWorkflows(
            @RequestParam String name) {

        return ResponseEntity.ok(workflowService.searchByName(name));
    }
    @GetMapping("/{id}")
    public ResponseEntity<WorkflowResponse> getWorkflowById(
            @PathVariable UUID id) {

        return ResponseEntity.ok(workflowService.getWorkflowById(id));
    }
    @PutMapping("/{id}")
    public ResponseEntity<WorkflowResponse> updateWorkflow(
            @PathVariable UUID id,
            @Valid @RequestBody WorkflowRequest request) {

        return ResponseEntity.ok(workflowService.updateWorkflow(id, request));
    }
    @PutMapping("/{id}/start-step/{stepId}")
    public ResponseEntity<WorkflowResponse> setStartStep(
            @PathVariable UUID id,
            @PathVariable UUID stepId) {

        return ResponseEntity.ok(workflowService.setStartStep(id, stepId));
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteWorkflow(@PathVariable UUID id) {
        workflowService.deleteWorkflow(id);
        return ResponseEntity.noContent().build();
    }
}