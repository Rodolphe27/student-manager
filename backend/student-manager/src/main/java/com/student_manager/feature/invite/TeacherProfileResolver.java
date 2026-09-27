package com.student_manager.feature.invite;

import com.student_manager.feature.auth.User;
import com.student_manager.feature.teacher.Teacher;
import com.student_manager.feature.teacher.TeacherRepository;
import com.student_manager.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * {@link ProfileResolver} implementation for the Teacher profile type.
 */
@Component
@RequiredArgsConstructor
public class TeacherProfileResolver implements ProfileResolver {

    private final TeacherRepository teacherRepository;

    /**
     * {@inheritDoc}
     *
     * @return {@link ProfileType#TEACHER}
     */
    @Override
    public ProfileType supports() {
        return ProfileType.TEACHER;
    }

    /**
     * {@inheritDoc}
     *
     * @param targetId the id of the teacher to check
     * @return {@code true} if a teacher with this id exists
     */
    @Override
    public boolean profileExists(Long targetId) {
        return teacherRepository.existsById(targetId);
    }

    /**
     * {@inheritDoc}
     *
     * @param targetId the id of the teacher to link
     * @param user the account to attach to the teacher
     * @throws ResourceNotFoundException if no teacher exists with {@code targetId}
     */
    @Override
    public void linkAccount(Long targetId, User user) {
        Teacher teacher = teacherRepository.findById(targetId)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher", targetId));
        teacher.setAccount(user);
        teacherRepository.save(teacher);
    }
}
