package com.student_manager.shared.service;

import com.student_manager.shared.exception.ResourceNotFoundException;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.stream.Collectors;

/**
 * The load-or-404 / list-all / delete-or-404 shape shared by every simple
 * roster feature's {@code *ServiceImpl} (see {@code StudentServiceImpl},
 * {@code TeacherServiceImpl}) — extracted because those two classes were
 * ~80% identical otherwise, differing only in entity/DTO type and the
 * resource name used in error messages.
 * <p>
 * {@code create()}/{@code update()} deliberately stay in each concrete
 * service: their uniqueness checks and field copying are genuinely different
 * per feature and don't belong in a shared base.
 * <p>
 * A subclass implements the three template methods by returning its own
 * already-injected fields, rather than this base taking constructor
 * parameters itself — {@code @RequiredArgsConstructor} on a Lombok subclass
 * doesn't reliably pick up a superclass's constructor parameters, so this
 * keeps each service's Lombok-generated constructor simple and correct.
 *
 * @param <E> the JPA entity type
 * @param <D> the DTO type returned to callers
 */
public abstract class CrudServiceSupport<E, D> {

    /**
     * The subclass's already-injected JPA repository, used for every template
     * operation in this class.
     *
     * @return the entity repository
     */
    protected abstract JpaRepository<E, Long> repository();

    /** Used only in {@link ResourceNotFoundException} messages, e.g. "Student". */
    protected abstract String resourceName();

    /**
     * Converts a persisted entity to the DTO type returned to callers.
     *
     * @param entity the entity to convert
     * @return the corresponding DTO
     */
    protected abstract D toDTO(E entity);

    /** Loads the entity or throws {@link ResourceNotFoundException} — for use by create()/update() too. */
    protected E loadOrThrow(Long id) {
        return repository().findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(resourceName(), id));
    }

    /**
     * Loads a single entity by id and converts it to its DTO.
     *
     * @param id the entity id to load
     * @return the corresponding DTO
     * @throws ResourceNotFoundException if no entity with that id exists
     */
    public D findById(Long id) {
        return toDTO(loadOrThrow(id));
    }

    /**
     * Loads every entity in the table and converts each to its DTO.
     *
     * @return a list of all entities as DTOs
     */
    // TODO(SEC-8) [MEDIUM]: unbounded — returns the entire table with no pagination. Switch
    // to Page<D> findAll(Pageable pageable) and thread page/size query params through the
    // controllers that call this (StudentController, TeacherController).
    public List<D> findAll() {
        return repository().findAll()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Deletes the entity with the given id.
     *
     * @param id the entity id to delete
     * @throws ResourceNotFoundException if no entity with that id exists
     */
    public void delete(Long id) {
        if (!repository().existsById(id)) {
            throw new ResourceNotFoundException(resourceName(), id);
        }
        repository().deleteById(id);
    }
}
