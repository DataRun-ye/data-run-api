package org.nmcpye.datarun.jpa.common;

import io.hypersistence.utils.spring.repository.BaseJpaRepository;
import org.nmcpye.datarun.security.CurrentUserDetails;

import java.util.List;

/**
 * Datarun Extension of {@code BaseJpaRepository}
 * The {@code BaseJpaRepository} fixes many of the problems that the default Spring Data {@code JpaRepository}
 * suffers from.
 * <p>
 * For more details about how to use it, check out <a href=
 * "https://vladmihalcea.com/basejparepository-hypersistence-utils/">this article</a> on <a href="https://vladmihalcea.com/">vladmihalcea.com</a>.
 *
 * @author Vlad Mihalcea
 * @version 2.21.0
 */
public interface BaseJpaIdentifiableRepository<T, ID>
    extends BaseJpaRepository<T, ID> {

    List<T> findAll();

    Class<T> getEntityClass();

    /**
     * Can user Save the given object instance.
     *
     * @param object the object instance.
     * @param user   the user currently in the security context.
     */
    boolean canSave(T object, CurrentUserDetails user);

    /**
     * Can user Update the given object instance.
     *
     * @param object the object instance.
     * @param user   User
     */
    boolean canUpdate(T object, CurrentUserDetails user);

    /**
     * Can user Remove the given object instance.
     *
     * @param object the object instance to delete.
     * @param user   User
     */
    boolean canDelete(T object, CurrentUserDetails user);

    /**
     * Can user Remove the given object instance.
     *
     * @param object the object instance to delete.
     * @param user   User
     */
    boolean canRead(T object, CurrentUserDetails user);
}
