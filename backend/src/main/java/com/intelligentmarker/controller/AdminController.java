package com.intelligentmarker.controller;

import com.intelligentmarker.model.AuditLog;
import com.intelligentmarker.model.User;
import com.intelligentmarker.repository.AuditLogRepository;
import com.intelligentmarker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Admin-only API endpoints
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminController {
    
    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;
    private final PasswordEncoder passwordEncoder;
    
    /**
     * Get all users list
     */
    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers(
        @RequestParam(required = false) String role
    ) {
        try {
            List<User> users;
            
            if (role != null && !role.isEmpty()) {
                // Filter by role
                User.UserRole userRole = User.UserRole.valueOf(role);
                users = userRepository.findByRole(userRole);
            } else {
                // Get all users
                users = userRepository.findAll();
            }
            
            List<Map<String, Object>> result = users.stream()
                .map(u -> {
                    Map<String, Object> map = new java.util.HashMap<>();
                    map.put("id", u.getId());
                    map.put("username", u.getUsername());
                    map.put("email", u.getEmail());
                    map.put("fullName", u.getFullName());
                    map.put("role", u.getRole());
                    map.put("createdAt", u.getCreatedAt());
                    return map;
                })
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("Failed to fetch users", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }
    
    /**
     * Get audit logs
     */
    @GetMapping("/audit-logs")
    public ResponseEntity<?> getAuditLogs(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "50") int size,
        @RequestParam(required = false) String action
    ) {
        try {
            List<AuditLog> allLogs;
            
            if (action != null && !action.isEmpty()) {
                allLogs = auditLogRepository.findByAction(action);
            } else {
                allLogs = auditLogRepository.findAll();
            }
            
            // Sort by timestamp in descending order
            allLogs.sort((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()));
            

            // Manual pagination
            int start = page * size;
            int end = Math.min(start + size, allLogs.size());
            List<AuditLog> pagedLogs = start < allLogs.size()
                ? allLogs.subList(start, end)
                : List.of();
            
            List<Map<String, Object>> result = pagedLogs.stream()
                .map(log -> {
                    Map<String, Object> map = new java.util.HashMap<>();
                    map.put("id", log.getId());
                    map.put("action", log.getAction());
                    map.put("entityType", log.getEntityType());
                    map.put("entityId", log.getEntityId());
                    map.put("userId", log.getActor() != null ? log.getActor().getId() : null);
                    map.put("username", log.getActor() != null ? log.getActor().getUsername() : "System");
                    map.put("userRole", log.getActor() != null ? log.getActor().getRole().toString() : "SYSTEM");
                    map.put("details", log.getDetails());
                    map.put("createdAt", log.getTimestamp());
                    return map;
                })
                .collect(Collectors.toList());
            
            Map<String, Object> response = new java.util.HashMap<>();
            response.put("content", result);
            response.put("totalElements", allLogs.size());
            response.put("totalPages", (int) Math.ceil((double) allLogs.size() / size));
            response.put("currentPage", page);
            response.put("pageSize", size);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Failed to fetch audit logs", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }
    
    /**
     * Delete user（soft delete or hard delete）
     */
    @DeleteMapping("/users/{userId}")
    public ResponseEntity<?> deleteUser(@PathVariable Long userId) {
        try {
            User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            // Hard delete (soft delete might be needed in real applications)
            userRepository.delete(user);
            
            log.info("User deleted: {}", userId);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "User deleted successfully"
            ));
            
        } catch (Exception e) {
            log.error("Failed to delete user", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }
    
    /**
     * Get user info
     */
    @GetMapping("/stats")
    public ResponseEntity<?> getSystemStats() {
        try {
            long totalUsers = userRepository.count();
            long studentCount = userRepository.findByRole(User.UserRole.STUDENT).size();
            long teacherCount = userRepository.findByRole(User.UserRole.TEACHER).size();
            
            // Search for TECHNICAL_TEAM role or ADMIN role
            long techCount = 0;
            try {
                techCount = userRepository.findByRole(User.UserRole.TECHNICAL_TEAM).size();
            } catch (IllegalArgumentException e) {
                // If TECHNICAL_TEAM role does not exist, search for ADMIN role
                try {
                    techCount = userRepository.findByRole(User.UserRole.ADMIN).size();
                } catch (IllegalArgumentException ex) {
                    log.warn("Neither TECHNICAL_TEAM nor ADMIN role exists");
                }
            }
            long adminCount = techCount;
            
            Map<String, Object> stats = new java.util.HashMap<>();
            stats.put("totalUsers", totalUsers);
            stats.put("students", studentCount);
            stats.put("teachers", teacherCount);
            stats.put("admins", adminCount);
            
            return ResponseEntity.ok(stats);
            
        } catch (Exception e) {
            log.error("Failed to fetch stats", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }
    
    /**
     * Reset password(Admin role)
     */
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> request) {
        try {
            Long userId = Long.valueOf(request.get("userId"));
            String newPassword = request.get("newPassword");
            
            User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            // Encrypt password
            user.setPassword(passwordEncoder.encode(newPassword));
            userRepository.save(user);
            
            log.info("✅ Password reset for user: {} ({})", user.getUsername(), user.getRole());
            
            return ResponseEntity.ok(Map.of(
                "success", true,

                "message", "Password reset successful"
            ));
            
        } catch (Exception e) {
            log.error("Failed to reset password", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }
    
    /**
     * Batch reset demo user passwords to "password"
     */
    @PostMapping("/reset-demo-passwords")
    public ResponseEntity<?> resetDemoPasswords() {
        try {
            // Demo user list
            String[] demoUsers = {
                "teacher@uni.edu",
                "student@uni.edu",
                "student2@uni.edu",
                "admin@uni.edu"
            };
            
            String defaultPassword = "password";
            String encodedPassword = passwordEncoder.encode(defaultPassword);
            int count = 0;
            
            for (String username : demoUsers) {
                userRepository.findByUsername(username).ifPresent(user -> {
                    user.setPassword(encodedPassword);
                    userRepository.save(user);
                    log.info("✅ Reset password for demo user: {}", username);
                });
                count++;
            }
            
            log.info("✅ Reset {} demo user passwords", count);
            
            return ResponseEntity.ok(Map.of(
                "success", true,

                "message", "Demo user passwords have been reset to: password",
                "count", count
            ));
            
        } catch (Exception e) {
            log.error("Failed to reset demo passwords", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }
}

