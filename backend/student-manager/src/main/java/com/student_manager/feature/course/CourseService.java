package com.student_manager.feature.course;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

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
     * Returns one page of courses, optionally filtered by a search text
     * (code or title) and/or a status.
     *
     * @param query    the search text, or {@code null}/blank for no text filter
     * @param status   the status to keep, or {@code null} for every status
     * @param pageable the requested page, size and sort
     * @return the requested page of courses
     */
    Page<CourseDTO> search(String query, CourseStatus status, Pageable pageable);

    /**
     * Lists every course as a lightweight {@link CourseOption}, for selection lists.
     *
     * @return all courses, ordered by code
     */
    List<CourseOption> options();

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
