package com.student_manager.feature.teacher;

import java.util.List;

public interface TeacherService {
    TeacherDTO findById(Long id);
    List<TeacherDTO> findAll();
    TeacherDTO create(CreateTeacherRequest request);
    TeacherDTO update(Long id, CreateTeacherRequest request);
    void delete(Long id);
}
