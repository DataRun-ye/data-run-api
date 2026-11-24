package org.nmcpye.datarun.characterization.a2;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.nmcpye.datarun.characterization.a2.A2Payloads.Item;
import org.nmcpye.datarun.jpa.datasubmission.command.SubmissionUploads;
import org.nmcpye.datarun.jpa.datasubmission.revision.JournalReconciliation;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.nmcpye.datarun.characterization.a2.A2Payloads.Group.G3;
import static org.nmcpye.datarun.characterization.a2.A2Payloads.NODES;
import static org.nmcpye.datarun.characterization.a2.A2Payloads.item;
import static org.nmcpye.datarun.characterization.a2.A2Payloads.ordinary;
import static org.nmcpye.datarun.characterization.a2.A2Payloads.uid;
import static org.nmcpye.datarun.characterization.a2.A2Seed.ADMIN;
import static org.nmcpye.datarun.characterization.a2.A2Seed.ASSIGN_T1_V1;
import static org.nmcpye.datarun.characterization.a2.A2Seed.FIELD_1;
import static org.nmcpye.datarun.characterization.a2.A2Seed.FORM_A;
import static org.nmcpye.datarun.characterization.a2.A2Seed.version;

/**
 * A revision is written in the transaction of the change it records, before the outbox row that announces it: a
 * failure right after the revision, or right after the outbox row, leaves nothing of the change. The failures are
 * injected by test triggers on the journal and the outbox, for the submission and table a test names. Not a golden:
 * these read the database directly.
 */
class A2RevisionTransactionIT extends A2GoldenSupport {

    private static final String FUTURE = "2099-01-01T00:00:00Z";
    private static final String RELEASE = "/api/v1/admin/heldSubmissions/release";

    private static Item formA(String uid, String name) {
        return item(G3, uid, FORM_A, version("FORM_A.v1"), ASSIGN_T1_V1, ordinary(G3, uid, name, "r1", "s1", 1));
    }

    @BeforeEach
    void injectFailures() {
        inTransaction(() -> {
            jdbc.getJdbcTemplate().execute("CREATE TABLE IF NOT EXISTS a2_injected_failure (uid varchar(11), at text)");
            jdbc.getJdbcTemplate().execute("""
                CREATE OR REPLACE FUNCTION a2_injected_failure() RETURNS trigger LANGUAGE plpgsql AS $$
                BEGIN
                    IF EXISTS (SELECT 1 FROM a2_injected_failure f JOIN data_submission s ON s.uid = f.uid
                               WHERE s.id = NEW.submission_id AND f.at = TG_TABLE_NAME) THEN
                        RAISE EXCEPTION 'injected failure after the % row', TG_TABLE_NAME;
                    END IF;
                    RETURN NEW;
                END $$""");
            jdbc.getJdbcTemplate().execute("DROP TRIGGER IF EXISTS a2_injected_failure ON submission_revision");
            jdbc.getJdbcTemplate().execute("DROP TRIGGER IF EXISTS a2_injected_failure ON outbox");
            jdbc.getJdbcTemplate().execute("""
                CREATE TRIGGER a2_injected_failure AFTER INSERT ON submission_revision
                FOR EACH ROW EXECUTE FUNCTION a2_injected_failure()""");
            jdbc.getJdbcTemplate().execute("""
                CREATE TRIGGER a2_injected_failure AFTER INSERT ON outbox
                FOR EACH ROW EXECUTE FUNCTION a2_injected_failure()""");
        });
    }

    @AfterEach
    void removeInjectedFailures() {
        inTransaction(() -> {
            jdbc.getJdbcTemplate().execute("DROP TRIGGER IF EXISTS a2_injected_failure ON outbox");
            jdbc.getJdbcTemplate().execute("DROP TRIGGER IF EXISTS a2_injected_failure ON submission_revision");
            jdbc.getJdbcTemplate().execute("DROP FUNCTION IF EXISTS a2_injected_failure()");
            jdbc.getJdbcTemplate().execute("DROP TABLE IF EXISTS a2_injected_failure");
        });
    }

    /** The next write of the submission fails right after its row in {@code table}. */
    private void failAfter(String uid, String table) {
        inTransaction(() -> jdbc.update("INSERT INTO a2_injected_failure (uid, at) VALUES (:uid, :at)",
            Map.of("uid", uid, "at", table)));
    }

    /** The submission's row, revisions and outbox rows, as text: "version deleted | revisions | events". */
    private String stored(String uid) {
        List<String> row = jdbc.queryForList(
            "SELECT lock_version || ' ' || deleted FROM data_submission WHERE uid = :uid", Map.of("uid", uid), String.class);
        List<String> revisions = jdbc.queryForList("""
            SELECT r.version || ' ' || r.kind FROM submission_revision r JOIN data_submission s ON s.id = r.submission_id
            WHERE s.uid = :uid ORDER BY r.version""", Map.of("uid", uid), String.class);
        List<String> events = jdbc.queryForList("""
            SELECT event_type || ' ' || submission_version FROM outbox WHERE submission_uid = :uid ORDER BY outbox_id""",
            Map.of("uid", uid), String.class);
        return (row.isEmpty() ? "none" : row.get(0)) + " | " + String.join(", ", revisions) + " | "
            + String.join(", ", events);
    }

