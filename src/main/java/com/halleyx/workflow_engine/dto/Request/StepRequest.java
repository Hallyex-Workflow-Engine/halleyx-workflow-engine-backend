package com.halleyx.workflow_engine.dto.Request;


import com.halleyx.workflow_engine.entity.Enum.StepType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class StepRequest {

    @NotBlank(message = "Step name is required")
    private String name;

    @NotNull(message = "Step type is required")
    private StepType stepType;

    @NotNull(message = "Step order is required")
    private Integer stepOrder;

    private String metadata;
}