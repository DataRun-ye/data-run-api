package org.nmcpye.datarun.jpa.auditing.repository;

import org.nmcpye.datarun.jpa.auditing.EntityAuditEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for the EntityAuditEvent entity.
 */
@Repository
public interface EntityAuditEventRepository
    extends JpaRepository<EntityAuditEvent, Long> {
    List<EntityAuditEvent> findAllByEntityTypeAndEntityId(String entityType, String entityId);

    @Query("SELECT max(a.commitVersion) FROM EntityAuditEvent a where a.entityType = :type and a.entityId = :entityId")
    Integer findMaxCommitVersion(@Param("type") String type, @Param("entityId") String entityId);

    @Query("SELECT DISTINCT (a.entityType) from EntityAuditEvent a")
    List<String> findAllEntityTypes();

    Page<EntityAuditEvent> findAllByEntityType(String entityType, Pageable pageRequest);

    @Query(
        "SELECT ae FROM EntityAuditEvent ae where ae.entityType = :type and ae.entityId = :entityId and " +
            "ae.commitVersion =(SELECT max(a.commitVersion) FROM EntityAuditEvent a where a.entityType = :type and " +
            "a.entityId = :entityId and a.commitVersion < :commitVersion)"
    )
    EntityAuditEvent findOneByEntityTypeAndEntityIdAndCommitVersion(
        @Param("type") String type,
        @Param("entityId") String entityId,
        @Param("commitVersion") Integer commitVersion
    );

    // nmc
    //–– New: fetch everything after a given sequence (paginated)
    Page<EntityAuditEvent> findAllByIdGreaterThan(
        Long eventSequence, Pageable pageRequest);

    //–– Optionally, filter further by entityType
    Page<EntityAuditEvent> findAllByEntityTypeAndIdGreaterThan(
        String entityType, Long eventSequence, Pageable pageRequest);
}
