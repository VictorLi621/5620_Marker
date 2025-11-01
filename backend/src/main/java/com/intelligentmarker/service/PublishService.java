package com.intelligentmarker.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intelligentmarker.model.*;
import com.intelligentmarker.repository.GradeRepository;
import com.intelligentmarker.repository.GradeSnapshotRepository;
import com.intelligentmarker.repository.SubmissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Publish service
 * Creates immutable snapshots and notifies students
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PublishService {
    
    private final GradeRepository gradeRepository;
    private final GradeSnapshotRepository snapshotRepository;
    private final SubmissionRepository submissionRepository;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;
    
    /**
     * Publish grade to student
     * @param submissionId Submission ID
     * @param publishedBy Publisher (teacher)
     * @param notes Publish notes
     */
    @Transactional
    public GradeSnapshot publishGrade(Long submissionId, User publishedBy, String notes) {
        log.info("Publishing grade for submission {}", submissionId);

        // 1. Get submission and grade
        Submission submission = submissionRepository.findById(submissionId)
            .orElseThrow(() -> new RuntimeException("Submission not found"));

        Grade grade = gradeRepository.findBySubmission(submission)
            .orElseThrow(() -> new RuntimeException("Grade not found"));

        // 2. Determine final score (teacher score takes priority, otherwise use AI score)
        BigDecimal finalScore = grade.getTeacherScore() != null
            ? grade.getTeacherScore()
            : grade.getAiScore();

        // 3. Calculate version number
        List<GradeSnapshot> existingSnapshots =
            snapshotRepository.findBySubmissionOrderByVersionNumberDesc(submission);
        int versionNumber = existingSnapshots.isEmpty() ? 1 : existingSnapshots.get(0).getVersionNumber() + 1;

        // 4. Create immutable snapshot
        GradeSnapshot snapshot = new GradeSnapshot();
        snapshot.setSubmission(submission);
        snapshot.setFinalScore(finalScore);
        snapshot.setPublishedBy(publishedBy);
        snapshot.setVersionNumber(versionNumber);
        snapshot.setPublishNotes(notes);

        // 5. Merge AI feedback and teacher comments
        String combinedFeedback = buildCombinedFeedback(grade);
        snapshot.setFeedback(combinedFeedback);
        
        // 6. Save detailed grading breakdown
        try {
            Map<String, Object> breakdown = Map.of(
                "aiScore", grade.getAiScore(),
                "aiConfidence", grade.getAiConfidence(),
                "teacherScore", grade.getTeacherScore(),
                "teacherComments", grade.getTeacherComments() != null ? grade.getTeacherComments() : "",
                "aiFeedback", grade.getAiFeedback() != null ? grade.getAiFeedback() : "{}"
            );
            snapshot.setDetailedBreakdown(objectMapper.writeValueAsString(breakdown));
        } catch (Exception e) {
            log.error("Failed to serialize breakdown", e);
        }

        snapshot = snapshotRepository.save(snapshot);

        // 7. Update Grade status
        grade.setStatus(Grade.GradeStatus.PUBLISHED);
        grade.setPublishedAt(LocalDateTime.now());
        gradeRepository.save(grade);

        // 8. Update Submission status
        submission.setStatus(Submission.SubmissionStatus.PUBLISHED);
        submissionRepository.save(submission);

        // 9. Notify student
        notificationService.notifyStudentGradePublished(submission);

        // 10. Record audit log
        auditLogService.log(
            publishedBy,
            "PUBLISH_GRADE",
            "GRADE_SNAPSHOT",
            snapshot.getId(),
            Map.of(
                "submissionId", submissionId,
                "finalScore", finalScore,
                "versionNumber", versionNumber,
                "studentId", submission.getStudent().getId()
            )
        );
        
        log.info("Grade published: submission={}, score={}, version={}", 
                submissionId, finalScore, versionNumber);
        
        return snapshot;
    }
    
    /**
     * Build combined feedback (AI + Teacher)
     */
    private String buildCombinedFeedback(Grade grade) {
        StringBuilder feedback = new StringBuilder();
        
        feedback.append("=== AI Scoring Feedback ===\n");
        feedback.append("Score: ").append(grade.getAiScore()).append("\n");
        feedback.append("Confidence: ").append(grade.getAiConfidence()).append("\n");
        
        if (grade.getAiFeedback() != null) {
            feedback.append("Detailed Feedback:\n").append(grade.getAiFeedback()).append("\n\n");
        }
        
        if (grade.getTeacherComments() != null && !grade.getTeacherComments().isEmpty()) {
            feedback.append("=== Teacher Comments ===\n");
            feedback.append(grade.getTeacherComments()).append("\n");
        }
        
        if (grade.getTeacherScore() != null) {
            feedback.append("\nFinal Score (Adjusted by Teacher): ").append(grade.getTeacherScore()).append("\n");
        }

        return feedback.toString();
    }
}

