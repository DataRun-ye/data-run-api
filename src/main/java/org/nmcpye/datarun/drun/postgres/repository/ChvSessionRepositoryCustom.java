package org.nmcpye.datarun.drun.postgres.repository;

import org.nmcpye.datarun.domain.ChvSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for the ChvSession entity.
 */
@Repository
public interface ChvSessionRepositoryCustom
    extends IdentifiableRepository<ChvSession> {

    Optional<ChvSession> findByUid(String uid);

    default Optional<ChvSession> findOneWithEagerRelationshipsByUser(Long id) {
        return this.findOneWithToOneRelationshipsByUser(id);
    }

    default Page<ChvSession> findAllWithEagerRelationshipsByUser(Pageable pageable) {
        return this.findAllWithToOneRelationshipsByUser(pageable);
    }

    @Query(
        value = "select chvSession from ChvSession chvSession " +
            "left join chvSession.team " +
            "left join chvSession.activity " +
            "where chvSession.team.userInfo.login = ?#{authentication.name}",
        countQuery = "select count(chvSession) from ChvSession chvSession " +
            "where chvSession.team.userInfo.login = ?#{authentication.name}"
    )
    Page<ChvSession> findAllByUser(Pageable pageable);


    @Query(
        value = "select chvSession from ChvSession chvSession " +
            "left join fetch chvSession.team " +
            "left join chvSession.activity " +
            "where chvSession.team.userInfo.login = ?#{authentication.name}",
        countQuery = "select count(chvSession) from ChvSession chvSession " +
            "where chvSession.team.userInfo.login = ?#{authentication.name}"
    )
    Page<ChvSession> findAllWithToOneRelationshipsByUser(Pageable pageable);

//    @Query("select chvSession from ChvSession chvSession " +
//        "left join fetch chvSession.team " +
//        "left join chvSession.activity " +
//        "where chvSession.team.userInfo.login = ?#{authentication.name}")
//    List<ChvSession> findAllWithToOneRelationshipsByUser();

    @Query("select chvSession from ChvSession chvSession " +
        "left join fetch chvSession.team " +
        "left join chvSession.activity " +
        "where chvSession.id =:id and chvSession.team.userInfo.login = ?#{authentication.name}")
    Optional<ChvSession> findOneWithToOneRelationshipsByUser(@Param("id") Long id);
}
