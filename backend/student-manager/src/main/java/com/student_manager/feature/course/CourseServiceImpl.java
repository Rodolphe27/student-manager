package com.student_manager.feature.course;
import com.student_manager.shared.exception.ResourceNotFoundException;
import com.student_manager.shared.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CourseServiceImpl implements CourseService {

    private final CourseRepository repository;

    @Override
    public CourseDTO findById(Long id) {
        log.info("Fetching course with id: {}", id);
        Course course = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course", id));
        return toDTO(course);
    }

    @Override
    public List<CourseDTO> findAll() {
        log.info("Fetching all courses");
        return repository.findAll()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<CourseDTO> findByStatus(CourseStatus status) {
        log.info("Fetching courses with status: {}", status);
        return repository.findByStatus(status)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public CourseDTO create(CreateCourseRequest request) {
        log.info("Creating course: {}", request.getCode());

        if (repository.existsByCode(request.getCode())) {
            throw new ValidationException("Course code already exists: " + request.getCode());
        }

        Course course = new Course();
        course.setCode(request.getCode());
        course.setTitle(request.getTitle());
        course.setDescription(request.getDescription());
        course.setCreditHours(request.getCreditHours());
        course.setStatus(request.getStatus() != null ? request.getStatus() : CourseStatus.ACTIVE);

        Course saved = repository.save(course);
        log.info("Course created with id: {}", saved.getId());
        return toDTO(saved);
    }

    @Override
    public CourseDTO update(Long id, CreateCourseRequest request) {
        log.info("Updating course with id: {}", id);
        Course course = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course", id));

        course.setCode(request.getCode());
        course.setTitle(request.getTitle());
        course.setDescription(request.getDescription());
        course.setCreditHours(request.getCreditHours());
        if (request.getStatus() != null) {
            course.setStatus(request.getStatus());
        }

        Course saved = repository.save(course);
        log.info("Course updated with id: {}", saved.getId());
        return toDTO(saved);
    }

    @Override
    public void delete(Long id) {
        log.info("Deleting course with id: {}", id);
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("Course", id);
        }
        repository.deleteById(id);
        log.info("Course deleted with id: {}", id);
    }

    private CourseDTO toDTO(Course course) {
        CourseDTO dto = new CourseDTO();
        dto.setId(course.getId());
        dto.setCode(course.getCode());
        dto.setTitle(course.getTitle());
        dto.setDescription(course.getDescription());
        dto.setCreditHours(course.getCreditHours());
        dto.setStatus(course.getStatus());
        dto.setActive(course.isActive());
        return dto;
    }
}
