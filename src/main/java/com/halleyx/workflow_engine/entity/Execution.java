package com.halleyx.workflow_engine.entity;

import com.halleyx.workflow_engine.entity.Enum.ExecutionStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "executions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Execution {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false,
            columnDefinition = "VARCHAR(36)")
    private UUID id;

    @Column(name = "workflow_id", nullable = false,
            columnDefinition = "VARCHAR(36)")
    private UUID workflowId;

    @Column(name = "workflow_version", nullable = false)
    private Integer workflowVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ExecutionStatus status = ExecutionStatus.PENDING;

    @Column(name = "input_data", columnDefinition = "TEXT")
    private String inputData;

    @Column(name = "logs", columnDefinition = "TEXT")
    private String logs = "[]";

    @Column(name = "current_step_id",
            columnDefinition = "VARCHAR(36)")
    private UUID currentStepId;

    @Column(name = "retries", nullable = false)
    private Integer retries = 0;

    @Column(name = "triggered_by")
    private String triggeredBy;

    @CreationTimestamp
    @Column(name = "started_at", updatable = false)
    private LocalDateTime startedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;
}