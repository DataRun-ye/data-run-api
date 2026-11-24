package org.nmcpye.datarun.characterization.a2;

import org.junit.jupiter.api.Test;
import org.nmcpye.datarun.characterization.a2.A2Payloads.Item;

import java.util.Map;

import static org.nmcpye.datarun.characterization.a2.A2Payloads.Group.G3;
import static org.nmcpye.datarun.characterization.a2.A2Payloads.body;
import static org.nmcpye.datarun.characterization.a2.A2Payloads.households;
import static org.nmcpye.datarun.characterization.a2.A2Payloads.item;
import static org.nmcpye.datarun.characterization.a2.A2Payloads.ordinary;
import static org.nmcpye.datarun.characterization.a2.A2Payloads.uid;
import static org.nmcpye.datarun.characterization.a2.A2Seed.ASSIGN_T1_V1;
import static org.nmcpye.datarun.characterization.a2.A2Seed.ASSIGN_T1_V2;
import static org.nmcpye.datarun.characterization.a2.A2Seed.ASSIGN_T1_V5;
import static org.nmcpye.datarun.characterization.a2.A2Seed.ASSIGN_T5_V1;
import static org.nmcpye.datarun.characterization.a2.A2Seed.DELETER;
import static org.nmcpye.datarun.characterization.a2.A2Seed.DISTRICT;
import static org.nmcpye.datarun.characterization.a2.A2Seed.EDITOR;
import static org.nmcpye.datarun.characterization.a2.A2Seed.FIELD_1;
import static org.nmcpye.datarun.characterization.a2.A2Seed.FORM_A;
import static org.nmcpye.datarun.characterization.a2.A2Seed.FORM_R;
import static org.nmcpye.datarun.characterization.a2.A2Seed.HOUSE_V1;
import static org.nmcpye.datarun.characterization.a2.A2Seed.VILLAGE_1;
import static org.nmcpye.datarun.characterization.a2.A2Seed.VILLAGE_5;
import static org.nmcpye.datarun.characterization.a2.A2Seed.VILLAGE_5_NAME;
import static org.nmcpye.datarun.characterization.a2.A2Seed.version;

/** Tier 2, W1 and W2: the rest of the versioned and practice routes. */
class A2VersionedUploadTier2IT extends A2GoldenSupport {

    private static final String PAST = "2020-01-01T00:00:00Z";
    private static final String FUTURE = "2099-01-01T00:00:00Z";
    /** After every scenario's phone start time (2026-11-20T05:00Z), before any run of these tests. */
    private static final String BETWEEN = "2026-11-21T00:00:00Z";

    private static Item formA(String uid, String name, String assignment) {
        return item(G3, uid, FORM_A, version("FORM_A.v1"), assignment, ordinary(G3, uid, name, "r1", "s1", 1));
    }

    @Test
    @PinnedBug(changeBy = "C1/D1",
        what = "an unchanged re-send after the village was renamed writes an UPDATE carrying the new name: a "
            + "server-side context change churns every record re-sent afterwards")
    void w1_06_unchangedResendAfterARename() {
        String uid = uid("w106");
        try {
            scenario("W1-06", "An unchanged re-send after the village was renamed writes an UPDATE")
                .upload("field1 creates", G3, FIELD_1, formA(uid, "first", ASSIGN_T1_V5))
                .sql("the village is renamed", "UPDATE org_unit SET name = :name WHERE uid = :uid",
                    Map.of("name", "a2 village 5 renamed", "uid", VILLAGE_5))
                .upload("re-send unchanged", G3, FIELD_1, formA(uid, "first", ASSIGN_T1_V5))
                .verify();
        } finally {
            inTransaction(() -> jdbc.update("UPDATE org_unit SET name = :name WHERE uid = :uid",
                Map.of("name", VILLAGE_5_NAME, "uid", VILLAGE_5)));
        }
    }

    @Test
    void w1_08_moveToAnotherAssignment() {
        String uid = uid("w108");
        scenario("W1-08", "A re-send on another assignment the user may submit on moves the record")
            .upload("field1 creates on village 1", G3, FIELD_1, formA(uid, "first", ASSIGN_T1_V1))
            .upload("re-send on village 2", G3, FIELD_1, formA(uid, "first", ASSIGN_T1_V2))
            .verify();
    }

    @Test
    void w1_09_sameUidTwiceInOneRequest() {
        String uid = uid("w109");
        scenario("W1-09", "The same uid twice in one request: created, then updated")
            .upload("one request, the uid twice", G3, FIELD_1,
                formA(uid, "first", ASSIGN_T1_V1), formA(uid, "second", ASSIGN_T1_V1))
            .verify();
    }

