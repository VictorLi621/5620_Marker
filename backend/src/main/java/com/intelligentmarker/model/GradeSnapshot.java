package com.intelligentmarker.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "grade_snapshots")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GradeSnapshot {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "submission_id", nullable = false)
    private Submission submission;
    
    @Column(precision = 5, scale = 2, nullable = false)
    private BigDecimal finalScore;
    
    @Column(columnDefinition = "TEXT")
    private String feedback; // Complete feedback (AI + teacher)

    @Column(columnDefinition = "TEXT")
    private String detailedBreakdown; // Detailed grading breakdown (JSON)

    @ManyToOne
    @JoinColumn(name = "published_by", nullable = false)
    private User publishedBy;

    @Column(nullable = false)
    private Integer versionNumber; // Version number, increments on republishing after appeal

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime snapshotAt; // Snapshot creation time (immutable)

    @Column(columnDefinition = "TEXT")
    private String publishNotes; // Publication notes
}

