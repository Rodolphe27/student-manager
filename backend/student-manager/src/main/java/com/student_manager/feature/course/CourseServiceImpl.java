package com.student_manager.feature.course;

import com.student_manager.shared.exception.ResourceNotFoundException;
import com.student_manager.shared.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

// Generic "Fetching .../Updating .../Deleting ... with id: {}" lines below are
// commented out, not deleted — RequestLoggingFilter (shared/config) already
// logs method + path + status, and the id in those lines was always just the
// path variable. Lines carrying request-body data (code) or a newly assigned
// id, which the filter can't see, are kept.
/**
 * Default {@link CourseService} implementation backed by {@link CourseRepository}.
 * Handles entity/DTO mapping and enforces course-code uniqueness on create and update.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CourseServiceImpl implements CourseService {

    private final CourseRepository repository;

    /**
     * {@inheritDoc}
     *
     * @param id the course id
     * @return the matching course as a DTO
     * @throws ResourceNotFoundException if no course exists with the given id
     */
    @Override
    public CourseDTO findById(Long id) {
        // log.info("Fetching course with id: {}", id);
        Course course = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course", id));
        return toDTO(course);
    }

    /**
     * {@inheritDoc}
     *
     * @return the list of all courses
     */
    // TODO(SEC-8) [MEDIUM]: unbounded — returns every course row, no pagination. Switch to
    // Page<CourseDTO> findAll(Pageable pageable) and thread page/size params through CourseController.
    @Override
    public List<CourseDTO> findAll() {
        // log.info("Fetching all courses");
        return repository.findAll()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     *
     * @param status the status to filter by
     * @return the matching courses
     */
    @Override
    public List<CourseDTO> findByStatus(CourseStatus status) {
        // log.info("Fetching courses with status: {}", status);
        return repository.findByStatus(status)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     *
     * @param request the data for the course to create
     * @return the newly created course
     * @throws ValidationException if the course code is already in use
     */
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

    /**
     * {@inheritDoc}
     *
     * @param id the id of the course to update
     * @param request the replacement course data
     * @return the updated course
     * @throws ResourceNotFoundException if no course exists with the given id
     * @throws ValidationException if the new course code is already used by another course
     */
    @Override
    public CourseDTO update(Long id, CreateCourseRequest request) {
        // log.info("Updating course with id: {}", id);
        Course course = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course", id));

        if (repository.existsByCodeAndIdNot(request.getCode(), id)) {
            throw new ValidationException("Course code already exists: " + request.getCode());
        }

        course.setCode(request.getCode());
        course.setTitle(request.getTitle());
        course.setDescription(request.getDescription());
        course.setCreditHours(request.getCreditHours());
        if (request.getStatus() != null) {
            course.setStatus(request.getStatus());
        }

        Course saved = repository.save(course);
        // log.info("Course updated with id: {}", saved.getId());
        return toDTO(saved);
    }

    /**
     * {@inheritDoc}
     *
     * @param id the id of the course to delete
     * @throws ResourceNotFoundException if no course exists with the given id
     */
    @Override
    public void delete(Long id) {
        // log.info("Deleting course with id: {}", id);
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("Course", id);
        }
        repository.deleteById(id);
        // log.info("Course deleted with id: {}", id);
    }

    /**
     * Converts a {@link Course} entity into its {@link CourseDTO} representation.
     *
     * @param course the entity to convert
     * @return the corresponding DTO
     */
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
