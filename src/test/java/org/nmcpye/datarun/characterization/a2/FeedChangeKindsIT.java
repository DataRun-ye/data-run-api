package org.nmcpye.datarun.characterization.a2;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.nmcpye.datarun.etl.orchestrator.EtlOrchestrator;
import org.nmcpye.datarun.submission.normalized.DerivedIds;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.nmcpye.datarun.characterization.a2.A2Payloads.Group.G0;
import static org.nmcpye.datarun.characterization.a2.A2Payloads.Group.G3;
import static org.nmcpye.datarun.characterization.a2.A2Payloads.NODES;
import static org.nmcpye.datarun.characterization.a2.A2Payloads.item;
import static org.nmcpye.datarun.characterization.a2.A2Payloads.metadata;
import static org.nmcpye.datarun.characterization.a2.A2Payloads.ordinary;
import static org.nmcpye.datarun.characterization.a2.A2Payloads.rowId;
import static org.nmcpye.datarun.characterization.a2.A2Payloads.uid;
import static org.nmcpye.datarun.characterization.a2.A2Seed.ADMIN;
import static org.nmcpye.datarun.characterization.a2.A2Seed.ASSIGN_T1_V1;
import static org.nmcpye.datarun.characterization.a2.A2Seed.FIELD_1;
import static org.nmcpye.datarun.characterization.a2.A2Seed.FORM_A;
import static org.nmcpye.datarun.characterization.a2.A2Seed.version;

/**
 * Every kind of change that exists today reaches the submission feed through the real write paths, the production
 * filter chain and the application's ETL with the switch on: an upload, a re-send, rows added, removed and churned (a
 * legacy re-send whose rows the server gives new ids), an admin delete, and a held upload's release. After each change
 * the feed holds the journal's head, rewrites only what changed and marks what is gone. Not a golden: the A2 seed and
 * routes, with values invented.
 */
@TestPropertySource(properties = {
    "datarun.activation.submission-feed=true",
    "spring.datasource.hikari.maximum-pool-size=4"})
class FeedChangeKindsIT extends A2GoldenSupport {

    private static final String RELEASE = "/api/v1/admin/heldSubmissions/release";
    private static final String NAME_FIELD = "a2-de-a2DeName001";
    private static final String ITEM_FIELD = "a2-de-a2DeItem001";

    @Autowired
    private EtlOrchestrator orchestrator;

    @BeforeEach
    void cleanTheFeed() {
        clearTheFeed();
    }

    @AfterEach
    void clearTheFeed() {
        inTransaction(() -> {
            jdbc.update("DELETE FROM feed.submission_row", Map.of());
            jdbc.update("DELETE FROM feed.submission", Map.of());
            jdbc.update("DELETE FROM feed.freshness", Map.of());
            jdbc.update("DELETE FROM feed_ops.pending", Map.of());
            jdbc.update("DELETE FROM feed_ops.normalization_flags", Map.of());
            jdbc.update("DELETE FROM outbox_processing", Map.of());
            jdbc.update("DELETE FROM etl_runs", Map.of());
            jdbc.update("DELETE FROM analytics.tall_canonical WHERE submission_uid LIKE 'a2%'", Map.of());
            jdbc.update("DELETE FROM analytics.events WHERE submission_uid LIKE 'a2%'", Map.of());
        });
    }

    /** Runs the application's ETL until no outbox row is due. */
    private void process() {
        while (orchestrator.runOnce("a2-feed", 50).claimed() > 0) {
            // every claimed row ends success, failed with a later retry, or dead
        }
    }

    private String submissionId(String uid) {
        return jdbc.queryForObject("SELECT id FROM data_submission WHERE uid = :uid", Map.of("uid", uid), String.class);
    }

    private long head(String id) {
        return jdbc.queryForObject("SELECT max(version) FROM journal.submission_revision WHERE submission_id = :id",
            Map.of("id", id), Long.class);
    }

