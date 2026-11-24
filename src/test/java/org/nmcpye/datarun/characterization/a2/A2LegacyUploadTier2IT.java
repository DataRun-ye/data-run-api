package org.nmcpye.datarun.characterization.a2;

import org.junit.jupiter.api.Test;
import org.nmcpye.datarun.characterization.a2.A2Payloads.Item;

import static org.nmcpye.datarun.characterization.a2.A2Payloads.BULK_LEGACY;
import static org.nmcpye.datarun.characterization.a2.A2Payloads.Group.G1;
import static org.nmcpye.datarun.characterization.a2.A2Payloads.body;
import static org.nmcpye.datarun.characterization.a2.A2Payloads.item;
import static org.nmcpye.datarun.characterization.a2.A2Payloads.ordinary;
import static org.nmcpye.datarun.characterization.a2.A2Payloads.uid;
import static org.nmcpye.datarun.characterization.a2.A2Seed.ADMIN;
import static org.nmcpye.datarun.characterization.a2.A2Seed.ASSIGN_T3_V3;
import static org.nmcpye.datarun.characterization.a2.A2Seed.FIELD_1;
import static org.nmcpye.datarun.characterization.a2.A2Seed.FORM_A;
import static org.nmcpye.datarun.characterization.a2.A2Seed.version;

/** Tier 2, W3: the legacy route's order of checks, and admins on it. */
class A2LegacyUploadTier2IT extends A2GoldenSupport {

    private static Item formA(String uid, String name, String assignment) {
        return item(G1, uid, FORM_A, version("FORM_A.v1"), assignment, ordinary(G1, uid, name, "r1", "s1", 1));
    }

    @Test
    void w3_07_versionCheckedBeforeAccess() {
        scenario("W3-07", "An unauthorized user with an unknown version gets the version message, not E4114")
            .upload("another team's assignment, unknown version", G1, FIELD_1,
                formA(uid("w307"), "a", ASSIGN_T3_V3).set("formVersion", "a2NoSuchVer"))
            .verify();
    }

    @Test
    void w3_10_adminOnTheLegacyRoute() {
        scenario("W3-10", "An admin skips the access checks on the legacy route, but an assignment is still required")
            .call("admin, no assignment", "POST", BULK_LEGACY, ADMIN,
                body(formA(uid("w310a"), "a", ASSIGN_T3_V3).remove("assignment")))
            .call("admin, a team he is not in", "POST", BULK_LEGACY, ADMIN,
                body(formA(uid("w310b"), "b", ASSIGN_T3_V3)))
            .verify();
    }
}
