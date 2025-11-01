package com.intelligentmarker.repository;

import com.intelligentmarker.model.LearningPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LearningPlanRepository extends JpaRepository<LearningPlan, Long> {

    @Query("SELECT lp FROM LearningPlan lp WHERE lp.studentId = :studentId ORDER BY lp.createdAt DESC")
    List<LearningPlan> findByStudentId(@Param("studentId") Long studentId);

    @Query("SELECT lp FROM LearningPlan lp WHERE lp.gradeId = :gradeId")
    Optional<LearningPlan> findByGradeId(@Param("gradeId") Long gradeId);

    @Query("SELECT lp FROM LearningPlan lp WHERE lp.studentId = :studentId AND lp.status = :status ORDER BY lp.createdAt DESC")
    List<LearningPlan> findByStudentIdAndStatus(@Param("studentId") Long studentId, @Param("status") String status);
}
