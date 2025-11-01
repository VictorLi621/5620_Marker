package com.intelligentmarker.controller;

import com.intelligentmarker.model.Assignment;
import com.intelligentmarker.model.Grade;
import com.intelligentmarker.model.Submission;
import com.intelligentmarker.model.User;
import com.intelligentmarker.repository.AssignmentRepository;
import com.intelligentmarker.repository.GradeRepository;
import com.intelligentmarker.repository.SubmissionRepository;
import com.intelligentmarker.repository.UserRepository;
import com.intelligentmarker.service.AuditLogService;
import com.intelligentmarker.service.PublishService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Grade-related APIs
 */
@RestController
@RequestMapping("/api/grades")
@Slf4j
@RequiredArgsConstructor
public class GradeController {

    private final GradeRepository gradeRepository;
    private final SubmissionRepository submissionRepository;
    private final UserRepository userRepository;
    private final AssignmentRepository assignmentRepository;
    private final PublishService publishService;
    private final AuditLogService auditLogService;

    /**
     * Get pending submissions for teacher review (only returns assignments created by this teacher)
     */
    @GetMapping("/teacher/{teacherId}/pending")
    public ResponseEntity<?> getTeacherPendingGrades(
        @PathVariable Long teacherId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        try {
            User teacher = userRepository.findById(teacherId)
                .orElseThrow(() -> new RuntimeException("Teacher not found"));

            // Get all assignments created by the teacher
            List<Assignment> teacherAssignments = assignmentRepository.findByTeacher(teacher);

            // Get all submissions for these assignments
            List<Map<String, Object>> allPendingGrades = new ArrayList<>();
            for (Assignment assignment : teacherAssignments) {
                List<Submission> submissions = submissionRepository.findByAssignment(assignment);

                for (Submission submission : submissions) {
                    gradeRepository.findBySubmission(submission).ifPresent(grade -> {
                        // Only return AI-graded but unpublished grades
                        if (grade.getStatus() == Grade.GradeStatus.NEEDS_REVIEW ||
                            grade.getStatus() == Grade.GradeStatus.HIGH_CONFIDENCE ||
                            grade.getStatus() == Grade.GradeStatus.APPROVED) {

                            Map<String, Object> gradeMap = new java.util.HashMap<>();
                            gradeMap.put("gradeId", grade.getId());
                            gradeMap.put("submissionId", submission.getId());
                            gradeMap.put("studentId", submission.getStudent().getId());
                            gradeMap.put("studentName", submission.getStudent().getFullName());
                            gradeMap.put("assignmentId", assignment.getId());
                            gradeMap.put("assignmentTitle", assignment.getTitle());
                            gradeMap.put("courseCode", assignment.getCourseCode());
                            gradeMap.put("aiScore", grade.getAiScore());
                            gradeMap.put("aiConfidence", grade.getAiConfidence());
                            gradeMap.put("teacherScore", grade.getTeacherScore());
                            gradeMap.put("status", grade.getStatus());
                            gradeMap.put("published", grade.getPublishedAt() != null);
                            gradeMap.put("submittedAt", submission.getCreatedAt());
                            allPendingGrades.add(gradeMap);
                        }
                    });
                }
            }

            // Manual pagination
            int start = page * size;
            int end = Math.min(start + size, allPendingGrades.size());
            List<Map<String, Object>> pagedGrades = start < allPendingGrades.size()
                ? allPendingGrades.subList(start, end)
                : new ArrayList<>();

            Map<String, Object> response = new java.util.HashMap<>();
            response.put("content", pagedGrades);
            response.put("totalElements", allPendingGrades.size());
            response.put("totalPages", (int) Math.ceil((double) allPendingGrades.size() / size));
            response.put("currentPage", page);
            response.put("pageSize", size);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to fetch teacher pending grades", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }

    /**
     * Student acknowledges receipt of grade
     */
    @PostMapping("/{gradeId}/acknowledge")
    public ResponseEntity<?> acknowledgeGrade(@PathVariable Long gradeId, @RequestBody Map<String, Object> request) {
        try {
            Long studentId = Long.valueOf(request.get("studentId").toString());

            User student = userRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found"));

            Grade grade = gradeRepository.findById(gradeId)
                .orElseThrow(() -> new RuntimeException("Grade not found"));

            // Update grade status to acknowledged (can add an acknowledgedAt field)
            // Here we simply record an audit log
            auditLogService.log(
                student,
                "ACKNOWLEDGE_GRADE",
                "GRADE",
                gradeId,
                Map.of("submissionId", grade.getSubmission().getId())
            );

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Grade acknowledged"
            ));

        } catch (Exception e) {
            log.error("Failed to acknowledge grade", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }

    /**
     * Get all grades for a student
     */
    @GetMapping("/student/{studentId}")
    public ResponseEntity<?> getStudentGrades(@PathVariable Long studentId) {
        try {
            User student = userRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found"));

            List<Submission> submissions = submissionRepository.findByStudent(student);

            List<Map<String, Object>> grades = submissions.stream()
                .map(submission -> {
                    return gradeRepository.findBySubmission(submission)
                        .map(grade -> {
                            java.util.Map<String, Object> gradeMap = new java.util.HashMap<>();
                            gradeMap.put("id", grade.getId());
                            gradeMap.put("submissionId", submission.getId());
                            gradeMap.put("assignmentTitle", submission.getAssignment().getTitle());
                            gradeMap.put("aiScore", grade.getAiScore());
                            gradeMap.put("aiConfidence", grade.getAiConfidence());
                            gradeMap.put("teacherScore", grade.getTeacherScore());
                            gradeMap.put("teacherComments", grade.getTeacherComments());
                            gradeMap.put("aiFeedback", grade.getAiFeedback());
                            gradeMap.put("status", grade.getStatus());
                            gradeMap.put("publishedAt", grade.getPublishedAt());
                            return gradeMap;
                        })
                        .orElse(null);
                })
                .filter(g -> g != null && g.get("publishedAt") != null) // Only return published grades
                .collect(Collectors.toList());

            return ResponseEntity.ok(grades);

        } catch (Exception e) {
            log.error("Failed to fetch student grades", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }

    /**
     * Get grade details
     */
    @GetMapping("/submission/{submissionId}")
    public ResponseEntity<?> getGradeBySubmission(@PathVariable Long submissionId) {
        try {
            Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new RuntimeException("Submission not found"));

            Grade grade = gradeRepository.findBySubmission(submission)
                .orElseThrow(() -> new RuntimeException("Grade not found"));

            // Build response with proper null handling
            Map<String, Object> response = new java.util.HashMap<>();
            response.put("id", grade.getId());
            response.put("submissionId", submissionId);
            response.put("aiScore", grade.getAiScore());
            response.put("aiConfidence", grade.getAiConfidence());
            response.put("teacherScore", grade.getTeacherScore());  // Allow null
            response.put("teacherComments", grade.getTeacherComments() != null ? grade.getTeacherComments() : "");
            response.put("aiFeedback", grade.getAiFeedback() != null ? grade.getAiFeedback() : "{}");
            response.put("status", grade.getStatus());
            response.put("publishedAt", grade.getPublishedAt());  // Allow null

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }

    /**
     * Teacher reviews and adjusts score
     */
    @PutMapping("/{gradeId}/review")
    public ResponseEntity<?> reviewGrade(
        @PathVariable Long gradeId,
        @RequestBody Map<String, Object> request
    ) {
        try {
            Long teacherId = Long.valueOf(request.get("teacherId").toString());
            BigDecimal teacherScore = new BigDecimal(request.get("teacherScore").toString());
            String comments = request.get("comments").toString();

            User teacher = userRepository.findById(teacherId)
                .orElseThrow(() -> new RuntimeException("Teacher not found"));

            Grade grade = gradeRepository.findById(gradeId)
                .orElseThrow(() -> new RuntimeException("Grade not found"));

            // Update teacher score and comments
            grade.setTeacherScore(teacherScore);
            grade.setTeacherComments(comments);
            grade.setReviewedBy(teacher);
            grade.setStatus(Grade.GradeStatus.APPROVED);

            grade = gradeRepository.save(grade);

            // Record audit log
            auditLogService.log(
                teacher,
                "REVIEW_GRADE",
                "GRADE",
                gradeId,
                Map.of(
                    "teacherScore", teacherScore,
                    "comments", comments
                )
            );

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Grade reviewed successfully",
                "gradeId", gradeId
            ));

        } catch (Exception e) {
            log.error("Review failed", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }

    /**
     * Publish grade
     */
    @PostMapping("/publish/{submissionId}")
    public ResponseEntity<?> publishGrade(
        @PathVariable Long submissionId,
        @RequestBody Map<String, Object> request
    ) {
        try {
            Long teacherId = Long.valueOf(request.get("teacherId").toString());
            String notes = request.getOrDefault("notes", "").toString();

            User teacher = userRepository.findById(teacherId)
                .orElseThrow(() -> new RuntimeException("Teacher not found"));

            var snapshot = publishService.publishGrade(submissionId, teacher, notes);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Grade published successfully",
                "snapshotId", snapshot.getId(),
                "versionNumber", snapshot.getVersionNumber()
            ));

        } catch (Exception e) {
            log.error("Publish failed", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }
}
