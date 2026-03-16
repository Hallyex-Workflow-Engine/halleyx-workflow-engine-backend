package com.halleyx.workflow_engine.engine;



import com.halleyx.workflow_engine.entity.Execution;
import com.halleyx.workflow_engine.entity.Rule;
import lombok.extern.slf4j.Slf4j;

import org.mvel2.MVEL;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class RuleEngine {

    /**
     * Main method — called by ExecutionService after each step completes.
     *
     * Goes through rules one by one in priority order.
     * First rule that evaluates to TRUE wins.
     * Returns the nextStepId of the winning rule.
     * Returns null if nextStepId is null (workflow ends).
     */
    public String evaluate(List<Rule> rules,
                           Map<String, Object> inputData,
                           Execution execution) {

        if (rules == null || rules.isEmpty()) {
            throw new IllegalStateException(
                    "No rules found for step. Add at least one DEFAULT rule.");
        }

        // go through each rule in priority order (1, 2, 3 ...)
        for (Rule rule : rules) {

            boolean matched = evaluateCondition(
                    rule.getConditionExpr(), inputData);

            // log every rule check — shows in execution logs
            log.info("Rule [priority={}] condition='{}' result={}",
                    rule.getPriority(), rule.getConditionExpr(), matched);

            if (matched) {
                // this rule matched — return its next step
                log.info("Rule matched! Next step id = {}", rule.getNextStepId());

                // null means workflow should end after this step
                if (rule.getNextStepId() == null) {
                    return null;
                }
                return rule.getNextStepId().toString();
            }
        }

        // no rule matched at all — DEFAULT was missing
        throw new IllegalStateException(
                "No rule matched. Make sure you have a DEFAULT rule as the last rule.");
    }

    /**
     * Evaluates ONE condition string against input data.
     * Uses MVEL library to parse and evaluate the expression.
     *
     * Examples:
     *   "DEFAULT"                          → always true
     *   "amount > 100"                     → true if amount is 250
     *   "amount > 100 && country == 'US'"  → true if both conditions met
     *   "amount <= 100 || priority == 'Low'" → true if either condition met
     *   "contains(department, 'Finance')"  → true if department contains Finance
     */
    private boolean evaluateCondition(String condition,
                                      Map<String, Object> inputData) {
        // handle DEFAULT — always true, no evaluation needed
        if (condition == null || condition.trim().equalsIgnoreCase("DEFAULT")) {
            return true;
        }

        try {
            // MVEL.evalToBoolean() does all the heavy lifting:
            // - parses the condition string
            // - looks up variables from inputData map
            // - evaluates operators: >, <, ==, !=, &&, ||, >=, <=
            // - handles functions: contains(), startsWith(), endsWith()
            // - returns true or false
            Boolean result = MVEL.evalToBoolean(condition, inputData);
            return Boolean.TRUE.equals(result);

        } catch (Exception e) {
            // if condition has a syntax error or unknown variable
            log.error("Rule evaluation failed for condition '{}': {}",
                    condition, e.getMessage());
            return false;
        }
    }

    /**
     * Bonus method — evaluates all rules and returns detailed results.
     * Used for building the evaluated_rules log entry.
     * Shows which rules were checked and what each returned.
     */
    public List<Map<String, Object>> evaluateWithDetails(
            List<Rule> rules,
            Map<String, Object> inputData) {

        List<Map<String, Object>> results = new ArrayList<>();
        boolean winnerFound = false;

        for (Rule rule : rules) {
            Map<String, Object> detail = new LinkedHashMap<>();
            detail.put("priority",  rule.getPriority());
            detail.put("condition", rule.getConditionExpr());

            if (winnerFound) {
                // once a winner is found, remaining rules are skipped
                detail.put("result",  false);
                detail.put("skipped", true);
            } else {
                boolean matched = evaluateCondition(rule.getConditionExpr(), inputData);
                detail.put("result",  matched);
                detail.put("skipped", false);
                if (matched) {
                    detail.put("winner",   true);
                    detail.put("nextStep", rule.getNextStepId());
                    winnerFound = true;
                }
            }

            results.add(detail);
        }

        return results;
    }
}