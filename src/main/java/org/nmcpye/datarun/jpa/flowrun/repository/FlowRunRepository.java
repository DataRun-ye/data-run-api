package org.nmcpye.datarun.jpa.flowrun.repository;

import org.nmcpye.datarun.jpa.accessfilter.FlowRunSummary;
import org.nmcpye.datarun.jpa.common.JpaIdentifiableRepository;
import org.nmcpye.datarun.jpa.flowrun.FlowRun;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Repository
public interface FlowRunRepository
    extends JpaIdentifiableRepository<FlowRun>, FlowRunRepositoryWithBagRelationships {

    Optional<FlowRun> findByUidOrCode(String uid, String code);

    @Override
    default List<FlowRun> findAllByCodeIn(Collection<String> codes) {
        return Collections.emptyList();
    }

    @Override
    default Boolean existsByCode(String code) {
        return false;
    }

    @Override
    default Optional<FlowRun> findFirstByCode(String code) {
        return Optional.empty();
    }

    @Override
    default Optional<FlowRun> findFirstByName(String name) {
        return Optional.empty();
    }

    default List<FlowRun> findByNameLike(String name) {
        return Collections.emptyList();
    }

    List<FlowRun> findAllByOrgUnitCodeIn(Collection<String> orgUnitCodes);

    Optional<FlowRun> findFirstByOrgUnitName(String orgUnitName);

    Optional<FlowRun> findFirstByOrgUnitCode(String code);

    List<FlowRun> findAllByOrgUnitNameLike(String orgUnitName);

    @Query(
        "select flowRun from FlowRun flowRun " +
            "left join  flowRun.team t " +
            "left join flowRun.activity a " +
            "where t.uid IN :teamUIDs and (:includeDisabled = true OR t.disabled = false) " +
            "and (:includeDisabled = true OR a.disabled = false)")
    List<FlowRun> findAllByTeams(@Param("teamUIDs") Collection<String> teamUIDs,
                                 @Param("includeDisabled") boolean includeDisabled);

    List<FlowRun> findAllByTeamUidIn(Collection<String> teamUIDs);


    //    @Query("select flowRun from FlowRun flowRun where flowRun.path like concat(:path, '%')")
    List<FlowRun> findAllByPathContaining(String path);

    @Query(
        "select flowRun from FlowRun flowRun " +
            "left join  flowRun.team t " +
            "left join flowRun.orgUnit ou " +
            "left join flowRun.activity a " +
            "where t.uid =:team and ou.uid =:orgUnit and a.uid =:activity order by a.id Limit 1"
    )
    Optional<FlowRun> findFirstByTeamAndOrgUnit(@Param("team") String team,
                                                @Param("orgUnit") String orgUnit, @Param("activity") String activity);

    @Query(
        value = "select flowRun from FlowRun flowRun " +
            "left join flowRun.activity " +
            "left join flowRun.orgUnit " +
            "left join flowRun.team " +
            "join flowRun.team.users user " +
            "where flowRun.activity.disabled =:disabled and " +
            "flowRun.team.disabled =:disabled and user.login = ?#{authentication.name}"
    )
    List<FlowRun> findAllByStatusUser(@Param("disabled") boolean disabled);

    Page<FlowRun> findAllByPathIsNull(Pageable pageable);

    /// / new test
    @Query("SELECT ou.id FROM FlowRun ou WHERE ou.parent.id IN :parentIds")
    List<Long> findChildIdsByParentIds(@Param("parentIds") List<Long> parentIds);

    @Query(value = """
        SELECT DISTINCT assi.*
        FROM flowRun assi
        WHERE assi.uid IN (
            SELECT unnest(string_to_array(acc.path, ','))
            FROM flowRun acc
            WHERE acc.id IN :accessibleIds
        )
        """, nativeQuery = true)
    Page<FlowRun> findWithAncestors(@Param("accessibleIds") List<Long> accessibleIds, Pageable pageable);

    @Query(value = """
        SELECT assi.*
        FROM flowRun assi
        WHERE EXISTS (
            SELECT 1
            FROM flowRun acc
            WHERE acc.id IN :accessibleIds
            AND assi.path LIKE CONCAT(acc.path, ',%')
        )
        """, nativeQuery = true)
    List<FlowRun> findWithDescendants(@Param("accessibleIds") List<Long> accessibleIds);


    @Query(value = """
        SELECT assi.*
        FROM flowRun assi
        WHERE EXISTS (
            SELECT 1
            FROM flowRun acc
            WHERE acc.id IN :accessibleIds
            AND assi.path LIKE CONCAT(acc.path, ',%')
        )
        """, nativeQuery = true)
    List<FlowRunSummary> findSummariesDescendants(@Param("accessibleIds") List<Long> accessibleIds);

    @Query(value = """
        SELECT assi.*
        FROM flowRun assi
        Join team t on t.id = assi.team_id
        WHERE t.uid IN :teamIds
        """, nativeQuery = true)
    Page<FlowRunSummary> findSummariesTeam(@Param("teamIds") Collection<String> teamIds, Pageable pageable);

    @Query(value = """
        SELECT assi.*
        FROM flowRun assi
        Join team t on t.id = assi.team_id
        WHERE t.uid IN :teamId
        """, nativeQuery = true)
    Page<FlowRunSummary> findSummariesTeam(@Param("teamId") String teamId, Pageable pageable);
}
