package com.intelligentmarker.service;

import com.intelligentmarker.model.*;
import com.intelligentmarker.repository.AppealRepository;
import com.intelligentmarker.repository.GradeRepository;
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
 * Appeal service
 * Handles student grade appeals
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AppealService {
    
    private final AppealRepository appealRepository;
    private final SubmissionRepository submissionRepository;
    private final GradeRepository gradeRepository;
    private final NotificationService notificationService;
    private final PublishService publishService;
    private final AuditLogService auditLogService;
    
    /**
     * Create appeal
     */
    @Transactional
    public Appeal createAppeal(Long submissionId, User student, String reason) {
        log.info("Creating appeal for submission {} by student {}", submissionId, student.getId());
        
        Submission submission = submissionRepository.findById(submissionId)
            .orElseThrow(() -> new RuntimeException("Submission not found"));
        
        // Verify student has permission to appeal
        if (!submission.getStudent().getId().equals(student.getId())) {
            throw new RuntimeException("Unauthorized: Not your submission");
        }

        // Check if grade is published
        if (submission.getStatus() != Submission.SubmissionStatus.PUBLISHED) {
            throw new RuntimeException("Cannot appeal: Grade not published yet");
        }

        // Create appeal
        Appeal appeal = new Appeal();
        appeal.setSubmission(submission);
        appeal.setStudent(student);
        appeal.setReason(reason);
        appeal.setStatus(Appeal.AppealStatus.PENDING);

        appeal = appealRepository.save(appeal);

        // Update grade status
        Grade grade = gradeRepository.findBySubmission(submission)
            .orElseThrow(() -> new RuntimeException("Grade not found"));
        grade.setStatus(Grade.GradeStatus.APPEALED);
        gradeRepository.save(grade);

        // Record audit log
        auditLogService.log(
            student,
            "CREATE_APPEAL",
            "APPEAL",
            appeal.getId(),
            Map.of("submissionId", submissionId, "reason", reason)
        );

        // Notify teacher
        notificationService.notifyTeacherReviewNeeded(submission);
        
        log.info("Appeal created: {}", appeal.getId());
        
        return appeal;
    }
    
    /**
     * Resolve appeal (teacher review)
     */
    @Transactional
    public Appeal resolveAppeal(Long appealId, User teacher, Appeal.AppealStatus status, 
                                String resolution, BigDecimal newScore) {
        log.info("Resolving appeal {} by teacher {}", appealId, teacher.getId());
        
        Appeal appeal = appealRepository.findById(appealId)
            .orElseThrow(() -> new RuntimeException("Appeal not found"));
        
        if (appeal.getStatus() != Appeal.AppealStatus.PENDING && 
            appeal.getStatus() != Appeal.AppealStatus.REVIEWING) {
            throw new RuntimeException("Appeal already resolved");
        }
        
        appeal.setStatus(status);
        appeal.setResolution(resolution);
        appeal.setResolvedBy(teacher);
        appeal.setResolvedAt(LocalDateTime.now());
        
        appeal = appealRepository.save(appeal);
        
        Grade grade = gradeRepository.findBySubmission(appeal.getSubmission())
            .orElseThrow(() -> new RuntimeException("Grade not found"));

        // If appeal approved, update score and republish
        if (status == Appeal.AppealStatus.APPROVED && newScore != null) {
            // Update teacher score
            grade.setTeacherScore(newScore);
            grade.setTeacherComments(
                (grade.getTeacherComments() != null ? grade.getTeacherComments() + "\n\n" : "") +
                "[Adjusted after appeal] " + resolution
            );
            grade.setStatus(Grade.GradeStatus.APPROVED);
            gradeRepository.save(grade);

            // Republish with new version snapshot
            publishService.publishGrade(
                appeal.getSubmission().getId(),
                teacher,
                "Republished after appeal (Appeal #" + appealId + ")"
            );
        } else if (status == Appeal.AppealStatus.REJECTED) {
            // Reject appeal: do not modify score, only restore grade status to published
            grade.setStatus(Grade.GradeStatus.PUBLISHED);
            grade.setTeacherComments(
                (grade.getTeacherComments() != null ? grade.getTeacherComments() + "\n\n" : "") +
                "[Appeal rejected] " + resolution
            );
            gradeRepository.save(grade);
            log.info("Appeal rejected, grade status restored to PUBLISHED without score change");
        }

        // Notify student
        notificationService.notifyStudentAppealResolved(
            appealId,
            appeal.getStudent(),
            resolution
        );

        // Record audit log
        auditLogService.log(
            teacher,
            "RESOLVE_APPEAL",
            "APPEAL",
            appealId,
            Map.of(
                "status", status,
                "resolution", resolution,
                "newScore", newScore != null ? newScore : "N/A"
            )
        );
        
        log.info("Appeal resolved: {}, status={}", appealId, status);
        
        return appeal;
    }
    
    /**
     * Get pending appeals list
     */
    public List<Appeal> getPendingAppeals() {
        return appealRepository.findByStatus(Appeal.AppealStatus.PENDING);
    }
    
    /**
     * Get all appeals for a submission
     */
    public List<Appeal> getAppealsBySubmission(Long submissionId) {
        Submission submission = submissionRepository.findById(submissionId)
            .orElseThrow(() -> new RuntimeException("Submission not found"));
        
        return appealRepository.findBySubmission(submission);
    }
}

