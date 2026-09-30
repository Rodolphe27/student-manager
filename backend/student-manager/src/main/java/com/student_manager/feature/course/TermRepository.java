package com.student_manager.feature.course;

import org.springframework.data.jpa.repository.JpaRepository;


/**
 * Spring Data repository providing CRUD and lookup operations for {@link Term} entities.
 */
public interface TermRepository extends JpaRepository<Term, Long> {

    /**
     * Checks whether a term with the given name already exists.
     *
     * @param name the term name to check
     * @return {@code true} if a term with that name exists
     */
    boolean existsByName(String name);
}
