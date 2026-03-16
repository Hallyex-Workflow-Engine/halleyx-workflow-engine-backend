package com.halleyx.workflow_engine.repo;

import com.halleyx.workflow_engine.entity.Workflow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WorkflowRepo extends JpaRepository<Workflow,UUID> {
    List<Workflow> findByIsActiveTrue();
    List<Workflow> findByNameContainingIgnoreCase(String name);
    Optional<Workflow> findByIdAndVersion(UUID id, Integer version);

}
