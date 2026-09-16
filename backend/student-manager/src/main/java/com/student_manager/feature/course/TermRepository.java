package com.student_manager.feature.course;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TermRepository extends JpaRepository<Term, Long> {
    Optional<Term> findByName(String name);
    boolean existsByName(String name);
}