    private static String failure(Scenario scenario, String uid) {
        return scenario.lastBody().path("failed").path(uid).asText(null);
    }

    @Test
    void aNewSubmissionWhoseRevisionFailsIsNotStored() {
        String uid = uid("frev1");
        failAfter(uid, "submission_revision");

        Scenario scenario = scenario("tx", "a creation fails after its revision")
            .upload("new", G3, FIELD_1, formA(uid, "first"));

        assertThat(failure(scenario, uid)).isEqualTo(SubmissionUploads.UNEXPECTED_FAILURE);
        assertThat(stored(uid)).isEqualTo("none |  | ");
    }

    @Test
    void aNewSubmissionWhoseOutboxRowFailsIsNotStored() {
        String uid = uid("fbox1");
        failAfter(uid, "outbox");

        Scenario scenario = scenario("tx", "a creation fails after its outbox row")
            .upload("new", G3, FIELD_1, formA(uid, "first"));

        assertThat(failure(scenario, uid)).isEqualTo(SubmissionUploads.UNEXPECTED_FAILURE);
        assertThat(stored(uid)).isEqualTo("none |  | ");
    }

    @Test
    void aChangeWhoseRevisionFailsLeavesTheStoredVersion() {
        String uid = uid("frev2");
        Scenario scenario = scenario("tx", "a change fails after its revision")
            .upload("new", G3, FIELD_1, formA(uid, "first"));
        failAfter(uid, "submission_revision");

        scenario.upload("changed", G3, FIELD_1, formA(uid, "changed"));

        assertThat(failure(scenario, uid)).isEqualTo(SubmissionUploads.UNEXPECTED_FAILURE);
        assertThat(stored(uid)).isEqualTo("0 false | 0 CREATE | SAVE 0");
        assertThat(jdbc.queryForObject("SELECT form_data #>> '{main,a2_name}' FROM data_submission WHERE uid = :uid",
            Map.of("uid", uid), String.class)).isEqualTo("first");
    }

    @Test
    void anAdminDeleteWhoseOutboxRowFailsLeavesTheSubmissionLive() {
        String uid = uid("fbox2");
        Scenario scenario = scenario("tx", "an admin delete fails after its outbox row")
            .upload("new", G3, FIELD_1, formA(uid, "first"));
        failAfter(uid, "outbox");

        scenario.call("admin deletes", "DELETE", "/api/v1/dataSubmission/" + uid, ADMIN, null);

        assertThat(scenario.lastStatus()).isEqualTo(500);
        assertThat(stored(uid)).isEqualTo("0 false | 0 CREATE | SAVE 0");
    }

    @Test
    void aReleaseWhoseRevisionFailsSavesNothingAndStaysHeld() {
        String uid = uid("frev3");
        Scenario scenario = scenario("tx", "a release fails after its revision")
            .goLive("campaign", null, null, FUTURE)
            .upload("held before go-live", G3, FIELD_1, formA(uid, "first"));
        failAfter(uid, "submission_revision");
        ObjectNode release = NODES.objectNode();
        release.set("uids", NODES.arrayNode().add(uid));

        scenario.call("admin releases it", "POST", RELEASE, ADMIN, release);

        assertThat(scenario.lastBody().path("failed").has(uid)).as("the release reports it failed").isTrue();
        assertThat(stored(uid)).isEqualTo("none |  | ");
        assertThat(jdbc.queryForObject("SELECT state FROM held_submission WHERE uid = :uid", Map.of("uid", uid),
            String.class)).isEqualTo("HELD");
    }

    /** The reconciliation is not vacuous: changes made behind the journal are reported. */
    @Test
    void changesMadeBehindTheJournalAreReported() {
        String changed = uid("bsql1");
        String bumped = uid("bsql2");
        scenario("tx", "two submissions changed by SQL")
            .upload("new", G3, FIELD_1, formA(changed, "first"))
            .upload("new", G3, FIELD_1, formA(bumped, "first"));
        inTransaction(() -> {
            jdbc.update("UPDATE data_submission SET status = 'DONE' WHERE uid = :uid", Map.of("uid", changed));
            jdbc.update("UPDATE data_submission SET lock_version = 1 WHERE uid = :uid", Map.of("uid", bumped));
        });

        assertThat(JournalReconciliation.findings(jdbc, "s.uid IN (:uids)", Map.of("uids", List.of(changed, bumped))))
            .containsExactly(changed + ": the head revision's state is not the row's",
                bumped + ": head revision at 0, row at 1");
    }
}
