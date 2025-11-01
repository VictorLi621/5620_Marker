package com.intelligentmarker.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Course entity
 */
@Entity
@Table(name = "courses")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Course {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String courseCode; // Course code (unique identifier)

    @Column(nullable = false)
    private String courseName; // Course name

    @Column(columnDefinition = "TEXT")
    private String description; // Course description

    @ManyToOne
    @JoinColumn(name = "teacher_id")
    private User teacher; // Course instructor

    private String semester; // Semester, e.g.: 2024-S1

    private Integer capacity; // Course capacity

    private Integer enrolled = 0; // Number of enrolled students

    @Column(nullable = false)
    private Boolean active = true; // Is active
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    private LocalDateTime updatedAt;
    
    @Enumerated(EnumType.STRING)
    private CourseStatus status = CourseStatus.ACTIVE;
    
    public enum CourseStatus {
        ACTIVE,      // In progress
        COMPLETED,   // Completed
        ARCHIVED     // Archived
    }
}

