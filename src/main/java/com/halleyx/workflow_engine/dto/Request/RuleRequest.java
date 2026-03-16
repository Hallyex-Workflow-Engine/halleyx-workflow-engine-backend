package com.halleyx.workflow_engine.dto.Request;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class RuleRequest {

    @NotBlank(message = "Condition is required")
    private String conditionExpr;

    private UUID nextStepId;

    @NotNull(message = "Priority is required")
    private Integer priority;
}