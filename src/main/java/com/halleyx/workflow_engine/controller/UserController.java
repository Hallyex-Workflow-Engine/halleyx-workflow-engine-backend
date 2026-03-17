package com.halleyx.workflow_engine.controller;


import com.halleyx.workflow_engine.dto.Request.RegisterRequest;
import com.halleyx.workflow_engine.dto.Response.UserResponse;
import com.halleyx.workflow_engine.entity.Enum.Role;
import com.halleyx.workflow_engine.entity.User;
import com.halleyx.workflow_engine.repo.UserRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired private UserRepo userRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        return ResponseEntity.ok(
                userRepository.findAll().stream()
                        .map(u -> new UserResponse(u.getId(), u.getName(),
                                u.getEmail(), u.getRole()))
                        .collect(Collectors.toList())
        );
    }

    @PostMapping
    public ResponseEntity<?> createUser(@RequestBody RegisterRequest req) {
        if (userRepository.findByEmail(req.getEmail()).isPresent()) {
            return ResponseEntity.badRequest().body("Email already exists");
        }
        User user = new User();
        user.setName(req.getName());
        user.setEmail(req.getEmail());
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setRole(req.getRole() != null ? req.getRole() : Role.EMPLOYEE);
        userRepository.save(user);
        return ResponseEntity.ok(
                new UserResponse(user.getId(), user.getName(),
                        user.getEmail(), user.getRole())
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable String id) {
        userRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/role")
    public ResponseEntity<?> updateRole(@PathVariable String id,
                                        @RequestBody Map<String, String> body) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setRole(Role.valueOf(body.get("role")));
        userRepository.save(user);
        return ResponseEntity.ok(
                new UserResponse(user.getId(), user.getName(),
                        user.getEmail(), user.getRole())
        );
    }
}