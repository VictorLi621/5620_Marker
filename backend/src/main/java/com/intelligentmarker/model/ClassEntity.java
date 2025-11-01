package com.intelligentmarker.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Class entity - Conforms to the Student.classId design in Stage 1 class diagram
 *
 * Design philosophy:
 * - A course (e.g. ELEC5620) can have multiple classes (Class A, Class B)
 * - Each class is taught by one teacher
 * - Students can see all assignments for their class after joining
 * - Teachers can only see submissions for their own class
 */
@Entity
@Table(name = "classes")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClassEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * Class ID - Global unique identifier
     * Format: {course code}-{semester}-{class number}
     * Example: ELEC5620-2024-S1-A
     */
    @Column(name = "class_id", unique = true, nullable = false, length = 100)
    private String classId;

    /**
     * Course code (e.g. ELEC5620)
     */
    @Column(name = "course_code", nullable = false, length = 50)
    private String courseCode;

    /**
     * Semester (e.g. 2024-S1)
     */
    @Column(length = 50)
    private String semester;

    /**
     * Class name (for display)
     */
    @Column(nullable = false)
    private String name;

    /**
     * Course instructor
     */
    @ManyToOne
    @JoinColumn(name = "teacher_id", nullable = false)
    private User teacher;

    /**
     * Class description
     */
    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * Class capacity (maximum number of students)
     */
    @Column(name = "capacity")
    private Integer capacity;

    /**
     * Is active
     */
    @Column(nullable = false)
    private Boolean active = true;

    /**
     * Creation time
     */
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    /**
     * Update time
     */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * List of students in the class (many-to-many relationship)
     */
    @ManyToMany
    @JoinTable(
        name = "class_students",
        joinColumns = @JoinColumn(name = "class_id"),
        inverseJoinColumns = @JoinColumn(name = "student_id")
    )
    private List<User> students = new ArrayList<>();
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    /**
     * Add student to class
     */
    public void addStudent(User student) {
        if (!students.contains(student)) {
            students.add(student);
        }
    }

    /**
     * Remove student from class
     */
    public void removeStudent(User student) {
        students.remove(student);
    }

    /**
     * Check if class is full
     */
    public boolean isFull() {
        return capacity != null && students.size() >= capacity;
    }

    /**
     * Get current number of students
     */
    public int getCurrentSize() {
        return students.size();
    }
}

