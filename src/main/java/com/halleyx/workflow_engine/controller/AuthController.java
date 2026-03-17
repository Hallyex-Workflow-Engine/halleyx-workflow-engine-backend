package com.halleyx.workflow_engine.controller;

import com.halleyx.workflow_engine.dto.Request.LoginRequest;
import com.halleyx.workflow_engine.dto.Request.RegisterRequest;
import com.halleyx.workflow_engine.entity.Enum.Role;
import com.halleyx.workflow_engine.entity.User;
import com.halleyx.workflow_engine.repo.UserRepo;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired private AuthenticationManager authenticationManager;
    @Autowired private UserRepo userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private HttpSessionSecurityContextRepository securityContextRepository;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest req) {
        if (userRepository.findByEmail(req.getEmail()).isPresent()) {
            return ResponseEntity.badRequest().body("Email already exists");
        }
        User user = new User();
        user.setName(req.getName());
        user.setEmail(req.getEmail());
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setRole(Role.EMPLOYEE);
        userRepository.save(user);
        return ResponseEntity.ok("Registered successfully");
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req,
                                   HttpServletRequest request,
                                   HttpServletResponse response) {
        try {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(req.getEmail(), req.getPassword())
            );

            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(auth);
            SecurityContextHolder.setContext(context);
            securityContextRepository.saveContext(context, request, response);

            User user = userRepository.findByEmail(req.getEmail()).get();
            return ResponseEntity.ok(Map.of(
                    "id",    user.getId(),
                    "name",  user.getName(),
                    "email", user.getEmail(),
                    "role",  user.getRole()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(401).body("Invalid credentials");
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> me() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()
                || auth.getName().equals("anonymousUser")) {
            return ResponseEntity.status(401).body("Not logged in");
        }
        User user = userRepository.findByEmail(auth.getName()).orElse(null);
        if (user == null) {
            return ResponseEntity.status(404).body("User not found");
        }
        return ResponseEntity.ok(Map.of(
                "id",    user.getId(),
                "name",  user.getName(),
                "email", user.getEmail(),
                "role",  user.getRole()
        ));
    }
    @GetMapping("/api/users")
    public ResponseEntity<?> getAllUsers() {
        List<User> users = userRepository.findAll();
        return ResponseEntity.ok(users.stream().map(u -> Map.of(
                "id",    u.getId(),
                "name",  u.getName(),
                "email", u.getEmail(),
                "role",  u.getRole()
        )).collect(Collectors.toList()));
    }

    @PostMapping("/api/users")
    public ResponseEntity<?> createUser(@RequestBody RegisterRequest req) {
        if (userRepository.findByEmail(req.getEmail()).isPresent()) {
            return ResponseEntity.badRequest().body("Email already exists");
        }
        User user = new User();
        user.setName(req.getName());
        user.setEmail(req.getEmail());
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setRole(req.getRole()); // admin sets the role
        userRepository.save(user);
        return ResponseEntity.ok("User created");
    }

    @DeleteMapping("/api/users/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable String id) {
        userRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
    @GetMapping("/hash")
    public String hash(@RequestParam String password) {
        return passwordEncoder.encode(password);
    }
}