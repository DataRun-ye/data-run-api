package org.nmcpye.datarun.characterization.a2;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.nmcpye.datarun.characterization.a2.A2Payloads.Group;
import org.nmcpye.datarun.characterization.a2.A2Payloads.Item;

import java.util.Map;

import static org.nmcpye.datarun.characterization.a2.A2Payloads.Group.G1;
import static org.nmcpye.datarun.characterization.a2.A2Payloads.Group.G3;
import static org.nmcpye.datarun.characterization.a2.A2Payloads.NODES;
import static org.nmcpye.datarun.characterization.a2.A2Payloads.item;
import static org.nmcpye.datarun.characterization.a2.A2Payloads.ordinary;
import static org.nmcpye.datarun.characterization.a2.A2Payloads.uid;
import static org.nmcpye.datarun.characterization.a2.A2Seed.ADMIN;
import static org.nmcpye.datarun.characterization.a2.A2Seed.ASSIGN_T1_V1;
import static org.nmcpye.datarun.characterization.a2.A2Seed.ASSIGN_T1_V5;
import static org.nmcpye.datarun.characterization.a2.A2Seed.ASSIGN_T5_V1;
import static org.nmcpye.datarun.characterization.a2.A2Seed.DELETER;
import static org.nmcpye.datarun.characterization.a2.A2Seed.FIELD_1;
import static org.nmcpye.datarun.characterization.a2.A2Seed.FORM_A;
import static org.nmcpye.datarun.characterization.a2.A2Seed.VILLAGE_5;
import static org.nmcpye.datarun.characterization.a2.A2Seed.VILLAGE_5_NAME;
import static org.nmcpye.datarun.characterization.a2.A2Seed.version;

/**
 * Tier 1, J: the change record (step C). Each scenario's snapshots include the journal: one revision per change that
 * moved the submission's version, naming the command, the actor (a user id, the admin for a release and a delete),
 * the uploader a release acts for, its note, and the state after the change. The outbox row that announces each
 * revision carries the same version.
 */
class A2RevisionsTier1IT extends A2GoldenSupport {

    private static final String FUTURE = "2099-01-01T00:00:00Z";
    private static final String RELEASE = "/api/v1/admin/heldSubmissions/release";

    private static Item formA(Group group, String uid, String name, String assignment) {
        return item(group, uid, FORM_A, version("FORM_A.v1"), assignment, ordinary(group, uid, name, "r1", "s1", 1));
    }

    private static Item formA(String uid, String name) {
        return formA(G3, uid, name, ASSIGN_T1_V1);
    }

    @Test
    void j_01_anUploadCreatesVersionZero() {
        String uid = uid("j01");
        scenario("J-01", "An upload: CREATE at version 0 by the uploader, announced as SAVE 0")
            .withRevisions()
            .upload("field1 creates", G3, FIELD_1, formA(uid, "first"))
            .verify();
    }

    @Test
    void j_02_aChangedResendIsTheNextVersionAndAnUnchangedOneNothing() {
        String uid = uid("j02");
        scenario("J-02", "A changed re-send: UPDATE at version 1; the same re-send again: no revision, no event")
            .withRevisions()
            .upload("field1 creates", G3, FIELD_1, formA(uid, "first"))
            .upload("field1 re-sends changed", G3, FIELD_1, formA(uid, "changed"))
            .upload("field1 re-sends the same again", G3, FIELD_1, formA(uid, "changed"))
            .verify();
    }

    @Test
    void j_03_anUploadThatDeletesIsADeleteByTheUploader() {
        String uid = uid("j03");
        scenario("J-03", "deleted=true from a user with a delete grant: DELETE at version 1 by that user")
            .withRevisions()
            .upload("the deleter creates", G3, DELETER, formA(G3, uid, "first", ASSIGN_T5_V1))
            .upload("the deleter sends deleted=true", G3, DELETER,
                formA(G3, uid, "first", ASSIGN_T5_V1).set("deleted", true))
            .upload("and again", G3, DELETER, formA(G3, uid, "first", ASSIGN_T5_V1).set("deleted", true))
            .verify();
    }

