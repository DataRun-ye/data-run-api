package org.nmcpye.datarun.characterization.a2;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.nmcpye.datarun.characterization.a2.A2Payloads.Group;
import org.nmcpye.datarun.characterization.a2.A2Payloads.Item;
import org.nmcpye.datarun.jpa.datasubmission.conflict.ConflictRehearsal;
import org.nmcpye.datarun.jpa.datasubmission.revision.JournalReconciliation;
import org.nmcpye.datarun.jpa.datasubmission.revision.SubmissionRevisionReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.nmcpye.datarun.characterization.a2.A2Payloads.Group.G0;
import static org.nmcpye.datarun.characterization.a2.A2Payloads.Group.G1;
import static org.nmcpye.datarun.characterization.a2.A2Payloads.Group.G3;
import static org.nmcpye.datarun.characterization.a2.A2Payloads.households;
import static org.nmcpye.datarun.characterization.a2.A2Payloads.item;
import static org.nmcpye.datarun.characterization.a2.A2Payloads.ordinary;
import static org.nmcpye.datarun.characterization.a2.A2Payloads.uid;
import static org.nmcpye.datarun.characterization.a2.A2Seed.ADMIN;
import static org.nmcpye.datarun.characterization.a2.A2Seed.ASSIGN_T1_V1;
import static org.nmcpye.datarun.characterization.a2.A2Seed.ASSIGN_T1_V2;
import static org.nmcpye.datarun.characterization.a2.A2Seed.ASSIGN_T3_V3;
import static org.nmcpye.datarun.characterization.a2.A2Seed.EDITOR;
import static org.nmcpye.datarun.characterization.a2.A2Seed.FIELD_1;
import static org.nmcpye.datarun.characterization.a2.A2Seed.FIELD_2;
import static org.nmcpye.datarun.characterization.a2.A2Seed.FORM_A;
import static org.nmcpye.datarun.characterization.a2.A2Seed.FORM_R;
import static org.nmcpye.datarun.characterization.a2.A2Seed.HOUSE_V1;
import static org.nmcpye.datarun.characterization.a2.A2Seed.HOUSE_V2;
import static org.nmcpye.datarun.characterization.a2.A2Seed.version;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Conflict detection switched on, through the production filter chain, on the A2 seed (spec, Conflicts): server-side
 * changes are seeded as their methods will write them ({@link ReviewSeed}). Every upload item is answered
 * {@code updated} or refused exactly as today; what changes is what the server keeps.
 * <ul>
 *   <li>An unchanged re-send, and a copy equal to the current state, write nothing, with or without row ids.</li>
 *   <li>A changed copy after a server-side change is kept aside, with its base version, its route's shape and whether
 *       its rows lack ids; re-sent while open, it counts or replaces the copy.</li>
 *   <li>A kept conflict stays kept under unchanged re-sends; a plain accept is the uploader's next base.</li>
 *   <li>After a move, unchanged re-sends write nothing and are no longer refused, and a changed copy the upload checks
 *       refuse for access is kept aside; any other refusal, and an access refusal without a move, stands.</li>
 *   <li>Another user's copy is checked first and refused as today, never queued; an authorized one takes the first
 *       revision as its base.</li>
 *   <li>A detection error keeps the copy aside and never fails the item; practice uploads are not detected.</li>
 * </ul>
 * Not a golden: it reads the database directly.
 */
@TestPropertySource(properties = "datarun.activation.conflict-detection=true")
class ConflictDetectionIT extends A2GoldenSupport {

    @MockitoSpyBean
    private SubmissionRevisionReader journalReader;

    @Autowired
    private DataSource dataSource;

    private ReviewSeed seed;

    @BeforeEach
    void seedServerSideChanges() {
        seed = new ReviewSeed(jdbc, transactionManager);
    }

