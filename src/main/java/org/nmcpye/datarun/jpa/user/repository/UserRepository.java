package org.nmcpye.datarun.jpa.user.repository;

import org.nmcpye.datarun.jpa.common.JpaIdentifiableRepository;
import org.nmcpye.datarun.jpa.user.User;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for the {@link User} entity.
 */
@Repository
//public interface UserRepository extends BaseJpaIdentifiableRepository<User, Long> {
public interface UserRepository
    extends JpaIdentifiableRepository<User> {
    String USERS_BY_LOGIN_CACHE = "usersByLogin";

    String USERS_BY_EMAIL_CACHE = "usersByEmail";

    String USER_TEAM_IDS_CACHE = "userTeamIdsByLogin";
    String USER_GROUP_IDS_CACHE = "userGroupIdsByLogin";

    String USER_ACTIVITY_IDS_CACHE = "userActivityIdsByLogin";

//    String USER_FORM_IDS_CACHE = "userFormIdsByLogin";

    String USER_TEAM_FORM_ACCESS_CACHE = "userFormAccessByTeamAndForm";


    /// ////
    @Override
    default List<User> findAllByCodeIn(Collection<String> codes) {
        return Collections.emptyList();
    }

    @Override
    default Boolean existsByCode(String code) {
        return false;
    }


    @Override
    default Optional<User> findFirstByCode(String code) {
        return Optional.empty();
    }

    @Override
    default Optional<User> findFirstByName(String name) {
        return Optional.empty();
    }

    default List<User> findByNameLike(String name) {
        return Collections.emptyList();
    }

    /// /////
    Optional<User> findOneByActivationKey(String activationKey);

    List<User> findAllByActivatedIsFalseAndActivationKeyIsNotNullAndCreatedDateBefore(Instant dateTime);

    Optional<User> findOneByResetKey(String resetKey);

    Optional<User> findOneByEmailIgnoreCase(String email);

    @EntityGraph(attributePaths = "authorities")
    Optional<User> findOneByLogin(String login);

    List<User> findByLoginIn(Collection<String> logins);

    @EntityGraph(attributePaths = {"authorities", "roles"})
    @Cacheable(cacheNames = USERS_BY_LOGIN_CACHE)
    Optional<User> findOneWithAuthoritiesByLogin(String login);

    @EntityGraph(attributePaths = {"authorities", "roles"})
    @Cacheable(cacheNames = USERS_BY_EMAIL_CACHE)
    Optional<User> findOneWithAuthoritiesByEmailIgnoreCase(String email);

    Page<User> findAllByIdNotNullAndActivatedIsTrue(Pageable pageable);
}