    @Test
    void j_04_anAdminDeleteIsADeleteByTheAdmin() {
        String uid = uid("j04");
        scenario("J-04", "An admin delete: DELETE at version 1 by the admin, announced as DELETE 1")
            .withRevisions()
            .upload("field1 creates", G3, FIELD_1, formA(uid, "first"))
            .call("admin deletes", "DELETE", "/api/v1/dataSubmission/" + uid, ADMIN, null)
            .call("admin deletes again: nothing", "DELETE", "/api/v1/dataSubmission/" + uid, ADMIN, null)
            .verify();
    }

    @Test
    void j_05_aReleaseIsTheAdminActingForTheUploader() {
        String uid = uid("j05");
        ObjectNode release = NODES.objectNode();
        release.set("uids", NODES.arrayNode().add(uid));
        release.put("note", "area went live");
        scenario("J-05", "A held upload has no revision; its release is CREATE by the admin on behalf of the uploader, "
            + "with the note; the uploader's later re-send is theirs")
            .withRevisions()
            .goLive("campaign", null, null, FUTURE)
            .upload("held before go-live", G3, FIELD_1, formA(uid, "first"))
            .call("admin releases it with a note", "POST", RELEASE, ADMIN, release)
            .upload("field1 re-sends changed (still before go-live, now real data)", G3, FIELD_1,
                formA(uid, "changed"))
            .verify();
    }

    @Test
    void j_06_aLegacyUploadIsRecordedAsSuch() {
        String uid = uid("j06");
        scenario("J-06", "An upload on the legacy route: CREATE by the uploader, command LEGACY_UPLOAD")
            .withRevisions()
            .upload("field1 creates (G1)", G1, FIELD_1, formA(G1, uid, "first", ASSIGN_T1_V1))
            .upload("field1 re-sends changed (G1)", G1, FIELD_1, formA(G1, uid, "changed", ASSIGN_T1_V1))
            .verify();
    }

    @Test
    void j_07_practiceAndHeldUploadsWriteNoRevision() {
        scenario("J-07", "A practice upload and a held upload are checked and saved, then rolled back: no revision")
            .withRevisions()
            .practice("practice", FIELD_1, formA(uid("j07a"), "practice"))
            .goLive("campaign", null, null, FUTURE)
            .upload("held before go-live", G3, FIELD_1, formA(uid("j07b"), "held"))
            .verify();
    }

    @Test
    void j_08_aResendOntoADeletedSubmissionIsAnUpdateAnnouncedAsDelete() {
        String uid = uid("j08");
        scenario("J-08", "A changed re-send onto a deleted submission: UPDATE at version 2, still deleted, announced as "
            + "DELETE 2")
            .withRevisions()
            .upload("field1 creates", G3, FIELD_1, formA(uid, "first"))
            .call("admin deletes", "DELETE", "/api/v1/dataSubmission/" + uid, ADMIN, null)
            .upload("field1 re-sends changed", G3, FIELD_1, formA(uid, "changed"))
            .verify();
    }

    /** Whatever the write persists is a change: here only the labels copied from the renamed village. */
    @Test
    void j_09_aResendAfterARenameRecordsTheNewLabels() {
        String uid = uid("j09");
        try {
            scenario("J-09", "An unchanged re-send after the village was renamed: the copied name changes, so UPDATE "
                + "at version 1 with the new name")
                .withRevisions()
                .upload("field1 creates", G3, FIELD_1, formA(G3, uid, "first", ASSIGN_T1_V5))
                .sql("the village is renamed", "UPDATE org_unit SET name = :name WHERE uid = :uid",
                    Map.of("name", "a2 village 5 renamed", "uid", VILLAGE_5))
                .upload("re-send unchanged", G3, FIELD_1, formA(G3, uid, "first", ASSIGN_T1_V5))
                .verify();
        } finally {
            inTransaction(() -> jdbc.update("UPDATE org_unit SET name = :name WHERE uid = :uid",
                Map.of("name", VILLAGE_5_NAME, "uid", VILLAGE_5)));
        }
    }
}
