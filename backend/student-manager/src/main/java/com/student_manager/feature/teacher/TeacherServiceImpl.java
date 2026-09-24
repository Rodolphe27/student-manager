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
@Slf4j
@Service
@RequiredArgsConstructor
public class TeacherServiceImpl extends CrudServiceSupport<Teacher, TeacherDTO> implements TeacherService {

    private final TeacherRepository repository;

    @Override
    protected JpaRepository<Teacher, Long> repository() {
        return repository;
    }

    @Override
    protected String resourceName() {
        return "Teacher";
    }

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
