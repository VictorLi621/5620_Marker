package com.intelligentmarker.controller;

import com.intelligentmarker.model.Assignment;
import com.intelligentmarker.model.ClassEntity;
import com.intelligentmarker.model.Grade;
import com.intelligentmarker.model.Submission;
import com.intelligentmarker.model.User;
import com.intelligentmarker.repository.AssignmentRepository;
import com.intelligentmarker.repository.ClassRepository;
import com.intelligentmarker.repository.GradeRepository;
import com.intelligentmarker.repository.SubmissionRepository;
import com.intelligentmarker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Assignment related APIs
 */
@RestController
@RequestMapping("/api/assignments")
@Slf4j
@RequiredArgsConstructor
public class AssignmentController {
    
    private final AssignmentRepository assignmentRepository;
    private final UserRepository userRepository;
    private final ClassRepository classRepository;
    private final SubmissionRepository submissionRepository;
    private final GradeRepository gradeRepository;
    
    /**
     * Create an assignment
     */
    @PostMapping("/create")
    public ResponseEntity<?> createAssignment(@RequestBody Map<String, Object> request) {
        try {
            Long teacherId = Long.valueOf(request.get("teacherId").toString());
            String courseCode = request.get("courseCode").toString();
            String title = request.get("title").toString();
            String description = request.get("description").toString();
            Integer totalMarks = Integer.valueOf(request.get("totalMarks").toString());
            
            User teacher = userRepository.findById(teacherId)
                .orElseThrow(() -> new RuntimeException("Teacher not found"));
            
            if (!teacher.getRole().equals(User.UserRole.TEACHER)) {
                throw new RuntimeException("Only teachers can create assignments");
            }
            
            Assignment assignment = new Assignment();
            assignment.setTeacher(teacher);
            assignment.setCourseCode(courseCode);
            assignment.setTitle(title);
            assignment.setDescription(description);
            assignment.setTotalMarks(totalMarks);
            assignment.setStatus(Assignment.AssignmentStatus.PUBLISHED);
            
            // New: Support specifying class ID
            if (request.containsKey("classId") && request.get("classId") != null) {
                Long classId = Long.valueOf(request.get("classId").toString());
                ClassEntity classEntity = classRepository.findById(classId)
                    .orElseThrow(() -> new RuntimeException("Class not found"));
                

                // Validate that the teacher is the instructor of the class
                if (!classEntity.getTeacher().getId().equals(teacherId)) {
                    throw new RuntimeException("You can only create assignments for your own classes");
                }
                
                assignment.setClassEntity(classEntity);

                assignment.setCourseCode(classEntity.getCourseCode()); // Get course code from class
                
                log.info("Assignment linked to class: {}", classEntity.getClassId());
            }
            
            // Handle optional due date
            if (request.containsKey("dueDate") && request.get("dueDate") != null && !request.get("dueDate").toString().isEmpty()) {
                try {
                    String dueDateStr = request.get("dueDate").toString();
                    assignment.setDueDate(java.time.LocalDateTime.parse(dueDateStr, 
                        java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                } catch (Exception e) {
                    log.warn("Failed to parse due date, skipping");
                }
            }
            
            assignment = assignmentRepository.save(assignment);
            
            log.info("Assignment created: {} by teacher {}", assignment.getId(), teacherId);
            
            Map<String, Object> response = new java.util.HashMap<>();
            response.put("success", true);
            response.put("message", "Assignment created successfully");
            response.put("assignmentId", assignment.getId());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Failed to create assignment", e);
            Map<String, Object> response = new java.util.HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * Get all assignments (paginated) - can be filtered by teacher
     */
    @GetMapping
    public ResponseEntity<?> getAllAssignments(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(required = false) Long teacherId
    ) {
        try {
            List<Assignment> allAssignments;
            

            // If teacher ID is specified, only return assignments for that teacher
            if (teacherId != null) {
                User teacher = userRepository.findById(teacherId)
                    .orElseThrow(() -> new RuntimeException("Teacher not found"));
                allAssignments = assignmentRepository.findByTeacher(teacher);
                log.info("Filtering assignments for teacher: {}", teacherId);
            } else {
                allAssignments = assignmentRepository.findAll();
            }
            

            // Manual pagination
            int start = page * size;
            int end = Math.min(start + size, allAssignments.size());
            List<Assignment> pagedAssignments = start < allAssignments.size() 
                ? allAssignments.subList(start, end) 
                : new ArrayList<>();
            
            List<Map<String, Object>> result = pagedAssignments.stream()
                .map(a -> {
                    Map<String, Object> map = new java.util.HashMap<>();
                    map.put("id", a.getId());
                    map.put("title", a.getTitle());
                    map.put("description", a.getDescription() != null ? a.getDescription() : "");
                    map.put("courseCode", a.getCourseCode());
                    map.put("totalMarks", a.getTotalMarks());
                    map.put("status", a.getStatus());
                    map.put("dueDate", a.getDueDate() != null ? a.getDueDate().toString() : "");
                    map.put("teacherName", a.getTeacher() != null ? a.getTeacher().getFullName() : "");
                    return map;
                })
                .collect(Collectors.toList());
            
            // Return paginated data
            Map<String, Object> response = new java.util.HashMap<>();
            response.put("content", result);
            response.put("totalElements", allAssignments.size());
            response.put("totalPages", (int) Math.ceil((double) allAssignments.size() / size));
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
     * Get assignment info
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getAssignment(@PathVariable Long id) {
        try {
            Assignment assignment = assignmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Assignment not found"));

            return ResponseEntity.ok(Map.of(
                "id", assignment.getId(),
                "title", assignment.getTitle(),
                "description", assignment.getDescription(),
                "courseCode", assignment.getCourseCode(),
                "totalMarks", assignment.getTotalMarks(),
                "instructions", assignment.getInstructions() != null ? assignment.getInstructions() : "",
                "status", assignment.getStatus(),
                "dueDate", assignment.getDueDate() != null ? assignment.getDueDate().toString() : ""
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }

    /**
     * Get assignment details with statistics
     */
    @GetMapping("/{id}/details")
    public ResponseEntity<?> getAssignmentDetails(@PathVariable Long id) {
        try {
            Assignment assignment = assignmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Assignment not found"));

            // Get all submissions for this assignment
            List<Submission> submissions = submissionRepository.findByAssignment(assignment);

            // Calculate statistics
            int totalSubmissions = submissions.size();
            int scoredCount = 0;
            int publishedCount = 0;
            int pendingCount = 0;

            List<Map<String, Object>> submissionDetails = new ArrayList<>();

            for (Submission submission : submissions) {
                Map<String, Object> subMap = new java.util.HashMap<>();
                subMap.put("submissionId", submission.getId());
                subMap.put("studentId", submission.getStudent().getId());
                subMap.put("studentName", submission.getStudent().getFullName());
                subMap.put("originalFileName", submission.getOriginalFileName());
                subMap.put("submittedAt", submission.getCreatedAt());
                subMap.put("status", submission.getStatus());

                // Get grade information
                gradeRepository.findBySubmission(submission).ifPresent(grade -> {
                    subMap.put("gradeId", grade.getId());
                    subMap.put("aiScore", grade.getAiScore());
                    subMap.put("teacherScore", grade.getTeacherScore());
                    subMap.put("finalScore", grade.getTeacherScore() != null ? grade.getTeacherScore() : grade.getAiScore());
                    subMap.put("gradeStatus", grade.getStatus());
                    subMap.put("publishedAt", grade.getPublishedAt());
                });

                submissionDetails.add(subMap);

                // Update statistics
                if (submission.getStatus() == Submission.SubmissionStatus.SCORED ||
                    submission.getStatus() == Submission.SubmissionStatus.REVIEWED ||
                    submission.getStatus() == Submission.SubmissionStatus.PUBLISHED) {
                    scoredCount++;
                }

                if (submission.getStatus() == Submission.SubmissionStatus.PUBLISHED) {
                    publishedCount++;
                } else {
                    pendingCount++;
                }
            }

            Map<String, Object> response = new java.util.HashMap<>();
            response.put("id", assignment.getId());
            response.put("title", assignment.getTitle());
            response.put("description", assignment.getDescription());
            response.put("courseCode", assignment.getCourseCode());
            response.put("totalMarks", assignment.getTotalMarks());
            response.put("instructions", assignment.getInstructions() != null ? assignment.getInstructions() : "");
            response.put("status", assignment.getStatus());
            response.put("dueDate", assignment.getDueDate());
            response.put("teacherName", assignment.getTeacher().getFullName());

            // Statistics
            response.put("totalSubmissions", totalSubmissions);
            response.put("scoredCount", scoredCount);
            response.put("publishedCount", publishedCount);
            response.put("pendingCount", pendingCount);

            // Submission details
            response.put("submissions", submissionDetails);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to get assignment details", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }
}

