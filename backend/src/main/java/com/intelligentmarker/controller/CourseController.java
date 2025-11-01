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

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Course Management API
 */
@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
@Slf4j
public class CourseController {
    
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final CourseEnrollmentRepository enrollmentRepository;
    private final AuditLogService auditLogService;
    
    /**
     * Create course (technical team)
     */
    @PostMapping("/create")
    public ResponseEntity<?> createCourse(@RequestBody Map<String, Object> request) {
        try {
            String courseCode = (String) request.get("courseCode");
            String courseName = (String) request.get("courseName");
            String description = (String) request.get("description");
            String semester = (String) request.get("semester");
            Integer capacity = request.get("capacity") != null 
                ? Integer.parseInt(request.get("capacity").toString()) 
                : 50;
            Long teacherId = request.get("teacherId") != null 
                ? Long.parseLong(request.get("teacherId").toString()) 
                : null;
            
            // Check courseCode
            if (courseRepository.findByCourseCode(courseCode).isPresent()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "Course code already exists"
                ));
            }
            
            Course course = new Course();
            course.setCourseCode(courseCode);
            course.setCourseName(courseName);
            course.setDescription(description);
            course.setSemester(semester);
            course.setCapacity(capacity);
            
            if (teacherId != null) {
                User teacher = userRepository.findById(teacherId)
                    .orElseThrow(() -> new RuntimeException("Teacher not found"));
                
                // Verify the user is a teacher
                if (!User.UserRole.TEACHER.equals(teacher.getRole())) {
                    return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "error", "Instructor must have teacher role, current role: " + teacher.getRole()
                    ));
                }
                
                course.setTeacher(teacher);
            }
            
            courseRepository.save(course);
            
            log.info("Course created: {}", courseCode);
            
            // Record log
            try {
                Map<String, Object> auditDetails = new HashMap<>();
                auditDetails.put("courseCode", courseCode);
                auditDetails.put("courseName", courseName);
                auditDetails.put("semester", semester);
                auditDetails.put("capacity", capacity);
                if (teacherId != null) {
                    auditDetails.put("teacherId", teacherId);
                }
                
                auditLogService.log(null, "CREATE_COURSE", "Course", course.getId(), auditDetails);
            } catch (Exception e) {
                log.warn("Failed to log audit for course creation", e);
            }
            
            return ResponseEntity.ok(Map.of(
                "success", true,

                "message", "Course created successfully",
                "course", course
            ));
            
        } catch (Exception e) {
            log.error("Failed to create course", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }
    
    /**
     * Get all courses
     */
    @GetMapping
    public ResponseEntity<?> getAllCourses(
        @RequestParam(required = false) String semester,
        @RequestParam(required = false) Boolean active
    ) {
        try {
            List<Course> courses;
            
            if (semester != null) {
                courses = courseRepository.findBySemester(semester);
            } else if (active != null) {
                courses = courseRepository.findByActive(active);
            } else {
                courses = courseRepository.findAll();
            }
            
            List<Map<String, Object>> result = courses.stream()
                .map(this::courseToMap)
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("Failed to fetch courses", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }
    
    /**
     * Get course by id
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getCourseById(@PathVariable Long id) {
        try {
            Course course = courseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Course not found"));
            
            return ResponseEntity.ok(courseToMap(course));
            
        } catch (Exception e) {
            log.error("Failed to fetch course", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }
    
    /**
     * Get course by course code
     */
    @GetMapping("/code/{courseCode}")
    public ResponseEntity<?> getCourseByCourseCode(@PathVariable String courseCode) {
        try {
            Course course = courseRepository.findByCourseCode(courseCode)
                .orElseThrow(() -> new RuntimeException("Course not found"));
            
            return ResponseEntity.ok(courseToMap(course));
            
        } catch (Exception e) {
            log.error("Failed to fetch course", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }
    
    /**
     * Update  course
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateCourse(
        @PathVariable Long id,
        @RequestBody Map<String, Object> request
    ) {
        try {
            Course course = courseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Course not found"));
            
            if (request.containsKey("courseName")) {
                course.setCourseName((String) request.get("courseName"));
            }
            if (request.containsKey("description")) {
                course.setDescription((String) request.get("description"));
            }
            if (request.containsKey("semester")) {
                course.setSemester((String) request.get("semester"));
            }
            if (request.containsKey("capacity")) {
                course.setCapacity(Integer.parseInt(request.get("capacity").toString()));
            }
            if (request.containsKey("active")) {
                course.setActive(Boolean.parseBoolean(request.get("active").toString()));
            }
            if (request.containsKey("teacherId")) {
                Long teacherId = Long.parseLong(request.get("teacherId").toString());
                User teacher = userRepository.findById(teacherId)
                    .orElseThrow(() -> new RuntimeException("Teacher not found"));
                course.setTeacher(teacher);
            }
            
            courseRepository.save(course);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Course updated successfully",
                "course", course
            ));
            
        } catch (Exception e) {
            log.error("Failed to update course", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }
    
    /**
     * Delete course
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCourse(@PathVariable Long id) {
        try {
            Course course = courseRepository.findById(id)

                .orElseThrow(() -> new RuntimeException("Course not found"));
            
            courseRepository.delete(course);
            
            return ResponseEntity.ok(Map.of(
                "success", true,

                "message", "Course deleted successfully"
            ));
            
        } catch (Exception e) {
            log.error("Failed to delete course", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }
    
    /**
     * Get courses for teacher
     * Includes: 1. Courses where teacher is main instructor (Course.teacher)  2. Courses where teacher is enrolled (CourseEnrollment)
     */
    @GetMapping("/teacher/{teacherId}")
    public ResponseEntity<?> getTeacherCourses(@PathVariable Long teacherId) {
        try {
            User teacher = userRepository.findById(teacherId)
                .orElseThrow(() -> new RuntimeException("Teacher not found"));
            
            log.info("🔍 Fetching courses for teacher: id={}, role={}", teacherId, teacher.getRole());
            
            // Method 1: Courses where teacher is main instructor (Course.teacher)
            List<Course> mainCourses = courseRepository.findByTeacher(teacher);
            log.info("📚 Found {} courses where teacher is main instructor", mainCourses.size());
            
            // Method 2: Courses where teacher is enrolled (via CourseEnrollment)
            Set<String> allCourseCodes = new HashSet<>();
            mainCourses.forEach(c -> allCourseCodes.add(c.getCourseCode()));
            
            // ✅ Query courses where teacher is enrolled in CourseEnrollment
            List<CourseEnrollment> teacherEnrollments = enrollmentRepository.findByStudentAndActiveTrue(teacher);
            log.info("📝 Found {} course enrollments for teacher", teacherEnrollments.size());
            teacherEnrollments.forEach(e -> {
                allCourseCodes.add(e.getCourseCode());
                log.info("  - Added course from enrollment: {}", e.getCourseCode());
            });
            
            log.info("✅ Total unique course codes: {}", allCourseCodes);
            
            // Get complete information for all related courses
            List<Course> allCourses = courseRepository.findAll().stream()
                .filter(c -> allCourseCodes.contains(c.getCourseCode()))
                .collect(Collectors.toList());
            
            List<Map<String, Object>> result = allCourses.stream()
                .map(this::courseToMap)
                .collect(Collectors.toList());
            
            log.info("🎯 Returning {} courses for teacher {}", result.size(), teacherId);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("Failed to fetch teacher courses", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }
    
    // Helper method
    private Map<String, Object> courseToMap(Course course) {
        Map<String, Object> map = new java.util.HashMap<>();
        map.put("id", course.getId());
        map.put("courseCode", course.getCourseCode());
        map.put("courseName", course.getCourseName());
        map.put("description", course.getDescription());
        map.put("semester", course.getSemester());
        map.put("capacity", course.getCapacity());
        map.put("enrolled", course.getEnrolled());
        map.put("active", course.getActive());
        map.put("status", course.getStatus());
        map.put("createdAt", course.getCreatedAt());
        
        if (course.getTeacher() != null) {
            map.put("teacherId", course.getTeacher().getId());
            map.put("teacherName", course.getTeacher().getFullName());
            map.put("teacherUsername", course.getTeacher().getUsername());
        }
        
        return map;
    }
}

