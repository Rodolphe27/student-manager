package com.student_manager;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Entry point for the Student Manager Spring Boot application. Bootstraps the
 * application context and enables JPA auditing so {@code @CreatedDate}/
 * {@code @LastModifiedDate} fields on entities (see {@code BaseEntity}) are
 * populated automatically.
 */
@SpringBootApplication
@EnableJpaAuditing
public class StudentManagerApplication {

    /**
     * Starts the Spring Boot application.
     *
     * @param args command-line arguments passed through to Spring Boot
     */
    public static void main(String[] args) {
        SpringApplication.run(StudentManagerApplication.class, args);
    }
}
