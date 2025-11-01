package com.intelligentmarker.repository;

import com.intelligentmarker.model.NotificationAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationAttemptRepository extends JpaRepository<NotificationAttempt, Long> {
    
    @Query("SELECT n FROM NotificationAttempt n WHERE n.status = 'FAILED' " +
           "AND n.attemptCount < 3 AND n.nextRetryAt <= :now")
    List<NotificationAttempt> findFailedNotificationsForRetry(LocalDateTime now);
}

