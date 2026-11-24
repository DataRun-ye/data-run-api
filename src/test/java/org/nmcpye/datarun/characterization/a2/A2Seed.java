package org.nmcpye.datarun.characterization.a2;

import org.springframework.http.MediaType;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * The configuration every A2 golden runs against: one campaign, its org units, teams, field users, assignments,
 * catalog entries and the forms, published through the real publish route so that element paths are the ones
 * production builds. Seeded once per database (the Testcontainer lives as long as the test JVM) and never
 * changed by a scenario, except where a scenario restores what it changed.
 */
final class A2Seed {

    static final String SEED_LOGIN = "a2-seed";

    static final String ACTIVITY = "a2Activity1";

    static final String COUNTRY = "a2Country01";
    static final String DISTRICT = "a2District1";
    static final String VILLAGE_1 = "a2Village01";
    static final String VILLAGE_2 = "a2Village02";
    static final String VILLAGE_3 = "a2Village03";
    static final String VILLAGE_4 = "a2Village04";
    /** Renamed by one scenario, which restores it. Its Reference catalog is empty. */
    static final String VILLAGE_5 = "a2Village05";
    static final String VILLAGE_5_NAME = "a2 village 5";

    static final String FORM_A = "a2FormAAAA1";
    static final String FORM_R = "a2FormRefR1";
    static final String FORM_X = "a2FormXXXX1";

    /** Team one: field1 and field2, add on FORM_A and FORM_R; FORM_X sits on its assignment without a grant. */
    static final String TEAM_ONE = "a2TeamOne01";
    /** Team two manages team one: add and edit-from-users on FORM_A. */
    static final String TEAM_TWO = "a2TeamTwo01";
    /** Team three: field3, add on FORM_A and FORM_X. */
    static final String TEAM_THREE = "a2TeamThr01";
    /** Team four: field4, add on FORM_A. Its grant is revoked by one scenario, which restores it. */
    static final String TEAM_FOUR = "a2TeamFou01";
    static final String TEAM_FOUR_GRANTS = "[{\"form\":\"" + FORM_A + "\",\"permissions\":[\"ADD_SUBMISSIONS\"]}]";
    /** Team five: deleter, add, edit and delete on FORM_A. */
    static final String TEAM_FIVE = "a2TeamFiv01";

    static final String FIELD_1 = "a2-field1";
    static final String FIELD_2 = "a2-field2";
    static final String EDITOR = "a2-editor";
    static final String FIELD_3 = "a2-field3";
    static final String FIELD_4 = "a2-field4";
    static final String DELETER = "a2-deleter";
    static final String NO_TEAM = "a2-noteam";
    /** Seeded by the changelog's user.csv. */
    static final String ADMIN = "admin";

    /** Team one, village 1: FORM_A, FORM_R, FORM_X. */
    static final String ASSIGN_T1_V1 = "a2AssignT1a";
    /** Team one, village 2: FORM_A, FORM_R. */
    static final String ASSIGN_T1_V2 = "a2AssignT1b";
    /** Team three, village 3: FORM_A, FORM_X. */
    static final String ASSIGN_T3_V3 = "a2AssignT3a";
    /** Team one, village 3, soft-deleted: FORM_A. One assignment per activity, org unit and team. */
    static final String ASSIGN_DELETED = "a2AssignDel";
    /** Team one, village 5 (renamed by one scenario; an empty Reference catalog): FORM_A, FORM_R. */
    static final String ASSIGN_T1_V5 = "a2AssignRen";
    /** Team four, village 4: FORM_A. */
    static final String ASSIGN_T4_V4 = "a2AssignT4a";
    /** Team five, village 1: FORM_A. */
    static final String ASSIGN_T5_V1 = "a2AssignT5a";
    /** Team one, village 4, whose catalog holds 501 entries (two pages of 500): FORM_R. */
    static final String ASSIGN_T1_V4 = "a2AssignPag";

    static final List<String> ASSIGNMENTS = List.of(
        ASSIGN_T1_V1, ASSIGN_T1_V2, ASSIGN_T3_V3, ASSIGN_DELETED, ASSIGN_T1_V5, ASSIGN_T4_V4, ASSIGN_T5_V1,
        ASSIGN_T1_V4);
    static final List<String> ORG_UNITS = List.of(COUNTRY, DISTRICT, VILLAGE_1, VILLAGE_2, VILLAGE_3, VILLAGE_4, VILLAGE_5);

    static final String HOUSE_V1 = "a2HouseV1aa";
    static final String HOUSE_V2 = "a2HouseV2aa";

