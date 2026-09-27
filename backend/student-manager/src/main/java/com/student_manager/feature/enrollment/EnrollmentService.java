package com.student_manager.feature.enrollment;

import java.util.List;

/**
 * Business operations for managing enrollments: lookup, creation, and
 * lifecycle transitions (confirm/cancel/grade).
 */
public interface EnrollmentService {

    /**
     * Looks up a single enrollment by id.
     *
     * @param id the enrollment id
     * @return the matching enrollment
     * @throws com.student_manager.shared.exception.ResourceNotFoundException if no enrollment has that id
     */
    EnrollmentDTO findById(Long id);

    /**
     * Lists every enrollment in the system.
     *
     * @return all enrollments
     */
    List<EnrollmentDTO> findAll();

    /**
     * Lists all enrollments belonging to a given student.
     *
     * @param studentId the student id
     * @return that student's enrollments
     */
    List<EnrollmentDTO> findByStudentId(Long studentId);

    /**
     * Lists all enrollments (the roster) for a given course.
     *
     * @param courseId the course id
     * @return that course's enrollments
     */
    List<EnrollmentDTO> findByCourseId(Long courseId);

    /**
     * Enrolls a student in a course.
     *
     * @param request the student/course pair to enroll
     * @return the newly created enrollment
     * @throws com.student_manager.shared.exception.ResourceNotFoundException if the student or course does not exist
     * @throws com.student_manager.shared.exception.ValidationException if the course is inactive or the student is already enrolled in it
     */
    EnrollmentDTO create(CreateEnrollmentRequest request);

    /**
     * Confirms a pending enrollment.
     *
     * @param id the enrollment id
     * @return the confirmed enrollment
     * @throws com.student_manager.shared.exception.ResourceNotFoundException if no enrollment has that id
     * @throws com.student_manager.shared.exception.ValidationException if the enrollment is already confirmed or has been cancelled
     */
    EnrollmentDTO confirm(Long id);

    /**
     * Cancels (withdraws) an enrollment, clearing any grade it carried.
     *
     * @param id the enrollment id
     * @return the cancelled enrollment
     * @throws com.student_manager.shared.exception.ResourceNotFoundException if no enrollment has that id
     * @throws com.student_manager.shared.exception.ValidationException if the enrollment is already cancelled
     */
    EnrollmentDTO cancel(Long id);

    /**
     * Assigns a grade to a confirmed enrollment.
     *
     * @param id the enrollment id
     * @param request the grade to assign
     * @return the updated enrollment
     * @throws com.student_manager.shared.exception.ResourceNotFoundException if no enrollment has that id
     * @throws com.student_manager.shared.exception.ValidationException if the enrollment is not confirmed
     */
    EnrollmentDTO updateGrade(Long id, UpdateGradeRequest request);

    /**
     * Deletes an enrollment outright.
     *
     * @param id the enrollment id
     * @throws com.student_manager.shared.exception.ResourceNotFoundException if no enrollment has that id
     */
    void delete(Long id);
}
