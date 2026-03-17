package com.halleyx.workflow_engine.repo;

import com.halleyx.workflow_engine.entity.Step;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface StepRepo extends JpaRepository<Step,UUID> {

    List<Step> findByWorkflowIdOrderByStepOrderAsc(UUID workflowId);
    void deleteByWorkflowId(UUID workflowId);
    int countByWorkflowId(UUID workflowId);

    List<Step> findByWorkflowId(UUID id);
}
