package org.nmcpye.datarun.jpa.flowinstance.repository;

import org.nmcpye.datarun.common.enumeration.FlowStatus;
import org.nmcpye.datarun.jpa.common.JpaIdentifiableRepository;
import org.nmcpye.datarun.jpa.flowinstance.FlowInstance;
import org.nmcpye.datarun.jpa.flowtype.FlowType;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Repository for managing FlowInstance entities.
 *
 * @author Hamza Assada 20/03/2023
 */
@Repository
public interface FlowInstanceRepository
    extends JpaIdentifiableRepository<FlowInstance>, FlowInstanceRepositoryWithBagRelationships {

    /**
     * find flow instances by their status
     *
     * @param status flow status
     * @return list of found FlowInstances
     */
    List<FlowInstance> findByStatus(FlowStatus status);

    /**
     * find flow instances by their associated FlowType
     *
     * @param flowType flow Type
     * @return list of found FlowInstances
     */
    List<FlowInstance> findByFlowType(FlowType flowType);

    /**
     * find flow instances by orgUnitId in FlowScope (navigating through the one-to-one relationship)
     *
     * @param orgUnitId organization unit's id
     * @return list of found FlowInstances
     */
    List<FlowInstance> findByFlowScope_OrgUnitId(String orgUnitId);

    /**
     * find flow instances by scopeDate in FlowScope
     *
     * @param scopeDate scope date
     * @return list of found FlowInstances
     */
    List<FlowInstance> findByFlowScope_ScopeDate(LocalDate scopeDate);

    /**
     * find flow instances by a range of scopeDates
     *
     * @param startDate start date
     * @param endDate   end date
     * @return list of found FlowInstances
     */
    List<FlowInstance> findByFlowScope_ScopeDateBetween(LocalDate startDate, LocalDate endDate);

    /**
     * find flow instances by entityInstanceId in FlowScope
     *
     * @param primaryEntityInstanceId entity instance id
     * @return list of found FlowInstances
     */
    List<FlowInstance> findByFlowScope_PrimaryEntityInstanceId(String primaryEntityInstanceId);

    /**
     * find FlowInstances where a specific ScopeAttribute exists
     * joining to the EAV table.
     *
     * @param attributeKey   attribute key
     * @param attributeValue attribute value
     * @return list of found FlowInstances
     */
    @Query("SELECT fi FROM FlowInstance fi JOIN fi.flowScope fs JOIN fs.attributes sa WHERE sa.key = :attributeKey AND sa.value = :attributeValue")
    List<FlowInstance> findByFlowScopeAttribute(String attributeKey, String attributeValue);

//    @Query("SELECT f.entityInstanceId AS entityInstanceId, " +
//        "f.flowType.id AS flowType, " +
//        "COUNT(*) AS totalFlows, " +
//        "MIN(f.plannedDate) AS firstUsed, " +
//        "MAX(f.completedAt) AS lastUsed " +
//        "FROM FlowInstance f " +
//        "WHERE f.entityInstanceId IS NOT NULL " +
//        "GROUP BY f.entityInstanceId, f.flowType.id")
//    List<EntityUsageSummary> summarizeEntityUsage();
//
//    @Query("SELECT f.assignedTeamId AS teamId, f.flowType.id AS flowType, " +
//        "SUM(CASE WHEN f.status = 'COMPLETED' THEN 1 ELSE 0 END) AS completedCount, " +
//        "SUM(CASE WHEN f.status = 'IN_PROGRESS' THEN 1 ELSE 0 END) AS inProgressCount, " +
//        "SUM(CASE WHEN f.status = 'ERROR' THEN 1 ELSE 0 END) AS errorCount " +
//        "FROM FlowInstance f " +
//        "GROUP BY f.assignedTeamId, f.flowType.id")
//    List<FlowStatusByTeamReport> reportFlowStatusByTeam();

//    /**
//     * Find all IN_PROGRESS FlowInstances for a given team.
//     * The team is stored inside the JSONB 'scopes' column under key 'team'.
//     */
//    @Query(
//        value = "SELECT * FROM flow_instance fi " +
//            "WHERE fi.status = 'IN_PROGRESS' " +
//            "  AND fi.scopes->>'team' = :teamId",
//        nativeQuery = true
//    )
//    List<FlowInstance> findInProgressByTeam(@Param("teamId") String teamId);
//
//    /**
//     * Find by a scope key/value pair in JSONB scopes.
//     * E.g. find by date or orgUnit.
//     */
//    @Query(value =
//        "SELECT * FROM flow_instance fi\n" +
//            " WHERE fi.scopes->>:key = :value",
//        nativeQuery = true)
//    List<FlowInstance> findByScope(
//        @Param("key") String scopeKey,
//        @Param("value") String scopeValue);

//    @Query(value = "SELECT * FROM flow_instance fi " +
//        "WHERE (fi.scopes->>'date')::date BETWEEN :start AND :end",
//        nativeQuery = true)
//    List<FlowInstance> findByDateRange(@Param("start") String start, @Param("end") String end);

    @Override
    default List<FlowInstance> findAllByCodeIn(Collection<String> codes) {
        return Collections.emptyList();
    }

    @Override
    default Boolean existsByCode(String code) {
        return false;
    }

    @Override
    default Optional<FlowInstance> findFirstByCode(String code) {
        return Optional.empty();
    }

    @Override
    default Optional<FlowInstance> findFirstByName(String name) {
        return Optional.empty();
    }

    default List<FlowInstance> findByNameLike(String name) {
        return Collections.emptyList();
    }

//
//    @Query(
//        "select flowInstance from FlowInstance flowInstance " +
//            "left join  flowInstance.team t " +
//            "left join flowInstance.activity a " +
//            "where t.uid IN :teamUIDs and (:includeDisabled = true OR t.disabled = false) " +
//            "and (:includeDisabled = true OR a.disabled = false)")
//    List<FlowInstance> findAllByTeams(@Param("teamUIDs") Collection<String> teamUIDs,
//                                      @Param("includeDisabled") boolean includeDisabled);

//    List<FlowInstance> findAllByTeamUidIn(Collection<String> teamUIDs);


//    //    @Query("select flowInstance from FlowRun flowInstance where flowInstance.path like concat(:path, '%')")
//    List<FlowInstance> findAllByPathContaining(String path);

//    @Query(
//        "select flowInstance from FlowInstance flowInstance " +
//            "left join  flowInstance.team t " +
//            "left join flowInstance.orgUnit ou " +
//            "left join flowInstance.activity a " +
//            "where t.uid =:team and ou.uid =:orgUnit and a.uid =:activity order by a.id Limit 1"
//    )
//    Optional<FlowInstance> findFirstByTeamAndOrgUnit(@Param("team") String team,
//                                                     @Param("orgUnit") String orgUnit, @Param("activity") String activity);

//    @Query(
//        value = "select flowInstance from FlowInstance flowInstance " +
//            "left join flowInstance.activity " +
//            "left join flowInstance.orgUnit " +
//            "left join flowInstance.team " +
//            "join flowInstance.team.users user " +
//            "where flowInstance.activity.disabled =:disabled and " +
//            "flowInstance.team.disabled =:disabled and user.login = ?#{authentication.name}"
//    )
//    List<FlowInstance> findAllByStatusUser(@Param("disabled") boolean disabled);
//
//    Page<FlowInstance> findAllByPathIsNull(Pageable pageable);

//    /// / new test
//    @Query("SELECT ou.id FROM FlowInstance ou WHERE ou.parent.id IN :parentIds")
//    List<Long> findChildIdsByParentIds(@Param("parentIds") List<Long> parentIds);
//
//    @Query(value = """
//        SELECT DISTINCT assi.*
//        FROM flowInstance assi
//        WHERE assi.uid IN (
//            SELECT unnest(string_to_array(acc.path, ','))
//            FROM flowInstance acc
//            WHERE acc.id IN :accessibleIds
//        )
//        """, nativeQuery = true)
//    Page<FlowInstance> findWithAncestors(@Param("accessibleIds") List<Long> accessibleIds, Pageable pageable);

//    @Query(value = """
//        SELECT assi.*
//        FROM flowInstance assi
//        WHERE EXISTS (
//            SELECT 1
//            FROM flowInstance acc
//            WHERE acc.id IN :accessibleIds
//            AND assi.path LIKE CONCAT(acc.path, ',%')
//        )
//        """, nativeQuery = true)
//    List<FlowInstance> findWithDescendants(@Param("accessibleIds") List<Long> accessibleIds);
//

//    @Query(value = """
//        SELECT assi.*
//        FROM flowInstance assi
//        WHERE EXISTS (
//            SELECT 1
//            FROM flowInstance acc
//            WHERE acc.id IN :accessibleIds
//            AND assi.path LIKE CONCAT(acc.path, ',%')
//        )
//        """, nativeQuery = true)
//    List<FlowRunSummary> findSummariesDescendants(@Param("accessibleIds") List<Long> accessibleIds);

//    @Query(value = """
//        SELECT assi.*
//        FROM flowInstance assi
//        Join team t on t.id = assi.team_id
//        WHERE t.uid IN :teamIds
//        """, nativeQuery = true)
//    Page<FlowRunSummary> findSummariesTeam(@Param("teamIds") Collection<String> teamIds, Pageable pageable);
//
//    @Query(value = """
//        SELECT assi.*
//        FROM flowInstance assi
//        Join team t on t.id = assi.team_id
//        WHERE t.uid IN :teamId
//        """, nativeQuery = true)
//    Page<FlowRunSummary> findSummariesTeam(@Param("teamId") String teamId, Pageable pageable);
}
