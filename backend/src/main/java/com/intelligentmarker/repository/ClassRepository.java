package com.intelligentmarker.repository;

import com.intelligentmarker.model.ClassEntity;
import com.intelligentmarker.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClassRepository extends JpaRepository<ClassEntity, Long> {
    
    /**
     * Find class by classId
     */
    Optional<ClassEntity> findByClassId(String classId);

    /**
     * Find all classes for a teacher
     */
    List<ClassEntity> findByTeacherAndActiveTrue(User teacher);

    /**
     * Find all classes for a course code
     */
    List<ClassEntity> findByCourseCodeAndActiveTrue(String courseCode);

    /**
     * Find all classes a student is enrolled in
     */
    @Query("SELECT c FROM ClassEntity c JOIN c.students s WHERE s = :student AND c.active = true")
    List<ClassEntity> findByStudentAndActiveTrue(@Param("student") User student);

    /**
     * Check if student is in a class
     */
    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END " +
           "FROM ClassEntity c JOIN c.students s " +
           "WHERE c.id = :classId AND s = :student AND c.active = true")
    boolean isStudentInClass(@Param("classId") Long classId, @Param("student") User student);

    /**
     * Find all classes for a semester
     */
    List<ClassEntity> findBySemesterAndActiveTrue(String semester);
}

