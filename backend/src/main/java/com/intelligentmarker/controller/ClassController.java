package com.intelligentmarker.controller;

import com.intelligentmarker.model.ClassEntity;
import com.intelligentmarker.model.User;
import com.intelligentmarker.repository.ClassRepository;
import com.intelligentmarker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Class Management API
 * Used by technical team and teachers
 */
@RestController
@RequestMapping("/api/classes")
@RequiredArgsConstructor
@Slf4j
public class ClassController {
    
    private final ClassRepository classRepository;
    private final UserRepository userRepository;
    
    /**
     * Create class (technical team or teacher)
     */
    @PostMapping("/create")
    public ResponseEntity<?> createClass(@RequestBody Map<String, Object> request) {
        try {
            String classId = request.get("classId").toString();
            String courseCode = request.get("courseCode").toString();
            String name = request.get("name").toString();
            Long teacherId = Long.valueOf(request.get("teacherId").toString());
            
            // Check classId
            if (classRepository.findByClassId(classId).isPresent()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "Class ID already exists"
                ));
            }
            
            User teacher = userRepository.findById(teacherId)
                .orElseThrow(() -> new RuntimeException("Teacher not found"));
            
            ClassEntity classEntity = new ClassEntity();
            classEntity.setClassId(classId);
            classEntity.setCourseCode(courseCode);
            classEntity.setName(name);
            classEntity.setTeacher(teacher);
            
            // Optional fields
            if (request.containsKey("semester")) {
                classEntity.setSemester(request.get("semester").toString());
            }
            if (request.containsKey("description")) {
                classEntity.setDescription(request.get("description").toString());
            }
            if (request.containsKey("capacity")) {
                classEntity.setCapacity(Integer.valueOf(request.get("capacity").toString()));
            }
            
            classEntity = classRepository.save(classEntity);
            
