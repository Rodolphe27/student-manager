package com.student_manager.feature.student;

import com.student_manager.feature.auth.UserRepository;
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
public class StudentServiceImpl implements StudentService {

    private final StudentRepository repository;
    private final UserRepository userRepository;

    @Override
    public StudentDTO findById(Long id) {
        Objects.requireNonNull(id, "Student id must not be null");
        log.info("Fetching student with id: {}", id);
        Student student = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student", id));
        return toDTO(student);
    }

    @Override
    public StudentDTO findByAccountUsername(String username) {
        Objects.requireNonNull(username, "username must not be null");
        log.info("Fetching student linked to account: {}", username);
        String email = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("No account found: " + username))
                .getEmail();
        Student student = repository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No student record is linked to account: " + username));
        return toDTO(student);
    }

    @Override
    public List<StudentDTO> findAll() {
        log.info("Fetching all students");
        return repository.findAll()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
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
        Objects.requireNonNull(id, "Student id must not be null");
        log.info("Updating student with id: {}", id);
        Student student = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student", id));

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
        log.info("Student updated with id: {}", saved.getId());
        return toDTO(saved);
    }

    @Override
    public void delete(Long id) {
        Objects.requireNonNull(id, "Student id must not be null");
        log.info("Deleting student with id: {}", id);
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("Student", id);
        }
        repository.deleteById(id);
        log.info("Student deleted with id: {}", id);
    }

    private StudentDTO toDTO(Student student) {
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
}
