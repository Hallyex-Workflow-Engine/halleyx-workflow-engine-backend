package com.halleyx.workflow_engine.service;

import com.halleyx.workflow_engine.dto.Request.ChangePasswordRequest;
import com.halleyx.workflow_engine.dto.Request.RegisterRequest;
import com.halleyx.workflow_engine.dto.Request.UpdateProfileRequest;
import com.halleyx.workflow_engine.dto.Response.UserResponse;
import com.halleyx.workflow_engine.entity.Enum.Role;
import com.halleyx.workflow_engine.entity.User;
import com.halleyx.workflow_engine.repo.UserRepo;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class UserService {

    private final UserRepo userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepo userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository  = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // ── Read ──────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public UserResponse getMyProfile(String email) {
        return toResponse(findByEmailOrThrow(email));
    }

    // ── Write ─────────────────────────────────────────────────────────────────

    public UserResponse createUser(RegisterRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Email already exists: " + request.getEmail());
        }
        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole() != null ? request.getRole() : Role.EMPLOYEE);
        user.setIsActive(true);
        return toResponse(userRepository.save(user));
    }

    public void deleteUser(String id) {
        if (!userRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "User not found with id: " + id);
        }
        userRepository.deleteById(id);
    }

    // Self-update: name, phone, avatarUrl (no role or isActive)
    public UserResponse updateMyProfile(String email, UpdateProfileRequest request) {
        User user = findByEmailOrThrow(email);
        user.setName(request.getName());
        if (request.getPhone()     != null) user.setPhone(request.getPhone());
        if (request.getAvatarUrl() != null) user.setAvatarUrl(request.getAvatarUrl());
        return toResponse(userRepository.save(user));
    }

    public void changePassword(String email, ChangePasswordRequest request) {
        User user = findByEmailOrThrow(email);
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Old password is incorrect");
        }
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    // Admin partial update — only non-null fields are applied
    public UserResponse updateUser(String id, UpdateProfileRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User not found: " + id));

        if (request.getName()      != null) user.setName(request.getName());
        if (request.getPhone()     != null) user.setPhone(request.getPhone());
        if (request.getAvatarUrl() != null) user.setAvatarUrl(request.getAvatarUrl());
        if (request.getRole()      != null) user.setRole(request.getRole());
        if (request.getIsActive()  != null) user.setIsActive(request.getIsActive());

        return toResponse(userRepository.save(user));
    }

    // Toggle isActive — no request body, just flips the flag
    public UserResponse toggleActive(String id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User not found: " + id));
        user.setIsActive(!Boolean.TRUE.equals(user.getIsActive()));
        return toResponse(userRepository.save(user));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private User findByEmailOrThrow(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User not found: " + email));
    }

    private UserResponse toResponse(User u) {
        return new UserResponse(
                u.getId(),
                u.getName(),
                u.getEmail(),
                u.getRole(),
                u.getPhone(),
                u.getAvatarUrl(),
                u.getIsActive(),
                u.getCreatedAt(),
                u.getUpdatedAt()
        );
    }
}