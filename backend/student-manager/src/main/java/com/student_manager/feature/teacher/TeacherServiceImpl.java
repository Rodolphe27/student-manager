package com.student_manager.feature.teacher;

import com.student_manager.shared.exception.ResourceNotFoundException;
import com.student_manager.shared.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TeacherServiceImpl implements TeacherService {

    private final TeacherRepository repository;

    @Override
    public TeacherDTO findById(Long id) {
        Objects.requireNonNull(id, "Teacher id must not be null");
        log.info("Fetching teacher with id: {}", id);
        Teacher teacher = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher", id));
        return toDTO(teacher);
    }

    @Override
    public List<TeacherDTO> findAll() {
        log.info("Fetching all teachers");
        return repository.findAll()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
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
        Objects.requireNonNull(id, "Teacher id must not be null");
        log.info("Updating teacher with id: {}", id);
        Teacher teacher = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher", id));

        if (repository.existsByEmailAndIdNot(request.getEmail(), id)) {
            throw new ValidationException("Email already exists: " + request.getEmail());
        }

        teacher.setFirstName(request.getFirstName());
        teacher.setLastName(request.getLastName());
        teacher.setEmail(request.getEmail());
        teacher.setDepartment(request.getDepartment());

        Teacher saved = repository.save(teacher);
        log.info("Teacher updated with id: {}", saved.getId());
        return toDTO(saved);
    }

    @Override
    public void delete(Long id) {
        Objects.requireNonNull(id, "Teacher id must not be null");
        log.info("Deleting teacher with id: {}", id);
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("Teacher", id);
        }
        repository.deleteById(id);
        log.info("Teacher deleted with id: {}", id);
    }

    private TeacherDTO toDTO(Teacher teacher) {
        TeacherDTO dto = new TeacherDTO();
        dto.setId(teacher.getId());
        dto.setFirstName(teacher.getFirstName());
        dto.setLastName(teacher.getLastName());
        dto.setEmail(teacher.getEmail());
        dto.setDepartment(teacher.getDepartment());
        dto.setFullName(teacher.getFullName());
        return dto;
    }
}
