package com.student_manager.feature.student;

import java.util.List;

public interface StudentService {
    StudentDTO findById(Long id);
    List<StudentDTO> findAll();
    StudentDTO create(CreateStudentRequest request);
    StudentDTO update(Long id, CreateStudentRequest request);
    void delete(Long id);
}
