package org.nmcpye.datarun.characterization.a2;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.nmcpye.datarun.characterization.a2.A2Payloads.Item;

import java.util.List;
import java.util.Map;

import static org.nmcpye.datarun.characterization.a2.A2Payloads.BULK_VERSIONED;
import static org.nmcpye.datarun.characterization.a2.A2Payloads.FINISH_LATER;
import static org.nmcpye.datarun.characterization.a2.A2Payloads.Group.G0;
import static org.nmcpye.datarun.characterization.a2.A2Payloads.Group.G2;
import static org.nmcpye.datarun.characterization.a2.A2Payloads.Group.G3;
import static org.nmcpye.datarun.characterization.a2.A2Payloads.NODES;
import static org.nmcpye.datarun.characterization.a2.A2Payloads.body;
import static org.nmcpye.datarun.characterization.a2.A2Payloads.households;
import static org.nmcpye.datarun.characterization.a2.A2Payloads.item;
import static org.nmcpye.datarun.characterization.a2.A2Payloads.note;
import static org.nmcpye.datarun.characterization.a2.A2Payloads.ordinary;
import static org.nmcpye.datarun.characterization.a2.A2Payloads.rowId;
import static org.nmcpye.datarun.characterization.a2.A2Payloads.uid;
import static org.nmcpye.datarun.characterization.a2.A2Seed.ADMIN;
import static org.nmcpye.datarun.characterization.a2.A2Seed.ASSIGN_DELETED;
import static org.nmcpye.datarun.characterization.a2.A2Seed.ASSIGN_T1_V1;
import static org.nmcpye.datarun.characterization.a2.A2Seed.ASSIGN_T3_V3;
import static org.nmcpye.datarun.characterization.a2.A2Seed.EDITOR;
import static org.nmcpye.datarun.characterization.a2.A2Seed.FIELD_1;
import static org.nmcpye.datarun.characterization.a2.A2Seed.FIELD_2;
import static org.nmcpye.datarun.characterization.a2.A2Seed.FIELD_3;
import static org.nmcpye.datarun.characterization.a2.A2Seed.FORM_A;
import static org.nmcpye.datarun.characterization.a2.A2Seed.FORM_R;
import static org.nmcpye.datarun.characterization.a2.A2Seed.FORM_X;
import static org.nmcpye.datarun.characterization.a2.A2Seed.HOUSE_V1;
import static org.nmcpye.datarun.characterization.a2.A2Seed.HOUSE_V2;
import static org.nmcpye.datarun.characterization.a2.A2Seed.NO_TEAM;
import static org.nmcpye.datarun.characterization.a2.A2Seed.version;

/** Tier 1, W1 and W2: the versioned upload route as G2 and G3 phones use it. */
class A2VersionedUploadTier1IT extends A2GoldenSupport {

    private static final String FUTURE = "2099-01-01T00:00:00Z";

    private static Item formA(String uid, String name) {
        return item(G3, uid, FORM_A, version("FORM_A.v1"), ASSIGN_T1_V1, ordinary(G3, uid, name, "r1", "s1", 1));
    }

    @Test
    void w1_01_newOrdinarySubmission() {
        String uid = uid("w101");
        scenario("W1-01", "G3 sends a new FORM_A submission")
            .upload("new", G3, FIELD_1, formA(uid, "first"))
            .verify();
    }

    @Test
    void w1_01b_newOrdinarySubmissionWithoutVersionHeader() {
        String uid = uid("w101b");
        scenario("W1-01b", "G2 sends a new FORM_A submission (no X-App-Version)")
            .upload("new", G2, FIELD_1,
                item(G2, uid, FORM_A, version("FORM_A.v1"), ASSIGN_T1_V1, ordinary(G2, uid, "first", "r1", "s1", 1)))
            .verify();
    }

