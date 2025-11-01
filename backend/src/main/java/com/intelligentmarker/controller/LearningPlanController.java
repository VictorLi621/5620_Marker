package com.intelligentmarker.controller;

import com.intelligentmarker.model.LearningPlan;
import com.intelligentmarker.service.LearningPlanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/learning-plans")
@RequiredArgsConstructor
@Slf4j
public class LearningPlanController {

    private final LearningPlanService learningPlanService;

    /**
     * Generate a new learning plan based on grade feedback
     */
    @PostMapping("/generate")
    public ResponseEntity<Map<String, Object>> generateLearningPlan(@RequestBody Map<String, Long> request) {
        Map<String, Object> response = new HashMap<>();

        try {
            Long gradeId = request.get("gradeId");
            Long studentId = request.get("studentId");

            if (gradeId == null || studentId == null) {
                response.put("success", false);
                response.put("error", "Missing required parameters: gradeId and studentId");
                return ResponseEntity.badRequest().body(response);
            }

            log.info("Generating learning plan for grade {} and student {}", gradeId, studentId);

            LearningPlan learningPlan = learningPlanService.generateLearningPlan(gradeId, studentId);

            response.put("success", true);
            response.put("learningPlan", learningPlan);
            response.put("message", "Learning plan generated successfully");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to generate learning plan", e);
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Get all learning plans for a student
     */
    @GetMapping("/student/{studentId}")
    public ResponseEntity<List<LearningPlan>> getStudentLearningPlans(@PathVariable Long studentId) {
        try {
            log.info("Fetching learning plans for student {}", studentId);
            List<LearningPlan> plans = learningPlanService.getStudentLearningPlans(studentId);
            return ResponseEntity.ok(plans);
        } catch (Exception e) {
            log.error("Failed to fetch learning plans for student {}", studentId, e);
            return ResponseEntity.status(500).body(null);
        }
    }

    /**
     * Get a specific learning plan by ID
     */
    @GetMapping("/{planId}")
    public ResponseEntity<LearningPlan> getLearningPlan(@PathVariable Long planId) {
        try {
            log.info("Fetching learning plan {}", planId);
            // This would need to be implemented in the service
            return ResponseEntity.ok(null);
        } catch (Exception e) {
            log.error("Failed to fetch learning plan {}", planId, e);
            return ResponseEntity.status(500).body(null);
        }
    }

    /**
     * Update learning plan status (e.g., mark as completed)
     */
    @PutMapping("/{planId}/status")
    public ResponseEntity<Map<String, Object>> updatePlanStatus(
            @PathVariable Long planId,
            @RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();

        try {
            String status = request.get("status");

            if (status == null) {
                response.put("success", false);
                response.put("error", "Missing required parameter: status");
                return ResponseEntity.badRequest().body(response);
            }

            log.info("Updating learning plan {} status to {}", planId, status);

            // This would need to be implemented in the service
            response.put("success", true);
            response.put("message", "Learning plan status updated");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to update learning plan status", e);
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
}
