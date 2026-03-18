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
    public Execution processStep(Execution execution, Map<String, Object> inputData) {

        if (execution.getCurrentStepId() == null) return execution;

        if (execution.getRetries() != null && execution.getRetries() > 10) {
            execution.setStatus(ExecutionStatus.FAILED);
            return executionRepository.save(execution);
        }

        Step step = stepRepository.findById(execution.getCurrentStepId())
                .orElseThrow(() -> new RuntimeException("Step not found"));

        // APPROVAL — stop and wait for human
        if (step.getStepType() == StepType.APPROVAL) {
            execution.setStatus(ExecutionStatus.IN_PROGRESS);
            return executionRepository.save(execution);
        }

        addLog(execution, step, null, null);

        List<Rule> rules = ruleRepository.findByStepIdOrderByPriorityAsc(step.getId());
        String nextStepId = ruleEngine.evaluate(rules, inputData, execution);

        if (nextStepId == null) {
            // ✅ if step name contains "reject" → FAILED, otherwise COMPLETED
            boolean isRejection = step.getName().toLowerCase().contains("reject");
            execution.setStatus(isRejection ? ExecutionStatus.FAILED : ExecutionStatus.COMPLETED);
            execution.setEndedAt(LocalDateTime.now());
            execution.setCurrentStepId(null);
            return executionRepository.save(execution);
        }

        execution.setCurrentStepId(UUID.fromString(nextStepId));
        execution = executionRepository.save(execution);
        return processStep(execution, inputData);
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

            }
        }
        return result;
    }

    public ExecutionResponse approveStep(UUID executionId, String approverId) {
        Execution execution = findExecution(executionId);
        if (execution.getStatus() != ExecutionStatus.IN_PROGRESS) {
            throw new IllegalStateException("Execution is not waiting for approval");
        }
        Step currentStep = findStep(execution.getCurrentStepId());

        if (currentStep.getStepType() != StepType.APPROVAL) {
            throw new IllegalStateException("Current step is not an approval step");
        }
        addLog(execution, currentStep, approverId, null);
        Map<String, Object> inputData = fromJson(execution.getInputData());
        List<Rule> rules = ruleRepository.findByStepIdOrderByPriorityAsc(currentStep.getId());

        if (rules.isEmpty()) {
            throw new RuntimeException("No rules found for approval step");
        }
        String nextStepId = ruleEngine.evaluate(rules, inputData, execution);

        if (nextStepId == null || nextStepId.isBlank()) {

            execution.setStatus(ExecutionStatus.COMPLETED);
            execution.setEndedAt(LocalDateTime.now());
            execution.setCurrentStepId(null);

            execution = executionRepository.save(execution);

            return toResponse(execution, findWorkflow(execution.getWorkflowId()));
        }
        execution.setCurrentStepId(UUID.fromString(nextStepId));
        execution.setStatus(ExecutionStatus.IN_PROGRESS);

        execution = executionRepository.save(execution);
        execution = processStep(execution, inputData);

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

        ExecutionResponse response = modelMapper.map(execution, ExecutionResponse.class);

        response.setWorkflowName(workflow.getName());

        response.setInputData(fromJson(execution.getInputData()));

        response.setLogs(logsFromJson(execution.getLogs()));

        if (execution.getCurrentStepId() != null) {
            stepRepository.findById(execution.getCurrentStepId())
                    .ifPresent(step -> {
                        response.setCurrentStepName(step.getName());
                        try {
                            if (step.getMetadata() != null && !step.getMetadata().isBlank()) {
                                Map<String, Object> meta = objectMapper.readValue(
                                        step.getMetadata(), new TypeReference<>() {}
                                );
                                response.setCurrentStepAssigneeEmail(
                                        (String) meta.get("assignee_email")
                                );
                            }
                        } catch (Exception ignored) {}
                    });
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

    public List<ExecutionResponse> getPendingApprovals(String email) {
        List<Execution> inProgress = executionRepository.findAllInProgress();
        List<ExecutionResponse> result = new ArrayList<>();

        for (Execution execution : inProgress) {
            try {
                if (execution.getCurrentStepId() == null) continue;

                Step currentStep = stepRepository
                        .findById(execution.getCurrentStepId())
                        .orElse(null);

                if (currentStep == null) continue;

                if (currentStep.getStepType() != StepType.APPROVAL) continue;
                String metadata = currentStep.getMetadata();
                if (metadata == null || metadata.isBlank()) continue;

                Map<String, Object> meta = objectMapper.readValue(
                        metadata, new TypeReference<>() {}
                );

                String assigneeEmail = (String) meta.get("assignee_email");
                if (email.equalsIgnoreCase(assigneeEmail)) {
                    Workflow workflow = workflowRepository
                            .findById(execution.getWorkflowId())
                            .orElse(null);
                    if (workflow == null) continue;
                    result.add(toResponse(execution, workflow));
                }

            } catch (Exception e) {
            }
        }
        return result;
    }
    public ExecutionResponse rejectStep(UUID executionId, String rejectorId, String comment) {
        Execution execution = findExecution(executionId);

        if (execution.getStatus() != ExecutionStatus.IN_PROGRESS)
            throw new IllegalStateException("Execution is not in progress");

        Step currentStep = findStep(execution.getCurrentStepId());

        if (currentStep.getStepType() != StepType.APPROVAL)
            throw new IllegalStateException("Current step is not an approval step");

        addLogWithComment(execution, currentStep, rejectorId, "REJECTED: " + comment);

        execution.setStatus(ExecutionStatus.FAILED);
        execution.setEndedAt(LocalDateTime.now());
        execution.setCurrentStepId(null);
        execution = executionRepository.save(execution);

        return toResponse(execution, findWorkflow(execution.getWorkflowId()));
    }

    private void addLogWithComment(Execution execution, Step step,
                                   String actorId, String comment) {
        try {
            List<Map<String, Object>> logs = objectMapper.readValue(
                    execution.getLogs(), new TypeReference<>() {});

            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("step_name",   step.getName());
            entry.put("step_type",   step.getStepType().toString());
            entry.put("status",      "rejected");
            entry.put("actor_id",    actorId);
            entry.put("comment",     comment);
            entry.put("timestamp",   LocalDateTime.now().toString());

            logs.add(entry);
            execution.setLogs(objectMapper.writeValueAsString(logs));
        } catch (Exception ignored) {
            execution.setLogs("[]");
        }
    }
}