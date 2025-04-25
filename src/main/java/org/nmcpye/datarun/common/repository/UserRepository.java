package org.nmcpye.datarun.common.repository;

import org.nmcpye.datarun.common.jpa.repository.JpaAuditableRepository;
import org.nmcpye.datarun.domain.User;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Spring Data JPA repository for the {@link User} entity.
 */
@Repository
//public interface UserRepository extends JpaRepository<User, Long> {
public interface UserRepository
    extends JpaAuditableRepository<User> {
    String USERS_BY_LOGIN_CACHE = "usersByLogin";

    String USERS_BY_EMAIL_CACHE = "usersByEmail";

    String USER_TEAM_IDS_CACHE = "userTeamIdsByLogin";
    String USER_GROUP_IDS_CACHE = "userGroupIdsByLogin";

    String USER_ACTIVITY_IDS_CACHE = "userActivityIdsByLogin";
    String USER_FORM_IDS_CACHE = "userFormIdsByLogin";


    Optional<User> findOneByActivationKey(String activationKey);

    List<User> findAllByActivatedIsFalseAndActivationKeyIsNotNullAndCreatedDateBefore(Instant dateTime);

    Optional<User> findOneByResetKey(String resetKey);

    Optional<User> findOneByEmailIgnoreCase(String email);

    @EntityGraph(attributePaths = "authorities")
    Optional<User> findOneByLogin(String login);

    User findUserByLogin(String login);

    List<User> findByLoginIn(Set<String> logins);

    @EntityGraph(attributePaths = {"authorities", "roles"})
    @Cacheable(cacheNames = USERS_BY_LOGIN_CACHE)
    Optional<User> findOneWithAuthoritiesByLogin(String login);

    @EntityGraph(attributePaths = {"authorities", "roles"})
    @Cacheable(cacheNames = USERS_BY_EMAIL_CACHE)
    Optional<User> findOneWithAuthoritiesByEmailIgnoreCase(String email);

    Page<User> findAllByIdNotNullAndActivatedIsTrue(Pageable pageable);
}