    // ---------------------------------------------------------------- changes nothing

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = Group.class, names = {"G3", "G0"})
    void anUnchangedReSendAfterAServerSideEditChangesNothing(Group group) throws Exception {
        String uid = uid("cdSame" + group.name());
        assertThat(answer(upload(group, FIELD_1, formA(group, uid, "first")))).isEqualTo(created(uid));
        seed.edit(uid, ADMIN, "edited on the server");

        for (int i = 0; i < 3; i++) {
            assertThat(answer(upload(group, FIELD_1, formA(group, uid, "first")))).isEqualTo(updated(uid));
        }

        assertThat(revisions(uid)).containsExactly("0 CREATE " + command(group), "1 UPDATE EDIT");
        assertThat(conflicts(uid)).isEmpty();
        assertThat(reconciliation(uid)).isEmpty();
    }

    /** Apps 5.2 and 5.3 send rows without ids; the legacy route used to mint new ones, and write, at every re-send. */
    @Test
    void aCopyWithoutRowIdsReSentUnchangedWritesNothing() throws Exception {
        String uid = uid("cdG0Same");
        assertThat(answer(upload(G0, FIELD_1, formA(G0, uid, "first")))).isEqualTo(created(uid));

        for (int i = 0; i < 3; i++) {
            assertThat(answer(upload(G0, FIELD_1, formA(G0, uid, "first")))).isEqualTo(updated(uid));
        }

        assertThat(revisions(uid)).containsExactly("0 CREATE LEGACY_UPLOAD");
        assertThat(conflicts(uid)).isEmpty();
    }

    @Test
    void aCopyEqualToTheCurrentStateChangesNothing() throws Exception {
        String uid = uid("cdCurrent");
        assertThat(answer(upload(G3, FIELD_1, formA(G3, uid, "first")))).isEqualTo(created(uid));
        seed.edit(uid, ADMIN, "edited on the server");

        assertThat(answer(upload(G3, FIELD_1, formA(G3, uid, "edited on the server")))).isEqualTo(updated(uid));

        assertThat(revisions(uid)).containsExactly("0 CREATE UPLOAD", "1 UPDATE EDIT");
        assertThat(conflicts(uid)).isEmpty();
    }

    /**
     * Phones never send a status, but API callers can, and an upload writes it: a copy that changes only the status is
     * a changed copy, saved as today, or kept aside after a server-side change; re-sent unchanged, it writes nothing.
     */
    @Test
    void aCopyThatChangesOnlyTheStatusIsAChangedCopy() throws Exception {
        String uid = uid("cdStatus");
        assertThat(answer(upload(G3, FIELD_1, formA(G3, uid, "first")))).isEqualTo(created(uid));
        Item done = formA(G3, uid, "first");
        done.node.put("status", "DONE");

        assertThat(answer(upload(G3, FIELD_1, done))).isEqualTo(updated(uid));
        assertThat(revisions(uid)).as("saved as today").containsExactly("0 CREATE UPLOAD", "1 UPDATE UPLOAD");
        assertThat(jdbc.queryForObject("SELECT status FROM data_submission WHERE uid = :uid", Map.of("uid", uid),
            String.class)).isEqualTo("DONE");

        seed.edit(uid, ADMIN, "edited on the server");
        Item cancelled = formA(G3, uid, "first");
        cancelled.node.put("status", "CANCELLED");
        assertThat(answer(upload(G3, FIELD_1, done))).as("unchanged").isEqualTo(updated(uid));
        assertThat(answer(upload(G3, FIELD_1, cancelled))).as("changed").isEqualTo(updated(uid));

        assertThat(revisions(uid)).containsExactly("0 CREATE UPLOAD", "1 UPDATE UPLOAD", "2 UPDATE EDIT");
        assertThat(conflicts(uid)).singleElement().satisfies(conflict -> assertThat(conflict)
            .containsEntry("cause", "SERVER_SIDE_CHANGE").containsEntry("base_version", 1L));
    }

    @Test
    void withoutAServerSideChangeAChangedCopyIsSavedAsToday() throws Exception {
        String uid = uid("cdApply");
        assertThat(answer(upload(G3, FIELD_1, formA(G3, uid, "first")))).isEqualTo(created(uid));

        assertThat(answer(upload(G3, FIELD_1, formA(G3, uid, "changed on the phone")))).isEqualTo(updated(uid));

        assertThat(revisions(uid)).containsExactly("0 CREATE UPLOAD", "1 UPDATE UPLOAD");
        assertThat(conflicts(uid)).isEmpty();
    }

    // ---------------------------------------------------------------- kept aside

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = Group.class, names = {"G3", "G0"})
    void aChangedCopyAfterAServerSideEditIsKeptAside(Group group) throws Exception {
        String uid = uid("cdKept" + group.name());
        assertThat(answer(upload(group, FIELD_1, formA(group, uid, "first")))).isEqualTo(created(uid));
        seed.edit(uid, ADMIN, "edited on the server");
        Item changed = formA(group, uid, "changed on the phone");

        assertThat(answer(upload(group, FIELD_1, changed))).isEqualTo(updated(uid));

        assertThat(revisions(uid)).as("the submission is unchanged")
            .containsExactly("0 CREATE " + command(group), "1 UPDATE EDIT");
        assertThat(conflicts(uid)).singleElement().satisfies(conflict -> {
            assertThat(conflict).containsEntry("state", "OPEN").containsEntry("cause", "SERVER_SIDE_CHANGE")
                .containsEntry("base_version", 0L).containsEntry("uploader_user_id", seed.userId(FIELD_1))
                .containsEntry("rows_lack_ids", group == G0)
                .containsEntry("upload_route", group.versioned() ? "VERSIONED" : "LEGACY")
                .containsEntry("resend_count", 0).containsEntry("version", 0L);
            JsonNode copy = json.readTree(conflict.get("phone_copy").toString());
            assertThat(copy.get("formData")).as("the document as received, rows without ids left so")
                .isEqualTo(changed.node.get("formData"));
            assertThat(copy.get("uid").asText()).isEqualTo(uid);
        });
    }

    @Test
    void aReSendWhileOpenCountsTheSameCopyOrReplacesItWithADifferentOne() throws Exception {
        String uid = uid("cdResend");
        assertThat(answer(upload(G3, FIELD_1, formA(G3, uid, "first")))).isEqualTo(created(uid));
        seed.edit(uid, ADMIN, "edited on the server");
        upload(G3, FIELD_1, formA(G3, uid, "changed on the phone"));
        Map<String, Object> opened = conflicts(uid).get(0);

        assertThat(answer(upload(G3, FIELD_1, formA(G3, uid, "changed on the phone")))).isEqualTo(updated(uid));
        Map<String, Object> again = conflicts(uid).get(0);
        assertThat(again).containsEntry("resend_count", 1).containsEntry("version", 0L)
            .containsEntry("first_received_at", opened.get("first_received_at"));
        assertThat((java.sql.Timestamp) again.get("last_received_at"))
            .isAfter((java.sql.Timestamp) opened.get("last_received_at"));

        assertThat(answer(upload(G3, FIELD_1, formA(G3, uid, "changed again")))).isEqualTo(updated(uid));
        assertThat(conflicts(uid)).singleElement().satisfies(replaced -> {
            assertThat(replaced).containsEntry("state", "OPEN").containsEntry("resend_count", 2)
                .containsEntry("version", 1L).containsEntry("first_received_at", opened.get("first_received_at"));
            assertThat(json.readTree(replaced.get("phone_copy").toString()).at("/formData/main/a2_name").asText())
                .isEqualTo("changed again");
        });
        assertThat(revisions(uid)).containsExactly("0 CREATE UPLOAD", "1 UPDATE EDIT");
    }

    @Test
    void aKeptConflictStaysKeptUnderUnchangedReSends() throws Exception {
        String uid = uid("cdKeep");
        assertThat(answer(upload(G3, FIELD_1, formA(G3, uid, "first")))).isEqualTo(created(uid));
        seed.edit(uid, ADMIN, "edited on the server");
        upload(G3, FIELD_1, formA(G3, uid, "changed on the phone"));
        seed.keep(conflictId(uid), ADMIN);

        for (int i = 0; i < 3; i++) {
            assertThat(answer(upload(G3, FIELD_1, formA(G3, uid, "changed on the phone")))).isEqualTo(updated(uid));
        }

        assertThat(conflicts(uid)).singleElement().satisfies(kept -> assertThat(kept).containsEntry("state", "KEPT"));
        assertThat(revisions(uid)).containsExactly("0 CREATE UPLOAD", "1 UPDATE EDIT");
    }

    /** A plain accept leaves the state equal to the uploader's copy: it is their base for what comes next. */
    @Test
    void aPlainAcceptIsTheUploadersNextBaseAndAnAcceptThatReappliedEditsIsNot() throws Exception {
        String plain = uid("cdAccept");
        assertThat(answer(upload(G3, FIELD_1, formA(G3, plain, "first")))).isEqualTo(created(plain));
        seed.edit(plain, ADMIN, "edited on the server");
        upload(G3, FIELD_1, formA(G3, plain, "changed on the phone"));
        seed.accept(conflictId(plain), ADMIN, false, null);

        assertThat(answer(upload(G3, FIELD_1, formA(G3, plain, "changed on the phone")))).isEqualTo(updated(plain));
        assertThat(answer(upload(G3, FIELD_1, formA(G3, plain, "changed after the accept"))))
            .isEqualTo(updated(plain));
        assertThat(revisions(plain)).as("saved: no server-side change since the accept")
            .containsExactly("0 CREATE UPLOAD", "1 UPDATE EDIT", "2 UPDATE CONFLICT_ACCEPT", "3 UPDATE UPLOAD");

        String reapplied = uid("cdReapply");
        assertThat(answer(upload(G3, FIELD_1, formA(G3, reapplied, "first")))).isEqualTo(created(reapplied));
        seed.edit(reapplied, ADMIN, "edited on the server");
        upload(G3, FIELD_1, formA(G3, reapplied, "changed on the phone"));
        seed.accept(conflictId(reapplied), ADMIN, true, "edited on the server");

        assertThat(answer(upload(G3, FIELD_1, formA(G3, reapplied, "changed after the accept"))))
            .isEqualTo(updated(reapplied));
        assertThat(revisions(reapplied)).as("kept aside: the edit since the base still stands")
            .containsExactly("0 CREATE UPLOAD", "1 UPDATE EDIT", "2 UPDATE CONFLICT_ACCEPT");
        assertThat(conflicts(reapplied)).extracting(conflict -> conflict.get("state"))
            .containsExactlyInAnyOrder("ACCEPTED", "OPEN");
    }

    /** After field1's own accepted copy (their base, revision 2), the only change is the editor's accepted copy. */
    @Test
    void anotherUploadersAcceptIsAServerSideChange() throws Exception {
        String uid = uid("cdOtherAcc");
        assertThat(answer(upload(G3, FIELD_1, formA(G3, uid, "first")))).isEqualTo(created(uid));
        seed.edit(uid, ADMIN, "edited on the server");
        upload(G3, FIELD_1, formA(G3, uid, "field1's copy"));
        seed.accept(conflictId(uid), ADMIN, false, null);
        upload(G3, EDITOR, formA(G3, uid, "the editor's copy"));
        seed.accept(conflictId(uid), ADMIN, false, null);
        assertThat(revisions(uid)).containsExactly(
            "0 CREATE UPLOAD", "1 UPDATE EDIT", "2 UPDATE CONFLICT_ACCEPT", "3 UPDATE CONFLICT_ACCEPT");

        assertThat(answer(upload(G3, FIELD_1, formA(G3, uid, "field1 changes again")))).isEqualTo(updated(uid));

        assertThat(revisions(uid)).hasSize(4);
        assertThat(conflicts(uid)).filteredOn(conflict -> "OPEN".equals(conflict.get("state")))
            .singleElement().satisfies(open -> assertThat(open)
                .containsEntry("uploader_user_id", seed.userId(FIELD_1)).containsEntry("base_version", 2L));
    }

    @Test
    void anAdminDeleteIsAServerSideChange() throws Exception {
        String uid = uid("cdAdminDel");
        assertThat(answer(upload(G3, FIELD_1, formA(G3, uid, "first")))).isEqualTo(created(uid));
        assertThat(adminDelete(uid).getStatus()).isEqualTo(204);

        assertThat(answer(upload(G3, FIELD_1, formA(G3, uid, "first")))).as("unchanged").isEqualTo(updated(uid));
        assertThat(answer(upload(G3, FIELD_1, formA(G3, uid, "changed on the phone")))).as("changed")
            .isEqualTo(updated(uid));

        assertThat(revisions(uid)).as("no phone copy changes the deleted submission any more")
            .containsExactly("0 CREATE UPLOAD", "1 DELETE ADMIN_DELETE");
        assertThat(conflicts(uid)).singleElement().satisfies(conflict -> assertThat(conflict)
            .containsEntry("state", "OPEN").containsEntry("base_version", 0L));
    }

    /** The release before the journal deleted it unrecorded; the gap repair recorded a deleted baseline. */
    @Test
    void aDeletedBaselineAfterTheBaseIsAServerSideChange() throws Exception {
        String uid = uid("cdGapDel");
        assertThat(answer(upload(G3, FIELD_1, formA(G3, uid, "first")))).isEqualTo(created(uid));
        seed.deletedUnrecorded(uid);

        assertThat(answer(upload(G3, FIELD_1, formA(G3, uid, "first")))).as("unchanged").isEqualTo(updated(uid));
        assertThat(answer(upload(G3, FIELD_1, formA(G3, uid, "changed on the phone")))).isEqualTo(updated(uid));

        assertThat(revisions(uid)).containsExactly("0 CREATE UPLOAD", "1 BASELINE GAP_REPAIR");
        assertThat(conflicts(uid)).singleElement().satisfies(conflict -> assertThat(conflict)
            .containsEntry("cause", "SERVER_SIDE_CHANGE").containsEntry("base_version", 0L));
    }

    // ---------------------------------------------------------------- Reference values

    /**
     * After an admin delete a changed copy would be kept aside, but the Reference checks come first, against the copy's
     * own assignment, so their refusals stand as today: an entry twice in a field (E4118) and an entry of another org
     * unit (E4119), for field1, whose base it is, and for an admin, who has none. A copy that passes them is kept aside
     * and creates no entry.
     */
    @Test
    void referenceRefusalsStandBeforeACopyIsKeptAside() throws Exception {
        String uid = uid("cdRefs");
        String reference = version("FORM_R.v1");
        assertThat(answer(upload(G3, FIELD_1, item(G3, uid, FORM_R, reference, ASSIGN_T1_V1,
            households(G3, uid, HOUSE_V1))))).isEqualTo(created(uid));
        assertThat(adminDelete(uid).getStatus()).isEqualTo(204);

        for (String uploader : List.of(FIELD_1, ADMIN)) {
            assertThat(answer(upload(G3, uploader, item(G3, uid, FORM_R, reference, ASSIGN_T1_V1,
                households(G3, uid, HOUSE_V1, HOUSE_V1)))))
                .as(uploader + ", an entry twice in a field")
                .startsWith("200 created=[] updated=[] failed={\"" + uid + "\":\"E4118: ");
            assertThat(answer(upload(G3, uploader, item(G3, uid, FORM_R, reference, ASSIGN_T1_V1,
                households(G3, uid, HOUSE_V2)))))
                .as(uploader + ", an entry of another org unit")
                .startsWith("200 created=[] updated=[] failed={\"" + uid + "\":\"E4119: ");
        }
        assertThat(conflicts(uid)).isEmpty();

        String newHouse = "a2HouseCdK1";
        assertThat(answer(upload(G3, FIELD_1, item(G3, uid, FORM_R, reference, ASSIGN_T1_V1,
            households(G3, uid, HOUSE_V1, newHouse)).definitions(newHouse, "Echo Foxtrot Golf Hotel"))))
            .isEqualTo(updated(uid));
        assertThat(conflicts(uid)).singleElement().satisfies(conflict -> assertThat(conflict)
            .containsEntry("cause", "SERVER_SIDE_CHANGE").containsEntry("uploader_user_id", seed.userId(FIELD_1)));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM reference_entry WHERE uid = :uid",
            Map.of("uid", newHouse), Integer.class)).as("no entry until the copy is accepted").isZero();
        assertThat(revisions(uid)).containsExactly("0 CREATE UPLOAD", "1 DELETE ADMIN_DELETE");
    }

    // ---------------------------------------------------------------- after a move

    /**
     * Moved to team three's village, out of field1's reach: their unchanged re-sends, which the stored-record check
     * refuses today (E4124), are answered updated and write nothing; repeated, they open nothing.
     */
    @ParameterizedTest(name = "{0}")
    @EnumSource(value = Group.class, names = {"G3", "G0"})
    void unchangedReSendsAfterAMoveWriteNothingAndOpenNothing(Group group) throws Exception {
        String uid = uid("cdMvSm" + group.name());
        assertThat(answer(upload(group, FIELD_1, formA(group, uid, "first")))).isEqualTo(created(uid));
        seed.move(uid, ADMIN, ASSIGN_T3_V3);

        for (int i = 0; i < 3; i++) {
            assertThat(answer(upload(group, FIELD_1, formA(group, uid, "first")))).isEqualTo(updated(uid));
        }

        assertThat(revisions(uid)).containsExactly("0 CREATE " + command(group), "1 UPDATE MOVE");
        assertThat(conflicts(uid)).isEmpty();
        assertThat(reconciliation(uid)).isEmpty();
    }

    /** Moved out of field1's reach: a changed copy, refused today with E4124, is kept aside instead. */
    @ParameterizedTest(name = "{0}")
    @EnumSource(value = Group.class, names = {"G3", "G0"})
    void afterAMoveAChangedCopyRefusedForAccessIsKeptAside(Group group) throws Exception {
        String uid = uid("cdMvKp" + group.name());
        assertThat(answer(upload(group, FIELD_1, formA(group, uid, "first")))).isEqualTo(created(uid));
        seed.move(uid, ADMIN, ASSIGN_T3_V3);

        for (int i = 0; i < 2; i++) {
            assertThat(answer(upload(group, FIELD_1, formA(group, uid, "changed on the phone"))))
                .isEqualTo(updated(uid));
        }

        assertThat(revisions(uid)).containsExactly("0 CREATE " + command(group), "1 UPDATE MOVE");
        assertThat(conflicts(uid)).singleElement().satisfies(conflict -> assertThat(conflict)
            .containsEntry("state", "OPEN").containsEntry("cause", "SERVER_SIDE_CHANGE")
            .containsEntry("base_version", 0L).containsEntry("rows_lack_ids", group == G0)
            .containsEntry("resend_count", 1));
    }

    /**
     * Moved within field1's reach: an unchanged re-send no longer moves it back, and a changed copy passes the checks
     * and is kept aside, as after any server-side change.
     */
    @Test
    void aMoveWithinTheUploadersReachIsAServerSideChange() throws Exception {
        String uid = uid("cdMvReach");
        assertThat(answer(upload(G3, FIELD_1, formA(G3, uid, "first")))).isEqualTo(created(uid));
        seed.move(uid, ADMIN, ASSIGN_T1_V2);

        assertThat(answer(upload(G3, FIELD_1, formA(G3, uid, "first")))).as("unchanged").isEqualTo(updated(uid));
        assertThat(answer(upload(G3, FIELD_1, formA(G3, uid, "changed on the phone")))).isEqualTo(updated(uid));

        assertThat(revisions(uid)).containsExactly("0 CREATE UPLOAD", "1 UPDATE MOVE");
        assertThat(jdbc.queryForObject("SELECT assignment_uid FROM data_submission WHERE uid = :uid",
            Map.of("uid", uid), String.class)).isEqualTo(ASSIGN_T1_V2);
        assertThat(conflicts(uid)).singleElement().satisfies(conflict -> assertThat(conflict)
            .containsEntry("cause", "SERVER_SIDE_CHANGE").containsEntry("base_version", 0L));
    }

    /** After a move, a refusal that is not about access (E4125: field1 may not delete) stays a refusal. */
    @Test
    void afterAMoveAnyOtherRefusalStaysARefusal() throws Exception {
        String uid = uid("cdMvOther");
        assertThat(answer(upload(G3, FIELD_1, formA(G3, uid, "first")))).isEqualTo(created(uid));
        seed.move(uid, ADMIN, ASSIGN_T1_V2);
        Item deleting = formA(G3, uid, "first");
        deleting.node.put("deleted", true);

        assertThat(answer(upload(G3, FIELD_1, deleting)))
            .startsWith("200 created=[] updated=[] failed={\"" + uid + "\":\"E4125: ");

        assertThat(conflicts(uid)).isEmpty();
        assertThat(revisions(uid)).containsExactly("0 CREATE UPLOAD", "1 UPDATE MOVE");
    }

    /** Without a move since the base, an access refusal (E4114: a copy naming team three's village) stands. */
    @Test
    void withoutAMoveAnAccessRefusalStaysARefusal() throws Exception {
        String uid = uid("cdNoMove");
        assertThat(answer(upload(G3, FIELD_1, formA(G3, uid, "first")))).isEqualTo(created(uid));
        seed.edit(uid, ADMIN, "edited on the server");
        Item elsewhere = item(G3, uid, FORM_A, version("FORM_A.v1"), ASSIGN_T3_V3,
            ordinary(G3, uid, "changed on the phone", "r1", "s1", 1));

        assertThat(answer(upload(G3, FIELD_1, elsewhere)))
            .startsWith("200 created=[] updated=[] failed={\"" + uid + "\":\"E4114: ");

        assertThat(conflicts(uid)).isEmpty();
        assertThat(revisions(uid)).containsExactly("0 CREATE UPLOAD", "1 UPDATE EDIT");
    }

    // ---------------------------------------------------------------- deleted before the journal

    /**
     * Deleted before the journal began, its first revision a deleted baseline, which is a server-side change for every
     * uploader: a changed copy from its creator is kept aside instead of landing on the deleted submission.
     */
    @Test
    void aChangedCopyOfASubmissionDeletedBeforeTheJournalIsKeptAside() throws Exception {
        String uid = uid("cdPreDel");
        seed.storedBeforeTheJournal(uid, FIELD_1, ASSIGN_T1_V1, version("FORM_A.v1"),
            formA(G3, uid, "first").node.get("formData").toString(), true);

        assertThat(answer(upload(G3, FIELD_1, formA(G3, uid, "changed on the phone")))).isEqualTo(updated(uid));

        assertThat(revisions(uid)).containsExactly("3 BASELINE MIGRATION");
        assertThat(conflicts(uid)).singleElement().satisfies(conflict -> assertThat(conflict)
            .containsEntry("cause", "SERVER_SIDE_CHANGE").containsEntry("base_version", 3L));
    }

    /**
     * Apps 5.2 and 5.3 re-send it unchanged, rows without ids: today each re-send wrote a revision with new row ids.
     * The first is kept aside, and the next ones only count on the open conflict.
     */
    @Test
    void unchangedG0ReSendsOfASubmissionDeletedBeforeTheJournalOnlyCount() throws Exception {
        String uid = uid("cdPreDelG0");
        seed.storedBeforeTheJournal(uid, FIELD_1, ASSIGN_T1_V1, version("FORM_A.v1"),
            formA(G1, uid, "first").node.get("formData").toString(), true);

        for (int i = 0; i < 3; i++) {
            assertThat(answer(upload(G0, FIELD_1, formA(G0, uid, "first")))).isEqualTo(updated(uid));
        }

        assertThat(revisions(uid)).containsExactly("3 BASELINE MIGRATION");
        assertThat(conflicts(uid)).singleElement().satisfies(conflict -> assertThat(conflict)
            .containsEntry("state", "OPEN").containsEntry("rows_lack_ids", true).containsEntry("resend_count", 2)
            .containsEntry("version", 0L));
    }

    // ---------------------------------------------------------------- other uploaders

    /**
     * A submission older than the journal, made by field2: field1 (same team, no edit-from-users) has no base of
     * their own, so the upload checks run first and refuse the copy as today; it never enters the queue, even after a
     * server-side change. Its creator's baseline is their base.
     */
    @Test
    void aForeignUploadersCopyOfASubmissionOlderThanTheJournalIsRefusedAsTodayAndNeverQueued() throws Exception {
        String uid = uid("cdForeign");
        seed.storedBeforeTheJournal(uid, FIELD_2, ASSIGN_T1_V1, version("FORM_A.v1"),
            formA(G3, uid, "first").node.get("formData").toString());
        seed.edit(uid, ADMIN, "edited on the server");

        assertThat(answer(upload(G3, FIELD_1, formA(G3, uid, "changed by another user"))))
            .startsWith("200 created=[] updated=[] failed={\"" + uid + "\":\"E4126: ");
        assertThat(answer(upload(G3, FIELD_1, formA(G3, uid, "edited on the server"))))
            .as("refused before any shortcut, though equal to the current state")
            .startsWith("200 created=[] updated=[] failed={\"" + uid + "\":\"E4126: ");
        assertThat(conflicts(uid)).isEmpty();

        assertThat(answer(upload(G3, FIELD_2, formA(G3, uid, "first")))).as("the creator, unchanged")
            .isEqualTo(updated(uid));
        assertThat(answer(upload(G3, FIELD_2, formA(G3, uid, "changed by the creator"))))
            .isEqualTo(updated(uid));
        assertThat(conflicts(uid)).singleElement().satisfies(conflict -> assertThat(conflict)
            .containsEntry("uploader_user_id", seed.userId(FIELD_2)).containsEntry("base_version", 3L));
        assertThat(revisions(uid)).containsExactly("3 BASELINE MIGRATION", "4 UPDATE EDIT");
    }

    /**
     * The editor may change team one's submissions: authorized, with no base of their own, their base is the first
     * revision.
     */
    @Test
    void anAuthorizedUploaderWithoutABaseOfTheirOwnTakesTheFirstRevision() throws Exception {
        String quiet = uid("cdEdQuiet");
        assertThat(answer(upload(G3, FIELD_1, formA(G3, quiet, "first")))).isEqualTo(created(quiet));
        assertThat(answer(upload(G3, EDITOR, formA(G3, quiet, "the editor's copy")))).isEqualTo(updated(quiet));
        assertThat(revisions(quiet)).as("saved as today").containsExactly("0 CREATE UPLOAD", "1 UPDATE UPLOAD");

        String edited = uid("cdEdEdited");
        assertThat(answer(upload(G3, FIELD_1, formA(G3, edited, "first")))).isEqualTo(created(edited));
        seed.edit(edited, ADMIN, "edited on the server");
        assertThat(answer(upload(G3, EDITOR, formA(G3, edited, "the editor's copy")))).isEqualTo(updated(edited));
        assertThat(revisions(edited)).containsExactly("0 CREATE UPLOAD", "1 UPDATE EDIT");
        assertThat(conflicts(edited)).singleElement().satisfies(conflict -> assertThat(conflict)
            .containsEntry("uploader_user_id", seed.userId(EDITOR)).containsEntry("base_version", 0L));
    }

    /**
     * The editor has no base of their own: their copy equal to the first revision's state is not an unchanged re-send
     * of theirs. After the creator changed the submission, and with no server-side change, it applies as today.
     */
    @Test
    void anAuthorizedUploadersCopyEqualToTheFirstRevisionAppliesAsTodayWithoutAServerSideChange() throws Exception {
        String uid = uid("cdEdFirst");
        assertThat(answer(upload(G3, FIELD_1, formA(G3, uid, "first")))).isEqualTo(created(uid));
        assertThat(answer(upload(G3, FIELD_1, formA(G3, uid, "changed by the creator")))).isEqualTo(updated(uid));

        assertThat(answer(upload(G3, EDITOR, formA(G3, uid, "first")))).isEqualTo(updated(uid));

        assertThat(revisions(uid)).containsExactly("0 CREATE UPLOAD", "1 UPDATE UPLOAD", "2 UPDATE UPLOAD");
        assertThat(jdbc.queryForObject("SELECT form_data #>> '{main,a2_name}' FROM data_submission WHERE uid = :uid",
            Map.of("uid", uid), String.class)).isEqualTo("first");
        assertThat(conflicts(uid)).isEmpty();
    }

    /** The same copy after a server-side change since the first revision is kept aside, with that revision as base. */
    @Test
    void anAuthorizedUploadersCopyEqualToTheFirstRevisionIsKeptAsideAfterAServerSideChange() throws Exception {
        String uid = uid("cdEdFirsK");
        assertThat(answer(upload(G3, FIELD_1, formA(G3, uid, "first")))).isEqualTo(created(uid));
        seed.edit(uid, ADMIN, "edited on the server");

        assertThat(answer(upload(G3, EDITOR, formA(G3, uid, "first")))).isEqualTo(updated(uid));

        assertThat(revisions(uid)).containsExactly("0 CREATE UPLOAD", "1 UPDATE EDIT");
        assertThat(conflicts(uid)).singleElement().satisfies(conflict -> assertThat(conflict)
            .containsEntry("uploader_user_id", seed.userId(EDITOR)).containsEntry("base_version", 0L)
            .containsEntry("cause", "SERVER_SIDE_CHANGE"));
    }

    // ---------------------------------------------------------------- never fail, and what is not detected

    /**
     * Detection's read fails in the database, which aborts the transaction: its savepoint is rolled back, the copy is
     * kept aside with DETECTION_ERROR and the tag it was given, the item is answered updated, and the conflict
     * commits.
     */
    @Test
    void aDetectionErrorKeepsTheCopyAsideAndNeverFailsTheItem() throws Exception {
        String uid = uid("cdError");
        assertThat(answer(upload(G3, FIELD_1, formA(G3, uid, "first")))).isEqualTo(created(uid));
        doAnswer(invocation -> jdbc.getJdbcTemplate().queryForObject("SELECT 1 / 0", Integer.class))
            .when(journalReader).current(anyString());

        assertThat(answer(upload(G3, FIELD_1, formA(G3, uid, "changed on the phone")))).isEqualTo(updated(uid));

        assertThat(conflicts(uid)).singleElement().satisfies(conflict -> assertThat(conflict)
            .containsEntry("cause", "DETECTION_ERROR").containsEntry("state", "OPEN")
            .containsEntry("base_version", null).containsEntry("rows_lack_ids", false));
        assertThat(revisions(uid)).containsExactly("0 CREATE UPLOAD");
    }

    @Test
    void practiceUploadsAreNotDetected() throws Exception {
        String uid = uid("cdPractice");
        assertThat(answer(upload(G3, FIELD_1, formA(G3, uid, "first")))).isEqualTo(created(uid));
        seed.edit(uid, ADMIN, "edited on the server");

        MockHttpServletRequestBuilder practice = post(A2Payloads.BULK_PRACTICE)
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer " + A2Tokens.token(jwtKey, FIELD_1))
            .content(json.writeValueAsString(A2Payloads.body(formA(G3, uid, "changed in practice"))));
        G3.headers().forEach(practice::header);
        assertThat(answer(mvc.perform(practice).andReturn().getResponse())).isEqualTo(updated(uid));

        assertThat(conflicts(uid)).isEmpty();
        assertThat(revisions(uid)).containsExactly("0 CREATE UPLOAD", "1 UPDATE EDIT");
    }

    // ---------------------------------------------------------------- the staging rehearsal

    /**
     * The staging rehearsal ({@link ConflictRehearsal}) on these scenarios' kind of data, through its own connections:
     * copies uploaded in three app generations' shapes, one changed by the editor, one changed in {@code _status}
     * alone, one older than the journal, and some changed on the server since. Re-sent unchanged, none is kept aside
     * and none fails; every changed copy after a seeded server-side change is kept aside, tagged when its rows lack
     * ids; the change of {@code _status} alone is found; the report holds counts only, and the database is left as it
     * was.
     */
    @Test
    @Timeout(120)
    void theRehearsalFindsNoConflictInUnchangedReSendsAndKeepsEveryChangedCopyAside() throws Exception {
        String value = "rhValue";
        for (Group group : List.of(G3, G1, G0)) {
            String uid = uid("rh" + group.name());
            assertThat(answer(upload(group, FIELD_1, formA(group, uid, value)))).isEqualTo(created(uid));
        }
        String edited = uid("rhEdited");
        assertThat(answer(upload(G3, FIELD_1, formA(G3, edited, value)))).isEqualTo(created(edited));
        seed.edit(edited, ADMIN, value + " on the server");
        String moved = uid("rhMoved");
        assertThat(answer(upload(G3, FIELD_1, formA(G3, moved, value)))).isEqualTo(created(moved));
        seed.move(moved, ADMIN, ASSIGN_T3_V3);
        String deleted = uid("rhDeleted");
        assertThat(answer(upload(G3, FIELD_1, formA(G3, deleted, value)))).isEqualTo(created(deleted));
        assertThat(adminDelete(deleted).getStatus()).isEqualTo(204);
        String byEditor = uid("rhEditor");
        assertThat(answer(upload(G3, FIELD_1, formA(G3, byEditor, value)))).isEqualTo(created(byEditor));
        assertThat(answer(upload(G3, EDITOR, formA(G3, byEditor, value + " by the editor"))))
            .isEqualTo(updated(byEditor));
        String status = uid("rhStatus");
        Item before = formA(G3, status, value);
        ((ObjectNode) before.node.get("formData")).put("_status", "rhStatusOne");
        Item after = formA(G3, status, value);
        ((ObjectNode) after.node.get("formData")).put("_status", "rhStatusTwo");
        assertThat(answer(upload(G3, FIELD_1, before))).isEqualTo(created(status));
        assertThat(answer(upload(G3, FIELD_1, after))).isEqualTo(updated(status));
        String older = uid("rhOlder");
        seed.storedBeforeTheJournal(older, FIELD_2, ASSIGN_T1_V1, version("FORM_A.v1"),
            formA(G3, older, value).node.get("formData").toString());
        String footprint = footprint();

        HikariDataSource pool = dataSource.unwrap(HikariDataSource.class);
        List<String> report;
        try (ConflictRehearsal rehearsal = new ConflictRehearsal(new ConflictRehearsal.Settings(pool.getJdbcUrl(),
            pool.getUsername(), pool.getPassword(), null, 100, 2, "s2", "a2rh%"))) {
            report = rehearsal.run();

            assertThat(rehearsal.unchanged.sorted()).as("nine copies, two shapes each").containsOnly(
                Map.entry("apps 6.0 and later (rows with ids), changes nothing, before the checks", 9L),
                Map.entry("apps 5.2 and 5.3 (rows without ids), changes nothing, before the checks", 9L));
            assertThat(rehearsal.unchangedConflicts()).isZero();
            assertThat(rehearsal.unchangedFailures()).isZero();
            assertThat(rehearsal.holders.sorted()).containsOnly(Map.entry("creator", 1L), Map.entry("upload", 8L));

            assertThat(rehearsal.changedNotKeptAside()).isZero();
            assertThat(rehearsal.changed.sorted()).as("every seeded kind, both shapes").containsKeys(
                "EDIT, apps 6.0 and later (rows with ids), upload, kept aside, SERVER_SIDE_CHANGE",
                "EDIT, apps 5.2 and 5.3 (rows without ids), upload, kept aside, SERVER_SIDE_CHANGE, rows lack ids",
                "MOVE, apps 6.0 and later (rows with ids), upload, refused for access (E4124), kept aside, "
                    + "SERVER_SIDE_CHANGE",
                "DELETE, apps 6.0 and later (rows with ids), upload, kept aside, SERVER_SIDE_CHANGE");
            assertThat(rehearsal.changed.sum("")).isGreaterThanOrEqualTo(18);

            assertThat(rehearsal.status.get("history: consecutive states, only _status differs")).isEqualTo(1);
            assertThat(rehearsal.status.get("history: submissions whose latest change differs only in _status"))
                .isEqualTo(1);
            assertThat(rehearsal.status.get("heads with a top-level _status")).isEqualTo(1);
            assertThat(rehearsal.status.get("heads whose re-sent copy differs from the stored document only in "
                + "_status")).isZero();
        }
        assertThat(String.join("\n", report)).as("counts only")
            .doesNotContain("a2rh", "a2-", value, "rhStatus");
        assertThat(footprint()).as("the database is as it was").isEqualTo(footprint);
    }

    /** What the rehearsal could change: its scope's rows and versions, revisions, outbox rows and conflicts. */
    private String footprint() {
        return jdbc.queryForObject("""
                SELECT count(*) || ' rows, versions ' || coalesce(sum(s.lock_version), 0) || ', deleted '
                       || count(*) FILTER (WHERE s.deleted) || ', documents ' || md5(string_agg(s.form_data::text,
                       '|' ORDER BY s.id)) || ', revisions '
                       || (SELECT count(*) FROM journal.submission_revision r WHERE r.submission_id IN
                              (SELECT id FROM data_submission WHERE uid LIKE 'a2rh%')) || ', outbox '
                       || (SELECT count(*) FROM outbox WHERE submission_uid LIKE 'a2rh%') || ', conflicts '
                       || (SELECT count(*) FROM submission_review.conflict c WHERE c.submission_id IN
                              (SELECT id FROM data_submission WHERE uid LIKE 'a2rh%'))
                FROM data_submission s WHERE s.uid LIKE 'a2rh%'""",
            Map.of(), String.class);
    }

    // ---------------------------------------------------------------- helpers

    private static Item formA(Group group, String uid, String name) {
        return item(group, uid, FORM_A, version("FORM_A.v1"), ASSIGN_T1_V1, ordinary(group, uid, name, "r1", "s1", 1));
    }

    private static String command(Group group) {
        return group.versioned() ? "UPLOAD" : "LEGACY_UPLOAD";
    }

    private MockHttpServletResponse upload(Group group, String login, Item... items) throws Exception {
        MockHttpServletRequestBuilder request = post(group.route())
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer " + A2Tokens.token(jwtKey, login))
            .content(json.writeValueAsString(A2Payloads.body(items)));
        group.headers().forEach(request::header);
        return mvc.perform(request).andReturn().getResponse();
    }

    private MockHttpServletResponse adminDelete(String uid) throws Exception {
        return mvc.perform(delete("/api/v1/dataSubmission/" + uid)
                .header("Authorization", "Bearer " + A2Tokens.token(jwtKey, ADMIN)))
            .andReturn().getResponse();
    }

    /** What the phone applies: the status and the three lists. */
    private String answer(MockHttpServletResponse response) throws Exception {
        JsonNode answer = json.readTree(response.getContentAsString(StandardCharsets.UTF_8));
        return response.getStatus() + " created=" + answer.path("created") + " updated=" + answer.path("updated")
            + " failed=" + answer.path("failed");
    }

    private static String created(String uid) {
        return "200 created=[\"" + uid + "\"] updated=[] failed={}";
    }

    private static String updated(String uid) {
        return "200 created=[] updated=[\"" + uid + "\"] failed={}";
    }

    private List<String> revisions(String uid) {
        return jdbc.query("""
                SELECT r.version, r.kind, r.command FROM journal.submission_revision r
                JOIN data_submission s ON s.id = r.submission_id
                WHERE s.uid = :uid ORDER BY r.version""",
            Map.of("uid", uid),
            (result, n) -> result.getLong("version") + " " + result.getString("kind") + " "
                + result.getString("command"));
    }

    private List<Map<String, Object>> conflicts(String uid) {
        return jdbc.queryForList("""
                SELECT c.* FROM submission_review.conflict c JOIN data_submission s ON s.id = c.submission_id
                WHERE s.uid = :uid ORDER BY c.first_received_at, c.id""",
            Map.of("uid", uid));
    }

    private String conflictId(String uid) {
        return (String) conflicts(uid).stream().filter(conflict -> "OPEN".equals(conflict.get("state")))
            .findFirst().orElseThrow().get("id");
    }

    private List<String> reconciliation(String uid) {
        return JournalReconciliation.findings(jdbc, "s.uid = :uid", Map.of("uid", uid));
    }
}
