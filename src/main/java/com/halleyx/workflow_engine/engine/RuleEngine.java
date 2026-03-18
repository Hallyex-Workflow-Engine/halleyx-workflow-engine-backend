package com.halleyx.workflow_engine.engine;

import com.halleyx.workflow_engine.entity.Execution;
import com.halleyx.workflow_engine.entity.Rule;
import lombok.extern.slf4j.Slf4j;
import org.mvel2.MVEL;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@Slf4j
// important mvel evaluates  rule conditions ( amount > 100) from inputData to decide next workflow step
public class RuleEngine {

    public String evaluate(List<Rule> rules,
                           Map<String, Object> inputData,
                           Execution execution) {

        if (rules == null || rules.isEmpty()) {
            throw new IllegalStateException(
                    "No rules found for step. Add at least one DEFAULT rule.");
        }

        for (Rule rule : rules) {

            boolean matched = evaluateCondition(
                    rule.getConditionExpr(), inputData);

            log.info("Rule [priority={}] condition='{}' result={}",
                    rule.getPriority(), rule.getConditionExpr(), matched);

            if (matched) {
                log.info("Rule matched! Next step id = {}", rule.getNextStepId());
                if (rule.getNextStepId() == null) {
                    return null;
                }
                try {
                    return rule.getNextStepId().toString();
                } catch (Exception e) {
                    throw new IllegalStateException(
                            "Invalid nextStepId in rule: " + rule.getNextStepId());
                }
            }
        }
        throw new IllegalStateException(
                "No rule matched. Ensure a DEFAULT rule exists.");
    }
    private boolean evaluateCondition(String condition,
                                      Map<String, Object> inputData) {

        if (condition == null || condition.trim().equalsIgnoreCase("DEFAULT")) {
            return true;
        }

        try {
            Boolean result = MVEL.evalToBoolean(condition, inputData);
            return Boolean.TRUE.equals(result);

        } catch (Exception e) {
            throw new RuntimeException(
                    "Invalid rule condition: '" + condition + "' → " + e.getMessage()
            );
        }
    }
    public List<Map<String, Object>> evaluateWithDetails(
            List<Rule> rules,
            Map<String, Object> inputData) {

        List<Map<String, Object>> results = new ArrayList<>();
        boolean winnerFound = false;

        for (Rule rule : rules) {

            Map<String, Object> detail = new LinkedHashMap<>();
            detail.put("priority", rule.getPriority());
            detail.put("condition", rule.getConditionExpr());

            if (winnerFound) {
                detail.put("result", false);
                detail.put("skipped", true);
            } else {
                boolean matched = evaluateCondition(
                        rule.getConditionExpr(), inputData);

                detail.put("result", matched);
                detail.put("skipped", false);

                if (matched) {
                    detail.put("winner", true);
                    detail.put("nextStep", rule.getNextStepId());
                    winnerFound = true;
                }
            }

            results.add(detail);
        }

        return results;
    }
}