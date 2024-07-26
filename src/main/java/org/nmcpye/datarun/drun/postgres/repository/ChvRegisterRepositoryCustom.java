package org.nmcpye.datarun.drun.postgres.repository;

import org.nmcpye.datarun.domain.ChvRegister;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for the ChvRegister entity.
 */
@Repository
public interface ChvRegisterRepositoryCustom
    extends IdentifiableRepository<ChvRegister> {

    Optional<ChvRegister> findByUid(String uid);

    default Optional<ChvRegister> findOneWithEagerRelationshipsByUser(Long id) {
        return this.findOneWithToOneRelationshipsByUser(id);
    }

    default Page<ChvRegister> findAllWithEagerRelationshipsByUser(Pageable pageable) {
        return this.findAllWithToOneRelationshipsByUser(pageable);
    }

    @Query(
        value = "select chvRegister from ChvRegister chvRegister " +
            "left join chvRegister.team " +
            "left join chvRegister.activity " +
            "where chvRegister.team.userInfo.login = ?#{authentication.name}",
        countQuery = "select count(chvRegister) from ChvSession chvRegister " +
            "where chvRegister.team.userInfo.login = ?#{authentication.name}"
    )
    Page<ChvRegister> findAllByUser(Pageable pageable);

    @Query(
        value = "select chvRegister from ChvRegister chvRegister " +
            "left join fetch chvRegister.location " +
            "left join fetch chvRegister.activity " +
            "left join fetch chvRegister.team " +
            "where chvRegister.team.userInfo.login = ?#{authentication.name}",
        countQuery = "select count(chvRegister) from ChvRegister chvRegister " +
            "where chvRegister.team.userInfo.login = ?#{authentication.name}"
    )
    Page<ChvRegister> findAllWithToOneRelationshipsByUser(Pageable pageable);

//    @Query(
//        "select chvRegister from ChvRegister chvRegister " +
//            "left join fetch chvRegister.location " +
//            "left join fetch chvRegister.activity " +
//            "left join fetch chvRegister.team " +
//            "where chvRegister.team.userInfo.login = ?#{authentication.name}"
//    )
//    List<ChvRegister> findAllWithToOneRelationshipsByUser();

    @Query(
        "select chvRegister from ChvRegister chvRegister " +
            "left join fetch chvRegister.location " +
            "left join fetch chvRegister.activity " +
            "left join fetch chvRegister.team " +
            "where chvRegister.id =:id " +
            "and chvRegister.team.userInfo.login = ?#{authentication.name}"
    )
    Optional<ChvRegister> findOneWithToOneRelationshipsByUser(@Param("id") Long id);
}
