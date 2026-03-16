package com.halleyx.workflow_engine.service;
import com.halleyx.workflow_engine.dto.Request.WorkflowRequest;
import com.halleyx.workflow_engine.dto.Response.WorkflowResponse;
import com.halleyx.workflow_engine.entity.Workflow;
import com.halleyx.workflow_engine.repo.StepRepo;
import com.halleyx.workflow_engine.repo.WorkflowRepo;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
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
    private ModelMapper modelMapper;


    public WorkflowResponse createWorkflow(WorkflowRequest request) {
        Workflow workflow = modelMapper.map(request, Workflow.class);
        workflow.setVersion(1);
        workflow.setIsActive(true);
        Workflow saved = workflowRepository.save(workflow);
        return toResponse(saved);
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