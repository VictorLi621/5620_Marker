package com.intelligentmarker.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "notification_attempts")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationAttempt {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(nullable = false)
    private String notificationType; // GRADE_PUBLISHED, APPEAL_RESOLVED
    
    @Column(nullable = false)
    private Long referenceId; // submissionId or appealId
    
    @Column(columnDefinition = "TEXT", nullable = false)
    private String message;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationStatus status;
    
    @Column(nullable = false)
    private Integer attemptCount; // Current attempt count
    
    @Column(columnDefinition = "TEXT")
    private String errorMessage; // Failure reason

    private LocalDateTime lastAttemptAt;

    private LocalDateTime nextRetryAt; // Next retry time
    
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    private LocalDateTime updatedAt;
    
    public enum NotificationStatus {
        PENDING,    // Pending send
        SENT,       // Sent
        FAILED,     // Failed (will retry)
        EXHAUSTED   // Retries exhausted
    }
}

