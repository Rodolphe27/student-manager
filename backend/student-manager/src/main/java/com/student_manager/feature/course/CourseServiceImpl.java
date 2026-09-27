package com.student_manager.feature.course;

import com.student_manager.shared.exception.ResourceNotFoundException;
import com.student_manager.shared.exception.ValidationException;
import com.student_manager.shared.service.CrudServiceSupport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

// findById/findAll/delete come from CrudServiceSupport — see that class for why
// create()/update() stay here. Generic "Fetching .../Updating .../Deleting ...
// with id: {}" lines that used to live in those methods are gone with them;
// RequestLoggingFilter (shared/config) already logs method + path + status, and
// the id was always just the path variable. Lines carrying request-body data
// (code) or a newly assigned id, which the filter can't see, are kept.
/**
 * Default {@link CourseService} implementation backed by {@link CourseRepository}.
 * {@code findById}/{@code findAll}/{@code delete} are inherited from {@link
 * CrudServiceSupport}; this class adds status filtering and the {@code create}/
 * {@code update} operations whose code-uniqueness checks are specific to courses.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CourseServiceImpl extends CrudServiceSupport<Course, CourseDTO> implements CourseService {

    private final CourseRepository repository;

    /**
     * @return the JPA repository backing the inherited CRUD operations
     */
    @Override
    protected JpaRepository<Course, Long> repository() {
        return repository;
    }

    /**
     * @return the resource name used in {@link ResourceNotFoundException} messages
     */
    @Override
    protected String resourceName() {
        return "Course";
    }

    /**
     * Converts a {@link Course} entity into its {@link CourseDTO} representation.
     *
     * @param course the entity to convert
     * @return the corresponding DTO
     */
    @Override
    protected CourseDTO toDTO(Course course) {
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
        Course course = loadOrThrow(id);

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
        return toDTO(saved);
    }
}
