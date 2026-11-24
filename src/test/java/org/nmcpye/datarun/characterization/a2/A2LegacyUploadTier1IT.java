package org.nmcpye.datarun.characterization.a2;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.nmcpye.datarun.characterization.a2.A2Payloads.Group;
import org.nmcpye.datarun.characterization.a2.A2Payloads.Item;

import java.util.Map;

import static org.nmcpye.datarun.characterization.a2.A2Payloads.BULK_LEGACY;
import static org.nmcpye.datarun.characterization.a2.A2Payloads.Group.G0;
import static org.nmcpye.datarun.characterization.a2.A2Payloads.Group.G1;
import static org.nmcpye.datarun.characterization.a2.A2Payloads.NODES;
import static org.nmcpye.datarun.characterization.a2.A2Payloads.body;
import static org.nmcpye.datarun.characterization.a2.A2Payloads.households;
import static org.nmcpye.datarun.characterization.a2.A2Payloads.item;
import static org.nmcpye.datarun.characterization.a2.A2Payloads.note;
import static org.nmcpye.datarun.characterization.a2.A2Payloads.ordinary;
import static org.nmcpye.datarun.characterization.a2.A2Payloads.uid;
import static org.nmcpye.datarun.characterization.a2.A2Seed.ADMIN;
import static org.nmcpye.datarun.characterization.a2.A2Seed.ASSIGN_T1_V1;
import static org.nmcpye.datarun.characterization.a2.A2Seed.ASSIGN_T3_V3;
import static org.nmcpye.datarun.characterization.a2.A2Seed.FIELD_1;
import static org.nmcpye.datarun.characterization.a2.A2Seed.FIELD_2;
import static org.nmcpye.datarun.characterization.a2.A2Seed.FIELD_3;
import static org.nmcpye.datarun.characterization.a2.A2Seed.FORM_A;
import static org.nmcpye.datarun.characterization.a2.A2Seed.FORM_R;
import static org.nmcpye.datarun.characterization.a2.A2Seed.FORM_X;
import static org.nmcpye.datarun.characterization.a2.A2Seed.version;

/** Tier 1, W3: the legacy upload route, as G0 (v5.2.0+17 to v5.3.0+20) and G1 (v6.0.0, v6.0.1) phones use it. */
class A2LegacyUploadTier1IT extends A2GoldenSupport {

    private static final String FUTURE = "2099-01-01T00:00:00Z";

    private static Item formA(Group group, String uid, String name, String assignment) {
        return item(group, uid, FORM_A, version("FORM_A.v1"), assignment, ordinary(group, uid, name, "r1", "s1", 1));
    }

    private static Item unknownHouse(Group group, String uid) {
        return item(group, uid, FORM_R, version("FORM_R.v1"), ASSIGN_T1_V1, households(group, uid, "a2HouseUnkn"));
    }

    @Test
    void w3_01_newFromG1() {
        String uid = uid("w301");
        scenario("W3-01", "G1 sends a new FORM_A submission on the legacy route")
            .upload("new", G1, FIELD_1, formA(G1, uid, "first", ASSIGN_T1_V1))
            .verify();
    }

    @Test
    void w3_02_newFromG0() {
        String uid = uid("w302");
        Item g0 = formA(G0, uid, "first", ASSIGN_T1_V1);
        ((ObjectNode) g0.node.get("formData").get("main"))
            .set("extras", NODES.arrayNode().add(NODES.objectNode().put("k", "v")));
        scenario("W3-02", "G0 sends repeat rows without metadata: the server adds it to template repeats only")
            .upload("new", G0, FIELD_1, g0)
            .verify();
    }

    @Test
    @PinnedBug(changeBy = "D1",
        what = "a G0 re-send of an unchanged record gets new repeat-row ids, so it is written as an UPDATE and its "
            + "rows lose their identity")
    void w3_03_g0ResendRegeneratesRowIds() {
        String uid = uid("w303");
        Item g0 = formA(G0, uid, "first", ASSIGN_T1_V1);
        scenario("W3-03", "G0 re-sends unchanged: new repeat-row ids make it an UPDATE")
            .upload("new", G0, FIELD_1, g0)
            .upload("re-send unchanged", G0, FIELD_1, g0.copy())
            .verify();
    }

    @Test
    @PinnedBug(changeBy = "B1/B2",
        what = "the legacy route stores a Reference value unchecked and creates no reference entry")
    void w3_04_referenceValueUnchecked() {
        String uid = uid("w304");
        scenario("W3-04", "A Reference value on the legacy route is stored unchecked and creates no entry")
            .upload("unknown household, no definition", G1, FIELD_1, unknownHouse(G1, uid))
            .verify();
    }

    @Test
    @PinnedBug(changeBy = "B1",
        what = "a legacy upload made before the campaign's go-live is saved instead of held")
    void w3_05_legacyIsNeverHeld() {
        String uid = uid("w305");
        scenario("W3-05", "A legacy upload before the campaign's go-live is saved, never held")
            .goLive("campaign", null, null, FUTURE)
            .upload("new before go-live", G1, FIELD_1, formA(G1, uid, "first", ASSIGN_T1_V1))
            .verify();
    }

