package com.intelligentmarker.repository;

import com.intelligentmarker.model.Rubric;
import com.intelligentmarker.model.Assignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RubricRepository extends JpaRepository<Rubric, Long> {
    List<Rubric> findByAssignment(Assignment assignment);
}

