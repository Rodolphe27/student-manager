package com.student_manager.feature.course;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Spring Data repository providing CRUD and lookup operations for {@link Term} entities.
 */
public interface TermRepository extends JpaRepository<Term, Long> {

    /**
     * Finds a term by its unique name.
     *
     * @param name the term name to look up
     * @return the matching term, or empty if none exists
     */
    Optional<Term> findByName(String name);

    /**
     * Checks whether a term with the given name already exists.
     *
     * @param name the term name to check
     * @return {@code true} if a term with that name exists
     */
    boolean existsByName(String name);
}
