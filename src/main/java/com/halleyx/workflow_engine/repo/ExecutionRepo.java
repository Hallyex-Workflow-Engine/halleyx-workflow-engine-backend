package com.halleyx.workflow_engine.repo;

import com.halleyx.workflow_engine.entity.Enum.ExecutionStatus;
import com.halleyx.workflow_engine.entity.Execution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ExecutionRepo extends JpaRepository<Execution,UUID> {
    List<Execution> findByWorkflowIdOrderByStartedAtDesc(UUID workflowId);
    List<Execution> findByStatus(ExecutionStatus status);

    List<Execution> findAllByOrderByStartedAtDesc();
}
