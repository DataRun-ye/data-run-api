package org.nmcpye.datarun.characterization.a2;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.nmcpye.datarun.characterization.a2.A2Payloads.Item;

import static org.nmcpye.datarun.characterization.a2.A2Payloads.Group.G3;
import static org.nmcpye.datarun.characterization.a2.A2Payloads.item;
import static org.nmcpye.datarun.characterization.a2.A2Payloads.ordinary;
import static org.nmcpye.datarun.characterization.a2.A2Payloads.uid;
import static org.nmcpye.datarun.characterization.a2.A2Seed.ADMIN;
import static org.nmcpye.datarun.characterization.a2.A2Seed.ASSIGN_T1_V1;
import static org.nmcpye.datarun.characterization.a2.A2Seed.FIELD_1;
import static org.nmcpye.datarun.characterization.a2.A2Seed.FORM_A;
import static org.nmcpye.datarun.characterization.a2.A2Seed.version;

/** Tier 1, W4 to W6: the generic submission writes and the admin delete. */
class A2GenericWritesTier1IT extends A2GoldenSupport {

    private static Item formA(String uid, String name) {
        return item(G3, uid, FORM_A, version("FORM_A.v1"), ASSIGN_T1_V1, ordinary(G3, uid, name, "r1", "s1", 1));
    }

    /** A body for the generic routes: the entity's own fields, with an id that cannot bind. */
    private static ObjectNode entityBody(String uid, String name) {
        ObjectNode body = formA(uid, name).node.deepCopy();
        body.remove("lastSyncMassage");
        body.remove("lastSyncDate");
        body.put("id", "01HA2000000000000000000099");
        body.put("lockVersion", 0);
        return body;
    }

    @Test
    @PinnedBug(changeBy = "B2",
        what = "POST /dataSubmission can never save (its required id cannot be bound) and answers 400 instead of "
            + "being refused as a method (405)")
    void w4_01_genericPostAlwaysFailsValidation() {
        scenario("W4-01", "POST /dataSubmission answers 400 to an admin and to a field user")
            .call("admin", "POST", "/api/v1/dataSubmission", ADMIN, entityBody(uid("w401a"), "a"))
            .call("field user", "POST", "/api/v1/dataSubmission", FIELD_1, entityBody(uid("w401b"), "b"))
            .verify();
    }

    @Test
    @PinnedBug(changeBy = "B2",
        what = "POST /dataSubmission/return can never save and answers 400 instead of being refused as a method (405)")
    void w4_02_genericPostReturnAlwaysFailsValidation() {
        scenario("W4-02", "POST /dataSubmission/return answers 400")
            .call("admin", "POST", "/api/v1/dataSubmission/return", ADMIN, entityBody(uid("w402"), "a"))
            .verify();
    }

    @Test
    @PinnedBug(changeBy = "B2",
        what = "PUT /dataSubmission/{uid} can never save and answers 400 instead of being refused as a method (405)")
    void w5_01_adminPutAlwaysFailsValidation() {
        String uid = uid("w501");
        scenario("W5-01", "PUT /dataSubmission/{uid} by an admin answers 400 and changes nothing")
            .upload("field1 creates", G3, FIELD_1, formA(uid, "first"))
            .call("admin puts changed data", "PUT", "/api/v1/dataSubmission/" + uid, ADMIN, entityBody(uid, "changed"))
            .verify();
    }

    @Test
    void w6_01_adminDelete() {
        String uid = uid("w601");
        scenario("W6-01", "An admin delete soft-deletes and writes a DELETE event in the same transaction")
            .upload("field1 creates", G3, FIELD_1, formA(uid, "first"))
            .call("admin deletes", "DELETE", "/api/v1/dataSubmission/" + uid, ADMIN, null)
            .verify();
    }

    /** Fixed in B1: until then a second delete wrote a second DELETE event and moved deleted_at. */
    @Test
    void w6_02_adminDeleteTwice() {
        String uid = uid("w602");
        scenario("W6-02", "A second admin delete of a deleted record answers 204 and changes nothing")
            .upload("field1 creates", G3, FIELD_1, formA(uid, "first"))
            .call("admin deletes", "DELETE", "/api/v1/dataSubmission/" + uid, ADMIN, null)
            .call("admin deletes again", "DELETE", "/api/v1/dataSubmission/" + uid, ADMIN, null)
            .verify();
    }

    /** Fixed in B1: until then the delete answered 500. */
    @Test
    void w6_03_adminDeleteOfAnUnknownUid() {
        scenario("W6-03", "An admin delete of an unknown uid answers 404 and writes nothing")
            .call("admin deletes an unknown uid", "DELETE", "/api/v1/dataSubmission/a2nosuchsub", ADMIN, null)
            .verify();
    }

    @Test
    void w6_05_fieldUserDelete() {
        String uid = uid("w605");
        scenario("W6-05", "A field user's delete is refused with 403")
            .upload("field1 creates", G3, FIELD_1, formA(uid, "first"))
            .call("field1 deletes", "DELETE", "/api/v1/dataSubmission/" + uid, FIELD_1, null)
            .verify();
    }
}