    @Test
    void w1_02_repeatRowsWithoutIds() {
        String uid = uid("w102");
        scenario("W1-02", "API-shaped upload whose repeat rows carry no metadata: the server makes it")
            .call("new", "POST", BULK_VERSIONED, FIELD_1, body(
                item(G0, uid, FORM_A, version("FORM_A.v1"), ASSIGN_T1_V1, ordinary(G0, uid, "first", "r1", "s1", 1))))
            .verify();
    }

    @Test
    @PinnedBug(changeBy = "D1",
        what = "repeat rows that carry _id but lack _index, _parentId or _submissionUid are stored without those "
            + "fills: the filled copy is kept only when the server generated an _id")
    void w1_02b_repeatRowsWithIdsButNoOtherMetadata() {
        String uid = uid("w102b");
        ObjectNode data = ordinary(G0, uid, "first", "r1", "s1", 1);
        ObjectNode row = (ObjectNode) data.get("main").get("rows").get(0);
        row.put("_id", rowId(1));
        ((ObjectNode) row.get("subrows").get(0)).put("_id", rowId(2));
        scenario("W1-02b", "Repeat rows carry _id only: the server's _index/_parentId/_submissionUid fills are dropped")
            .call("new", "POST", BULK_VERSIONED, FIELD_1, body(
                item(G0, uid, FORM_A, version("FORM_A.v1"), ASSIGN_T1_V1, data)))
            .verify();
    }

    @Test
    void w1_03_nestedRowsWithoutIdsAndAListOutsideTheTemplate() {
        String uid = uid("w103");
        ObjectNode data = ordinary(G3, uid, "first", "r1", "s1", 1);
        ObjectNode row = (ObjectNode) data.get("main").get("rows").get(0);
        ((ObjectNode) row.get("subrows").get(0)).remove(List.of("_id", "_index", "_parentId", "_submissionUid"));
        ((ObjectNode) data.get("main")).set("extras", NODES.arrayNode().add(NODES.objectNode().put("k", "v")));
        scenario("W1-03", "Nested rows without metadata get it; a list of maps outside the template stays as sent")
            .call("new", "POST", BULK_VERSIONED, FIELD_1, body(
                item(G3, uid, FORM_A, version("FORM_A.v1"), ASSIGN_T1_V1, data)))
            .verify();
    }

    @Test
    void w1_04_resendUnchanged() {
        String uid = uid("w104");
        Item first = formA(uid, "first");
        scenario("W1-04", "G3 sends the same submission twice")
            .upload("new", G3, FIELD_1, first)
            .upload("re-send unchanged", G3, FIELD_1, first.copy())
            .verify();
    }

    @Test
    @PinnedBug(changeBy = "none planned: kept in B1 (first entry times stand)",
        what = "a re-send replaces the data but keeps the first start and finish entry times")
    void w1_05_resendChanged() {
        String uid = uid("w105");
        scenario("W1-05", "G3 re-sends with changed data and a later finish time: data replaced, entry times kept")
            .upload("new", G3, FIELD_1, formA(uid, "first"))
            .upload("re-send changed", G3, FIELD_1, formA(uid, "changed").set("finishedEntryTime", FINISH_LATER))
            .verify();
    }

    @Test
    @PinnedBug(changeBy = "E1",
        what = "a re-send on a newer version of the form replaces the data but keeps the old version uid and number")
    void w1_07_resendOnANewerVersion() {
        String uid = uid("w107");
        ObjectNode data = ordinary(G3, uid, "first", "r1", "s1", 1);
        ((ObjectNode) data.get("main")).put("a2_extra", "new field");
        scenario("W1-07", "Re-send on FORM_A v2: data replaced, version label kept")
            .upload("new on v1", G3, FIELD_1, formA(uid, "first"))
            .upload("re-send on v2", G3, FIELD_1,
                item(G3, uid, FORM_A, version("FORM_A.v2"), ASSIGN_T1_V1, data))
            .verify();
    }

