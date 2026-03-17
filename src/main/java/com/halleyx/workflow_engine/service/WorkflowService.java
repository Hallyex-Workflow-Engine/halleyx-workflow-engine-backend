package com.halleyx.workflow_engine.service;
import com.halleyx.workflow_engine.dto.Request.WorkflowRequest;
import com.halleyx.workflow_engine.dto.Response.WorkflowResponse;
import com.halleyx.workflow_engine.entity.Rule;
import com.halleyx.workflow_engine.entity.Step;
import com.halleyx.workflow_engine.entity.Workflow;
import com.halleyx.workflow_engine.repo.RuleRepo;
import com.halleyx.workflow_engine.repo.StepRepo;
import com.halleyx.workflow_engine.repo.WorkflowRepo;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class WorkflowService {

    @Autowired
    private WorkflowRepo workflowRepository;
    @Autowired
    private StepRepo stepRepository;
    @Autowired
    private RuleRepo ruleRepository;
    @Autowired
    private ModelMapper modelMapper;

    public WorkflowResponse createWorkflow(WorkflowRequest request) {

        Workflow workflow = modelMapper.map(request, Workflow.class);

        workflow.setVersion(1);
        workflow.setIsActive(true);

        Workflow saved = workflowRepository.save(workflow);

        return modelMapper.map(saved, WorkflowResponse.class);
    }

    public WorkflowResponse createNewVersion(UUID workflowId, WorkflowRequest request) {

        Workflow existing = workflowRepository.findById(workflowId)
                .orElseThrow(() -> new RuntimeException("Workflow not found"));

        existing.setIsActive(false);
        workflowRepository.save(existing);

        Workflow newVersion = modelMapper.map(request, Workflow.class);
        newVersion.setVersion(existing.getVersion() + 1);
        newVersion.setIsActive(true);

        Workflow savedWorkflow = workflowRepository.save(newVersion);

        List<Step> oldSteps = stepRepository.findByWorkflowId(existing.getId());

        Map<UUID, UUID> stepIdMapping = new HashMap<>();

        for (Step oldStep : oldSteps) {

            Step newStep = new Step();
            newStep.setWorkflowId(savedWorkflow.getId());
            newStep.setName(oldStep.getName());
            newStep.setStepType(oldStep.getStepType());
            newStep.setStepOrder(oldStep.getStepOrder());
            newStep.setMetadata(oldStep.getMetadata());

            Step savedStep = stepRepository.save(newStep);

            stepIdMapping.put(oldStep.getId(), savedStep.getId());
        }

        for (Step oldStep : oldSteps) {

            List<Rule> rules = ruleRepository.findByStepId(oldStep.getId());

            for (Rule oldRule : rules) {

                Rule newRule = new Rule();
                newRule.setStepId(stepIdMapping.get(oldStep.getId()));
                newRule.setConditionExpr(oldRule.getConditionExpr());
                newRule.setPriority(oldRule.getPriority());
                if (oldRule.getNextStepId() != null) {
                    newRule.setNextStepId(
                            stepIdMapping.get(oldRule.getNextStepId())
                    );
                }

                ruleRepository.save(newRule);
            }
        }

        return modelMapper.map(savedWorkflow, WorkflowResponse.class);
    }
    public List<WorkflowResponse> getAllWorkflows() {
        return workflowRepository.findByIsActiveTrue()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public WorkflowResponse getWorkflowById(UUID id) {
        Workflow workflow = workflowRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("WorkFlow not found"+id));
        return toResponse(workflow);
    }
    public List<WorkflowResponse> searchByName(String keyword) {
        return workflowRepository.findByNameContainingIgnoreCase(keyword)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public WorkflowResponse updateWorkflow(UUID id, WorkflowRequest request) {

        Workflow oldVersion = workflowRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("WorkFlow not found"+id));

        oldVersion.setIsActive(false);
        workflowRepository.save(oldVersion);
        Workflow newVersion = modelMapper.map(request, Workflow.class);
        newVersion.setVersion(oldVersion.getVersion() + 1);
        newVersion.setIsActive(true);
        newVersion.setStartStepId(oldVersion.getStartStepId());
        Workflow saved = workflowRepository.save(newVersion);
        return toResponse(saved);
    }
    public WorkflowResponse setStartStep(UUID workflowId, UUID stepId) {
        Workflow workflow = workflowRepository.findById(workflowId)
                .orElseThrow(() -> new RuntimeException("WorkFlow not found"+workflowId));

        workflow.setStartStepId(stepId);
        return toResponse(workflowRepository.save(workflow));
    }
    public void deleteWorkflow(UUID id) {
        Workflow workflow = workflowRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("WorkFlow not found"+id));

        workflow.setIsActive(false);
        workflowRepository.save(workflow);
    }
    private WorkflowResponse toResponse(Workflow workflow) {
        WorkflowResponse response = modelMapper.map(workflow, WorkflowResponse.class);
        int count = stepRepository
                .countByWorkflowId(workflow.getId());
        response.setStepCount(count);
        return response;
    }
}