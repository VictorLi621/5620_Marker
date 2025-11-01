package com.intelligentmarker.repository;

import com.intelligentmarker.model.Assignment;
import com.intelligentmarker.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AssignmentRepository extends JpaRepository<Assignment, Long> {
    List<Assignment> findByTeacher(User teacher);
    List<Assignment> findByStatus(Assignment.AssignmentStatus status);
}

