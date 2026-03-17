package com.halleyx.workflow_engine.repo;

import com.halleyx.workflow_engine.entity.Rule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;
@Repository
public interface RuleRepo extends JpaRepository<Rule,UUID> {
    List<Rule> findByStepIdOrderByPriorityAsc(UUID stepId);
    void deleteByStepId(UUID stepId);

    List<Rule> findByStepId(UUID id);
}
