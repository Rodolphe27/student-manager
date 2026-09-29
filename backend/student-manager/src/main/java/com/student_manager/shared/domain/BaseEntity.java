package com.student_manager.shared.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Common JPA base for entities: a generated identity id, Spring Data auditing
 * (when and by whom a row was created / last changed), and an optimistic-lock
 * version. The audit fields are populated via {@link AuditingEntityListener}
 * (see {@code @EnableJpaAuditing} on {@code StudentManagerApplication} and
 * {@code AuditingConfig}, which supplies the current username).
 */
@Getter
@Setter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    // Username of the logged-in account; null for rows created before auditing
    // existed or by an anonymous request (self-registration).
    @CreatedBy
    @Column(updatable = false)
    private String createdBy;

    @LastModifiedBy
    private String updatedBy;

    // Optimistic locking: every update increments it, and an update based on a stale
    // version fails instead of silently overwriting someone else's change. Primitive
    // (so Spring Data still decides new-vs-existing by the id) with a DB default of 0
    // so ddl-auto=update can add the column to tables that already have rows.
    @Version
    @Column(nullable = false, columnDefinition = "bigint default 0")
    private long version;
}
