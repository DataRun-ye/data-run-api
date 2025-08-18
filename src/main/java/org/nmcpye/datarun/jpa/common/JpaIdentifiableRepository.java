package org.nmcpye.datarun.jpa.common;

import jakarta.validation.constraints.Size;
import org.nmcpye.datarun.common.IdentifiableObjectRepository;
import org.nmcpye.datarun.common.exceptions.IllegalQueryException;
import org.nmcpye.datarun.common.feedback.ErrorCode;
import org.nmcpye.datarun.common.feedback.ErrorMessage;
import org.nmcpye.datarun.common.repository.CreateAccessDeniedException;
import org.nmcpye.datarun.common.repository.DeleteAccessDeniedException;
import org.nmcpye.datarun.common.repository.UpdateAccessDeniedException;
import org.nmcpye.datarun.security.CurrentUserDetails;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.ListPagingAndSortingRepository;
import org.springframework.data.repository.NoRepositoryBean;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@NoRepositoryBean
public interface JpaIdentifiableRepository<T extends JpaIdentifiableObject>
    extends BaseJpaIdentifiableRepository<T, String>,
    JpaSpecificationExecutor<T>,
    ListPagingAndSortingRepository<T, String>,
    IdentifiableObjectRepository<T, String> {

    /// custom ////////////////////
    Stream<T> streamByLastModifiedDateAfter(Instant lastModifiedDateAfter);

    /**
     * Saves the given object instance.
     *
     * @param object the object instance.
     * @param user   the user currently in the security context.
     */
    default T save(T object, CurrentUserDetails user) {
        if (canSave(object, user)) {
            return persist(object);
        } else {
            throw new CreateAccessDeniedException("You don't have the proper permissions to create: \n" + object.getUid());
        }
    }

    /**
     * Updates the given object instance.
     *
     * @param object the object instance.
     * @param user   User
     */
    default T update(T object, CurrentUserDetails user) {
        final var retrievedObject = findByUid(object.getUid())
            .orElseThrow(() -> new IllegalQueryException(new ErrorMessage(ErrorCode.E6201)));

        if (canUpdate(retrievedObject, user)) {
            return update(object);
        } else {
            throw new UpdateAccessDeniedException("You don't have the proper permissions to update: \n" + object.toString());
        }
    }

    /**
     * Update object. Bypasses the ACL system.
     *
     * @param object Object update
     */
    default void updateNoAcl(T object) {
        final var retrievedObject = findByUid(object.getUid())
            .orElseThrow(() -> new IllegalQueryException(new ErrorMessage(ErrorCode.E6201)));
        update(retrievedObject);
    }

    /**
     * Removes the given object instance.
     *
     * @param object the object instance to delete.
     * @param user   User
     */
    default void delete(T object, CurrentUserDetails user) {
        final var retrievedObject = findByUid(object.getUid())
            .orElseThrow(() -> new IllegalQueryException(new ErrorMessage(ErrorCode.E6201)));

        if (canDelete(retrievedObject, user)) {
            delete(object);
        } else {
            throw new DeleteAccessDeniedException("You don't have the proper permissions to delete: \n" + object.toString());
        }
    }

    default Optional<T> findByUid(@Size(max = 11) String uid, CurrentUserDetails user) {
        return findByUid(uid).filter(o -> canRead(o, user));
    }

    default Optional<T> findById(String id, CurrentUserDetails user) {
        return findById(id).filter(o -> canRead(o, user));
    }

    default List<T> findById(Collection<String> ids) {
        return findAllById(ids).stream().filter(o -> canRead(o, null)).collect(Collectors.toList());
    }

    default Optional<T> findByIdNoAcl(String id) {
        return findById(id);
    }

    default Optional<T> findFirstByCode(String code, CurrentUserDetails user) {
        return findFirstByCode(code).filter(o -> canRead(o, user));
    }

    // ------------------------------------------------------------
    // TODO make not Bypassess the acl, maybe in base Jpa extension
    void deleteAllByUidIn(Collection<String> uids);

    Boolean existsByUid(@Size(max = 11) String uid);

    Optional<T> findByUid(@Size(max = 11) String uid);

    Optional<T> findByIdOrUid(@Size(max = 26) String id, @Size(max = 11) String uid);

    List<T> findDistinctByIdInOrUidIn(Collection<String> ids, Collection<String> uids);

    List<T> findAllByUidIn(Collection<String> uids);

    Page<T> findAllByUidIn(Collection<String> uids, Pageable pageable);

    void deleteByUid(@Size(max = 11) String uid);

    List<T> findAllByCodeIn(Collection<String> codes);

    Boolean existsByCode(String code);

    Optional<T> findFirstByCode(String code);

    Optional<T> findFirstByName(String name);

    List<T> findByNameLike(String name);
}
