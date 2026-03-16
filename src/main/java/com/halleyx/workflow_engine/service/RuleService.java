package com.halleyx.workflow_engine.service;

import com.halleyx.workflow_engine.dto.Request.RuleRequest;
import com.halleyx.workflow_engine.entity.Rule;
import com.halleyx.workflow_engine.repo.RuleRepo;
import com.halleyx.workflow_engine.repo.StepRepo;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class RuleService {

    @Autowired
    private  RuleRepo ruleRepository;
    @Autowired
    private  StepRepo stepRepository;
    @Autowired
    private  ModelMapper modelMapper;

    public Rule addRule(UUID stepId, RuleRequest request) {
        stepRepository.findById(stepId)
                .orElseThrow(() -> new RuntimeException("Step not found: " + stepId));

        Rule rule = new Rule();
        rule.setStepId(stepId);
        rule.setConditionExpr(request.getConditionExpr());
        rule.setNextStepId(request.getNextStepId());
        rule.setPriority(request.getPriority());

        return ruleRepository.save(rule);
    }
    public List<Rule> getRulesByStep(UUID stepId) {
        stepRepository.findById(stepId)
                .orElseThrow(() -> new RuntimeException("step not found"+stepId));

        return ruleRepository.findByStepIdOrderByPriorityAsc(stepId);
    }

    public Rule getRuleById(UUID ruleId) {
        return ruleRepository.findById(ruleId)
                .orElseThrow(() -> new RuntimeException("Rule not found"+ruleId));
    }

    public Rule updateRule(UUID ruleId, RuleRequest request) {
        Rule existing = ruleRepository.findById(ruleId)
                .orElseThrow(() -> new RuntimeException("Rule not found: " + ruleId));

        existing.setConditionExpr(request.getConditionExpr());
        existing.setNextStepId(request.getNextStepId());
        existing.setPriority(request.getPriority());

        return ruleRepository.save(existing);
    }
    public void deleteRule(UUID ruleId) {
        ruleRepository.findById(ruleId)
                .orElseThrow(() -> new RuntimeException("step not found"+ruleId));

        ruleRepository.deleteById(ruleId);
    }
}