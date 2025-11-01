package com.intelligentmarker.repository;

import com.intelligentmarker.model.Appeal;
import com.intelligentmarker.model.Submission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AppealRepository extends JpaRepository<Appeal, Long> {
    List<Appeal> findBySubmission(Submission submission);
    List<Appeal> findByStatus(Appeal.AppealStatus status);
}