            log.info("Class created: {} by teacher {}", classId, teacherId);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Class created successfully",
                "classId", classEntity.getId(),
                "classIdentifier", classEntity.getClassId()
            ));
            
        } catch (Exception e) {
            log.error("Create class failed", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }
    
    /**
     * Add student to class (teacher)
     */
    @PostMapping("/{classId}/add-student")
    public ResponseEntity<?> addStudentToClass(
        @PathVariable Long classId,
        @RequestBody Map<String, Object> request
    ) {
        try {
            Long studentId = Long.valueOf(request.get("studentId").toString());
            
            ClassEntity classEntity = classRepository.findById(classId)
                .orElseThrow(() -> new RuntimeException("Class not found"));
            
            User student = userRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found"));
            
            // Check class full
            if (classEntity.isFull()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "Class is full"
                ));
            }
            
            // Check if student is already in class
            if (classEntity.getStudents().contains(student)) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "Student already in class"
                ));
            }
            
            classEntity.addStudent(student);
            classRepository.save(classEntity);
            
            log.info("Student {} added to class {}", studentId, classEntity.getClassId());
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Student added to class successfully",
                "currentSize", classEntity.getCurrentSize()
            ));
            
        } catch (Exception e) {
            log.error("Add student to class failed", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }
    
    /**
     * Add students to class in batch
     */
    @PostMapping("/{classId}/add-students-batch")
    public ResponseEntity<?> addStudentsBatch(
        @PathVariable Long classId,
        @RequestBody Map<String, Object> request
    ) {
        try {
            @SuppressWarnings("unchecked")
            List<Long> studentIds = (List<Long>) request.get("studentIds");
            
            ClassEntity classEntity = classRepository.findById(classId)
                .orElseThrow(() -> new RuntimeException("Class not found"));
            
            int successCount = 0;
            for (Long studentId : studentIds) {
                User student = userRepository.findById(studentId).orElse(null);
                if (student != null && "STUDENT".equals(student.getRole())) {
                    if (!classEntity.getStudents().contains(student) && !classEntity.isFull()) {
                        classEntity.addStudent(student);
                        successCount++;
                    }
                }
            }
            
            classRepository.save(classEntity);
            
            log.info("Batch added {} students to class {}", successCount, classEntity.getClassId());
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Successfully added " + successCount + " students",
                "addedCount", successCount,
                "currentSize", classEntity.getCurrentSize()
            ));
            
        } catch (Exception e) {
            log.error("Batch add students failed", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }
    
    /**
     * Remove student from class
     */
    @DeleteMapping("/{classId}/remove-student/{studentId}")
    public ResponseEntity<?> removeStudentFromClass(
        @PathVariable Long classId,
        @PathVariable Long studentId
    ) {
        try {
            ClassEntity classEntity = classRepository.findById(classId)
                .orElseThrow(() -> new RuntimeException("Class not found"));
            
            User student = userRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found"));
            
            classEntity.removeStudent(student);
            classRepository.save(classEntity);
            
            log.info("Student {} removed from class {}", studentId, classEntity.getClassId());
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Student removed from class successfully"
            ));
            
        } catch (Exception e) {
            log.error("Remove student from class failed", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }
    
    /**
     * Get class details
     */
    @GetMapping("/{classId}")
    public ResponseEntity<?> getClassDetails(@PathVariable Long classId) {
        try {
            ClassEntity classEntity = classRepository.findById(classId)
                .orElseThrow(() -> new RuntimeException("Class not found"));
            
            Map<String, Object> result = new java.util.HashMap<>();
            result.put("id", classEntity.getId());
            result.put("classId", classEntity.getClassId());
            result.put("courseCode", classEntity.getCourseCode());
            result.put("name", classEntity.getName());
            result.put("semester", classEntity.getSemester());
            result.put("description", classEntity.getDescription());
            result.put("teacherId", classEntity.getTeacher().getId());
            result.put("teacherName", classEntity.getTeacher().getFullName());
            result.put("capacity", classEntity.getCapacity());
            result.put("currentSize", classEntity.getCurrentSize());
            result.put("active", classEntity.getActive());
            result.put("createdAt", classEntity.getCreatedAt());
            
            // Student list
            List<Map<String, Object>> students = classEntity.getStudents().stream()
                .map(s -> Map.of(
                    "id", (Object) s.getId(),
                    "fullName", s.getFullName(),
                    "email", s.getEmail()
                ))
                .collect(Collectors.toList());
            result.put("students", students);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }
    
    /**
     * Get all classes for a teacher
     */
    @GetMapping("/teacher/{teacherId}")
    public ResponseEntity<?> getTeacherClasses(@PathVariable Long teacherId) {
        try {
            User teacher = userRepository.findById(teacherId)
                .orElseThrow(() -> new RuntimeException("Teacher not found"));
            
            List<ClassEntity> classes = classRepository.findByTeacherAndActiveTrue(teacher);
            
            List<Map<String, Object>> result = classes.stream()
                .map(c -> {
                    Map<String, Object> map = new java.util.HashMap<>();
                    map.put("id", c.getId());
                    map.put("classId", c.getClassId());
                    map.put("courseCode", c.getCourseCode());
                    map.put("name", c.getName());
                    map.put("semester", c.getSemester());
                    map.put("currentSize", c.getCurrentSize());
                    map.put("capacity", c.getCapacity());
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
     * Get all classes for a student
     */
    @GetMapping("/student/{studentId}")
    public ResponseEntity<?> getStudentClasses(@PathVariable Long studentId) {
        try {
            User student = userRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found"));
            
            List<ClassEntity> classes = classRepository.findByStudentAndActiveTrue(student);
            
            List<Map<String, Object>> result = classes.stream()
                .map(c -> {
                    Map<String, Object> map = new java.util.HashMap<>();
                    map.put("id", c.getId());
                    map.put("classId", c.getClassId());
                    map.put("courseCode", c.getCourseCode());
                    map.put("name", c.getName());
                    map.put("semester", c.getSemester());
                    map.put("teacherName", c.getTeacher().getFullName());
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
     * Gell classes (paginated)t a
     */
    @GetMapping
    public ResponseEntity<?> getAllClasses(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        try {
            List<ClassEntity> allClasses = classRepository.findAll();
            
            // Handle pagination
            int start = page * size;
            int end = Math.min(start + size, allClasses.size());
            List<ClassEntity> pagedClasses = start < allClasses.size()
                ? allClasses.subList(start, end)
                : List.of();
            
            List<Map<String, Object>> result = pagedClasses.stream()
                .map(c -> {
                    Map<String, Object> map = new java.util.HashMap<>();
                    map.put("id", c.getId());
                    map.put("classId", c.getClassId());
                    map.put("courseCode", c.getCourseCode());
                    map.put("name", c.getName());
                    map.put("semester", c.getSemester());
                    map.put("teacherName", c.getTeacher().getFullName());
                    map.put("currentSize", c.getCurrentSize());
                    map.put("capacity", c.getCapacity());
                    map.put("active", c.getActive());
                    return map;
                })
                .collect(Collectors.toList());
            
            Map<String, Object> response = new java.util.HashMap<>();
            response.put("content", result);
            response.put("totalElements", allClasses.size());
            response.put("totalPages", (int) Math.ceil((double) allClasses.size() / size));
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
}

