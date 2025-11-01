package com.intelligentmarker.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * Course enrollment table - Student and course association
 */
@Entity
@Table(name = "course_enrollments")
@Data
public class CourseEnrollment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * Course code (e.g. ELEC5620)
     */
    @Column(nullable = false)
    private String courseCode;

    /**
     * Student
     */
    @ManyToOne
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    /**
     * Course instructor
     */
    @ManyToOne
    @JoinColumn(name = "teacher_id")
    private User teacher;

    /**
     * Enrollment time
     */
    @Column(name = "enrolled_at")
    private LocalDateTime enrolledAt;

    /**
     * Is active
     */
    @Column(nullable = false)
    private Boolean active = true;
    
    @PrePersist
    protected void onCreate() {
        enrolledAt = LocalDateTime.now();
    }
}

