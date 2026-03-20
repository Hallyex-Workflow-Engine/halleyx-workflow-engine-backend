package com.halleyx.workflow_engine.controller;

import com.halleyx.workflow_engine.dto.Request.ChangePasswordRequest;
import com.halleyx.workflow_engine.dto.Request.RegisterRequest;
import com.halleyx.workflow_engine.dto.Request.UpdateProfileRequest;
import com.halleyx.workflow_engine.dto.Response.UserResponse;
import com.halleyx.workflow_engine.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @PostMapping
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(userService.createUser(request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable String id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMyProfile(Authentication auth) {
        return ResponseEntity.ok(userService.getMyProfile(auth.getName()));
    }

    // Self-update: name, phone, avatarUrl only (validated, no role/isActive)
    @PutMapping("/me")
    public ResponseEntity<UserResponse> updateMyProfile(
            Authentication auth,
            @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(userService.updateMyProfile(auth.getName(), request));
    }

    @PutMapping("/change-password")
    public ResponseEntity<String> changePassword(
            Authentication auth,
            @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(auth.getName(), request);
        return ResponseEntity.ok("Password updated successfully");
    }

    // Admin: partial update any user (role, name, phone, avatarUrl)
    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable String id,
            @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(userService.updateUser(id, request));
    }

    // Admin: toggle active/inactive — no body needed
    @PutMapping("/{id}/toggle-active")
    public ResponseEntity<UserResponse> toggleActive(@PathVariable String id) {
        return ResponseEntity.ok(userService.toggleActive(id));
    }
}