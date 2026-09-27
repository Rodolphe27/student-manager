package com.student_manager.feature.course;

import java.util.List;

/**
 * Service contract for managing {@link Course} resources, covering lookup,
 * creation, update and deletion.
 */
public interface CourseService {

    /**
     * Retrieves a single course by its id.
     *
     * @param id the course id
     * @return the matching course as a DTO
     * @throws com.student_manager.shared.exception.ResourceNotFoundException if no course exists with the given id
     */
    CourseDTO findById(Long id);

    /**
     * Retrieves every course in the system.
     *
     * @return the list of all courses
     */
    List<CourseDTO> findAll();

    /**
     * Retrieves all courses with the given status.
     *
     * @param status the status to filter by
     * @return the matching courses
     */
    List<CourseDTO> findByStatus(CourseStatus status);

    /**
     * Creates a new course.
     *
     * @param request the data for the course to create
     * @return the newly created course
     * @throws com.student_manager.shared.exception.ValidationException if the course code is already in use
     */
    CourseDTO create(CreateCourseRequest request);

    /**
     * Updates an existing course.
     *
     * @param id the id of the course to update
     * @param request the replacement course data
     * @return the updated course
     * @throws com.student_manager.shared.exception.ResourceNotFoundException if no course exists with the given id
     * @throws com.student_manager.shared.exception.ValidationException if the new course code is already used by another course
     */
    CourseDTO update(Long id, CreateCourseRequest request);

    /**
     * Deletes a course.
     *
     * @param id the id of the course to delete
     * @throws com.student_manager.shared.exception.ResourceNotFoundException if no course exists with the given id
     */
    void delete(Long id);
}
