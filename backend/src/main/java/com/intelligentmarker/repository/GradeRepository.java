package com.intelligentmarker.repository;

import com.intelligentmarker.model.Grade;
import com.intelligentmarker.model.Submission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GradeRepository extends JpaRepository<Grade, Long> {
    Optional<Grade> findBySubmission(Submission submission);
    List<Grade> findByStatus(Grade.GradeStatus status);
}