    @Test
    void w1_10_oneRejectedItemAmongTwoGood() {
        scenario("W1-10", "Three items: valid, a form the team may not submit (E4122), valid")
            .upload("three items", G3, FIELD_1,
                formA(uid("w110a"), "a"),
                item(G3, uid("w110b"), FORM_X, version("FORM_X.v1"), ASSIGN_T1_V1, note("x")),
                formA(uid("w110c"), "c"))
            .verify();
    }

    @Test
    void w1_11_storedRecordIsAnotherForm() {
        String uid = uid("w111");
        scenario("W1-11", "The stored record is FORM_X; a FORM_A upload of its uid is refused (E4123)")
            .upload("field3 stores FORM_X", G3, FIELD_3,
                item(G3, uid, FORM_X, version("FORM_X.v1"), ASSIGN_T3_V3, note("x")))
            .upload("field1 sends FORM_A on the same uid", G3, FIELD_1, formA(uid, "first"))
            .verify();
    }

    @Test
    void w1_12_storedAssignmentNotReachable() {
        String uid = uid("w112");
        scenario("W1-12", "The stored record sits on another team's assignment (E4124)")
            .upload("field3 stores it on team three", G3, FIELD_3,
                item(G3, uid, FORM_A, version("FORM_A.v1"), ASSIGN_T3_V3, ordinary(G3, uid, "first", "r1", "s1", 1)))
            .upload("field1 sends the same uid on team one", G3, FIELD_1, formA(uid, "first"))
            .verify();
    }

    @Test
    void w1_13a_anotherUsersRecordWithoutTheGrant() {
        String uid = uid("w113a");
        scenario("W1-13a", "Another user's record, without EDIT_SUBMISSIONS_FROM_USERS (E4126)")
            .upload("field1 creates", G3, FIELD_1, formA(uid, "first"))
            .upload("field2 re-sends changed", G3, FIELD_2, formA(uid, "changed"))
            .verify();
    }

    @Test
    void w1_13b_anotherUsersRecordWithTheGrant() {
        String uid = uid("w113b");
        scenario("W1-13b", "Another user's record, by a manager holding EDIT_SUBMISSIONS_FROM_USERS: updated")
            .upload("field1 creates", G3, FIELD_1, formA(uid, "first"))
            .upload("editor re-sends changed", G3, EDITOR, formA(uid, "changed"))
            .verify();
    }

    @Test
    @PinnedBug(changeBy = "a later C step",
        what = "a new uid sent with deleted=true is stored as a deleted row without deleted_at (its event is DELETE "
            + "since C)")
    void w1_15_newUidAlreadyDeleted() {
        String uid = uid("w115");
        scenario("W1-15", "A new uid sent with deleted=true is stored deleted, with a DELETE event")
            .call("new and deleted", "POST", BULK_VERSIONED, FIELD_1, body(formA(uid, "first").set("deleted", true)))
            .verify();
    }

    @Test
    @PinnedBug(changeBy = "E3/E4",
        what = "a re-send onto a soft-deleted record replaces its data, leaves it deleted and is reported as updated")
    void w1_16_resendOntoASoftDeletedRecord() {
        String uid = uid("w116");
        scenario("W1-16", "A re-send onto a soft-deleted record replaces its data and leaves it deleted")
            .upload("field1 creates", G3, FIELD_1, formA(uid, "first"))
            .call("admin deletes", "DELETE", "/api/v1/dataSubmission/" + uid, ADMIN, null)
            .upload("field1 re-sends changed", G3, FIELD_1, formA(uid, "changed"))
            .upload("field1 re-sends the same again", G3, FIELD_1, formA(uid, "changed"))
            .verify();
    }

