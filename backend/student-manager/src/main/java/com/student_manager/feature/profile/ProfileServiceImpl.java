package com.student_manager.feature.profile;

import com.student_manager.feature.auth.Role;
import com.student_manager.feature.auth.User;
import com.student_manager.feature.auth.UserRepository;
import com.student_manager.feature.student.Student;
import com.student_manager.feature.student.StudentRepository;
import com.student_manager.feature.teacher.Teacher;
import com.student_manager.feature.teacher.TeacherRepository;
import com.student_manager.shared.exception.InvalidCredentialsException;
import com.student_manager.shared.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Default {@link ProfileService}. A STUDENT/TEACHER account is tied to its
 * profile by the {@code account} FK, falling back to matching the account's
 * e-mail (accounts are created by an ADMIN and never explicitly linked); the
 * fallback match is persisted as the link so a later e-mail change cannot
 * orphan the profile.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProfileServiceImpl implements ProfileService {

    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public ProfileDTO get(String username) {
        User user = load(username);
        return toDTO(user, student(user), teacher(user));
    }

    @Override
    @Transactional
    public ProfileDTO update(String username, UpdateProfileRequest request) {
        User user = load(username);
        Optional<Student> student = student(user);
        Optional<Teacher> teacher = teacher(user);

        boolean usernameChanged = !user.getUsername().equals(request.getUsername());
        boolean emailChanged = !user.getEmail().equalsIgnoreCase(request.getEmail());
        if (usernameChanged && userRepository.existsByUsername(request.getUsername())) {
            throw new ValidationException("Username already exists: " + request.getUsername());
        }
        if (emailChanged && userRepository.existsByEmail(request.getEmail())) {
            throw new ValidationException("Email already exists: " + request.getEmail());
        }
        if ((student.isPresent() || teacher.isPresent())
                && (isBlank(request.getFirstName()) || isBlank(request.getLastName()))) {
            throw new ValidationException("First and last name are required");
        }

        if (student.isPresent()) {
            Student s = student.get();
            if (emailChanged && studentRepository.existsByEmailAndIdNot(request.getEmail(), s.getId())) {
                throw new ValidationException("Email already exists: " + request.getEmail());
            }
            s.setFirstName(request.getFirstName().trim());
            s.setLastName(request.getLastName().trim());
            s.setBirthDate(request.getBirthDate());
            s.setEmail(request.getEmail());
            s.setAccount(user);
            studentRepository.save(s);
        }
        if (teacher.isPresent()) {
            Teacher t = teacher.get();
            if (emailChanged && teacherRepository.existsByEmailAndIdNot(request.getEmail(), t.getId())) {
                throw new ValidationException("Email already exists: " + request.getEmail());
            }
            t.setFirstName(request.getFirstName().trim());
            t.setLastName(request.getLastName().trim());
            t.setDepartment(request.getDepartment());
            t.setEmail(request.getEmail());
            t.setAccount(user);
            teacherRepository.save(t);
        }

        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        if (student.isEmpty() && teacher.isEmpty()) {
            user.setFirstName(blankToNull(request.getFirstName()));
            user.setLastName(blankToNull(request.getLastName()));
        }
        userRepository.save(user);
        return toDTO(user, student, teacher);
    }

    @Override
    @Transactional
    public void changePassword(String username, ChangePasswordRequest request) {
        User user = load(username);
        // 400, not 401: the frontend treats a 401 as an expired session and logs out.
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new ValidationException("Current password is incorrect");
        }
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    private User load(String username) {
        return userRepository.findByUsername(username)
                .filter(User::isActive)
                .orElseThrow(InvalidCredentialsException::new);
    }

    private Optional<Student> student(User user) {
        if (user.getRole() != Role.STUDENT) {
            return Optional.empty();
        }
        return studentRepository.findByAccountUsername(user.getUsername())
                .or(() -> studentRepository.findByEmail(user.getEmail()));
    }

    private Optional<Teacher> teacher(User user) {
        if (user.getRole() != Role.TEACHER) {
            return Optional.empty();
        }
        return teacherRepository.findByAccountUsername(user.getUsername())
                .or(() -> teacherRepository.findByEmail(user.getEmail()));
    }

    private ProfileDTO toDTO(User user, Optional<Student> student, Optional<Teacher> teacher) {
        ProfileDTO dto = new ProfileDTO();
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setRole(user.getRole());
        if (student.isEmpty() && teacher.isEmpty()) {
            dto.setFirstName(user.getFirstName());
            dto.setLastName(user.getLastName());
        }
        student.ifPresent(s -> {
            dto.setFirstName(s.getFirstName());
            dto.setLastName(s.getLastName());
            dto.setMatriculationNumber(s.getMatriculationNumber());
            dto.setBirthDate(s.getBirthDate());
        });
        teacher.ifPresent(t -> {
            dto.setFirstName(t.getFirstName());
            dto.setLastName(t.getLastName());
            dto.setDepartment(t.getDepartment());
        });
        return dto;
    }

    private static String blankToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
