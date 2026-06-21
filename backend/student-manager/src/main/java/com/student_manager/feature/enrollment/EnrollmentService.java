package com.student_manager.feature.enrollment;

import java.util.List;

public interface EnrollmentService {
    EnrollmentDTO findById(Long id);
    List<EnrollmentDTO> findAll();
    List<EnrollmentDTO> findByStudentId(Long studentId);
    List<EnrollmentDTO> findByCourseId(Long courseId);
    EnrollmentDTO create(CreateEnrollmentRequest request);
    EnrollmentDTO confirm(Long id);
    EnrollmentDTO cancel(Long id);
    EnrollmentDTO updateGrade(Long id, UpdateGradeRequest request);
    void delete(Long id);
}
