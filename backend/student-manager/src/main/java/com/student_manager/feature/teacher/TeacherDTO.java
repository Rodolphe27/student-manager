package com.student_manager.feature.teacher;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data transfer object exposing a {@link Teacher} to API clients, decoupling
 * the wire representation from the JPA entity.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TeacherDTO {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String department;
    private String fullName;
    // Optimistic-lock version; send it back on update so a stale edit is rejected (409).
    private long version;
}
