package com.intelligentmarker.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "assignments")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Assignment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String title;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @ManyToOne
    @JoinColumn(name = "teacher_id", nullable = false)
    private User teacher;
    
    /**
     * Associated class (newly added)
     * Assignments now belong to a class instead of a generic courseCode
     */
    @ManyToOne
    @JoinColumn(name = "class_id")
    private ClassEntity classEntity;

    /**
     * Course code (retained for display and compatibility)
     * Actual association managed through classEntity
     */
    @Column(nullable = false)
    private String courseCode;
    
    private LocalDateTime dueDate;
    
    private Integer totalMarks; // Total marks

    @Column(columnDefinition = "TEXT")
    private String instructions; // Assignment instructions
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AssignmentStatus status;
    
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    private LocalDateTime updatedAt;
    
    public enum AssignmentStatus {
        DRAFT,
        PUBLISHED,
        CLOSED
    }
}

