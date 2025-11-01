package com.intelligentmarker.repository;

import com.intelligentmarker.model.Submission;
import com.intelligentmarker.model.User;
import com.intelligentmarker.model.Assignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubmissionRepository extends JpaRepository<Submission, Long> {
    List<Submission> findByStudent(User student);
    List<Submission> findByAssignment(Assignment assignment);
    Optional<Submission> findByStudentAndAssignment(User student, Assignment assignment);
    List<Submission> findByStatus(Submission.SubmissionStatus status);
}