    private Map<String, Object> record(String id) {
        List<Map<String, Object>> records = jdbc.queryForList("""
                SELECT position, version, deleted, form_id, field_values ->> :name AS name
                FROM feed.submission WHERE id = :id""",
            Map.of("id", id, "name", NAME_FIELD));
        return records.isEmpty() ? Map.of() : records.get(0);
    }

    /** The submission's row records by id: position, deleted, changed in version, parent, section, item value. */
    private Map<String, Map<String, Object>> rows(String id) {
        Map<String, Map<String, Object>> rows = new HashMap<>();
        jdbc.queryForList("""
                SELECT id, position, deleted, changed_in_version, parent_row_id, section_id, id_origin,
                       field_values ->> :item AS item
                FROM feed.submission_row WHERE submission_id = :id""",
            Map.of("id", id, "item", ITEM_FIELD)).forEach(row -> rows.put((String) row.get("id"), row));
        return rows;
    }

    private String rowsSection() {
        String formId = jdbc.queryForObject("SELECT id FROM data_template WHERE uid = :uid", Map.of("uid", FORM_A),
            String.class);
        String path = jdbc.queryForObject("""
                SELECT s ->> 'path' FROM data_template_version v CROSS JOIN LATERAL jsonb_array_elements(v.sections) s
                WHERE v.template_uid = :uid AND s ->> 'name' = 'rows' LIMIT 1""",
            Map.of("uid", FORM_A), String.class);
        return DerivedIds.sectionId(formId, path);
    }

    /** FORM_A data with the ordinary row (and its nested row), then a second row of the same section. */
    private static ObjectNode withSecondRow(String uid, String name) {
        ObjectNode data = ordinary(G3, uid, name, "item one", "sub one", 1);
        ObjectNode second = NODES.objectNode().put("a2_item", "item two");
        metadata(second, rowId(3), 2, uid, uid);
        ((com.fasterxml.jackson.databind.node.ArrayNode) data.get("main").get("rows")).add(second);
        return data;
    }

    private void assertTheFeedHoldsTheHead(String id) {
        assertThat(record(id)).containsEntry("version", head(id));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM feed_ops.pending", Map.of(), Long.class)).isZero();
    }