    @Test
    @PinnedBug(changeBy = "E4",
        what = "lost update: the phone's re-send of its old copy silently overwrites a change made on the server")
    void w1_17_lostUpdate() {
        String uid = uid("w117");
        Item original = formA(uid, "first");
        ObjectNode corrected = ordinary(G3, uid, "corrected on the server", "r1", "s1", 1);
        scenario("W1-17", "A server-side change is overwritten by the phone's next re-send of its old copy")
            .upload("field1 creates", G3, FIELD_1, original)
            .sql("a correction made on the server",
                "UPDATE data_submission SET form_data = CAST(:data AS jsonb) WHERE uid = :uid",
                Map.of("data", corrected.toString(), "uid", uid))
            .upload("field1 re-sends its old copy", G3, FIELD_1, original.copy())
            .verify();
    }

    @Test
    void w1_18_heldBeforeGoLive() {
        String uid = uid("w118");
        scenario("W1-18", "A new upload started before the campaign's go-live is held and reported as created")
            .goLive("campaign", null, null, FUTURE)
            .upload("new before go-live", G3, FIELD_1, formA(uid, "first"))
            .verify();
    }

    @Test
    void w1_19_resendWhileHeld() {
        String uid = uid("w119");
        scenario("W1-19", "A re-send while held refreshes the held upload; a discard survives a refresh")
            .goLive("campaign", null, null, FUTURE)
            .upload("new before go-live", G3, FIELD_1, formA(uid, "first"))
            .upload("re-send changed while held", G3, FIELD_1, formA(uid, "changed"))
            .call("admin discards", "POST", "/api/v1/admin/heldSubmissions/discard", ADMIN,
                NODES.objectNode().set("uids", NODES.arrayNode().add(uid)))
            .upload("re-send changed again", G3, FIELD_1, formA(uid, "changed again"))
            .verify();
    }

    @Test
    void w1_20_existingRecordIsNeverHeld() {
        String uid = uid("w120");
        scenario("W1-20", "An update of a stored record before a go-live is saved, not held")
            .upload("field1 creates", G3, FIELD_1, formA(uid, "first"))
            .goLive("campaign", null, null, FUTURE)
            .upload("re-send changed before go-live", G3, FIELD_1, formA(uid, "changed"))
            .verify();
    }

    @Test
    void w1_22_invalidItems() {
        scenario("W1-22", "Per-row refusals: no assignment, unknown assignment, unknown version, no version, no uid")
            .call("five invalid items", "POST", BULK_VERSIONED, FIELD_1, body(
                formA(uid("w122a"), "a").remove("assignment"),
                formA(uid("w122b"), "b").set("assignment", "a2NoSuchAsg"),
                formA(uid("w122c"), "c").set("formVersion", "a2NoSuchVer"),
                formA(uid("w122d"), "d").remove("formVersion"),
                formA(uid("w122e"), "e").remove("uid")))
            .verify();
    }

    @Test
    @PinnedBug(changeBy = "none planned (A3 nearest)",
        what = "an unreadable body gets a bare Spring ProblemDetail, without message or path: a fifth error shape")
    void w1_23_unreadableBodies() {
        scenario("W1-23", "An unreadable body fails the whole request with 400")
            .call("not JSON", "POST", BULK_VERSIONED, FIELD_1, "not json")
            .call("an object instead of a list", "POST", BULK_VERSIONED, FIELD_1, formA(uid("w123b"), "b").node)
            .call("an unreadable time in one item", "POST", BULK_VERSIONED, FIELD_1, body(
                formA(uid("w123c"), "c"), formA(uid("w123d"), "d").set("startEntryTime", "yesterday")))
            .verify();
    }

