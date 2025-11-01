package com.intelligentmarker.repository;

import com.intelligentmarker.model.GradeSnapshot;
import com.intelligentmarker.model.Submission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GradeSnapshotRepository extends JpaRepository<GradeSnapshot, Long> {
    List<GradeSnapshot> findBySubmissionOrderByVersionNumberDesc(Submission submission);
}

