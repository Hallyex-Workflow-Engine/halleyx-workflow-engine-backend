package com.halleyx.workflow_engine.dto.Request;


import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class WorkflowRequest {
    @NotBlank(message = "Workflow name is required")
    private String name;

    private String description;

    private String inputSchema;

    private String startStepId;
}
