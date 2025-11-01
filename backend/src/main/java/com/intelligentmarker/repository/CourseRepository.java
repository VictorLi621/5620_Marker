package com.intelligentmarker.repository;

import com.intelligentmarker.model.Course;
import com.intelligentmarker.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {
    Optional<Course> findByCourseCode(String courseCode);
    List<Course> findByTeacher(User teacher);
    List<Course> findByActive(Boolean active);
    List<Course> findBySemester(String semester);
}

