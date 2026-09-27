package com.student_manager.feature.enrollment;

import com.student_manager.feature.course.Course;
import com.student_manager.feature.course.CourseRepository;
import com.student_manager.feature.student.Student;
import com.student_manager.feature.student.StudentRepository;
import com.student_manager.shared.exception.ResourceNotFoundException;
import com.student_manager.shared.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

// Generic "Fetching .../Confirming .../Cancelling ... with id: {}" lines below
// are commented out, not deleted — RequestLoggingFilter (shared/config)
// already logs method + path + status, and the id in those lines was always
// just the path variable. create()'s lines are kept: the student/course ids
// come from the request body, and the new enrollment id doesn't exist until
// after the save.
/**
 * Default {@link EnrollmentService} implementation backed by JPA
 * repositories for enrollments, students, and courses.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EnrollmentServiceImpl implements EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final StudentRepository studentRepository;
    private final CourseRepository courseRepository;

    /**
     * Looks up a single enrollment by id.
     *
     * @param id the enrollment id
     * @return the matching enrollment
     * @throws ResourceNotFoundException if no enrollment has that id
     */
    @Override
    public EnrollmentDTO findById(Long id) {
        // log.info("Fetching enrollment with id: {}", id);
        Enrollment enrollment = enrollmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment", id));
        return toDTO(enrollment);
    }

    /**
     * Lists every enrollment in the system.
     *
     * @return all enrollments
     */
    // TODO(SEC-8) [MEDIUM]: unbounded — returns every enrollment row, no pagination. Switch to
    // Page<EnrollmentDTO> findAll(Pageable pageable) and thread page/size params through EnrollmentController.
    @Override
    public List<EnrollmentDTO> findAll() {
        // log.info("Fetching all enrollments");
        return enrollmentRepository.findAll()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Lists all enrollments belonging to a given student.
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
     * Lists all enrollments (the roster) for a given course.
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
    public EnrollmentDTO create(CreateEnrollmentRequest request) {
        log.info("Enrolling student {} in course {}", request.getStudentId(), request.getCourseId());

        Student student = studentRepository.findById(request.getStudentId())
                .orElseThrow(() -> new ResourceNotFoundException("Student", request.getStudentId()));

        Course course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new ResourceNotFoundException("Course", request.getCourseId()));

        if (!course.isActive()) {
            throw new ValidationException("Course is not active: " + course.getCode());
        }

        if (enrollmentRepository.existsByStudentIdAndCourseId(
                request.getStudentId(), request.getCourseId())) {
            throw new ValidationException("Student already enrolled in this course");
        }

        Enrollment enrollment = new Enrollment();
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
     * Confirms a pending enrollment.
     *
     * @param id the enrollment id
     * @return the confirmed enrollment
     * @throws ResourceNotFoundException if no enrollment has that id
     * @throws ValidationException if the enrollment is already confirmed or has been cancelled
     */
    @Override
    public EnrollmentDTO confirm(Long id) {
        // log.info("Confirming enrollment with id: {}", id);
        Enrollment enrollment = enrollmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment", id));

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
    public EnrollmentDTO cancel(Long id) {
        // log.info("Cancelling enrollment with id: {}", id);
        Enrollment enrollment = enrollmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment", id));

        if (EnrollmentStatus.CANCELLED.equals(enrollment.getStatus())) {
            throw new ValidationException("Enrollment is already cancelled");
        }

        // A cancelled (withdrawn) enrollment does not carry an academic grade:
        // clear any letter grade so CANCELLED + A-F can never coexist (issue #33).
        enrollment.setGrade(Grade.NOT_GRADED);
        enrollment.setStatus(EnrollmentStatus.CANCELLED);
        return toDTO(enrollmentRepository.save(enrollment));
    }

    /**
     * Assigns a grade to a confirmed enrollment.
     *
     * @param id the enrollment id
     * @param request the grade to assign
     * @return the updated enrollment
     * @throws ResourceNotFoundException if no enrollment has that id
     * @throws ValidationException if the enrollment is not confirmed
     */
    @Override
    public EnrollmentDTO updateGrade(Long id, UpdateGradeRequest request) {
        // log.info("Updating grade for enrollment: {}", id);
        Enrollment enrollment = enrollmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment", id));

        if (!EnrollmentStatus.CONFIRMED.equals(enrollment.getStatus())) {
            throw new ValidationException("Can only assign grade to confirmed enrollments");
        }

        enrollment.setGrade(request.getGrade());
        return toDTO(enrollmentRepository.save(enrollment));
    }

    /**
     * Deletes an enrollment outright.
     *
     * @param id the enrollment id
     * @throws ResourceNotFoundException if no enrollment has that id
     */
    @Override
    public void delete(Long id) {
        // log.info("Deleting enrollment with id: {}", id);
        if (!enrollmentRepository.existsById(id)) {
            throw new ResourceNotFoundException("Enrollment", id);
        }
        enrollmentRepository.deleteById(id);
    }

    /**
     * Maps an {@link Enrollment} entity to its client-facing {@link EnrollmentDTO},
     * denormalizing the related student's name and course's title/code.
     *
     * @param enrollment the entity to map
     * @return the resulting DTO
     */
    private EnrollmentDTO toDTO(Enrollment enrollment) {
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
        return dto;
    }
}
