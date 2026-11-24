package org.nmcpye.datarun.characterization.a2;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.nmcpye.datarun.characterization.a2.A2Payloads.Item;

import static org.nmcpye.datarun.characterization.a2.A2Payloads.Group.G3;
import static org.nmcpye.datarun.characterization.a2.A2Payloads.NODES;
import static org.nmcpye.datarun.characterization.a2.A2Payloads.item;
import static org.nmcpye.datarun.characterization.a2.A2Payloads.ordinary;
import static org.nmcpye.datarun.characterization.a2.A2Payloads.uid;
import static org.nmcpye.datarun.characterization.a2.A2Seed.ACTIVITY;
import static org.nmcpye.datarun.characterization.a2.A2Seed.ADMIN;
import static org.nmcpye.datarun.characterization.a2.A2Seed.ASSIGN_T1_V1;
import static org.nmcpye.datarun.characterization.a2.A2Seed.FIELD_1;
import static org.nmcpye.datarun.characterization.a2.A2Seed.FORM_A;
import static org.nmcpye.datarun.characterization.a2.A2Seed.version;

/** Tier 2, W7: releases by filter, batches, and refused selections. */
class A2HeldDecisionsTier2IT extends A2GoldenSupport {

    private static final String FUTURE = "2099-01-01T00:00:00Z";
    private static final String RELEASE = "/api/v1/admin/heldSubmissions/release";
    private static final String DISCARD = "/api/v1/admin/heldSubmissions/discard";

    private static Item formA(String uid, String name) {
        return item(G3, uid, FORM_A, version("FORM_A.v1"), ASSIGN_T1_V1, ordinary(G3, uid, name, "r1", "s1", 1));
    }

    private static ObjectNode campaignFilter() {
        ObjectNode body = NODES.objectNode();
        body.set("filter", NODES.objectNode().put("activity", ACTIVITY));
        return body;
    }

    @Test
    void w7_04_releaseByFilter() {
        String held = uid("w704a");
        String practice = uid("w704b");
        String discarded = uid("w704c");
        ObjectNode discard = NODES.objectNode();
        discard.set("uids", NODES.arrayNode().add(discarded));
        scenario("W7-04", "A release by filter takes only uploads still held before a go-live")
            .goLive("campaign", null, null, FUTURE)
            .upload("held", G3, FIELD_1, formA(held, "held"))
            .practice("practice", FIELD_1, formA(practice, "practice"))
            .upload("held, then discarded", G3, FIELD_1, formA(discarded, "discarded"))
            .call("admin discards one", "POST", DISCARD, ADMIN, discard)
            .call("admin releases the campaign", "POST", RELEASE, ADMIN, campaignFilter())
            .verify();
    }

    @Test
    void w7_05_batches() {
        ObjectNode limited = campaignFilter().put("limit", 1);
        scenario("W7-05", "A release takes at most one batch and says how many remain")
            .goLive("campaign", null, null, FUTURE)
            .upload("held one", G3, FIELD_1, formA(uid("w705a"), "a"))
            .upload("held two", G3, FIELD_1, formA(uid("w705b"), "b"))
            .call("admin releases with limit 1", "POST", RELEASE, ADMIN, limited)
            .verify();
    }

    @Test
    void w7_06_refusedSelections() {
        ObjectNode both = campaignFilter();
        both.set("uids", NODES.arrayNode().add(uid("w706")));
        ObjectNode noActivity = NODES.objectNode();
        noActivity.set("filter", NODES.objectNode());
        ObjectNode practice = NODES.objectNode();
        practice.set("filter", NODES.objectNode().put("activity", ACTIVITY).put("reason", "PRACTICE"));
        scenario("W7-06", "Selections a release refuses with 400")
            .call("neither uids nor filter", "POST", RELEASE, ADMIN, NODES.objectNode())
            .call("both uids and filter", "POST", RELEASE, ADMIN, both)
            .call("a filter without activity", "POST", RELEASE, ADMIN, noActivity)
            .call("practice uploads by filter", "POST", RELEASE, ADMIN, practice)
            .verify();
    }
}
