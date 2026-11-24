package org.nmcpye.datarun.characterization.a2;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.nmcpye.datarun.characterization.a2.A2Payloads.Item;

import java.util.Map;

import static org.nmcpye.datarun.characterization.a2.A2Payloads.Group.G3;
import static org.nmcpye.datarun.characterization.a2.A2Payloads.item;
import static org.nmcpye.datarun.characterization.a2.A2Payloads.ordinary;
import static org.nmcpye.datarun.characterization.a2.A2Payloads.uid;
import static org.nmcpye.datarun.characterization.a2.A2Seed.ADMIN;
import static org.nmcpye.datarun.characterization.a2.A2Seed.ASSIGN_T1_V1;
import static org.nmcpye.datarun.characterization.a2.A2Seed.FIELD_1;
import static org.nmcpye.datarun.characterization.a2.A2Seed.FORM_A;
import static org.nmcpye.datarun.characterization.a2.A2Seed.version;

/** Tier 2, W5 and W6: a field user's PUT, and admin deletes that fail. */
class A2GenericWritesTier2IT extends A2GoldenSupport {

    private static Item formA(String uid, String name) {
        return item(G3, uid, FORM_A, version("FORM_A.v1"), ASSIGN_T1_V1, ordinary(G3, uid, name, "r1", "s1", 1));
    }

    @Test
    void w5_02_fieldUserPut() {
        String uid = uid("w502");
        ObjectNode body = formA(uid, "changed").node.deepCopy();
        body.remove("lastSyncMassage");
        body.remove("lastSyncDate");
        scenario("W5-02", "A field user's PUT answers 400, not 403: validation runs before method security")
            .upload("field1 creates", G3, FIELD_1, formA(uid, "first"))
            .call("field1 puts", "PUT", "/api/v1/dataSubmission/" + uid, FIELD_1, body)
            .verify();
    }

    @Test
    void w6_04_deleteByInternalId() {
        String uid = uid("w604");
        var scenario = scenario("W6-04", "An admin delete by the internal id (not the uid) answers 500")
            .upload("field1 creates", G3, FIELD_1, formA(uid, "first"));
        String id = jdbc.queryForObject("SELECT id FROM data_submission WHERE uid = :uid", Map.of("uid", uid),
            String.class);
        scenario.placeholder(id, "internal id")
            .call("admin deletes by internal id", "DELETE", "/api/v1/dataSubmission/" + id, ADMIN, null)
            .verify();
    }

    @Test
    void w6_06_deleteOfARowWithoutAVersionUid() {
        String uid = uid("w606");
        scenario("W6-06", "An admin delete of a row without template_version_uid answers 500 and writes nothing")
            .upload("field1 creates", G3, FIELD_1, formA(uid, "first"))
            .sql("the row loses its version uid (an old row's shape)",
                "UPDATE data_submission SET template_version_uid = NULL WHERE uid = :uid", Map.of("uid", uid))
            .call("admin deletes", "DELETE", "/api/v1/dataSubmission/" + uid, ADMIN, null)
            .verify();
    }
}
