package com.intelligentmarker.service;

import com.intelligentmarker.model.NotificationAttempt;
import com.intelligentmarker.model.Submission;
import com.intelligentmarker.model.User;
import com.intelligentmarker.repository.NotificationAttemptRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Notification service
 * Supports automatic retry mechanism (up to 3 times)
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationService {
    
    private final NotificationAttemptRepository notificationRepository;
    
    @Value("${app.notification.max-retry-attempts:3}")
    private int maxRetryAttempts;
    
    @Value("${app.notification.retry-delay-seconds:60}")
    private int retryDelaySeconds;
    
    /**
     * Notify teacher that submission needs review
     */
    @Transactional
    public void notifyTeacherReviewNeeded(Submission submission) {
        User teacher = submission.getAssignment().getTeacher();
        
        String message = String.format(
            "Submission #%d requires your review (Low AI confidence). " +
            "Student: %s, Assignment: %s",
            submission.getId(),
            submission.getStudent().getFullName(),
            submission.getAssignment().getTitle()
        );
        
        createNotification(teacher, "REVIEW_NEEDED", submission.getId(), message);
    }
    
    /**
     * Notify student that grade has been published
     */
    @Transactional
    public void notifyStudentGradePublished(Submission submission) {
        User student = submission.getStudent();
        
        String message = String.format(
            "Your grade for '%s' has been published. Click to view feedback.",
            submission.getAssignment().getTitle()
        );
        
        createNotification(student, "GRADE_PUBLISHED", submission.getId(), message);
    }
    
    /**
     * Notify student that appeal has been resolved
     */
    @Transactional
    public void notifyStudentAppealResolved(Long appealId, User student, String resolution) {
        String message = String.format(
            "Your appeal has been resolved: %s",
            resolution
        );
        
        createNotification(student, "APPEAL_RESOLVED", appealId, message);
    }
    
    /**
     * Create notification record
     */
    private void createNotification(User user, String type, Long referenceId, String message) {
        NotificationAttempt notification = new NotificationAttempt();
        notification.setUser(user);
        notification.setNotificationType(type);
        notification.setReferenceId(referenceId);
        notification.setMessage(message);
        notification.setStatus(NotificationAttempt.NotificationStatus.PENDING);
        notification.setAttemptCount(0);
        notification.setNextRetryAt(LocalDateTime.now());

        notificationRepository.save(notification);

        log.info("Notification created: {} for user {}", type, user.getEmail());

        // Immediately attempt to send
        sendNotification(notification);
    }

    /**
     * Send notification (simulates email/SMS sending)
     */
    private void sendNotification(NotificationAttempt notification) {
        notification.setAttemptCount(notification.getAttemptCount() + 1);
        notification.setLastAttemptAt(LocalDateTime.now());
        
        try {
            // Simulate sending notification (should call actual email/SMS service in production)
            boolean success = simulateSendEmail(notification.getUser().getEmail(), notification.getMessage());
            
            if (success) {
                notification.setStatus(NotificationAttempt.NotificationStatus.SENT);
                log.info("Notification sent successfully: {} (attempt {})", 
                        notification.getId(), notification.getAttemptCount());
            } else {
                handleSendFailure(notification);
            }
            
        } catch (Exception e) {
            log.error("Failed to send notification: {}", notification.getId(), e);
            notification.setErrorMessage(e.getMessage());
            handleSendFailure(notification);
        }
        
        notificationRepository.save(notification);
    }
    
    /**
     * Handle send failure
     */
    private void handleSendFailure(NotificationAttempt notification) {
        if (notification.getAttemptCount() < maxRetryAttempts) {
            notification.setStatus(NotificationAttempt.NotificationStatus.FAILED);

            // Calculate next retry time (exponential backoff)
            int delayMinutes = retryDelaySeconds * notification.getAttemptCount();
            notification.setNextRetryAt(LocalDateTime.now().plusSeconds(delayMinutes));
            
            log.warn("Notification failed, will retry at {}: {} (attempt {}/{})",
                    notification.getNextRetryAt(),
                    notification.getId(),
                    notification.getAttemptCount(),
                    maxRetryAttempts);
        } else {
            notification.setStatus(NotificationAttempt.NotificationStatus.EXHAUSTED);
            log.error("Notification retry exhausted: {} (all {} attempts failed)", 
                    notification.getId(), maxRetryAttempts);
        }
    }
    
    /**
     * Scheduled task: Check and retry failed notifications every minute
     */
    @Scheduled(fixedRate = 60000) // Execute every 60 seconds
    @Transactional
    public void retryFailedNotifications() {
        List<NotificationAttempt> failedNotifications = 
            notificationRepository.findFailedNotificationsForRetry(LocalDateTime.now());
        
        if (!failedNotifications.isEmpty()) {
            log.info("Retrying {} failed notifications", failedNotifications.size());
            
            for (NotificationAttempt notification : failedNotifications) {
                sendNotification(notification);
            }
        }
    }
    
    /**
     * Simulate email sending (80% success rate, for demonstrating retry mechanism)
     */
    private boolean simulateSendEmail(String email, String message) {
        // Production environment should use real email service (e.g. Aliyun Mail Push, SendGrid, etc.)
        // Simulation here: 40% failure rate for first two attempts to demonstrate retry
        double random = Math.random();

        log.info("Simulating email send to {}: {}", email, message);

        // 80% success rate
        return random > 0.2;
    }
}

