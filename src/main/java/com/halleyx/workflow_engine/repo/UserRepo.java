package com.halleyx.workflow_engine.repo;

import com.halleyx.workflow_engine.entity.Enum.Role;
import com.halleyx.workflow_engine.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepo extends JpaRepository<User, String> {
    Optional<User> findByEmail(String email);


    List<User> findByRoleAndIsActiveTrue(Role role);
}