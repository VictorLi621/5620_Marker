package com.intelligentmarker.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "submissions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Submission {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "student_id", nullable = false)
    private User student;
    
    @ManyToOne
    @JoinColumn(name = "assignment_id", nullable = false)
    private Assignment assignment;
    
    @Column(length = 500)
    private String originalDocUrl; // Aliyun OSS original file URL

    @Column(length = 500)
    private String anonymizedDocUrl; // Aliyun OSS anonymized file URL

    @Column(columnDefinition = "TEXT")
    private String ocrText; // OCR extracted text

    @Column(columnDefinition = "TEXT")
    private String anonymizedText; // Anonymized text
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubmissionStatus status;
    
    private String originalFileName;
    
    private String fileType; // pdf, docx, image
    
    @Column(columnDefinition = "TEXT")
    private String processingError; // Processing error message
    
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    private LocalDateTime updatedAt;
    
    public enum SubmissionStatus {
        UPLOADED,           // Uploaded
        OCR_PROCESSING,     // OCR processing
        ANONYMIZING,        // Anonymizing
        SCORING,            // AI scoring
        SCORED,             // Scoring completed
        NEEDS_REVIEW,       // Needs teacher review
        REVIEWED,           // Teacher reviewed
        PUBLISHED,          // Published
        FAILED              // Processing failed
    }
}

