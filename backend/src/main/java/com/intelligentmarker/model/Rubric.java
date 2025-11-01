package com.intelligentmarker.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "rubrics")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Rubric {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "assignment_id", nullable = false)
    private Assignment assignment;
    
    @Column(nullable = false)
    private String questionId; // Question ID, e.g. Q1, Q2

    @Column(columnDefinition = "TEXT", nullable = false)
    private String criteria; // Grading criteria description

    @Column(nullable = false)
    private Integer weight; // Score weight

    @Column(columnDefinition = "TEXT")
    private String keyPoints; // Key scoring points (newline or JSON)

    @Column(columnDefinition = "TEXT")
    private String sampleAnswer; // Reference answer
    
    @Enumerated(EnumType.STRING)
    private QuestionType questionType;
    
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    private LocalDateTime updatedAt;
    
    public enum QuestionType {
        OBJECTIVE,   // Objective questions (multiple choice, fill-in-blank)
        SUBJECTIVE   // Subjective questions (short answer, essay)
    }
}

