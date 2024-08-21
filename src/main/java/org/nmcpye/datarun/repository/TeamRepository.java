package org.nmcpye.datarun.repository;

import org.nmcpye.datarun.drun.postgres.domain.Team;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for the Team entity.
 */
@Repository
public interface TeamRepository extends JpaRepository<Team, Long> {
    @Query("select team from Team team where team.userInfo.login = ?#{authentication.name}")
    List<Team> findByUserInfoIsCurrentUser();

    default Optional<Team> findOneWithEagerRelationships(Long id) {
        return this.findOneWithToOneRelationships(id);
    }

    default List<Team> findAllWithEagerRelationships() {
        return this.findAllWithToOneRelationships();
    }

    default Page<Team> findAllWithEagerRelationships(Pageable pageable) {
        return this.findAllWithToOneRelationships(pageable);
    }

    @Query(
        value = "select team from Team team left join fetch team.activity left join fetch team.warehouse left join fetch team.userInfo",
        countQuery = "select count(team) from Team team"
    )
    Page<Team> findAllWithToOneRelationships(Pageable pageable);

    @Query("select team from Team team left join fetch team.activity left join fetch team.warehouse left join fetch team.userInfo")
    List<Team> findAllWithToOneRelationships();

    @Query(
        "select team from Team team left join fetch team.activity left join fetch team.warehouse left join fetch team.userInfo where team.id =:id"
    )
    Optional<Team> findOneWithToOneRelationships(@Param("id") Long id);
}
