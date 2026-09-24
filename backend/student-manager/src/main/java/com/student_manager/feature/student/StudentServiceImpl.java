package com.student_manager.feature.student;

import com.student_manager.feature.auth.UserRepository;
import com.student_manager.shared.exception.ResourceNotFoundException;
import com.student_manager.shared.exception.ValidationException;
import com.student_manager.shared.service.CrudServiceSupport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Optional;

// findById/findAll/delete come from CrudServiceSupport — see that class for
// why create()/update() stay here. Generic "Fetching .../Updating .../
// Deleting ... with id: {}" lines that used to live in those three methods
// are gone with them; RequestLoggingFilter (shared/config) already logs
// method + path + status, and the id was always just the path variable.
@Slf4j
@Service
@RequiredArgsConstructor
public class StudentServiceImpl extends CrudServiceSupport<Student, StudentDTO> implements StudentService {

    private final StudentRepository repository;
    private final UserRepository userRepository;

    @Override
    protected JpaRepository<Student, Long> repository() {
        return repository;
    }

    @Override
    protected String resourceName() {
        return "Student";
    }

    @Override
    protected StudentDTO toDTO(Student student) {
        StudentDTO dto = new StudentDTO();
        dto.setId(student.getId());
        dto.setFirstName(student.getFirstName());
        dto.setLastName(student.getLastName());
        dto.setMatriculationNumber(student.getMatriculationNumber());
        dto.setBirthDate(student.getBirthDate());
        dto.setEmail(student.getEmail());
        dto.setFullName(student.getFullName());
        return dto;
    }

    @Override
    public StudentDTO findByAccountUsername(String username) {
        Objects.requireNonNull(username, "username must not be null");
        log.info("Fetching student linked to account: {}", username);
        Student student = resolveByAccount(username)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No student record is linked to account: " + username));
        return toDTO(student);
    }

    @Override
    public boolean accountOwnsStudent(String username, Long studentId) {
        if (username == null || studentId == null) {
            return false;
        }
        return resolveByAccount(username)
                .map(student -> studentId.equals(student.getId()))
                .orElse(false);
    }

    /**
     * Resolves the student linked to an account, preferring the {@code account}
     * FK set by a claimed {@code RegistrationInvite}. Falls back to matching the
     * account's e-mail against {@link Student#getEmail()} for students who
     * predate the invite flow and were never explicitly linked.
     */
    private Optional<Student> resolveByAccount(String username) {
        return repository.findByAccountUsername(username)
                .or(() -> userRepository.findByUsername(username)
                        .flatMap(account -> repository.findByEmail(account.getEmail())));
    }

    @Override
    public StudentDTO create(CreateStudentRequest request) {
        log.info("Creating student: {}", request.getEmail());

        if (repository.existsByEmail(request.getEmail())) {
            throw new ValidationException("Email already exists: " + request.getEmail());
        }
        if (repository.existsByMatriculationNumber(request.getMatriculationNumber())) {
            throw new ValidationException("Matriculation number already exists: " + request.getMatriculationNumber());
        }

        Student student = new Student();
        student.setFirstName(request.getFirstName());
        student.setLastName(request.getLastName());
        student.setMatriculationNumber(request.getMatriculationNumber());
        student.setBirthDate(request.getBirthDate());
        student.setEmail(request.getEmail());

        Student saved = repository.save(student);
        log.info("Student created with id: {}", saved.getId());
        return toDTO(saved);
    }

    @Override
    public StudentDTO update(Long id, CreateStudentRequest request) {
        Student student = loadOrThrow(id);

        if (repository.existsByEmailAndIdNot(request.getEmail(), id)) {
            throw new ValidationException("Email already exists: " + request.getEmail());
        }
        if (repository.existsByMatriculationNumberAndIdNot(request.getMatriculationNumber(), id)) {
            throw new ValidationException("Matriculation number already exists: " + request.getMatriculationNumber());
        }

        student.setFirstName(request.getFirstName());
        student.setLastName(request.getLastName());
        student.setMatriculationNumber(request.getMatriculationNumber());
        student.setBirthDate(request.getBirthDate());
        student.setEmail(request.getEmail());

        Student saved = repository.save(student);
        return toDTO(saved);
    }
}
