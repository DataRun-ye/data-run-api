package org.nmcpye.datarun.jpa.assignment.repository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.nmcpye.datarun.IntegrationTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

/** The managed-team scope query (api #45) against a real PostgreSQL migrated by Liquibase. */
@IntegrationTest
class ManagedAssignmentScopeIT {
    private static final String TEAM = "itManaged01";
    private static final String OFF_TEAM = "itOffTeam01";
    private static final String OFF_ACTIVITY_TEAM = "itOffActv01";
    private static final String SUPERVISION = "itSupervise";

    @Autowired
    private AssignmentRepository assignments;

    @Autowired
    private NamedParameterJdbcTemplate jdbc;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @BeforeEach
    void seed() {
        inTransaction(() -> {
            update("INSERT INTO project (id, name, created_by) VALUES ('it45-project', 'it45', 'it45')");
            update("""
                INSERT INTO activity (id, uid, name, project_id, disabled, created_by) VALUES
                  ('it45-act-on', 'itActvOn001', 'on', 'it45-project', false, 'it45'),
                  ('it45-act-off', 'itActvOff01', 'off', 'it45-project', true, 'it45')""");
            update("""
                INSERT INTO team (id, uid, code, name, activity_id, disabled, created_by) VALUES
                  ('it45-team', '%s', 'T', 'T', 'it45-act-on', false, 'it45'),
                  ('it45-team-off', '%s', 'T-off', 'T-off', 'it45-act-on', true, 'it45'),
                  ('it45-team-offact', '%s', 'T-offact', 'T-offact', 'it45-act-off', false, 'it45')"""
                .formatted(TEAM, OFF_TEAM, OFF_ACTIVITY_TEAM));
            update("INSERT INTO org_unit (id, uid, code, name, path, created_by) VALUES "
                + "('it45-district', 'itDistrict1', 'D', 'D', ',itDistrict1', 'it45')");
            for (int i = 1; i <= 7; i++) {
                update(("INSERT INTO org_unit (id, uid, code, name, path, parent_id, created_by) VALUES "
                    + "('it45-v%d', 'itVillage0%d', 'V%d', 'V%d', ',itDistrict1,itVillage0%d', 'it45-district', 'it45')")
                    .formatted(i, i, i, i, i));
            }
            assignment("itAssign001", "it45-team", "it45-act-on", 1, "[\"itSupervise\", \"itHousehld\"]", false);
            assignment("itAssign002", "it45-team", "it45-act-on", 2, "[\"itHousehld\"]", false);
            assignment("itAssign003", "it45-team", "it45-act-on", 3, "null", false);
            assignment("itAssign004", "it45-team", "it45-act-on", 4, null, false);
            assignment("itAssign005", "it45-team", "it45-act-on", 5, "[\"itSupervise\"]", true);
            assignment("itAssign006", "it45-team-off", "it45-act-on", 6, "[\"itSupervise\"]", false);
            assignment("itAssign007", "it45-team-offact", "it45-act-off", 7, "[\"itSupervise\"]", false);
        });
    }

    @AfterEach
    void clean() {
        inTransaction(() -> {
            update("DELETE FROM assignment WHERE id LIKE 'it45-%'");
            update("DELETE FROM org_unit WHERE id LIKE 'it45-v%'");
            update("DELETE FROM org_unit WHERE id = 'it45-district'");
            update("DELETE FROM team WHERE id LIKE 'it45-%'");
            update("DELETE FROM activity WHERE id LIKE 'it45-%'");
            update("DELETE FROM project WHERE id = 'it45-project'");
        });
    }

    @Test
    void returnsLiveAssignmentsOfTheTeamsThatCarryAGrantedForm() {
        var scope = assignments.findManagedScope(Set.of(TEAM, OFF_TEAM, OFF_ACTIVITY_TEAM), Set.of(SUPERVISION), false);

        assertThat(scope)
            .extracting(ManagedAssignmentRef::getUid, ManagedAssignmentRef::getOrgUnitUid,
                ManagedAssignmentRef::getOrgUnitPath)
            .containsExactly(tuple("itAssign001", "itVillage01", ",itDistrict1,itVillage01"));
        // The default method works on the query's projection proxy.
        assertThat(scope.get(0).orgUnitAndAncestorUids()).containsExactly("itDistrict1", "itVillage01");
    }

    @Test
    void includesDisabledTeamsAndActivitiesOnlyWhenAsked() {
        var scope = assignments.findManagedScope(Set.of(TEAM, OFF_TEAM, OFF_ACTIVITY_TEAM), Set.of(SUPERVISION), true);

        assertThat(scope).extracting(ManagedAssignmentRef::getUid)
            .containsExactlyInAnyOrder("itAssign001", "itAssign006", "itAssign007");
    }

    private void assignment(String uid, String team, String activity, int village, String forms, boolean deleted) {
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("id", "it45-" + uid)
            .addValue("uid", uid)
            .addValue("team", team)
            .addValue("activity", activity)
            .addValue("orgUnit", "it45-v" + village)
            .addValue("forms", forms)
            .addValue("deleted", deleted);
        jdbc.update("""
            INSERT INTO assignment (id, uid, team_id, activity_id, org_unit_id, forms, deleted, created_by)
            VALUES (:id, :uid, :team, :activity, :orgUnit, CAST(:forms AS jsonb), :deleted, 'it45')""", params);
    }

    private void update(String sql) {
        jdbc.update(sql, Map.of());
    }

    private void inTransaction(Runnable work) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> work.run());
    }
}
