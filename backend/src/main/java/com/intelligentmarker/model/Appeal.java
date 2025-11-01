package com.intelligentmarker.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "appeals")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Appeal {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "submission_id", nullable = false)
    private Submission submission;
    
    @ManyToOne
    @JoinColumn(name = "student_id", nullable = false)
    private User student;
    
    @Column(columnDefinition = "TEXT", nullable = false)
    private String reason;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AppealStatus status;
    
    @Column(columnDefinition = "TEXT")
    private String resolution;
    
    @ManyToOne
    @JoinColumn(name = "resolved_by")
    private User resolvedBy;
    
    private LocalDateTime resolvedAt;
    
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    private LocalDateTime updatedAt;
    
    public enum AppealStatus {

        PENDING,    // Pending
        REVIEWING,  // Reviewing
        APPROVED,   // Approved (score adjusted)
        REJECTED,   // Rejected
        CLOSED      // Closed
    }
}

