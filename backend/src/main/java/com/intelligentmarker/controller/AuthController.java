package com.intelligentmarker.controller;

import com.intelligentmarker.model.User;
import com.intelligentmarker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Authentication related APIs (simplified version)
 */
@RestController
@RequestMapping("/api/auth")
@Slf4j
@RequiredArgsConstructor
public class AuthController {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    
    /**
     * Login
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> request) {
        try {
            String username = request.get("username");
            String password = request.get("password");
            
            log.info("Login attempt for user: {}", username);
            
            User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Invalid username or password"));
            

            // Verify password
            if (!passwordEncoder.matches(password, user.getPassword())) {
                log.warn("Failed login attempt for user: {} - incorrect password", username);
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "Invalid username or password"
                ));
            }
            
            log.info("User logged in successfully: {} ({})", username, user.getRole());
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "user", Map.of(
                    "id", user.getId(),
                    "username", user.getUsername(),
                    "email", user.getEmail(),
                    "fullName", user.getFullName(),
                    "role", user.getRole(),
                    "studentId", user.getStudentId() != null ? user.getStudentId() : ""
                )
            ));
            
        } catch (Exception e) {
            log.error("Login failed for user: {}", request.get("username"), e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }
    
    /**
     * Register
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> request) {
        try {
            String username = request.get("username");
            String email = request.get("email");
            String password = request.get("password");
            String fullName = request.get("fullName");
            String role = request.get("role"); // STUDENT or TEACHER
            String studentId = request.get("studentId"); // For students
            
            // Check if username already exists
            if (userRepository.findByUsername(username).isPresent()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "Username already exists"
                ));
            }
            

            // Validate if email already exists
            if (userRepository.findByEmail(email).isPresent()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "Email already exists"
                ));
            }
            
            // Create new user
            User user = new User();
            user.setUsername(username);
            user.setEmail(email);
            user.setPassword(passwordEncoder.encode(password));
            user.setFullName(fullName);
            user.setRole(User.UserRole.valueOf(role));
            
            if ("STUDENT".equals(role) && studentId != null) {
                user.setStudentId(studentId);
            }
            
            user = userRepository.save(user);
            
            log.info("New user registered: {} ({})", username, role);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Registration successful",
                "user", Map.of(
                    "id", user.getId(),
                    "username", user.getUsername(),
                    "email", user.getEmail(),
                    "fullName", user.getFullName(),
                    "role", user.getRole()
                )
            ));
            
        } catch (Exception e) {
            log.error("Registration failed", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }
    
    /**
     * Health check
     */
    @GetMapping("/health")
    public ResponseEntity<?> health() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "timestamp", System.currentTimeMillis()
        ));
    }
}

