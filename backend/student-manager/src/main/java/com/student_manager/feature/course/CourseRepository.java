package com.student_manager.feature.course;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {
    Optional<Course> findByCode(String code);
    boolean existsByCode(String code);
    boolean existsByCodeAndIdNot(String code, Long id);
    List<Course> findByStatus(CourseStatus status);

    /** Used by OwnershipGuard to check whether the given account teaches this course. */
    boolean existsByIdAndTeacher_Account_Username(Long id, String username);
}
