package com.halleyx.workflow_engine.dto.Response;


import com.halleyx.workflow_engine.entity.Enum.ExecutionStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ExecutionResponse {

    private UUID id;
    private UUID workflowId;
    private String workflowName;
    private Integer workflowVersion;
    private ExecutionStatus status;
    private Map<String, Object> inputData;
    private List<Map<String, Object>> logs;
    private UUID currentStepId;
    private String currentStepName;
    private Integer retries;
    private String triggeredBy;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private String currentStepAssigneeEmail; // add this field
}