package com.student_manager.feature.teacher;

import com.student_manager.shared.exception.ValidationException;
import com.student_manager.shared.service.CrudServiceSupport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

// findById/findAll/delete come from CrudServiceSupport — see that class for
// why create()/update() stay here. Generic "Fetching .../Updating .../
// Deleting ... with id: {}" lines that used to live in those three methods
// are gone with them; RequestLoggingFilter (shared/config) already logs
// method + path + status, and the id was always just the path variable.
/**
 * Default {@link TeacherService} implementation. Reuses the load-or-404 /
 * list-all / delete-or-404 behavior from {@link CrudServiceSupport} and adds
 * teacher-specific creation and update logic, including email uniqueness checks.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TeacherServiceImpl extends CrudServiceSupport<Teacher, TeacherDTO> implements TeacherService {

    private final TeacherRepository repository;

    /**
     * Supplies the underlying JPA repository for the shared CRUD template methods.
     *
     * @return the {@link TeacherRepository} used for persistence
     */
    @Override
    protected JpaRepository<Teacher, Long> repository() {
        return repository;
    }

    /**
     * Supplies the resource name used in {@code ResourceNotFoundException} messages.
     *
     * @return the literal "Teacher"
     */
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
     */
    @Override
    public TeacherDTO update(Long id, CreateTeacherRequest request) {
        Teacher teacher = loadOrThrow(id);

        if (repository.existsByEmailAndIdNot(request.getEmail(), id)) {
            throw new ValidationException("Email already exists: " + request.getEmail());
        }

        teacher.setFirstName(request.getFirstName());
        teacher.setLastName(request.getLastName());
        teacher.setEmail(request.getEmail());
        teacher.setDepartment(request.getDepartment());

        Teacher saved = repository.save(teacher);
        return toDTO(saved);
    }
}
