package com.halleyx.workflow_engine.service;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.halleyx.workflow_engine.dto.Request.ExecutionRequest;
import com.halleyx.workflow_engine.dto.Response.ExecutionResponse;
import com.halleyx.workflow_engine.engine.RuleEngine;
import com.halleyx.workflow_engine.entity.Enum.ExecutionStatus;
import com.halleyx.workflow_engine.entity.Enum.StepType;
import com.halleyx.workflow_engine.entity.Execution;
import com.halleyx.workflow_engine.entity.Step;
import com.halleyx.workflow_engine.repo.*;
import com.halleyx.workflow_engine.entity.Workflow;
import com.halleyx.workflow_engine.entity.Rule;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@Transactional
public class ExecutionService {

    @Autowired
    private  ExecutionRepo executionRepository;
    @Autowired
    private  WorkflowRepo  workflowRepository;
    @Autowired
    private  StepRepo  stepRepository;
    @Autowired
    private  RuleRepo ruleRepository;
    @Autowired
    private  RuleEngine  ruleEngine;
    @Autowired
    private  ModelMapper modelMapper;
    @Autowired
    private  ObjectMapper objectMapper;


    public ExecutionResponse startExecution(UUID workflowId, ExecutionRequest request) {

        Workflow workflow = workflowRepository.findById(workflowId)
                .orElseThrow(() -> new RuntimeException("Workflow id"+ workflowId));

        if (!workflow.getIsActive())
            throw new IllegalStateException("Cannot execute an inactive workflow");

        if (workflow.getStartStepId() == null)
            throw new IllegalStateException("Workflow has no start step. Add steps first.");

        // use ModelMapper to map request → Execution for common fields
        Execution execution = modelMapper.map(request, Execution.class);

        // set fields that are NOT in request — server decides these
        execution.setWorkflowId(workflowId);
        execution.setWorkflowVersion(workflow.getVersion());
        execution.setStatus(ExecutionStatus.IN_PROGRESS);
        execution.setInputData(toJson(request.getInputData()));
        execution.setCurrentStepId(workflow.getStartStepId());
        execution.setLogs("[]");
        execution.setRetries(0);

        execution = executionRepository.save(execution);

        // process first step
        execution = processStep(execution, request.getInputData());

        return toResponse(execution, workflow);
    }


    public ExecutionResponse getExecution(UUID executionId) {
        Execution execution = findExecution(executionId);
        Workflow workflow = findWorkflow(execution.getWorkflowId());
        return toResponse(execution, workflow);
    }


    public List<ExecutionResponse> getAllExecutions() {
        List<Execution> executions = executionRepository
                .findAllByOrderByStartedAtDesc();
        List<ExecutionResponse> result = new ArrayList<>();

        for (Execution execution : executions) {
            try {
                Workflow workflow = workflowRepository
                        .findById(execution.getWorkflowId())
                        .orElse(null);
                if (workflow == null) continue;
                result.add(toResponse(execution, workflow));
            } catch (Exception e) {
                // skip broken executions silently
            }
        }
        return result;
    }

    public ExecutionResponse approveStep(UUID executionId, String approverId) {

        Execution execution = findExecution(executionId);

        if (execution.getStatus() != ExecutionStatus.IN_PROGRESS)
            throw new IllegalStateException("Execution is not in progress");

        Step currentStep = findStep(execution.getCurrentStepId());

        if (currentStep.getStepType() != StepType.APPROVAL)
            throw new IllegalStateException("Current step is not an approval step");

        addLog(execution, currentStep, approverId, null);

        Map<String, Object> inputData = fromJson(execution.getInputData());

        List<Rule> rules = ruleRepository.findByStepIdOrderByPriorityAsc(currentStep.getId());
        String nextStepId = ruleEngine.evaluate(rules, inputData, execution);

        if (nextStepId == null) {
            execution.setStatus(ExecutionStatus.COMPLETED);
            execution.setEndedAt(LocalDateTime.now());
            execution.setCurrentStepId(null);
            execution = executionRepository.save(execution);
        } else {
            execution.setCurrentStepId(UUID.fromString(nextStepId));
            execution = executionRepository.save(execution);
            execution = processStep(execution, inputData);
        }

        return toResponse(execution, findWorkflow(execution.getWorkflowId()));
    }


