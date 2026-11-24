package org.nmcpye.datarun.characterization.a2;

import org.junit.jupiter.api.Test;

import static org.nmcpye.datarun.characterization.a2.A2Payloads.Group.G3;
import static org.nmcpye.datarun.characterization.a2.A2Payloads.item;
import static org.nmcpye.datarun.characterization.a2.A2Payloads.ordinary;
import static org.nmcpye.datarun.characterization.a2.A2Payloads.uid;
import static org.nmcpye.datarun.characterization.a2.A2Seed.ASSIGN_T1_V1;
import static org.nmcpye.datarun.characterization.a2.A2Seed.ASSIGN_T1_V2;
import static org.nmcpye.datarun.characterization.a2.A2Seed.ASSIGN_T5_V1;
import static org.nmcpye.datarun.characterization.a2.A2Seed.DELETER;
import static org.nmcpye.datarun.characterization.a2.A2Seed.FIELD_1;
import static org.nmcpye.datarun.characterization.a2.A2Seed.FORM_A;
import static org.nmcpye.datarun.characterization.a2.A2Seed.version;

/** Tier 1, phone reads a later step could break: the G0 submission pull, the catalog, the assignment-form flags. */
class A2PhoneReadsTier1IT extends A2GoldenSupport {

    @Test
    @PinnedBug(changeBy = "E2",
        what = "the G0 submission pull answers 500 to a user holding EDIT, because its filter names an attribute the "
            + "entity lacks; any fix must keep the pull inert for G0")
    void r1_g0SubmissionPull() {
        String mine = uid("r1a");
        String deleters = uid("r1b");
        scenario("R1", "GET dataSubmission/objects?paged=false (the G0 pull): empty without EDIT, 500 with it")
            .upload("field1 creates", G3, FIELD_1,
                item(G3, mine, FORM_A, version("FORM_A.v1"), ASSIGN_T1_V1, ordinary(G3, mine, "a", "r1", "s1", 1)))
            .upload("the deleter creates", G3, DELETER,
                item(G3, deleters, FORM_A, version("FORM_A.v1"), ASSIGN_T5_V1, ordinary(G3, deleters, "b", "r1", "s1", 1)))
            .call("field1 pulls (no EDIT grant)", "GET", "/api/v1/dataSubmission/objects?paged=false", FIELD_1, null)
            .call("the deleter pulls (EDIT grant)", "GET", "/api/v1/dataSubmission/objects?paged=false", DELETER, null)
            .verify();
    }

    @Test
    void r2_referenceCatalogPage() {
        scenario("R2", "GET assignments/{uid}/referenceEntries: the envelope and entry keys G2 and G3 parse")
            .call("field1 reads village 2's catalog", "GET",
                "/api/v1/assignments/" + ASSIGN_T1_V2 + "/referenceEntries?page=0&size=500", FIELD_1, null)
            .verify();
    }

    @Test
    void r3_assignmentFormFlags() {
        scenario("R3", "GET assignments/forms: accessibleForms flags; Reference forms only with referenceVersion=1")
            .sortBy("assignments", "id")
            .sortBy("accessibleForms", "form")
            .call("as G2 and G3 ask", "GET", "/api/v1/assignments/forms?paged=false&referenceVersion=1", FIELD_1, null)
            .call("as G0 and G1 ask", "GET", "/api/v1/assignments/forms?paged=false", FIELD_1, null)
            .verify();
    }
}
