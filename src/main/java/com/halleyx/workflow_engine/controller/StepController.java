package com.halleyx.workflow_engine.controller;


import com.halleyx.workflow_engine.config.ModelMapperConfig;
import com.halleyx.workflow_engine.dto.Request.StepRequest;
import com.halleyx.workflow_engine.entity.Step;
import com.halleyx.workflow_engine.service.StepService;
import jakarta.validation.Valid;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RequestMapping("/api")
@RestController
public class StepController {

    @Autowired
    private StepService stepService;
    @Autowired
    private ModelMapper modelMapper;

    @PostMapping("/workflows/{workflowId}/steps")
    public ResponseEntity<Step> addStep(@PathVariable UUID workflowId, @RequestBody StepRequest req) {
        Step step = stepService.addStep(workflowId, req);

        return ResponseEntity.status(HttpStatus.CREATED).body(step);
    }

    @GetMapping("/workflows/{workflowId}/steps")
    public ResponseEntity<List<Step>> getlistOfStepForWrokFlow(@PathVariable UUID workflowId)
    {
        List<Step> steps = stepService.getStepsByWorkflow(workflowId);
         return ResponseEntity.ok(steps);
    }
    @GetMapping("/steps/{id}")
    public ResponseEntity<Step> getStepById(@PathVariable UUID id) {
        return ResponseEntity.ok(stepService.getStepById(id));
    }

    @PutMapping("/steps/{id}")
    public ResponseEntity<Step> updateStep(
            @PathVariable UUID id,
            @Valid @RequestBody StepRequest request) {

        return ResponseEntity.ok(stepService.updateStep(id, request));
    }
    @DeleteMapping("/steps/{id}")
    public ResponseEntity<Void> deleteStep(@PathVariable UUID id) {
        stepService.deleteStep(id);
        return ResponseEntity.noContent().build();
    }
}
