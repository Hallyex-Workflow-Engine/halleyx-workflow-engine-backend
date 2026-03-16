package com.halleyx.workflow_engine.service;



import com.halleyx.workflow_engine.dto.Request.RuleRequest;
import com.halleyx.workflow_engine.dto.Request.StepRequest;
import com.halleyx.workflow_engine.entity.Step;
import com.halleyx.workflow_engine.entity.Workflow;
import com.halleyx.workflow_engine.repo.RuleRepo;
import com.halleyx.workflow_engine.repo.StepRepo;
import com.halleyx.workflow_engine.repo.WorkflowRepo;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class StepService {

    @Autowired
    private  StepRepo stepRepository;
    @Autowired
    private  WorkflowRepo workflowRepository;
    @Autowired
    private  RuleRepo ruleRepository;
    @Autowired
    private  ModelMapper modelMapper;


    public Step addStep(UUID workflowId, StepRequest request) {

        Workflow workflow = workflowRepository.findById(workflowId)
                .orElseThrow(() -> new RuntimeException("Workflow not found: " + workflowId));

        Step step = new Step();
        step.setWorkflowId(workflowId);
        step.setName(request.getName());
        step.setStepType(request.getStepType());
        step.setStepOrder(request.getStepOrder());
        step.setMetadata(request.getMetadata());
        Step saved = stepRepository.save(step);
        if (workflow.getStartStepId() == null) {
            workflow.setStartStepId(saved.getId());
            workflowRepository.save(workflow);
        }

        return saved;
    }
    public List<Step> getStepsByWorkflow(UUID workflowId) {
        workflowRepository.findById(workflowId)
                .orElseThrow(() -> new RuntimeException("work-flow not found"+workflowId));

        return stepRepository.findByWorkflowIdOrderByStepOrderAsc(workflowId);
    }

    public Step getStepById(UUID stepId) {
        return stepRepository.findById(stepId)
                .orElseThrow(() -> new RuntimeException("work-flow not found"+stepId));
    }
    public Step updateStep(UUID stepId, StepRequest request) {
        Step existing = stepRepository.findById(stepId)
                .orElseThrow(() -> new RuntimeException("Step not found: " + stepId));

        existing.setName(request.getName());
        existing.setStepType(request.getStepType());
        existing.setStepOrder(request.getStepOrder());
        existing.setMetadata(request.getMetadata());

        return stepRepository.save(existing);
    }
    public void deleteStep(UUID stepId) {
        stepRepository.findById(stepId)
                .orElseThrow(() -> new RuntimeException("work-flow not found"+stepId));

        ruleRepository.deleteByStepId(stepId);
        stepRepository.deleteById(stepId);
    }
}