    public ExecutionResponse cancelExecution(UUID executionId) {
        Execution execution = findExecution(executionId);

        if (execution.getStatus() != ExecutionStatus.IN_PROGRESS)
            throw new IllegalStateException("Only in-progress executions can be canceled");

        execution.setStatus(ExecutionStatus.CANCELED);
        execution.setEndedAt(LocalDateTime.now());
        execution = executionRepository.save(execution);

        return toResponse(execution, findWorkflow(execution.getWorkflowId()));
    }

    public ExecutionResponse retryExecution(UUID executionId) {
        Execution execution = findExecution(executionId);

        if (execution.getStatus() != ExecutionStatus.FAILED)
            throw new IllegalStateException("Only failed executions can be retried");

        execution.setStatus(ExecutionStatus.IN_PROGRESS);
        execution.setRetries(execution.getRetries() + 1);

        Map<String, Object> inputData = fromJson(execution.getInputData());
        execution = processStep(execution, inputData);

        return toResponse(execution, findWorkflow(execution.getWorkflowId()));
    }


    private Execution processStep(Execution execution, Map<String, Object> inputData) {

        if (execution.getCurrentStepId() == null) return execution;

        Step step = stepRepository.findById(execution.getCurrentStepId()).orElse(null);
        if (step == null) return execution;

        try {
            if (step.getStepType() == StepType.APPROVAL) {
                return executionRepository.save(execution);
            }

            addLog(execution, step, null, null);

            List<Rule> rules = ruleRepository.findByStepIdOrderByPriorityAsc(step.getId());
            String nextStepId = ruleEngine.evaluate(rules, inputData, execution);

            if (nextStepId == null) {
                execution.setStatus(ExecutionStatus.COMPLETED);
                execution.setEndedAt(LocalDateTime.now());
                execution.setCurrentStepId(null);
            } else {
                execution.setCurrentStepId(UUID.fromString(nextStepId));
                execution = executionRepository.save(execution);
                return processStep(execution, inputData);
            }

        } catch (Exception e) {
            execution.setStatus(ExecutionStatus.FAILED);
            execution.setEndedAt(LocalDateTime.now());
            addLog(execution, step, null, e.getMessage());
        }

        return executionRepository.save(execution);
    }

    private void addLog(Execution execution, Step step,
                        String approverId, String error) {
        try {
            List<Map<String, Object>> logs = objectMapper.readValue(
                    execution.getLogs(), new TypeReference<>() {});

            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("step_name",   step.getName());
            entry.put("step_type",   step.getStepType().toString());
            entry.put("status",      error == null ? "completed" : "failed");
            entry.put("approver_id", approverId);
            entry.put("error",       error);
            entry.put("timestamp",   LocalDateTime.now().toString());

            logs.add(entry);
            execution.setLogs(objectMapper.writeValueAsString(logs));

        } catch (Exception ignored) {
            execution.setLogs("[]");
        }
    }

    private ExecutionResponse toResponse(Execution execution, Workflow workflow) {

        // Step 1 — ModelMapper auto-copies 9 matching fields:
        // id, workflowId, workflowVersion, status, currentStepId,
        // retries, triggeredBy, startedAt, endedAt
        ExecutionResponse response = modelMapper.map(execution, ExecutionResponse.class);

        // Step 2 — manually set the 4 fields ModelMapper cannot handle

        // from Workflow object — different source
        response.setWorkflowName(workflow.getName());

        // String JSON → Map (type mismatch)
        response.setInputData(fromJson(execution.getInputData()));

        // String JSON → List (type mismatch)
        response.setLogs(logsFromJson(execution.getLogs()));

        // from Step table — needs a DB query
        if (execution.getCurrentStepId() != null) {
            String stepName = stepRepository
                    .findById(execution.getCurrentStepId())
                    .map(Step::getName)
                    .orElse(null);
            response.setCurrentStepName(stepName);
        }

        return response;
    }


    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "{}";
        }
    }

    private Map<String, Object> fromJson(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    private List<Map<String, Object>> logsFromJson(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private Execution findExecution(UUID id) {
        return executionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Execution id"+ id));
    }

    private Workflow findWorkflow(UUID id) {
        return workflowRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Workflow id"+ id));
    }

    private Step findStep(UUID id) {
        return stepRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Step id"+ id));
    }
}