    private static final String PASSWORD_HASH = "$2a$10$gSAhZrxMllrbgj/kkK9UceBPpChGWJA7SYIb1Mqo.n5aNLq1/oRrC";

    /** Published version uids (random at publish), by placeholder name. */
    private static final Map<String, String> VERSIONS = new LinkedHashMap<>();

    private A2Seed() {
    }

    static String version(String name) {
        String uid = VERSIONS.get(name);
        if (uid == null) {
            throw new IllegalStateException("No published version " + name);
        }
        return uid;
    }

    /** Placeholders for every value that is random per database: the published version uids. */
    static Map<String, String> placeholders() {
        Map<String, String> placeholders = new LinkedHashMap<>();
        VERSIONS.forEach((name, uid) -> placeholders.put(uid, "<" + name + ">"));
        return placeholders;
    }

    static synchronized void ensure(NamedParameterJdbcTemplate jdbc, PlatformTransactionManager transactionManager,
                                    MockMvc mvc, String jwtKey) throws Exception {
        Integer seeded = jdbc.queryForObject("SELECT count(*) FROM team WHERE uid = :uid",
            Map.of("uid", TEAM_ONE), Integer.class);
        if (seeded == null || seeded == 0) {
            new TransactionTemplate(transactionManager).executeWithoutResult(status -> seedRows(jdbc));
            publish(mvc, jwtKey, formA(false));
            publish(mvc, jwtKey, formA(true));
            publish(mvc, jwtKey, formR());
            publish(mvc, jwtKey, formX());
        }
        VERSIONS.clear();
        jdbc.query("""
                SELECT template_uid, version_number, uid FROM data_template_version
                WHERE template_uid IN (:forms) ORDER BY template_uid, version_number""",
            Map.of("forms", List.of(FORM_A, FORM_R, FORM_X)),
            rs -> {
                String form = switch (rs.getString("template_uid")) {
                    case FORM_A -> "FORM_A";
                    case FORM_R -> "FORM_R";
                    default -> "FORM_X";
                };
                VERSIONS.put(form + ".v" + rs.getInt("version_number"), rs.getString("uid"));
            });
    }

