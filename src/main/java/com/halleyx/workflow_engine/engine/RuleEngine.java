package com.halleyx.workflow_engine.engine;

import com.halleyx.workflow_engine.entity.Enum.Role;
import com.halleyx.workflow_engine.entity.Execution;
import com.halleyx.workflow_engine.entity.Rule;
import com.halleyx.workflow_engine.entity.User;
import com.halleyx.workflow_engine.repo.UserRepo;
import com.halleyx.workflow_engine.service.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.mvel2.MVEL;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Component
@Slf4j
public class RuleEngine {

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");

    private final EmailService emailService;
    private final UserRepo     userRepo;

    public RuleEngine(EmailService emailService, UserRepo userRepo) {
        this.emailService = emailService;
        this.userRepo     = userRepo;
    }


    public String evaluate(List<Rule> rules,
                           Map<String, Object> inputData,
                           Execution execution) {

        if (rules == null || rules.isEmpty()) {
            throw new IllegalStateException(
                    "No rules found for step. Add at least one DEFAULT rule.");
        }

        for (Rule rule : rules) {

            boolean matched = evaluateCondition(rule.getConditionExpr(), inputData);

            log.info("Rule [priority={}] condition='{}' result={}",
                    rule.getPriority(), rule.getConditionExpr(), matched);

            if (matched) {
                log.info("Rule matched! Next step id = {}", rule.getNextStepId());

                // ── Notify FINANCE_HEAD users by email ────────────────────
                notifyFinanceHeads(rule, execution);
                // ─────────────────────────────────────────────────────────

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
                    "Invalid rule condition: '" + condition + "' → " + e.getMessage());
        }
    }


    private void notifyFinanceHeads(Rule rule, Execution execution) {
        try {
            List<User> heads = userRepo.findByRoleAndIsActiveTrue((Role.FINANCE_HEAD));
            if (heads.isEmpty()) {
                log.debug("[RuleEngine] No active FINANCE_HEAD users — skipping email.");
                return;
            }

            String workflowName = execution != null && execution.getWorkflowId() != null
                    ? "Workflow #" + execution.getWorkflowId()
                    : "Unknown Workflow";

            String ruleName  = rule.getConditionExpr() != null ? rule.getConditionExpr() : "Unnamed Rule";
            String condition = rule.getConditionExpr() != null ? rule.getConditionExpr() : "DEFAULT";
            String time      = LocalDateTime.now().format(FMT);

            for (User user : heads) {
                String subject = "[Workflow Engine] Rule Triggered: " + ruleName;
                String body    = buildEmailBody(
                        user.getName(), workflowName, ruleName, condition, time);

                emailService.sendPlainText(user.getEmail(), user.getName(), subject, body);
                log.info("[RuleEngine] Email queued for FINANCE_HEAD {} <{}>",
                        user.getName(), user.getEmail());
            }
        } catch (Exception e) {
            log.error("[RuleEngine] Failed to send finance notification email: {}", e.getMessage());
        }
    }

    private String buildEmailBody(String recipientName, String workflowName,
                                  String ruleName, String condition, String time) {
        return String.join("\n",
                "Hi " + recipientName + ",",
                "",
                "A workflow rule has been triggered that requires your attention.",
                "",
                "─────────────────────────────",
                "  Workflow  : " + workflowName,
                "  Rule      : " + ruleName,
                "  Condition : " + condition,
                "  Time      : " + time,
                "─────────────────────────────",
                "",
                "Please log in to the Workflow Engine to review.",
                "",
                "This is an automated alert. Do not reply.",
                "Workflow Engine"
        );
    }
}