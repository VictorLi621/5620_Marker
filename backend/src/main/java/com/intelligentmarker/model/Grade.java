package com.intelligentmarker.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "grades")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Grade {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @OneToOne
    @JoinColumn(name = "submission_id", nullable = false, unique = true)
    private Submission submission;
    
    @Column(precision = 5, scale = 2)
    private BigDecimal aiScore; // AI score 0-100

    @Column(precision = 3, scale = 2)
    private BigDecimal aiConfidence; // AI confidence 0.00-1.00

    @Column(precision = 5, scale = 2)
    private BigDecimal teacherScore; // Teacher final score

    @Column(columnDefinition = "TEXT")
    private String aiFeedback; // AI structured feedback (JSON format)

    @Column(columnDefinition = "TEXT")
    private String teacherComments; // Teacher comments

    @Column(length = 500)
    private String teacherAudioUrl; // Teacher audio annotation URL (optional)
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GradeStatus status;
    
    @ManyToOne
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy; // Reviewing teacher
    
    private LocalDateTime reviewedAt;
    
    private LocalDateTime publishedAt;
    
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    private LocalDateTime updatedAt;
    
    public enum GradeStatus {
        HIGH_CONFIDENCE,    // High confidence, recommend auto-approval
        NEEDS_REVIEW,       // Needs manual review
        APPROVED,           // Teacher approved
        PUBLISHED,          // Published to student
        APPEALED            // Student appealed
    }
}

