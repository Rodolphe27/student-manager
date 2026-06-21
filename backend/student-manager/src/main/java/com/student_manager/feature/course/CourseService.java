package com.student_manager.feature.course;

import java.util.List;

public interface CourseService {
    CourseDTO findById(Long id);
    List<CourseDTO> findAll();
    List<CourseDTO> findByStatus(CourseStatus status);
    CourseDTO create(CreateCourseRequest request);
    CourseDTO update(Long id, CreateCourseRequest request);
    void delete(Long id);
}
