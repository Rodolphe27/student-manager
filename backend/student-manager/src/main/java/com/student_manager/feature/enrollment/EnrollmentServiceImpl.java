package com.student_manager.feature.enrollment;

import com.student_manager.feature.course.Course;
import com.student_manager.feature.course.CourseRepository;
import com.student_manager.feature.student.Student;
import com.student_manager.feature.student.StudentRepository;
import com.student_manager.shared.exception.ResourceNotFoundException;
import com.student_manager.shared.exception.ValidationException;
import com.student_manager.shared.service.CrudServiceSupport;
import lombok.RequiredArgsConstructor;
import com.student_manager.shared.repository.BaseRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

// findById/findAll/delete come from CrudServiceSupport — see that class for why
// the lifecycle transitions (create/confirm/cancel/updateGrade) stay here.
// Generic "Fetching .../Confirming .../Cancelling ... with id: {}" lines that
// used to live in the removed methods are gone with them; RequestLoggingFilter
// (shared/config) already logs method + path + status, and the id was always
// just the path variable. create()'s lines are kept: the student/course ids
// come from the request body, and the new enrollment id doesn't exist until
// after the save.
/**
 * Default {@link EnrollmentService} implementation backed by JPA repositories
 * for enrollments, students, and courses. {@code findById}/{@code findAll}/
 * {@code delete} are inherited from {@link CrudServiceSupport}; this class adds
 * the student/course-scoped lookups and the enrollment lifecycle transitions.
 * Reads run in read-only transactions (class default); writes override it.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EnrollmentServiceImpl extends CrudServiceSupport<Enrollment, EnrollmentDTO> implements EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final StudentRepository studentRepository;
    private final CourseRepository courseRepository;

    /**
     * @return the repository backing the inherited CRUD operations
     */
    @Override
    protected BaseRepository<Enrollment> repository() {
        return enrollmentRepository;
    }

    @Override
    public Page<EnrollmentDTO> search(EnrollmentStatus status, Long studentId, Long courseId, Long teacherId, Pageable pageable) {
        return search(EnrollmentSpecifications.filter(status, studentId, courseId, teacherId), pageable);
    }

    /**
     * @return the resource name used in {@link ResourceNotFoundException} messages
     */
    @Override
    protected String resourceName() {
        return "Enrollment";
    }

    /**
     * Maps an {@link Enrollment} entity to its client-facing {@link EnrollmentDTO},
     * denormalizing the related student's name and course's title/code.
     *
     * @param enrollment the entity to map
     * @return the resulting DTO
     */
    @Override
    protected EnrollmentDTO toDTO(Enrollment enrollment) {
        EnrollmentDTO dto = new EnrollmentDTO();
        dto.setId(enrollment.getId());
        dto.setStudentId(enrollment.getStudent().getId());
        dto.setStudentName(enrollment.getStudent().getFullName());
        dto.setCourseId(enrollment.getCourse().getId());
        dto.setCourseTitle(enrollment.getCourse().getTitle());
        dto.setCourseCode(enrollment.getCourse().getCode());
        dto.setEnrolledAt(enrollment.getEnrolledAt());
        dto.setStatus(enrollment.getStatus());
        dto.setGrade(enrollment.getGrade());
        dto.setConfirmed(enrollment.isConfirmed());
        dto.setGradeSeen(enrollment.isGradeSeen());
        return dto;
    }

    /**
     * {@inheritDoc}
     *
     * @param studentId the student id
     * @return that student's enrollments
     */
    @Override
    public List<EnrollmentDTO> findByStudentId(Long studentId) {
        // log.info("Fetching enrollments for student: {}", studentId);
        return enrollmentRepository.findByStudentId(studentId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     *
     * @param courseId the course id
     * @return that course's enrollments
     */
    @Override
    public List<EnrollmentDTO> findByCourseId(Long courseId) {
        // log.info("Fetching enrollments for course: {}", courseId);
        return enrollmentRepository.findByCourseId(courseId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Enrolls a student in a course. The student and course must exist, the
     * course must be active, and the student must not already have an
     * enrollment row for it (see {@link Enrollment}'s unique constraint).
     *
     * @param request the student/course pair to enroll
     * @return the newly created enrollment
     * @throws ResourceNotFoundException if the student or course does not exist
     * @throws ValidationException if the course is inactive or the student is already enrolled in it
     */
    @Override
    @Transactional
    public EnrollmentDTO create(CreateEnrollmentRequest request) {
        log.info("Enrolling student {} in course {}", request.getStudentId(), request.getCourseId());

        Student student = studentRepository.findById(request.getStudentId())
                .orElseThrow(() -> new ResourceNotFoundException("Student", request.getStudentId()));

        Course course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new ResourceNotFoundException("Course", request.getCourseId()));

        if (!course.isActive()) {
            throw new ValidationException("Course is not active: " + course.getCode());
        }

        Optional<Enrollment> previous = enrollmentRepository.findByStudentIdAndCourseId(
                request.getStudentId(), request.getCourseId());
        if (previous.isPresent() && !EnrollmentStatus.CANCELLED.equals(previous.get().getStatus())) {
            throw new ValidationException("Student already enrolled in this course");
        }

        // A student/course pair is unique, so enrolling again after a cancellation reopens the
        // cancelled enrollment as a fresh PENDING request instead of being blocked for good.
        Enrollment enrollment = previous.orElseGet(Enrollment::new);
        enrollment.setStudent(student);
        enrollment.setCourse(course);
        enrollment.setEnrolledAt(LocalDate.now());
        enrollment.setStatus(EnrollmentStatus.PENDING);
        enrollment.setGrade(Grade.NOT_GRADED);

        Enrollment saved = enrollmentRepository.save(enrollment);
        log.info("Enrollment created with id: {}", saved.getId());
        return toDTO(saved);
    }

    /**
     * {@inheritDoc}
     *
     * @param id the enrollment id
     * @return the confirmed enrollment
     * @throws ResourceNotFoundException if no enrollment has that id
     * @throws ValidationException if the enrollment is already confirmed or has been cancelled
     */
    @Override
    @Transactional
    public EnrollmentDTO confirm(Long id) {
        Enrollment enrollment = loadOrThrow(id);

        if (EnrollmentStatus.CANCELLED.equals(enrollment.getStatus())) {
            throw new ValidationException("Cannot confirm a cancelled enrollment");
        }
        if (EnrollmentStatus.CONFIRMED.equals(enrollment.getStatus())) {
            throw new ValidationException("Enrollment is already confirmed");
        }

        enrollment.setStatus(EnrollmentStatus.CONFIRMED);
        return toDTO(enrollmentRepository.save(enrollment));
    }

    /**
     * Cancels (withdraws) an enrollment. Since a cancelled enrollment cannot
     * carry a grade (see the note below and issue #33), any existing grade is
     * cleared back to {@link Grade#NOT_GRADED} as part of the transition.
     *
     * @param id the enrollment id
     * @return the cancelled enrollment
     * @throws ResourceNotFoundException if no enrollment has that id
     * @throws ValidationException if the enrollment is already cancelled
     */
    @Override
    @Transactional
    public EnrollmentDTO cancel(Long id) {
        Enrollment enrollment = loadOrThrow(id);

        if (EnrollmentStatus.CANCELLED.equals(enrollment.getStatus())) {
            throw new ValidationException("Enrollment is already cancelled");
        }

        // A cancelled (withdrawn) enrollment does not carry an academic grade:
        // clear any letter grade so CANCELLED + A-F can never coexist (issue #33).
        enrollment.setGrade(Grade.NOT_GRADED);
        enrollment.setGradeSeen(true);
        enrollment.setStatus(EnrollmentStatus.CANCELLED);
        return toDTO(enrollmentRepository.save(enrollment));
    }

    /**
     * {@inheritDoc}
     *
     * @param id the enrollment id
     * @param request the grade to assign
     * @return the updated enrollment
     * @throws ResourceNotFoundException if no enrollment has that id
     * @throws ValidationException if the enrollment is not confirmed
     */
    @Override
    @Transactional
    public EnrollmentDTO updateGrade(Long id, UpdateGradeRequest request) {
        Enrollment enrollment = loadOrThrow(id);

        if (!EnrollmentStatus.CONFIRMED.equals(enrollment.getStatus())) {
            throw new ValidationException("Can only assign grade to confirmed enrollments");
        }

        // Only a real change is news for the student: saving the same grade again stays quiet,
        // and taking a grade back (NOT_GRADED) leaves nothing to acknowledge.
        if (request.getGrade() != enrollment.getGrade()) {
            enrollment.setGradeSeen(request.getGrade() == Grade.NOT_GRADED);
        }
        enrollment.setGrade(request.getGrade());
        return toDTO(enrollmentRepository.save(enrollment));
    }

    @Override
    @Transactional
    public EnrollmentDTO markGradeSeen(Long id) {
        Enrollment enrollment = loadOrThrow(id);
        enrollment.setGradeSeen(true);
        return toDTO(enrollmentRepository.save(enrollment));
    }
}
