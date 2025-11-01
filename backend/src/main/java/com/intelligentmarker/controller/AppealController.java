package com.intelligentmarker.controller;

import com.intelligentmarker.model.Appeal;
import com.intelligentmarker.model.Grade;
import com.intelligentmarker.model.User;
import com.intelligentmarker.repository.GradeRepository;
import com.intelligentmarker.repository.UserRepository;
import com.intelligentmarker.service.AppealService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Appeal related APIs
 */
@RestController
@RequestMapping("/api/appeals")
@Slf4j
@RequiredArgsConstructor
public class AppealController {
    
    private final AppealService appealService;
    private final UserRepository userRepository;
    private final GradeRepository gradeRepository;
    
    /**
     * Create an appeal
     */
    @PostMapping
    public ResponseEntity<?> createAppeal(@RequestBody Map<String, Object> request) {
        try {
            Long submissionId = Long.valueOf(request.get("submissionId").toString());
            Long studentId = Long.valueOf(request.get("studentId").toString());
            String reason = request.get("reason").toString();
            
            User student = userRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found"));
            
            Appeal appeal = appealService.createAppeal(submissionId, student, reason);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "appealId", appeal.getId(),
                "message", "Appeal submitted successfully"
            ));
            
        } catch (Exception e) {
            log.error("Appeal creation failed", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }
    
    /**
     * Handle appeal
     */
    @PutMapping("/{appealId}/resolve")
    public ResponseEntity<?> resolveAppeal(
        @PathVariable Long appealId,
        @RequestBody Map<String, Object> request
    ) {
        try {
            Long teacherId = Long.valueOf(request.get("teacherId").toString());
            String statusStr = request.get("status").toString();
            String resolution = request.get("resolution") != null ? request.get("resolution").toString() : "";
            
            Appeal.AppealStatus status = Appeal.AppealStatus.valueOf(statusStr);
            
            User teacher = userRepository.findById(teacherId)
                .orElseThrow(() -> new RuntimeException("Teacher not found"));
            
            BigDecimal newScore = null;
            if (request.containsKey("newScore") && request.get("newScore") != null 
                && !request.get("newScore").toString().isEmpty()) {
                try {
                    newScore = new BigDecimal(request.get("newScore").toString());
                } catch (NumberFormatException e) {
                    log.warn("Invalid newScore format, using null");
                }
            }
            
            Appeal appeal = appealService.resolveAppeal(appealId, teacher, status, resolution, newScore);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Appeal resolved successfully",
                "status", appeal.getStatus()
            ));
            
        } catch (Exception e) {
            log.error("Appeal resolution failed: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }
    
    /**
     * Get pending appeals list (paginated)
     */
    @GetMapping("/pending")
    public ResponseEntity<?> getPendingAppeals(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        try {
            List<Appeal> allAppeals = appealService.getPendingAppeals();
            

            // Manual pagination
            int start = page * size;
            int end = Math.min(start + size, allAppeals.size());
            List<Appeal> pagedAppeals = allAppeals.subList(start, end);
            
            List<Map<String, Object>> result = pagedAppeals.stream()
                .map(a -> {
                    Map<String, Object> map = new java.util.HashMap<>();
                    map.put("id", a.getId());
                    map.put("submissionId", a.getSubmission().getId());
                    map.put("studentName", a.getStudent().getFullName());
                    map.put("reason", a.getReason());
                    map.put("status", a.getStatus());
                    map.put("createdAt", a.getCreatedAt());
                    

                    // Get current score
                    try {
                        Grade grade = gradeRepository.findBySubmission(a.getSubmission())
                            .orElse(null);
                        if (grade != null) {
                            map.put("currentScore", grade.getTeacherScore() != null ? grade.getTeacherScore() : grade.getAiScore());
                            map.put("assignmentTitle", a.getSubmission().getAssignment().getTitle());
                        }
                    } catch (Exception e) {
                        log.warn("Failed to get current score for appeal {}", a.getId());
                    }
                    
                    return map;
                })
                .toList();
            
            // Return paginated data
            Map<String, Object> response = new java.util.HashMap<>();
            response.put("content", result);
            response.put("totalElements", allAppeals.size());
            response.put("totalPages", (int) Math.ceil((double) allAppeals.size() / size));
            response.put("currentPage", page);
            response.put("pageSize", size);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }
    
    /**
     * Get all appeals for a submission
     */
    @GetMapping("/submission/{submissionId}")
    public ResponseEntity<?> getAppealsBySubmission(@PathVariable Long submissionId) {
        try {
            List<Appeal> appeals = appealService.getAppealsBySubmission(submissionId);
            
            List<Map<String, Object>> result = appeals.stream()
                .map(a -> {
                    Map<String, Object> map = new java.util.HashMap<>();
                    map.put("id", a.getId());
                    map.put("submissionId", a.getSubmission().getId());
                    map.put("reason", a.getReason());
                    map.put("status", a.getStatus());
                    map.put("resolution", a.getResolution());
                    map.put("createdAt", a.getCreatedAt());
                    map.put("resolvedAt", a.getResolvedAt());
                    if (a.getResolvedBy() != null) {
                        map.put("resolvedBy", a.getResolvedBy().getFullName());
                    }
                    return map;
                })
                .toList();
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("Failed to get appeals for submission {}", submissionId, e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }
}