    @Test
    @PinnedBug(changeBy = "B1/B2",
        what = "a legacy item without uid is stored under a uid the server makes and the phone never learns")
    void w3_06_missingUid() {
        Item noUid = item(G1, null, FORM_A, version("FORM_A.v1"), ASSIGN_T1_V1,
            ordinary(G1, "a2nouidxxxx", "first", "r1", "s1", 1)).remove("uid");
        var scenario = scenario("W3-06", "A legacy item without uid is stored under a uid the server makes")
            .call("no uid", "POST", BULK_LEGACY, FIELD_1, body(noUid));
        JsonNode created = scenario.lastBody().path("created").path(0);
        if (created.isTextual()) {
            scenario.placeholder(created.asText(), "server-made uid");
        }
        scenario.verify();
    }

    @Test
    @PinnedBug(changeBy = "versioning-policy step 4",
        what = "any referenceVersion other than exactly 1 falls through silently to the legacy route")
    void w3_08_referenceVersionOtherThanOneFallsThrough() {
        scenario("W3-08", "Any referenceVersion but exactly 1 takes the legacy route (an unknown household tells them apart)")
            .call("referenceVersion=0", "POST", BULK_LEGACY + "?referenceVersion=0", FIELD_1,
                body(unknownHouse(G1, uid("w308a"))))
            .call("referenceVersion=2", "POST", BULK_LEGACY + "?referenceVersion=2", FIELD_1,
                body(unknownHouse(G1, uid("w308b"))))
            .call("referenceVersion=abc", "POST", BULK_LEGACY + "?referenceVersion=abc", FIELD_1,
                body(unknownHouse(G1, uid("w308c"))))
            .call("referenceVersion=1 and 2", "POST", BULK_LEGACY + "?referenceVersion=1&referenceVersion=2", FIELD_1,
                body(unknownHouse(G1, uid("w308d"))))
            .verify();
    }

    @Test
    void w3_09_storedRecordGuardOnTheLegacyRoute() {
        String otherForm = uid("w309a");
        String otherTeam = uid("w309b");
        String otherUser = uid("w309c");
        scenario("W3-09", "The legacy route applies the stored-record guard: E4123, E4124, E4126")
            .upload("field3 stores FORM_X", G1, FIELD_3,
                item(G1, otherForm, FORM_X, version("FORM_X.v1"), ASSIGN_T3_V3, note("x")))
            .upload("field1 sends FORM_A on that uid", G1, FIELD_1, formA(G1, otherForm, "a", ASSIGN_T1_V1))
            .upload("field3 stores FORM_A on team three", G1, FIELD_3, formA(G1, otherTeam, "b", ASSIGN_T3_V3))
            .upload("field1 sends that uid on team one", G1, FIELD_1, formA(G1, otherTeam, "b", ASSIGN_T1_V1))
            .upload("field1 creates", G1, FIELD_1, formA(G1, otherUser, "c", ASSIGN_T1_V1))
            .upload("field2 re-sends changed", G1, FIELD_2, formA(G1, otherUser, "changed", ASSIGN_T1_V1))
            .verify();
    }
    @Test
    @PinnedBug(changeBy = "E4",
        what = "lost update on the legacy route: G0's re-send of its old copy silently overwrites a change made on "
            + "the server")
    void w3_11_g0LostUpdate() {
        String uid = uid("w311");
        Item original = formA(G0, uid, "first", ASSIGN_T1_V1);
        ObjectNode corrected = ordinary(G0, uid, "corrected on the server", "r1", "s1", 1);
        scenario("W3-11", "G0: a server-side change is overwritten by the phone's next legacy re-send")
            .upload("field1 creates", G0, FIELD_1, original)
            .sql("a correction made on the server",
                "UPDATE data_submission SET form_data = CAST(:data AS jsonb) WHERE uid = :uid",
                Map.of("data", corrected.toString(), "uid", uid))
            .upload("field1 re-sends its old copy", G0, FIELD_1, original.copy())
            .verify();
    }

    @Test
    @PinnedBug(changeBy = "E3/E4, C1",
        what = "a G0 re-send onto a soft-deleted record replaces its data, leaves it deleted and is reported as "
            + "updated")
    void w3_12_g0ResendOntoASoftDeletedRecord() {
        String uid = uid("w312");
        scenario("W3-12", "G0: a legacy re-send onto a soft-deleted record replaces its data and leaves it deleted")
            .upload("field1 creates", G0, FIELD_1, formA(G0, uid, "first", ASSIGN_T1_V1))
            .call("admin deletes", "DELETE", "/api/v1/dataSubmission/" + uid, ADMIN, null)
            .upload("field1 re-sends changed", G0, FIELD_1, formA(G0, uid, "changed", ASSIGN_T1_V1))
            .verify();
    }

    @Test
    void w3_13_g1ResendUnchangedThenChanged() {
        String uid = uid("w313");
        scenario("W3-13", "G1 re-sends unchanged (its row ids are stable: no write), then changed (an UPDATE)")
            .upload("new", G1, FIELD_1, formA(G1, uid, "first", ASSIGN_T1_V1))
            .upload("re-send unchanged", G1, FIELD_1, formA(G1, uid, "first", ASSIGN_T1_V1))
            .upload("re-send changed", G1, FIELD_1, formA(G1, uid, "changed", ASSIGN_T1_V1))
            .verify();
    }

    @Test
    void w3_14_g0RefusalsAreStrings() {
        String uid = uid("w314");
        scenario("W3-14", "G0: a refused re-send (another user's record) comes back as the string G0 needs")
            .upload("field1 creates", G0, FIELD_1, formA(G0, uid, "first", ASSIGN_T1_V1))
            .upload("field2 re-sends changed", G0, FIELD_2, formA(G0, uid, "changed", ASSIGN_T1_V1))
            .verify();
    }
}
