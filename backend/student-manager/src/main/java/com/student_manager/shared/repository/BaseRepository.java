package com.student_manager.shared.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.NoRepositoryBean;

/**
 * Common base for the feature repositories: standard CRUD + paging from
 * {@link JpaRepository}, plus {@link JpaSpecificationExecutor} so list
 * endpoints can combine search/filter {@code Specification}s with a
 * {@code Pageable} (see {@code CrudServiceSupport#search}).
 *
 * @param <E> the entity type (always keyed by a {@code Long} id)
 */
@NoRepositoryBean
public interface BaseRepository<E> extends JpaRepository<E, Long>, JpaSpecificationExecutor<E> {
}
