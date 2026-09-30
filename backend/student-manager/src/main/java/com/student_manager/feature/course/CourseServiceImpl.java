package com.student_manager.feature.course;

import com.student_manager.feature.teacher.TeacherRepository;
import com.student_manager.shared.exception.ResourceNotFoundException;
import com.student_manager.shared.exception.ValidationException;
import com.student_manager.shared.service.CrudServiceSupport;
import lombok.RequiredArgsConstructor;
import com.student_manager.shared.repository.BaseRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
 * Reads run in read-only transactions (class default); writes override it.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseServiceImpl extends CrudServiceSupport<Course, CourseDTO> implements CourseService {

    private final CourseRepository repository;
    private final TeacherRepository teacherRepository;
    private final TermRepository termRepository;

    /**
     * @return the repository backing the inherited CRUD operations
     */
    @Override
    protected BaseRepository<Course> repository() {
        return repository;
    }

    @Override
    public Page<CourseDTO> search(String query, CourseStatus status, Pageable pageable) {
        return search(CourseSpecifications.matching(query, status), pageable);
    }

    @Override
    public List<CourseOption> options(Long teacherId) {
        Sort byCode = Sort.by("code");
        return teacherId == null
                ? repository.findAllProjectedBy(byCode)
                : repository.findProjectedByTeacherId(teacherId, byCode);
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
        if (course.getTeacher() != null) {
            dto.setTeacherId(course.getTeacher().getId());
            dto.setTeacherName(course.getTeacher().getFullName());
        }
        if (course.getTerm() != null) {
            dto.setTermId(course.getTerm().getId());
            dto.setTermName(course.getTerm().getName());
        }
        dto.setVersion(course.getVersion());
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
    @Transactional
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

        assignTeacherAndTerm(course, request);
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
     * @throws org.springframework.orm.ObjectOptimisticLockingFailureException if the request's version is outdated
     */
    @Override
    @Transactional
    public CourseDTO update(Long id, CreateCourseRequest request) {
        Course course = loadOrThrow(id);
        checkVersion(course, request.getVersion());

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

        assignTeacherAndTerm(course, request);
        // Flush now so the returned DTO carries the incremented version.
        Course saved = repository.saveAndFlush(course);
        return toDTO(saved);
    }

    /** Resolves the optional teacher/term ids of a request; an unknown id is a 404. */
    private void assignTeacherAndTerm(Course course, CreateCourseRequest request) {
        course.setTeacher(request.getTeacherId() == null ? null
                : teacherRepository.findById(request.getTeacherId())
                        .orElseThrow(() -> new ResourceNotFoundException("Teacher", request.getTeacherId())));
        course.setTerm(request.getTermId() == null ? null
                : termRepository.findById(request.getTermId())
                        .orElseThrow(() -> new ResourceNotFoundException("Term", request.getTermId())));
    }
}