    @Test
    void anUploadAReSendRowsAddedRemovedAndChurnedAndAnAdminDeleteEachReachTheFeed() {
        String uid = uid("fdKinds");
        String section = rowsSection();
        scenario("FEED-KINDS", "every kind of change, published")
            .upload("an upload", G3, FIELD_1, item(G3, uid, FORM_A, version("FORM_A.v1"), ASSIGN_T1_V1,
                ordinary(G3, uid, "first", "item one", "sub one", 1)));
        process();
        String id = submissionId(uid);
        assertTheFeedHoldsTheHead(id);
        Map<String, Object> first = record(id);
        assertThat(first).containsEntry("deleted", false).containsEntry("name", "first");
        Map<String, Map<String, Object>> rows = rows(id);
        assertThat(rows).containsOnlyKeys(rowId(1), rowId(2));
        assertThat(rows.get(rowId(1))).containsEntry("parent_row_id", null).containsEntry("section_id", section)
            .containsEntry("item", "item one").containsEntry("deleted", false).containsEntry("id_origin", "STORED");
        assertThat(rows.get(rowId(2))).containsEntry("parent_row_id", rowId(1)).containsEntry("deleted", false);

        scenario("FEED-KINDS", "a re-send").upload("a re-send, changed", G3, FIELD_1,
            item(G3, uid, FORM_A, version("FORM_A.v1"), ASSIGN_T1_V1,
                ordinary(G3, uid, "second", "item one", "sub one", 1)));
        process();
        assertTheFeedHoldsTheHead(id);
        assertThat(record(id)).containsEntry("name", "second");
        assertThat((Long) record(id).get("position")).isGreaterThan((Long) first.get("position"));
        assertThat(rows(id)).as("rows that did not change are not rewritten").isEqualTo(rows);

        scenario("FEED-KINDS", "rows added").upload("a re-send with a row added", G3, FIELD_1,
            item(G3, uid, FORM_A, version("FORM_A.v1"), ASSIGN_T1_V1, withSecondRow(uid, "second")));
        process();
        assertTheFeedHoldsTheHead(id);
        Map<String, Map<String, Object>> added = rows(id);
        assertThat(added).containsOnlyKeys(rowId(1), rowId(2), rowId(3));
        assertThat(added.get(rowId(3))).containsEntry("deleted", false).containsEntry("item", "item two");
        assertThat(added.get(rowId(1))).isEqualTo(rows.get(rowId(1)));

        scenario("FEED-KINDS", "rows removed").upload("a re-send without that row", G3, FIELD_1,
            item(G3, uid, FORM_A, version("FORM_A.v1"), ASSIGN_T1_V1,
                ordinary(G3, uid, "second", "item one", "sub one", 1)));
        process();
        assertTheFeedHoldsTheHead(id);
        Map<String, Map<String, Object>> removed = rows(id);
        assertThat(removed.get(rowId(3))).as("a removed row: a deleted record keeping its last values")
            .containsEntry("deleted", true).containsEntry("item", "item two")
            .containsEntry("changed_in_version", head(id));

        scenario("FEED-KINDS", "rows churned").upload("a legacy re-send: rows without ids", G0, FIELD_1,
            item(G0, uid, FORM_A, version("FORM_A.v1"), ASSIGN_T1_V1,
                ordinary(G0, uid, "legacy", "item one", "sub one", 1)));
        process();
        assertTheFeedHoldsTheHead(id);
        Map<String, Map<String, Object>> churned = rows(id);
        assertThat(churned.get(rowId(1))).as("the old rows are gone").containsEntry("deleted", true);
        assertThat(churned.get(rowId(2))).containsEntry("deleted", true);
        assertThat(churned.values().stream().filter(row -> !(Boolean) row.get("deleted")).toList())
            .as("the new rows, under the ids the server gave them").hasSize(2)
            .allSatisfy(row -> assertThat((String) row.get("id")).isNotIn(rowId(1), rowId(2), rowId(3)));

        scenario("FEED-KINDS", "an admin delete")
            .call("the admin deletes it", "DELETE", "/api/v1/dataSubmission/" + uid, ADMIN, null);
        process();
        assertTheFeedHoldsTheHead(id);
        assertThat(record(id)).containsEntry("deleted", true).containsEntry("name", "legacy");
        assertThat(rows(id).values()).as("every row record deleted").allSatisfy(row ->
            assertThat(row).containsEntry("deleted", true));
    }

    @Test
    void aHeldUploadReachesTheFeedWhenItIsReleased() {
        String uid = uid("fdHeld");
        scenario("FEED-HELD", "a held upload, released")
            .goLive("campaign", null, null, "2099-01-01T00:00:00Z")
            .upload("held before the go-live", G3, FIELD_1, item(G3, uid, FORM_A, version("FORM_A.v1"),
                ASSIGN_T1_V1, ordinary(G3, uid, "held", "item one", "sub one", 1)));
        process();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM data_submission WHERE uid = :uid", Map.of("uid", uid),
            Long.class)).as("held, not stored").isZero();

        ObjectNode uids = NODES.objectNode();
        uids.set("uids", NODES.arrayNode().add(uid));
        scenario("FEED-HELD", "the release").call("the admin releases it", "POST", RELEASE, ADMIN, uids);
        process();

        String id = submissionId(uid);
        assertThat(jdbc.queryForObject("SELECT command FROM journal.submission_revision WHERE submission_id = :id",
            Map.of("id", id), String.class)).isEqualTo("HELD_RELEASE");
        assertTheFeedHoldsTheHead(id);
        assertThat(record(id)).containsEntry("name", "held").containsEntry("deleted", false);
        assertThat(rows(id)).containsOnlyKeys(rowId(1), rowId(2));
    }
}