    private static void seedRows(NamedParameterJdbcTemplate jdbc) {
        update(jdbc, "INSERT INTO project (id, name, created_by) VALUES ('a2-project', 'a2 project', 'a2-seed')");
        update(jdbc, """
            INSERT INTO activity (id, uid, name, project_id, disabled, created_by)
            VALUES ('a2-activity', 'a2Activity1', 'a2 activity', 'a2-project', false, 'a2-seed')""");

        orgUnit(jdbc, "a2-ou-country", COUNTRY, "A2C", "a2 country", null, "," + COUNTRY);
        orgUnit(jdbc, "a2-ou-district", DISTRICT, "A2D", "a2 district", "a2-ou-country", "," + COUNTRY + "," + DISTRICT);
        String districtPath = "," + COUNTRY + "," + DISTRICT + ",";
        orgUnit(jdbc, "a2-ou-v1", VILLAGE_1, "A2V1", "a2 village 1", "a2-ou-district", districtPath + VILLAGE_1);
        orgUnit(jdbc, "a2-ou-v2", VILLAGE_2, "A2V2", "a2 village 2", "a2-ou-district", districtPath + VILLAGE_2);
        orgUnit(jdbc, "a2-ou-v3", VILLAGE_3, "A2V3", "a2 village 3", "a2-ou-district", districtPath + VILLAGE_3);
        orgUnit(jdbc, "a2-ou-v4", VILLAGE_4, "A2V4", "a2 village 4", "a2-ou-district", districtPath + VILLAGE_4);
        orgUnit(jdbc, "a2-ou-v5", VILLAGE_5, "A2V5", VILLAGE_5_NAME, "a2-ou-district", districtPath + VILLAGE_5);

        team(jdbc, "a2-team-one", TEAM_ONE, "A2T1", """
            [{"form":"a2FormAAAA1","permissions":["ADD_SUBMISSIONS"]},
             {"form":"a2FormRefR1","permissions":["ADD_SUBMISSIONS"]}]""");
        team(jdbc, "a2-team-two", TEAM_TWO, "A2T2", """
            [{"form":"a2FormAAAA1","permissions":["ADD_SUBMISSIONS","EDIT_SUBMISSIONS_FROM_USERS"]}]""");
        team(jdbc, "a2-team-three", TEAM_THREE, "A2T3", """
            [{"form":"a2FormAAAA1","permissions":["ADD_SUBMISSIONS"]},
             {"form":"a2FormXXXX1","permissions":["ADD_SUBMISSIONS"]}]""");
        team(jdbc, "a2-team-four", TEAM_FOUR, "A2T4", TEAM_FOUR_GRANTS);
        team(jdbc, "a2-team-five", TEAM_FIVE, "A2T5", """
            [{"form":"a2FormAAAA1","permissions":["ADD_SUBMISSIONS","EDIT_SUBMISSIONS","DELETE_SUBMISSIONS"]}]""");
        update(jdbc, "INSERT INTO team_managed_teams (team_id, managed_team_id) VALUES ('a2-team-two', 'a2-team-one')");

        user(jdbc, "a2-user-field1", "a2UserFld01", FIELD_1, "a2-team-one");
        user(jdbc, "a2-user-field2", "a2UserFld02", FIELD_2, "a2-team-one");
        user(jdbc, "a2-user-editor", "a2UserEdt01", EDITOR, "a2-team-two");
        user(jdbc, "a2-user-field3", "a2UserFld03", FIELD_3, "a2-team-three");
        user(jdbc, "a2-user-field4", "a2UserFld04", FIELD_4, "a2-team-four");
        user(jdbc, "a2-user-deleter", "a2UserDel01", DELETER, "a2-team-five");
        user(jdbc, "a2-user-noteam", "a2UserNot01", NO_TEAM, null);

        assignment(jdbc, ASSIGN_T1_V1, "a2-team-one", "a2-ou-v1", List.of(FORM_A, FORM_R, FORM_X), false);
        assignment(jdbc, ASSIGN_T1_V2, "a2-team-one", "a2-ou-v2", List.of(FORM_A, FORM_R), false);
        assignment(jdbc, ASSIGN_T3_V3, "a2-team-three", "a2-ou-v3", List.of(FORM_A, FORM_X), false);
        assignment(jdbc, ASSIGN_DELETED, "a2-team-one", "a2-ou-v3", List.of(FORM_A), true);
        assignment(jdbc, ASSIGN_T1_V5, "a2-team-one", "a2-ou-v5", List.of(FORM_A, FORM_R), false);
        assignment(jdbc, ASSIGN_T4_V4, "a2-team-four", "a2-ou-v4", List.of(FORM_A), false);
        assignment(jdbc, ASSIGN_T5_V1, "a2-team-five", "a2-ou-v1", List.of(FORM_A), false);
        assignment(jdbc, ASSIGN_T1_V4, "a2-team-one", "a2-ou-v4", List.of(FORM_R), false);

        referenceEntry(jdbc, "a2-ref-v1", HOUSE_V1, "House One", "a2-ou-v1");
        referenceEntry(jdbc, "a2-ref-v2", HOUSE_V2, "House Two", "a2-ou-v2");
        update(jdbc, """
            INSERT INTO reference_entry (id, uid, display_name, org_unit_id, created_by)
            SELECT 'a2-refp-' || g, 'a2P' || lpad(g::text, 8, '0'), 'Page House ' || g, 'a2-ou-v4', 'a2-seed'
            FROM generate_series(1, 501) AS g""");

        dataElement(jdbc, "a2DeName001", "a2_name", "Text");
        dataElement(jdbc, "a2DeItem001", "a2_item", "Text");
        dataElement(jdbc, "a2DeSub0001", "a2_sub", "Text");
        dataElement(jdbc, "a2DeExtra01", "a2_extra", "Text");
        dataElement(jdbc, "a2DeHouse01", "a2_house", "Reference");
        dataElement(jdbc, "a2DeNote001", "a2_note", "Text");
    }

    private static String formA(boolean secondVersion) {
        String extra = secondVersion ? """
            {"id": "a2DeExtra01", "parent": "main", "type": "Text"},""" : "";
        return """
            {"uid": "a2FormAAAA1", "name": "a2 ordinary form",
             "fields": [
               %s
               {"id": "a2DeName001", "parent": "main", "type": "Text"},
               {"id": "a2DeItem001", "parent": "rows", "type": "Text"},
               {"id": "a2DeSub0001", "parent": "subrows", "type": "Text"}],
             "sections": [
               {"name": "main"},
               {"name": "rows", "parent": "main", "repeatable": true},
               {"name": "subrows", "parent": "rows", "repeatable": true}]}""".formatted(extra);
    }

    private static String formR() {
        return """
            {"uid": "a2FormRefR1", "name": "a2 reference form",
             "fields": [{"id": "a2DeHouse01", "parent": "households", "type": "Reference"}],
             "sections": [{"name": "main"}, {"name": "households", "parent": "main", "repeatable": true}]}""";
    }

