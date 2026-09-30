package com.student_manager.feature.student;

import com.student_manager.feature.auth.UserRepository;
import com.student_manager.shared.exception.ResourceNotFoundException;
import com.student_manager.feature.auth.AccountProvisioner;
import com.student_manager.feature.auth.Role;
import com.student_manager.shared.exception.ValidationException;
import com.student_manager.shared.service.CrudServiceSupport;
import lombok.RequiredArgsConstructor;
import com.student_manager.shared.repository.BaseRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

// findById/findAll/delete come from CrudServiceSupport — see that class for
// why create()/update() stay here. Generic "Fetching .../Updating .../
// Deleting ... with id: {}" lines that used to live in those three methods
// are gone with them; RequestLoggingFilter (shared/config) already logs
// method + path + status, and the id was always just the path variable.
/**
 * Default {@link StudentService} implementation. {@code findById}/{@code
 * findAll}/{@code delete} are inherited from {@link CrudServiceSupport};
 * this class adds the account-linkage lookups and the {@code create}/{@code
 * update} operations whose uniqueness checks are specific to students.
 * Reads run in read-only transactions (class default); writes override it.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudentServiceImpl extends CrudServiceSupport<Student, StudentDTO> implements StudentService {

    private final StudentRepository repository;
    private final UserRepository userRepository;
    private final AccountProvisioner accountProvisioner;

    /**
     * @return the repository backing the inherited CRUD operations
     */
    @Override
    protected BaseRepository<Student> repository() {
        return repository;
    }

    @Override
    public Page<StudentDTO> search(String query, Pageable pageable) {
        return search(StudentSpecifications.matching(query), pageable);
    }

    @Override
    public List<StudentOption> options() {
        return repository.findAllOptions();
    }

    /**
     * @return the resource name used in {@link ResourceNotFoundException} messages
     */
    @Override
    protected String resourceName() {
        return "Student";
    }

    /**
     * Maps a {@link Student} entity to its client-facing {@link StudentDTO}.
     *
     * @param student the entity to map
     * @return the resulting DTO
     */
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
        dto.setVersion(student.getVersion());
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

    /**
     * True when the account identified by {@code username} resolves to the
     * student row {@code studentId}. Never throws — a missing account or
     * student row simply yields {@code false}.
     *
     * @param username the account's username
     * @param studentId the student id to check ownership of
     * @return {@code true} if that account owns that student record
     */
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
     * FK set when an admin links an account. Falls back to matching the
     * account's e-mail against {@link Student#getEmail()} for students who
     * predate account linking and were never explicitly linked.
     */
    private Optional<Student> resolveByAccount(String username) {
        return repository.findByAccountUsername(username)
                .or(() -> userRepository.findByUsername(username)
                        .flatMap(account -> repository.findByEmail(account.getEmail())));
    }

    /**
     * Creates a new student profile.
     *
     * @param request the student's details
     * @return the created student
     * @throws ValidationException if the email or matriculation number is already in use
     */
    @Override
    @Transactional
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
        if (request.isCreateAccount()) {
            // Same transaction: a taken username/email rolls the new student back too.
            saved.setAccount(accountProvisioner.create(
                    request.getAccountUsername(), request.getEmail(), request.getAccountPassword(), Role.STUDENT));
        }
        log.info("Student created with id: {}", saved.getId());
        return toDTO(saved);
    }

    /**
     * Updates an existing student's details.
     *
     * @param id the student id
     * @param request the new details
     * @return the updated student
     * @throws com.student_manager.shared.exception.ResourceNotFoundException if no student has that id
     * @throws ValidationException if the email or matriculation number is already used by another student
     * @throws org.springframework.orm.ObjectOptimisticLockingFailureException if the request's version is outdated
     */
    @Override
    @Transactional
    public StudentDTO update(Long id, CreateStudentRequest request) {
        Student student = loadOrThrow(id);
        checkVersion(student, request.getVersion());

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

        // Flush now so the returned DTO carries the incremented version.
        Student saved = repository.saveAndFlush(student);
        return toDTO(saved);
    }
}
