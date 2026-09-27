package com.student_manager.feature.invite;

import com.student_manager.feature.auth.User;
import com.student_manager.feature.student.Student;
import com.student_manager.feature.student.StudentRepository;
import com.student_manager.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * {@link ProfileResolver} implementation for the Student profile type.
 */
@Component
@RequiredArgsConstructor
public class StudentProfileResolver implements ProfileResolver {

    private final StudentRepository studentRepository;

    /**
     * {@inheritDoc}
     *
     * @return {@link ProfileType#STUDENT}
     */
    @Override
    public ProfileType supports() {
        return ProfileType.STUDENT;
    }

    /**
     * {@inheritDoc}
     *
     * @param targetId the id of the student to check
     * @return {@code true} if a student with this id exists
     */
    @Override
    public boolean profileExists(Long targetId) {
        return studentRepository.existsById(targetId);
    }

    /**
     * {@inheritDoc}
     *
     * @param targetId the id of the student to link
     * @param user the account to attach to the student
     * @throws ResourceNotFoundException if no student exists with {@code targetId}
     */
    @Override
    public void linkAccount(Long targetId, User user) {
        Student student = studentRepository.findById(targetId)
                .orElseThrow(() -> new ResourceNotFoundException("Student", targetId));
        student.setAccount(user);
        studentRepository.save(student);
    }
}
