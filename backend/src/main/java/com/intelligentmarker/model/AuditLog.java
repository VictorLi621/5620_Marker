package com.intelligentmarker.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "actor_id")
    private User actor; // Actor (may be system user)

    @Column(nullable = false)
    private String action; // Action type: UPLOAD, SCORE, PUBLISH, APPEAL, ADJUST_GRADE

    @Column(nullable = false)
    private String entityType; // Entity type: SUBMISSION, GRADE, APPEAL

    @Column(nullable = false)
    private Long entityId; // Entity ID

    @Column(columnDefinition = "TEXT")
    private String details; // Detailed information (JSON format)

    private String ipAddress; // Actor IP address
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime timestamp;
    
    @Column(columnDefinition = "TEXT")
    private String changeDescription; // Change description
}

