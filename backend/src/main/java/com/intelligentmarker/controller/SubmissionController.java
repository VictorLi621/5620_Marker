package com.intelligentmarker.controller;

import com.intelligentmarker.model.Assignment;
import com.intelligentmarker.model.ClassEntity;
import com.intelligentmarker.model.Course;
import com.intelligentmarker.model.Submission;
import com.intelligentmarker.model.User;
import com.intelligentmarker.repository.AssignmentRepository;
import com.intelligentmarker.repository.ClassRepository;
import com.intelligentmarker.repository.CourseEnrollmentRepository;
import com.intelligentmarker.repository.CourseRepository;
import com.intelligentmarker.repository.UserRepository;
import com.intelligentmarker.service.SubmissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * Submission related APIs
 */
@RestController
@RequestMapping("/api/submissions")
@Slf4j
@RequiredArgsConstructor
public class SubmissionController {
    
    private final SubmissionService submissionService;
    private final UserRepository userRepository;
    private final AssignmentRepository assignmentRepository;
    private final CourseEnrollmentRepository courseEnrollmentRepository;
    private final CourseRepository courseRepository;
    private final ClassRepository classRepository;
    
    /**
     * Upload assignment
     */
    @PostMapping
    public ResponseEntity<?> uploadSubmission(
        @RequestParam("studentId") Long studentId,
        @RequestParam("assignmentId") Long assignmentId,
        @RequestParam("file") MultipartFile file
    ) {
        try {
            User student = userRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found"));
            
            Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new RuntimeException("Assignment not found"));
            

            // ✅ Verify course exists (prevent submission after course deletion)
            String courseCode = assignment.getCourseCode();
            Course course = courseRepository.findByCourseCode(courseCode).orElse(null);
            if (course == null) {
                log.warn("Course {} not found for assignment {}", courseCode, assignmentId);
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "The course has been deleted or does not exist, unable to submit assignment. Course code: " + courseCode
                ));
            }
            log.info("✅ Course {} exists, proceeding with submission", courseCode);
            
            // ✅ Check if student is in class (new logic)
            ClassEntity assignmentClass = assignment.getClassEntity();
            
            if (assignmentClass == null) {
                // Compatibility with old data: if assignment has no associated class, use legacy courseCode validation
                log.warn("Assignment {} has no class, using legacy courseCode validation", assignmentId);
                // courseCode is already defined above, use directly
                boolean isEnrolled = courseEnrollmentRepository
                    .findByStudentAndCourseCodeAndActiveTrue(student, courseCode)
                    .isPresent();
                

                if (!isEnrolled) {
                    log.warn("Student {} not enrolled in course {}", studentId, courseCode);
                    return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "error", "You are not enrolled in this course and cannot submit the assignment. Please contact the technical team to add the course."
                    ));
                }
            } else {
                // New logic: Check if student is in the class
                boolean isInClass = assignmentClass.getStudents().contains(student);
                
                if (!isInClass) {
                    log.warn("Student {} not in class {} ({})", 
                        studentId, assignmentClass.getClassId(), assignmentClass.getName());
                    return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "error", String.format("You are not in class %s and cannot submit this assignment. Please contact the technical team or instructor to be added to the class.", 
                            assignmentClass.getName())
                    ));
                }
                
                log.info("Student {} verified in class {}", studentId, assignmentClass.getClassId());
            }
            
            // Create submission record (within transaction)
            Submission submission = submissionService.createSubmission(student, assignment, file);
            
            // Trigger asynchronous processing (outside transaction to avoid rollback conflicts)
            submissionService.triggerAsyncProcessing(submission.getId());
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "submissionId", submission.getId(),
                "status", submission.getStatus(),
                "message", "Submission created. Processing in background..."
            ));
            
        } catch (Exception e) {
            log.error("Upload failed", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }
    
    /**
     * Get all submissions for an assignment
     */
    @GetMapping("/assignment/{assignmentId}")
    public ResponseEntity<?> getSubmissionsByAssignment(@PathVariable Long assignmentId) {
        try {
            Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new RuntimeException("Assignment not found"));
            
            var submissions = submissionService.getSubmissionsByAssignment(assignment);
            
            return ResponseEntity.ok(submissions.stream()
                .map(submission -> {
                    java.util.Map<String, Object> subMap = new java.util.HashMap<>();
                    subMap.put("id", submission.getId());
                    subMap.put("studentId", submission.getStudent().getId());
                    subMap.put("studentName", submission.getStudent().getFullName());
                    subMap.put("originalFileName", submission.getOriginalFileName());
                    subMap.put("status", submission.getStatus());
                    subMap.put("createdAt", submission.getCreatedAt());
                    return subMap;
                })
                .collect(java.util.stream.Collectors.toList()));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }
    
    /**
     * Get submission details
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getSubmission(@PathVariable Long id) {
        try {
            Submission submission = submissionService.getSubmission(id);
            
            return ResponseEntity.ok(Map.of(
                "id", submission.getId(),
                "status", submission.getStatus(),
                "studentName", submission.getStudent().getFullName(),
                "assignmentTitle", submission.getAssignment().getTitle(),
                "originalFileName", submission.getOriginalFileName(),
                "createdAt", submission.getCreatedAt(),
                "updatedAt", submission.getUpdatedAt(),
                "ocrTextPreview", submission.getOcrText() != null 
                    ? submission.getOcrText().substring(0, Math.min(200, submission.getOcrText().length())) 
                    : null
            ));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }
    
    /**
     * Get the full text content of a submission (for preview)
     */
    @GetMapping("/{id}/content")
    public ResponseEntity<?> getSubmissionContent(@PathVariable Long id) {
        try {
            Submission submission = submissionService.getSubmission(id);
            
            return ResponseEntity.ok(Map.of(
                "id", submission.getId(),
                "originalFileName", submission.getOriginalFileName(),
                "originalText", submission.getOcrText() != null ? submission.getOcrText() : "",
                "ocrText", submission.getOcrText() != null ? submission.getOcrText() : "",
                "anonymizedText", submission.getAnonymizedText() != null ? submission.getAnonymizedText() : "",
                "originalFileUrl", submission.getOriginalDocUrl() != null ? submission.getOriginalDocUrl() : ""
            ));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }
    
    /**
     * Get submission status (for polling)
     */
    @GetMapping("/{id}/status")
    public ResponseEntity<?> getSubmissionStatus(@PathVariable Long id) {
        try {
            Submission submission = submissionService.getSubmission(id);
            
            return ResponseEntity.ok(Map.of(
                "submissionId", id,
                "status", submission.getStatus(),
                "error", submission.getProcessingError() != null ? submission.getProcessingError() : ""
            ));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }
}

