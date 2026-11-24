package org.nmcpye.datarun.characterization.a2;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.nmcpye.datarun.characterization.a2.A2Payloads.Item;
import org.nmcpye.datarun.jpa.datasubmission.repository.DataSubmissionRepository;
import org.nmcpye.datarun.jpa.intake.GoLiveRuleService;
import org.nmcpye.datarun.jpa.intake.HeldState;
import org.nmcpye.datarun.jpa.intake.HeldSubmissionRepository;
import org.nmcpye.datarun.jpa.intake.HeldSubmissionRow;
import org.nmcpye.datarun.security.DomainUserDetailsService;
import org.nmcpye.datarun.web.rest.v1.datasubmission.service.SubmissionUploadService;
import org.nmcpye.datarun.web.rest.v1.intake.HeldDecisionRequest;
import org.nmcpye.datarun.web.rest.v1.intake.HeldSubmissionReview;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

import static org.nmcpye.datarun.characterization.a2.A2Payloads.Group.G3;
import static org.nmcpye.datarun.characterization.a2.A2Payloads.NODES;
import static org.nmcpye.datarun.characterization.a2.A2Payloads.item;
import static org.nmcpye.datarun.characterization.a2.A2Payloads.ordinary;
import static org.nmcpye.datarun.characterization.a2.A2Payloads.uid;
import static org.nmcpye.datarun.characterization.a2.A2Seed.ADMIN;
import static org.nmcpye.datarun.characterization.a2.A2Seed.ASSIGN_T1_V1;
import static org.nmcpye.datarun.characterization.a2.A2Seed.ASSIGN_T1_V4;
import static org.nmcpye.datarun.characterization.a2.A2Seed.FIELD_1;
import static org.nmcpye.datarun.characterization.a2.A2Seed.FORM_A;
import static org.nmcpye.datarun.characterization.a2.A2Seed.version;

/** Tier 2, sequences: delete then re-send, the release race, and the catalog's second page. */
class A2SequencesTier2IT extends A2GoldenSupport {

    private static final String FUTURE = "2099-01-01T00:00:00Z";

    @Autowired
    private HeldSubmissionRepository held;

    @Autowired
    private GoLiveRuleService goLiveRuleService;

    @Autowired
    private DataSubmissionRepository submissions;

    @Autowired
    private SubmissionUploadService uploads;

    @Autowired
    private DomainUserDetailsService users;

    @Autowired
    private ObjectMapper objectMapper;

    private static Item formA(String uid, String name) {
        return item(G3, uid, FORM_A, version("FORM_A.v1"), ASSIGN_T1_V1, ordinary(G3, uid, name, "r1", "s1", 1));
    }

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @PinnedBug(changeBy = "E3/E4, C1, B1",
        what = "a phone re-send after an admin delete is absorbed into the deleted record, and a second admin delete "
            + "writes a second DELETE event")
    void s3_deleteResendDelete() {
        String uid = uid("s3");
        scenario("S3", "Admin delete, a phone re-send absorbed, a second admin delete")
            .upload("field1 creates", G3, FIELD_1, formA(uid, "first"))
            .call("admin deletes", "DELETE", "/api/v1/dataSubmission/" + uid, ADMIN, null)
            .upload("field1 re-sends changed", G3, FIELD_1, formA(uid, "changed"))
            .call("admin deletes again", "DELETE", "/api/v1/dataSubmission/" + uid, ADMIN, null)
            .verify();
    }

    @Test
    @PinnedBug(changeBy = "B1/E4",
        what = "a re-send that lands between a release's read and its replay is lost: the old held copy is saved and "
            + "the next release marks the newer one superseded")
    void s4_resendDuringARelease() {
        String uid = uid("s4");
        Scenario scenario = scenario("S4",
            "A re-send lands between a release's read and its replay: the old copy is applied, the new one superseded");
        HeldSubmissionRepository hooked = new HeldSubmissionRepository(jdbc, objectMapper) {
            @Override
            public List<HeldSubmissionRow> findWithPayload(Collection<String> uids, Collection<HeldState> states) {
                List<HeldSubmissionRow> rows = super.findWithPayload(uids, states);
                scenario.upload("the phone re-sends changed while the release runs", G3, FIELD_1,
                    formA(uid, "changed during the release"));
                return rows;
            }

            @Override
            public boolean markDecided(String uid, HeldState state, Collection<HeldState> from,
                                       Instant seenLastModified, String decidedBy, String note) {
                return held.markDecided(uid, state, from, seenLastModified, decidedBy, note);
            }

            @Override
            public void recordReleaseError(String uid, String error) {
                held.recordReleaseError(uid, error);
            }
        };
        HeldSubmissionReview review =
            new HeldSubmissionReview(hooked, goLiveRuleService, submissions, uploads, users, objectMapper);
        ObjectNode release = NODES.objectNode();
        release.set("uids", NODES.arrayNode().add(uid));

        scenario
            .goLive("campaign", null, null, FUTURE)
            .upload("held before go-live", G3, FIELD_1, formA(uid, "first"))
            .action("admin releases it (service call, hooked read)", () -> {
                asAdmin();
                return review.release(new HeldDecisionRequest(List.of(uid), null, null, null));
            })
            .call("admin releases it again", "POST", "/api/v1/admin/heldSubmissions/release", ADMIN, release)
            .verify();
    }

    @Test
    @PinnedBug(changeBy = "none planned",
        what = "the catalog's nextPage link repeats page (?page=0&page=1&size=500); phones compute page numbers "
            + "themselves and do not follow it")
    void r2b_catalogPages() {
        scenario("R2b", "A 501-entry catalog is two pages of 500 and 1; the phone walks totalPages")
            .summarize("referenceEntries")
            .call("page 0", "GET", "/api/v1/assignments/" + ASSIGN_T1_V4 + "/referenceEntries?page=0&size=500",
                FIELD_1, null)
            .call("page 1", "GET", "/api/v1/assignments/" + ASSIGN_T1_V4 + "/referenceEntries?page=1&size=500",
                FIELD_1, null)
            .verify();
    }

    private void asAdmin() {
        UserDetails admin = users.loadUserByUsername(ADMIN);
        SecurityContextHolder.getContext().setAuthentication(
            UsernamePasswordAuthenticationToken.authenticated(admin, null, admin.getAuthorities()));
    }
}
