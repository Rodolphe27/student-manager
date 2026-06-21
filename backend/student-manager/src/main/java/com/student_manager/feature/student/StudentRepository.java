package com.student_manager.feature.student;

import org.springframework.data.jpa.repository.JpaRepository;


import java.util.Optional;
public interface StudentRepository extends JpaRepository<Student, Long> {
    Optional<Student> findByEmail(String email);
    Optional<Student> findByMatriculationNumber(String matriculationNumber);
    boolean existsByEmail(String email);
    boolean existsByMatriculationNumber(String matriculationNumber);
}
