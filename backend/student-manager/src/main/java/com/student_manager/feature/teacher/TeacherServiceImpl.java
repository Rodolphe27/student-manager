package com.student_manager.feature.teacher;

import com.student_manager.feature.auth.AccountProvisioner;
import com.student_manager.feature.auth.Role;
import com.student_manager.feature.auth.User;
import com.student_manager.feature.auth.UserRepository;
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
import java.util.Optional;

// findById/findAll/delete come from CrudServiceSupport — see that class for
// why create()/update() stay here. Generic "Fetching .../Updating .../
// Deleting ... with id: {}" lines that used to live in those three methods
// are gone with them; RequestLoggingFilter (shared/config) already logs
// method + path + status, and the id was always just the path variable.
/**
 * Default {@link TeacherService} implementation. Reuses the load-or-404 /
 * list-all / delete-or-404 behavior from {@link CrudServiceSupport} and adds
 * teacher-specific creation and update logic, including email uniqueness checks.
 * Reads run in read-only transactions (class default); writes override it.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TeacherServiceImpl extends CrudServiceSupport<Teacher, TeacherDTO> implements TeacherService {

    private final TeacherRepository repository;
    private final AccountProvisioner accountProvisioner;
    private final UserRepository userRepository;

    /**
     * Supplies the underlying repository for the shared CRUD template methods.
     *
     * @return the {@link TeacherRepository} used for persistence
     */
    @Override
    protected BaseRepository<Teacher> repository() {
        return repository;
    }

    @Override
    public Page<TeacherDTO> search(String query, Pageable pageable) {
        return search(TeacherSpecifications.matching(query), pageable);
    }

    /**
     * Supplies the resource name used in {@code ResourceNotFoundException} messages.
     *
     * @return the literal "Teacher"
     */
    @Override
    public List<TeacherOption> options() {
        return repository.findAllOptions();
    }

    @Override
    public Optional<Long> findIdByAccountUsername(String username) {
        if (username == null) {
            return Optional.empty();
        }
        return repository.findByAccountUsername(username)
                .or(() -> userRepository.findByUsername(username)
                        .flatMap(account -> repository.findByEmail(account.getEmail())))
                .map(Teacher::getId);
    }

    @Override
    protected String resourceName() {
        return "Teacher";
    }

    /**
     * Converts a {@link Teacher} entity into its {@link TeacherDTO} representation.
     *
     * @param teacher the entity to convert
     * @return the corresponding DTO
     */
    @Override
    protected TeacherDTO toDTO(Teacher teacher) {
        TeacherDTO dto = new TeacherDTO();
        dto.setId(teacher.getId());
        dto.setFirstName(teacher.getFirstName());
        dto.setLastName(teacher.getLastName());
        dto.setEmail(teacher.getEmail());
        dto.setDepartment(teacher.getDepartment());
        dto.setFullName(teacher.getFullName());
        dto.setVersion(teacher.getVersion());
        return dto;
    }

    /**
     * {@inheritDoc}
     *
     * @param request the data for the teacher to create
     * @return the newly created teacher
     * @throws ValidationException if the email is already in use
     */
    @Override
    @Transactional
    public TeacherDTO create(CreateTeacherRequest request) {
        log.info("Creating teacher: {}", request.getEmail());

        if (repository.existsByEmail(request.getEmail())) {
            throw new ValidationException("Email already exists: " + request.getEmail());
        }

        Teacher teacher = new Teacher();
        teacher.setFirstName(request.getFirstName());
        teacher.setLastName(request.getLastName());
        teacher.setEmail(request.getEmail());
        teacher.setDepartment(request.getDepartment());

        Teacher saved = repository.save(teacher);
        if (request.isCreateAccount()) {
            // Same transaction: a taken username/email rolls the new teacher back too.
            saved.setAccount(accountProvisioner.create(
                    request.getAccountUsername(), request.getEmail(), request.getAccountPassword(), Role.TEACHER));
        }
        log.info("Teacher created with id: {}", saved.getId());
        return toDTO(saved);
    }

    /**
     * {@inheritDoc}
     *
     * @param id the id of the teacher to update
     * @param request the replacement teacher data
     * @return the updated teacher
     * @throws com.student_manager.shared.exception.ResourceNotFoundException if no teacher exists with the given id
     * @throws ValidationException if the new email is already used by another teacher
     * @throws org.springframework.orm.ObjectOptimisticLockingFailureException if the request's version is outdated
     */
    @Override
    @Transactional
    public TeacherDTO update(Long id, CreateTeacherRequest request) {
        Teacher teacher = loadOrThrow(id);
        checkVersion(teacher, request.getVersion());

        if (repository.existsByEmailAndIdNot(request.getEmail(), id)) {
            throw new ValidationException("Email already exists: " + request.getEmail());
        }

        syncAccountEmail(teacher, request.getEmail());
        teacher.setFirstName(request.getFirstName());
        teacher.setLastName(request.getLastName());
        teacher.setEmail(request.getEmail());
        teacher.setDepartment(request.getDepartment());

        // Flush now so the returned DTO carries the incremented version.
        Teacher saved = repository.saveAndFlush(teacher);
        return toDTO(saved);
    }

    /**
     * Deletes the teacher profile and the login account that belongs to it, so no
     * orphaned account is left behind. A teacher who still runs courses cannot be
     * deleted (the database rejects it; surfaced as 409).
     */
    @Override
    @Transactional
    public void delete(Long id) {
        Teacher teacher = loadOrThrow(id);
        User account = teacher.getAccount();
        repository.delete(teacher);
        repository.flush();
        if (account != null) {
            userRepository.delete(account);
        }
    }

    /** Keeps the linked account's e-mail in step with the profile's when an admin changes it. */
    private void syncAccountEmail(Teacher teacher, String newEmail) {
        User account = teacher.getAccount();
        if (account == null || account.getEmail().equalsIgnoreCase(newEmail)) {
            return;
        }
        if (userRepository.existsByEmail(newEmail)) {
            throw new ValidationException("Email already exists: " + newEmail);
        }
        account.setEmail(newEmail);
        userRepository.save(account);
    }
}
