package com.intelligentmarker.controller;

import com.intelligentmarker.model.Course;
import com.intelligentmarker.model.CourseEnrollment;
import com.intelligentmarker.model.User;
import com.intelligentmarker.repository.CourseEnrollmentRepository;
import com.intelligentmarker.repository.CourseRepository;
import com.intelligentmarker.repository.UserRepository;
import com.intelligentmarker.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Course Enrollment Management API - For Technical Team Use
 */
@RestController
@RequestMapping("/api/enrollments")
@RequiredArgsConstructor
@Slf4j
public class CourseEnrollmentController {
    
    private final CourseEnrollmentRepository enrollmentRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;
    
    /**
     * Batch enroll students to a course
     */
    @PostMapping("/batch-enroll")
    public ResponseEntity<?> batchEnroll(@RequestBody Map<String, Object> request) {
        try {
            String courseCode = request.get("courseCode").toString();
            Long teacherId = Long.valueOf(request.get("teacherId").toString());
            @SuppressWarnings("unchecked")
            List<Long> studentIds = (List<Long>) request.get("studentIds");
            
            User teacher = userRepository.findById(teacherId)
                .orElseThrow(() -> new RuntimeException("Teacher not found"));
            
            int successCount = 0;
            for (Long studentId : studentIds) {
                User student = userRepository.findById(studentId).orElse(null);
                if (student != null && "STUDENT".equals(student.getRole())) {
                    // Check if already enrolled
                    if (enrollmentRepository.findByStudentAndCourseCodeAndActiveTrue(student, courseCode).isEmpty()) {
                        CourseEnrollment enrollment = new CourseEnrollment();
                        enrollment.setCourseCode(courseCode);
                        enrollment.setStudent(student);
                        enrollment.setTeacher(teacher);
                        enrollmentRepository.save(enrollment);
                        successCount++;
                    }
                }
            }
            
            log.info("Batch enrolled {} students to course {}", successCount, courseCode);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Successfully added " + successCount + " students",
                "enrolledCount", successCount
            ));
            
        } catch (Exception e) {
            log.error("Batch enrollment failed", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }
    
    /**

     * Enroll a user (student or teacher) to a course
     */
    @PostMapping("/enroll")
    public ResponseEntity<?> enrollStudent(@RequestBody Map<String, Object> request) {
        try {
            String courseCode = request.get("courseCode").toString();
            Long userId = Long.valueOf(request.get("studentId").toString()); // Keep field name for compatibility
            
            // ✅ Validate user ID
            if (userId <= 0) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "Invalid user ID"
                ));
            }
            
            User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found, ID: " + userId));
            
            // ✅ Validate role (only students or teachers, not technical team)
            if (User.UserRole.TECHNICAL_TEAM.equals(user.getRole()) || User.UserRole.ADMIN.equals(user.getRole())) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "Technical team/admin cannot be added to courses"
                ));
            }
            
            if (!User.UserRole.STUDENT.equals(user.getRole()) && !User.UserRole.TEACHER.equals(user.getRole())) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "Only students or teachers can be added to courses, current role: " + user.getRole()
                ));
            }
            
            // ✅ Check if user is already the main teacher of the course
            Course course = courseRepository.findByCourseCode(courseCode).orElse(null);
            if (course != null && course.getTeacher() != null && course.getTeacher().getId().equals(userId)) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "User is already the main teacher of this course, no need to add again"
                ));
            }
            
            // Check if already enrolled
            if (enrollmentRepository.findByStudentAndCourseCodeAndActiveTrue(user, courseCode).isPresent()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "User is already enrolled in this course"
                ));
            }
            
            CourseEnrollment enrollment = new CourseEnrollment();
            enrollment.setCourseCode(courseCode);
            enrollment.setStudent(user);
            // Leave teacher field empty, determined by teacher field in Course table
            
            CourseEnrollment savedEnrollment = enrollmentRepository.save(enrollment);
            
            String roleName = User.UserRole.STUDENT.equals(user.getRole()) ? "student" : "teacher";
            log.info("✅ User {} ({}) enrolled in course {}, enrollmentId: {}, active: {}", 
                userId, roleName, courseCode, savedEnrollment.getId(), savedEnrollment.getActive());
            
            // ✅ Verify by querying immediately after saving
            List<CourseEnrollment> verify = enrollmentRepository.findByStudentAndActiveTrue(user);
            log.info("✅ Verification: User {} now has {} active enrollments", userId, verify.size());
            verify.forEach(e -> log.info("  - Course: {}, Active: {}", e.getCourseCode(), e.getActive()));
            
            // ✅ Record audit log
            try {
                Map<String, Object> auditDetails = new HashMap<>();
                auditDetails.put("userId", userId);
                auditDetails.put("userRole", user.getRole().toString());
                auditDetails.put("userName", user.getFullName());
                auditDetails.put("userEmail", user.getEmail());
                auditDetails.put("courseCode", courseCode);
                auditDetails.put("action", "Add " + roleName + " to course");
                

                // Operator is null indicating technical team operation (can be obtained via Spring Security later)
                auditLogService.log(null, "ENROLL_USER", "CourseEnrollment", enrollment.getId(), auditDetails);
            } catch (Exception e) {
                log.warn("Failed to log audit for enrollment", e);
            }
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", roleName + " added successfully",
                "enrollmentId", enrollment.getId()
            ));
            
        } catch (Exception e) {
            log.error("Enrollment failed", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }
    
    /**
     * Query student's course enrollment list
     */
    @GetMapping("/student/{studentId}")
    public ResponseEntity<?> getStudentEnrollments(@PathVariable Long studentId) {
        try {
            User student = userRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found"));
            
            List<CourseEnrollment> enrollments = enrollmentRepository.findByStudentAndActiveTrue(student);
            
            List<Map<String, Object>> result = enrollments.stream()
                .map(e -> {
                    Map<String, Object> map = new java.util.HashMap<>();
                    map.put("id", e.getId());
                    map.put("courseCode", e.getCourseCode());
                    map.put("teacherName", e.getTeacher() != null ? e.getTeacher().getFullName() : "");
                    map.put("enrolledAt", e.getEnrolledAt());
                    return map;
                })
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }
    

    /**
     * Query course member list (including main teacher and enrolled students/teachers)
     */
    @GetMapping("/course/{courseCode}")
    public ResponseEntity<?> getCourseStudents(@PathVariable String courseCode) {
        try {
            List<Map<String, Object>> result = new ArrayList<>();
            
            // ✅ 1. First query and return the main teacher
            Course course = courseRepository.findByCourseCode(courseCode).orElse(null);
            if (course != null && course.getTeacher() != null) {
                User mainTeacher = course.getTeacher(); // Directly get because it's a @ManyToOne relationship
                Map<String, Object> teacherMap = new HashMap<>();
                teacherMap.put("enrollmentId", null); // Main teacher has no enrollmentId
                teacherMap.put("studentId", mainTeacher.getId());
                teacherMap.put("studentIdNumber", mainTeacher.getStudentId());
                teacherMap.put("username", mainTeacher.getUsername());
                teacherMap.put("studentName", mainTeacher.getFullName());
                teacherMap.put("email", mainTeacher.getEmail());
                teacherMap.put("role", mainTeacher.getRole().toString());
                teacherMap.put("enrolledAt", course.getCreatedAt()); // Use course creation time
                teacherMap.put("isMainTeacher", true); // Mark as main teacher
                result.add(teacherMap);
                
                log.info("✅ Course {} main teacher: {} (ID: {})", courseCode, mainTeacher.getFullName(), mainTeacher.getId());
            }
            
            // ✅ 2. Then query and return enrolled students and teaching assistants
            List<CourseEnrollment> enrollments = enrollmentRepository.findByCourseCodeAndActiveTrue(courseCode);
            List<Map<String, Object>> enrollmentList = enrollments.stream()
                .map(e -> {
                    User student = e.getStudent();
                    Map<String, Object> map = new HashMap<>();
                    map.put("enrollmentId", e.getId());
                    map.put("studentId", student.getId());
                    map.put("studentIdNumber", student.getStudentId());
                    map.put("username", student.getUsername());
                    map.put("studentName", student.getFullName());
                    map.put("email", student.getEmail());
                    map.put("role", student.getRole().toString());
                    map.put("enrolledAt", e.getEnrolledAt());
                    map.put("isMainTeacher", false); // Not the main teacher
                    
                    if (e.getTeacher() != null) {
                        map.put("teacherId", e.getTeacher().getId());
                        map.put("teacherName", e.getTeacher().getFullName());
                    }
                    
                    return map;
                })
                .collect(Collectors.toList());
            
            result.addAll(enrollmentList);
            
            log.info("✅ Course {} total members: {} (1 main teacher + {} enrollments)", 
                courseCode, result.size(), enrollmentList.size());
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("Failed to get course students", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }
    
    /**
     * Drop course enrollment
     */
    @DeleteMapping("/{enrollmentId}")
    public ResponseEntity<?> dropCourse(@PathVariable Long enrollmentId) {
        try {
            CourseEnrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new RuntimeException("Enrollment not found"));
            
            enrollment.setActive(false);
            enrollmentRepository.save(enrollment);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Successfully dropped course"
            ));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }
}

