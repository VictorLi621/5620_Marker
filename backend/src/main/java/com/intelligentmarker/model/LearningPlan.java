package com.intelligentmarker.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "learning_plans")
@Data
public class LearningPlan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "grade_id", nullable = false)
    private Long gradeId;

    @Column(name = "assignment_title")
    private String assignmentTitle;

    @Column(name = "plan_content", columnDefinition = "TEXT")
    private String planContent; // JSON format containing the learning plan

    @Column(name = "objective", columnDefinition = "TEXT")
    private String objective;

    @Column(name = "estimated_duration")
    private String estimatedDuration;

    @Column(name = "status")
    private String status; // ACTIVE, COMPLETED, ARCHIVED

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = "ACTIVE";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