    private static String formX() {
        return """
            {"uid": "a2FormXXXX1", "name": "a2 other form",
             "fields": [{"id": "a2DeNote001", "parent": "main", "type": "Text"}],
             "sections": [{"name": "main"}]}""";
    }

    private static void publish(MockMvc mvc, String jwtKey, String template) throws Exception {
        MvcResult result = mvc.perform(post("/api/v1/dataFormTemplates")
                .header("Authorization", "Bearer " + A2Tokens.token(jwtKey, ADMIN))
                .contentType(MediaType.APPLICATION_JSON)
                .content(template))
            .andReturn();
        if (result.getResponse().getStatus() != 200) {
            throw new IllegalStateException("Publishing an A2 template failed: " + result.getResponse().getStatus()
                + " " + result.getResponse().getContentAsString());
        }
    }

    private static void orgUnit(NamedParameterJdbcTemplate jdbc, String id, String uid, String code, String name,
                                String parentId, String path) {
        jdbc.update("""
                INSERT INTO org_unit (id, uid, code, name, path, parent_id, created_by)
                VALUES (:id, :uid, :code, :name, :path, :parent, 'a2-seed')""",
            new MapSqlParameterSource().addValue("id", id).addValue("uid", uid).addValue("code", code)
                .addValue("name", name).addValue("path", path).addValue("parent", parentId));
    }

    private static void team(NamedParameterJdbcTemplate jdbc, String id, String uid, String code, String grants) {
        jdbc.update("""
                INSERT INTO team (id, uid, code, name, activity_id, disabled, form_permissions, created_by)
                VALUES (:id, :uid, :code, :name, 'a2-activity', false, CAST(:grants AS jsonb), 'a2-seed')""",
            new MapSqlParameterSource().addValue("id", id).addValue("uid", uid).addValue("code", code)
                .addValue("name", "a2 " + code).addValue("grants", grants));
    }

    private static void user(NamedParameterJdbcTemplate jdbc, String id, String uid, String login, String teamId) {
        jdbc.update("""
                INSERT INTO app_user (id, uid, login, password_hash, activated, lang_key, created_by)
                VALUES (:id, :uid, :login, :hash, true, 'en', 'a2-seed')""",
            new MapSqlParameterSource().addValue("id", id).addValue("uid", uid).addValue("login", login)
                .addValue("hash", PASSWORD_HASH));
        jdbc.update("INSERT INTO app_user_authority (user_id, authority_name) VALUES (:id, 'ROLE_USER')",
            Map.of("id", id));
        if (teamId != null) {
            jdbc.update("INSERT INTO team_user (team_id, user_id) VALUES (:team, :id)",
                Map.of("team", teamId, "id", id));
        }
    }

    private static void assignment(NamedParameterJdbcTemplate jdbc, String uid, String teamId, String orgUnitId,
                                   List<String> forms, boolean deleted) {
        jdbc.update("""
                INSERT INTO assignment (id, uid, team_id, activity_id, org_unit_id, forms, deleted, created_by)
                VALUES (:id, :uid, :team, 'a2-activity', :orgUnit, CAST(:forms AS jsonb), :deleted, 'a2-seed')""",
            new MapSqlParameterSource().addValue("id", "a2-asg-" + uid).addValue("uid", uid).addValue("team", teamId)
                .addValue("orgUnit", orgUnitId)
                .addValue("forms", "[" + String.join(",", forms.stream().map(f -> "\"" + f + "\"").toList()) + "]")
                .addValue("deleted", deleted));
    }

    private static void referenceEntry(NamedParameterJdbcTemplate jdbc, String id, String uid, String name,
                                       String orgUnitId) {
        jdbc.update("""
                INSERT INTO reference_entry (id, uid, display_name, org_unit_id, created_by)
                VALUES (:id, :uid, :name, :orgUnit, 'a2-seed')""",
            Map.of("id", id, "uid", uid, "name", name, "orgUnit", orgUnitId));
    }

    private static void dataElement(NamedParameterJdbcTemplate jdbc, String uid, String name, String type) {
        jdbc.update("""
                INSERT INTO data_element (id, uid, code, name, type, created_by)
                VALUES (:id, :uid, :uid, :name, :type, 'a2-seed')""",
            Map.of("id", "a2-de-" + uid, "uid", uid, "name", name, "type", type));
    }

    private static void update(NamedParameterJdbcTemplate jdbc, String sql) {
        jdbc.update(sql, Map.of());
    }
}
