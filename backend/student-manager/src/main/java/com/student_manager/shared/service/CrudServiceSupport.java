package com.student_manager.shared.service;

import com.student_manager.shared.domain.BaseEntity;
import com.student_manager.shared.exception.ResourceNotFoundException;
import com.student_manager.shared.repository.BaseRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.transaction.annotation.Transactional;

/**
 * The load-or-404 / paged-search / delete-or-404 shape shared by every
 * feature's {@code *ServiceImpl} (see {@code StudentServiceImpl},
 * {@code TeacherServiceImpl}) — extracted because those classes were
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
public abstract class CrudServiceSupport<E extends BaseEntity, D> {

    /**
     * The subclass's already-injected repository, used for every template
     * operation in this class.
     *
     * @return the entity repository
     */
    protected abstract BaseRepository<E> repository();

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
     * Rejects an update that was based on an outdated copy of the entity. The
     * client sends back the {@code version} it loaded; if someone else saved
     * in the meantime the versions differ and the update fails with 409
     * instead of silently overwriting their change. A {@code null} expected
     * version (an older client) skips the check — the {@code @Version} column
     * still guards concurrent transactions.
     *
     * @param entity          the freshly loaded entity
     * @param expectedVersion the version the client based its edit on, or {@code null}
     * @throws ObjectOptimisticLockingFailureException if the versions differ
     */
    protected void checkVersion(E entity, Long expectedVersion) {
        if (expectedVersion != null && expectedVersion != entity.getVersion()) {
            throw new ObjectOptimisticLockingFailureException(entity.getClass(), entity.getId());
        }
    }

    /**
     * Loads a single entity by id and converts it to its DTO.
     *
     * @param id the entity id to load
     * @return the corresponding DTO
     * @throws ResourceNotFoundException if no entity with that id exists
     */
    @Transactional(readOnly = true)
    public D findById(Long id) {
        return toDTO(loadOrThrow(id));
    }

    /**
     * Returns one page of the entities matching {@code spec}, as DTOs. Paging,
     * sorting and the page-size cap come from the {@link Pageable} (see
     * {@code spring.data.web.pageable} in application.yml).
     *
     * @param spec     the search/filter criteria; {@code Specification.unrestricted()} for all rows
     * @param pageable the requested page, size and sort
     * @return the requested page of DTOs, with total counts
     */
    @Transactional(readOnly = true)
    public Page<D> search(Specification<E> spec, Pageable pageable) {
        return repository().findAll(spec, pageable).map(this::toDTO);
    }

    /**
     * Deletes the entity with the given id.
     *
     * @param id the entity id to delete
     * @throws ResourceNotFoundException if no entity with that id exists
     */
    @Transactional
    public void delete(Long id) {
        if (!repository().existsById(id)) {
            throw new ResourceNotFoundException(resourceName(), id);
        }
        repository().deleteById(id);
    }
}
