package org.nmcpye.datarun.characterization.a2;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.nmcpye.datarun.characterization.a2.A2Payloads.Item;

import java.util.Map;

import static org.nmcpye.datarun.characterization.a2.A2Payloads.Group.G3;
import static org.nmcpye.datarun.characterization.a2.A2Payloads.NODES;
import static org.nmcpye.datarun.characterization.a2.A2Payloads.item;
import static org.nmcpye.datarun.characterization.a2.A2Payloads.ordinary;
import static org.nmcpye.datarun.characterization.a2.A2Payloads.uid;
import static org.nmcpye.datarun.characterization.a2.A2Seed.ADMIN;
import static org.nmcpye.datarun.characterization.a2.A2Seed.ASSIGN_T1_V1;
import static org.nmcpye.datarun.characterization.a2.A2Seed.ASSIGN_T4_V4;
import static org.nmcpye.datarun.characterization.a2.A2Seed.FIELD_1;
import static org.nmcpye.datarun.characterization.a2.A2Seed.FIELD_4;
import static org.nmcpye.datarun.characterization.a2.A2Seed.FORM_A;
import static org.nmcpye.datarun.characterization.a2.A2Seed.TEAM_FOUR;
import static org.nmcpye.datarun.characterization.a2.A2Seed.TEAM_FOUR_GRANTS;
import static org.nmcpye.datarun.characterization.a2.A2Seed.version;

/** Tier 1, W7 and W8: releasing and discarding held uploads. */
class A2HeldDecisionsTier1IT extends A2GoldenSupport {

    private static final String FUTURE = "2099-01-01T00:00:00Z";
    private static final String RELEASE = "/api/v1/admin/heldSubmissions/release";
    private static final String DISCARD = "/api/v1/admin/heldSubmissions/discard";

    private static Item formA(String uid, String name, String assignment) {
        return item(G3, uid, FORM_A, version("FORM_A.v1"), assignment, ordinary(G3, uid, name, "r1", "s1", 1));
    }

    private static ObjectNode uids(String... uids) {
        ObjectNode body = NODES.objectNode();
        body.set("uids", NODES.arrayNode().addAll(java.util.Arrays.stream(uids).map(NODES::textNode).toList()));
        return body;
    }

    @Test
    void w7_01_releaseByUid() {
        String uid = uid("w701");
        scenario("W7-01", "A release replays the held upload as its submitter and records the admin's decision")
            .goLive("campaign", null, null, FUTURE)
            .upload("held before go-live", G3, FIELD_1, formA(uid, "first", ASSIGN_T1_V1))
            .call("admin releases it", "POST", RELEASE, ADMIN, uids(uid))
            .verify();
    }

    @Test
    void w7_02_releaseOfAUidThatIsNowReal() {
        String uid = uid("w702");
        scenario("W7-02", "A held upload whose uid reached the real data meanwhile is superseded, not applied")
            .goLive("campaign", null, null, FUTURE)
            .upload("held before go-live", G3, FIELD_1, formA(uid, "first", ASSIGN_T1_V1))
            .sql("the go-live rule is removed", "DELETE FROM go_live_rule WHERE activity_uid = :activity",
                Map.of("activity", A2Seed.ACTIVITY))
            .upload("re-send changed, now saved", G3, FIELD_1, formA(uid, "changed", ASSIGN_T1_V1))
            .call("admin releases the held copy", "POST", RELEASE, ADMIN, uids(uid))
            .verify();
    }

    @Test
    void w7_03_releaseRefusedForTheSubmitter() {
        String uid = uid("w703");
        try {
            scenario("W7-03", "A release the submitter may no longer make fails, is recorded, and stays held")
                .goLive("campaign", null, null, FUTURE)
                .upload("field4 is held", G3, FIELD_4, formA(uid, "first", ASSIGN_T4_V4))
                .sql("team four loses its grant", "UPDATE team SET form_permissions = CAST('[]' AS jsonb) WHERE uid = :team",
                    Map.of("team", TEAM_FOUR))
                .call("admin releases it", "POST", RELEASE, ADMIN, uids(uid))
                .verify();
        } finally {
            inTransaction(() -> jdbc.update(
                "UPDATE team SET form_permissions = CAST(:grants AS jsonb) WHERE uid = :team",
                Map.of("grants", TEAM_FOUR_GRANTS, "team", TEAM_FOUR)));
        }
    }

    @Test
    void w7_07_fieldUserCannotRelease() {
        String uid = uid("w707");
        scenario("W7-07", "A field user's release is refused by the filter chain")
            .goLive("campaign", null, null, FUTURE)
            .upload("held before go-live", G3, FIELD_1, formA(uid, "first", ASSIGN_T1_V1))
            .call("field1 releases", "POST", RELEASE, FIELD_1, uids(uid))
            .verify();
    }

    @Test
    void w7_08_releaseOfAPracticeUploadByUid() {
        String uid = uid("w708");
        scenario("W7-08", "A practice upload released by uid is saved into the real data as its submitter")
            .practice("practice upload", FIELD_1, formA(uid, "practice", ASSIGN_T1_V1))
            .call("admin releases it by uid", "POST", RELEASE, ADMIN, uids(uid))
            .verify();
    }

    @Test
    void w7_09_notesAndUidsThatAreNotHeld() {
        String discarded = uid("w709a");
        String released = uid("w709b");
        String superseded = uid("w709c");
        ObjectNode discard = uids(discarded).put("note", "training day");
        ObjectNode release = uids(released, superseded, uid("w709z")).put("note", "district started early");
        scenario("W7-09", "Each decision records the admin's note; a uid that is not held is left out of the result")
            .goLive("campaign", null, null, FUTURE)
            .upload("three held before go-live", G3, FIELD_1, formA(discarded, "a", ASSIGN_T1_V1),
                formA(released, "b", ASSIGN_T1_V1), formA(superseded, "c", ASSIGN_T1_V1))
            .call("admin discards one, with a note", "POST", DISCARD, ADMIN, discard)
            .sql("the go-live rule is removed", "DELETE FROM go_live_rule WHERE activity_uid = :activity",
                Map.of("activity", A2Seed.ACTIVITY))
            .upload("one is re-sent and now saved", G3, FIELD_1, formA(superseded, "c changed", ASSIGN_T1_V1))
            .call("admin releases two held uids and one never held, with a note", "POST", RELEASE, ADMIN, release)
            .verify();
    }

    /** The release replays an upload as the login stored with it; that login must still load. */
    @Test
    void w7_10_releaseWhenTheSubmitterIsGone() {
        String uid = uid("w710");
        scenario("W7-10", "A release whose stored submitter is no longer a user fails, is recorded, and stays held")
            .goLive("campaign", null, null, FUTURE)
            .upload("held before go-live", G3, FIELD_1, formA(uid, "first", ASSIGN_T1_V1))
            .sql("the held upload names a login that does not exist",
                "UPDATE held_submission SET submitted_by = 'a2-ghost' WHERE uid = :uid", Map.of("uid", uid))
            .call("admin releases it", "POST", RELEASE, ADMIN, uids(uid))
            .verify();
    }

    @Test
    void w8_01_discardThenReleaseByUid() {
        String uid = uid("w801");
        scenario("W8-01", "A discard labels the upload; a release by uid still applies it")
            .goLive("campaign", null, null, FUTURE)
            .upload("held before go-live", G3, FIELD_1, formA(uid, "first", ASSIGN_T1_V1))
            .call("admin discards it", "POST", DISCARD, ADMIN, uids(uid))
            .call("admin releases it by uid", "POST", RELEASE, ADMIN, uids(uid))
            .verify();
    }
}
