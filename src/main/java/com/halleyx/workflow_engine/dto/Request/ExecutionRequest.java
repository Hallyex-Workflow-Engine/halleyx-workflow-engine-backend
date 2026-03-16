package com.halleyx.workflow_engine.dto.Request;



import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

@Data
public class ExecutionRequest {

    @NotNull(message = "Input data is required")
    private Map<String, Object> inputData;

    private String triggeredBy;
}