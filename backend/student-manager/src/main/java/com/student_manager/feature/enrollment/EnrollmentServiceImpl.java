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

@Slf4j
@Service
@RequiredArgsConstructor
public class EnrollmentServiceImpl implements EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final StudentRepository studentRepository;
    private final CourseRepository courseRepository;

    @Override
    public EnrollmentDTO findById(Long id) {
        log.info("Fetching enrollment with id: {}", id);
        Enrollment enrollment = enrollmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment", id));
        return toDTO(enrollment);
    }

    @Override
    public List<EnrollmentDTO> findAll() {
        log.info("Fetching all enrollments");
        return enrollmentRepository.findAll()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<EnrollmentDTO> findByStudentId(Long studentId) {
        log.info("Fetching enrollments for student: {}", studentId);
        return enrollmentRepository.findByStudentId(studentId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<EnrollmentDTO> findByCourseId(Long courseId) {
        log.info("Fetching enrollments for course: {}", courseId);
        return enrollmentRepository.findByCourseId(courseId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

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

    @Override
    public EnrollmentDTO confirm(Long id) {
        log.info("Confirming enrollment with id: {}", id);
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

    @Override
    public EnrollmentDTO cancel(Long id) {
        log.info("Cancelling enrollment with id: {}", id);
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

    @Override
    public EnrollmentDTO updateGrade(Long id, UpdateGradeRequest request) {
        log.info("Updating grade for enrollment: {}", id);
        Enrollment enrollment = enrollmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment", id));

        if (!EnrollmentStatus.CONFIRMED.equals(enrollment.getStatus())) {
            throw new ValidationException("Can only assign grade to confirmed enrollments");
        }

        enrollment.setGrade(request.getGrade());
        return toDTO(enrollmentRepository.save(enrollment));
    }

    @Override
    public void delete(Long id) {
        log.info("Deleting enrollment with id: {}", id);
        if (!enrollmentRepository.existsById(id)) {
            throw new ResourceNotFoundException("Enrollment", id);
        }
        enrollmentRepository.deleteById(id);
    }

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