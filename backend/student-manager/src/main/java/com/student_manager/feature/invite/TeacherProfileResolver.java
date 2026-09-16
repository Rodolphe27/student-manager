package com.student_manager.feature.invite;

import com.student_manager.feature.auth.User;
import com.student_manager.feature.teacher.Teacher;
import com.student_manager.feature.teacher.TeacherRepository;
import com.student_manager.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TeacherProfileResolver implements ProfileResolver {

    private final TeacherRepository teacherRepository;

    @Override
    public ProfileType supports() {
        return ProfileType.TEACHER;
    }

    @Override
    public boolean profileExists(Long targetId) {
        return teacherRepository.existsById(targetId);
    }

    @Override
    public void linkAccount(Long targetId, User user) {
        Teacher teacher = teacherRepository.findById(targetId)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher", targetId));
        teacher.setAccount(user);
        teacherRepository.save(teacher);
    }
}
