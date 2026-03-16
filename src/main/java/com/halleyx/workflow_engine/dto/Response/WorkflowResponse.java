package com.halleyx.workflow_engine.dto.Response;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class WorkflowResponse {

    private UUID id;
    private String name;
    private Integer version;
    private Boolean isActive;
    private String inputSchema;
    private UUID startStepId;
    private int stepCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}