    @Test
    void w1_14_deleteThroughTheUpload() {
        String own = uid("w114a");
        String deleters = uid("w114b");
        scenario("W1-14", "deleted=true on one's own record: E4125 without a delete grant, a DELETE event with one")
            .upload("field1 creates", G3, FIELD_1, formA(own, "first", ASSIGN_T1_V1))
            .upload("field1 sends deleted=true", G3, FIELD_1, formA(own, "first", ASSIGN_T1_V1).set("deleted", true))
            .upload("the deleter creates", G3, DELETER, formA(deleters, "first", ASSIGN_T5_V1))
            .upload("the deleter sends deleted=true", G3, DELETER,
                formA(deleters, "first", ASSIGN_T5_V1).set("deleted", true))
            .verify();
    }

    @Test
    void w1_21a_startedBeforeReceivedAfter() {
        scenario("W1-21a", "Started before the go-live but received after it: held (the earlier time counts)")
            .goLive("campaign", null, null, BETWEEN)
            .upload("start 2026-11-20, received now", G3, FIELD_1, formA(uid("w121a"), "a", ASSIGN_T1_V1))
            .verify();
    }

    @Test
    void w1_21b_noStartTime() {
        scenario("W1-21b", "No start time: the receipt time counts, so it is saved")
            .goLive("campaign", null, null, BETWEEN)
            .upload("no startEntryTime", G3, FIELD_1,
                formA(uid("w121b"), "b", ASSIGN_T1_V1).remove("startEntryTime"))
            .verify();
    }

    @Test
    void w1_21c_nearestLevelWins() {
        scenario("W1-21c", "A district rule beats the campaign rule")
            .goLive("campaign", null, null, FUTURE)
            .goLive("district", DISTRICT, null, PAST)
            .upload("village 1 upload", G3, FIELD_1, formA(uid("w121c"), "c", ASSIGN_T1_V1))
            .verify();
    }

    @Test
    void w1_21d_formRuleBeatsEveryFormRuleAtOneLevel() {
        String reference = uid("w121e");
        scenario("W1-21d", "At one level, a rule for the form beats the rule for every form")
            .goLive("village every form", VILLAGE_1, null, FUTURE)
            .goLive("village FORM_A", VILLAGE_1, FORM_A, PAST)
            .upload("FORM_A upload", G3, FIELD_1, formA(uid("w121d"), "d", ASSIGN_T1_V1))
            .upload("FORM_R upload", G3, FIELD_1,
                item(G3, reference, FORM_R, version("FORM_R.v1"), ASSIGN_T1_V1, households(G3, reference, HOUSE_V1))
                    .definitions(HOUSE_V1, "House One"))
            .verify();
    }

    @Test
    void w1_27_managerOnAManagedTeamsAssignment() {
        String uid = uid("w127");
        scenario("W1-27", "A manager submits on a managed team's assignment with his own team's grant")
            .upload("editor creates on team one's assignment", G3, EDITOR, formA(uid, "first", ASSIGN_T1_V1))
            .verify();
    }

    @Test
    void w1_28_customAlias() {
        String uid = uid("w128");
        scenario("W1-28", "The /api/custom alias behaves as /api/v1")
            .call("new through /api/custom", "POST", "/api/custom/dataSubmission/bulk?referenceVersion=1", FIELD_1,
                G3.headers(), body(formA(uid, "first", ASSIGN_T1_V1)))
            .verify();
    }

    @Test
    void w2_02_practiceOnAStoredUid() {
        String uid = uid("w202");
        scenario("W2-02", "A practice upload of a stored uid: updated, the stored record untouched, held as PRACTICE")
            .upload("field1 creates", G3, FIELD_1, formA(uid, "first", ASSIGN_T1_V1))
            .practice("practice upload of the same uid", FIELD_1, formA(uid, "practice", ASSIGN_T1_V1))
            .verify();
    }

    @Test
    void w2_03_practiceResend() {
        String uid = uid("w203");
        scenario("W2-03", "A practice re-send refreshes the held practice upload")
            .practice("practice upload", FIELD_1, formA(uid, "first", ASSIGN_T1_V1))
            .practice("practice re-send changed", FIELD_1, formA(uid, "changed", ASSIGN_T1_V1))
            .verify();
    }

    @Test
    void w2_04_practiceRouteWithoutTheParameter() {
        scenario("W2-04", "The practice route without referenceVersion=1 answers 400")
            .call("no referenceVersion", "POST", "/api/v1/dataSubmission/bulk/practice", FIELD_1,
                body(formA(uid("w204"), "a", ASSIGN_T1_V1)))
            .verify();
    }
}
