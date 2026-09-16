package com.student_manager.feature.invite;

import com.student_manager.feature.auth.User;
import com.student_manager.feature.student.Student;
import com.student_manager.feature.student.StudentRepository;
import com.student_manager.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StudentProfileResolver implements ProfileResolver {

    private final StudentRepository studentRepository;

    @Override
    public ProfileType supports() {
        return ProfileType.STUDENT;
    }

    @Override
    public boolean profileExists(Long targetId) {
        return studentRepository.existsById(targetId);
    }

    @Override
    public void linkAccount(Long targetId, User user) {
        Student student = studentRepository.findById(targetId)
                .orElseThrow(() -> new ResourceNotFoundException("Student", targetId));
        student.setAccount(user);
        studentRepository.save(student);
    }
}
