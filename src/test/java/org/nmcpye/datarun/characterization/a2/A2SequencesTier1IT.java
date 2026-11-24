package org.nmcpye.datarun.characterization.a2;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.nmcpye.datarun.characterization.a2.A2Payloads.Group;
import org.nmcpye.datarun.characterization.a2.A2Payloads.Item;

import static org.nmcpye.datarun.characterization.a2.A2Payloads.Group.G2;
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

/** Tier 1, sequences: a lost response, and a held upload's whole life. */
class A2SequencesTier1IT extends A2GoldenSupport {

    private static final String FUTURE = "2099-01-01T00:00:00Z";

    private static Item formA(Group group, String uid, String name) {
        return item(group, uid, FORM_A, version("FORM_A.v1"), ASSIGN_T1_V1, ordinary(group, uid, name, "r1", "s1", 1));
    }

    @Test
    void s1_lostResponse() {
        String uid = uid("s1");
        scenario("S1", "G2: the response is lost; the phone re-sends with its failure in lastSyncMassage")
            .upload("send (the server commits; the phone never hears)", G2, FIELD_1, formA(G2, uid, "first"))
            .upload("re-send after the failure", G2, FIELD_1, formA(G2, uid, "first")
                .set("lastSyncMassage", "{\"v\":1,\"errorCode\":\"networkTimeout\"}")
                .set("lastSyncDate", "2026-11-20T05:11:00.000Z"))
            .verify();
    }

    @Test
    void s2_heldUploadLife() {
        String uid = uid("s2");
        ObjectNode release = NODES.objectNode();
        release.set("uids", NODES.arrayNode().add(uid));
        scenario("S2", "Held, refreshed, released, then re-sent unchanged and changed")
            .goLive("campaign", null, null, FUTURE)
            .upload("held before go-live", G3, FIELD_1, formA(G3, uid, "first"))
            .upload("re-send changed while held", G3, FIELD_1, formA(G3, uid, "changed"))
            .call("admin releases it", "POST", "/api/v1/admin/heldSubmissions/release", ADMIN, release)
            .upload("the phone re-sends unchanged", G3, FIELD_1, formA(G3, uid, "changed"))
            .upload("the phone re-sends changed", G3, FIELD_1, formA(G3, uid, "changed again"))
            .verify();
    }
}