    @Test
    void w1_24_referenceOutcomes() {
        String create = uid("w124a");
        String otherVillage = uid("w124b");
        String unknown = uid("w124c");
        String repeated = uid("w124d");
        String malformed = uid("w124e");
        String unused = uid("w124f");
        String reference = version("FORM_R.v1");
        scenario("W1-24", "Reference values: a new entry, and the refusals E4119, E4120, E4118, E4117, E4121")
            .upload("what a G3 phone can send", G3, FIELD_1,
                item(G3, create, FORM_R, reference, ASSIGN_T1_V1, households(G3, create, "a2HouseNew1"))
                    .definitions("a2HouseNew1", "Alpha Bravo Charlie Delta"),
                item(G3, otherVillage, FORM_R, reference, ASSIGN_T1_V1, households(G3, otherVillage, HOUSE_V2)),
                item(G3, unknown, FORM_R, reference, ASSIGN_T1_V1, households(G3, unknown, "a2HouseUnkn")),
                item(G3, repeated, FORM_R, reference, ASSIGN_T1_V1, households(G3, repeated, HOUSE_V1, HOUSE_V1))
                    .definitions(HOUSE_V1, "House One"))
            .call("what only an API caller can send", "POST", BULK_VERSIONED, FIELD_1, body(
                item(G3, malformed, FORM_R, reference, ASSIGN_T1_V1, households(G3, malformed, "bad-uid")),
                item(G3, unused, FORM_R, reference, ASSIGN_T1_V1, households(G3, unused, HOUSE_V1))
                    .definitions(HOUSE_V1, "House One", "a2HouseUnus", "Unused House")))
            .verify();
    }

    @Test
    @PinnedBug(changeBy = "none planned",
        what = "an upload on a soft-deleted assignment is accepted")
    void w1_25_unauthorized() {
        scenario("W1-25", "No token, no team, another team's assignment (E4114), a soft-deleted assignment (accepted)")
            .call("no token", "POST", BULK_VERSIONED, null, body(formA(uid("w125a"), "a")))
            .upload("a user with no team", G3, NO_TEAM, formA(uid("w125b"), "b"))
            .upload("another team's assignment", G3, FIELD_1,
                item(G3, uid("w125c"), FORM_A, version("FORM_A.v1"), ASSIGN_T3_V3,
                    ordinary(G3, uid("w125c"), "c", "r1", "s1", 1)))
            .upload("a soft-deleted assignment", G3, FIELD_1,
                item(G3, uid("w125d"), FORM_A, version("FORM_A.v1"), ASSIGN_DELETED,
                    ordinary(G3, uid("w125d"), "d", "r1", "s1", 1)))
            .verify();
    }

    @Test
    @PinnedBug(changeBy = "B2",
        what = "an admin upload skips scope, grant and stored-record checks, so another form's data sent on a stored "
            + "uid overwrites the record under its stored form")
    void w1_26_adminSkipsEveryCheck() {
        String uid = uid("w126");
        scenario("W1-26", "An admin re-send of another form's data overwrites the record under its stored form")
            .upload("field1 creates FORM_A", G3, FIELD_1, formA(uid, "first"))
            .call("admin sends FORM_X data on the same uid", "POST", BULK_VERSIONED, ADMIN, body(
                item(G3, uid, FORM_X, version("FORM_X.v1"), ASSIGN_T1_V1, note("x"))))
            .verify();
    }

    /** The version-number branch: no readable phone tag sends {@code version}, so only API callers reach it. */
    @Test
    void w1_30_versionNumberInsteadOfVersionUid() {
        Item byNumber = formA(uid("w130a"), "a").remove("formVersion");
        byNumber.node.put("version", 1);
        Item unknownNumber = formA(uid("w130b"), "b").remove("formVersion");
        unknownNumber.node.put("version", 99);
        scenario("W1-30", "API-shaped items naming a version number instead of a version uid: resolved by number; "
            + "an unknown number is refused")
            .call("two items", "POST", BULK_VERSIONED, FIELD_1, body(byNumber, unknownNumber))
            .verify();
    }

    @Test
    void w1_31_emptyBatches() {
        scenario("W1-31", "Empty versioned and practice batches answer an empty summary")
            .upload("an empty versioned batch", G3, FIELD_1)
            .practice("an empty practice batch", FIELD_1)
            .verify();
    }

    @Test
    void w2_01_practiceUpload() {
        String uid = uid("w201");
        scenario("W2-01", "A practice upload is checked, rolled back and kept as PRACTICE")
            .practice("practice upload", FIELD_1, formA(uid, "practice"))
            .verify();
    }
}
