package com.intelligentmarker.repository;

import com.intelligentmarker.model.CourseEnrollment;
import com.intelligentmarker.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CourseEnrollmentRepository extends JpaRepository<CourseEnrollment, Long> {
    
    /**
     * Find all course enrollment records for a student
     */
    List<CourseEnrollment> findByStudentAndActiveTrue(User student);

    /**
     * Find all students for a course
     */
    List<CourseEnrollment> findByCourseCodeAndActiveTrue(String courseCode);

    /**
     * Check if student is enrolled in a course
     */
    Optional<CourseEnrollment> findByStudentAndCourseCodeAndActiveTrue(User student, String courseCode);

    /**
     * Find all students for a teacher's courses
     */
    List<CourseEnrollment> findByTeacherAndActiveTrue(User teacher);
}

