package org.nmcpye.datarun.characterization.a2;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.nmcpye.datarun.characterization.a2.A2Payloads.Item;
import org.nmcpye.datarun.jpa.datasubmission.command.HeldSubmissionDecisions;
import org.nmcpye.datarun.jpa.accessfilter.AssignmentFormAccessService;
import org.nmcpye.datarun.jpa.assignment.repository.AssignmentRepository;
import org.nmcpye.datarun.jpa.datasubmission.command.SubmissionActor;
import org.nmcpye.datarun.jpa.datasubmission.command.SubmissionCommand;
import org.nmcpye.datarun.jpa.datasubmission.command.SubmissionCommandService;
import org.nmcpye.datarun.jpa.datasubmission.command.SubmissionUploads;
import org.nmcpye.datarun.jpa.datasubmission.repository.DataSubmissionRepository;
import org.nmcpye.datarun.jpa.datasubmission.service.DataSubmissionService;
import org.nmcpye.datarun.jpa.datasubmission.validation.StoredSubmissionGuard;
import org.nmcpye.datarun.jpa.datatemplate.service.TemplateVersionResolver;
import org.nmcpye.datarun.jpa.intake.GoLiveRuleService;
import org.nmcpye.datarun.jpa.intake.HeldState;
import org.nmcpye.datarun.jpa.intake.HeldSubmissionRepository;
import org.nmcpye.datarun.jpa.intake.HeldSubmissionRow;
import org.nmcpye.datarun.jpa.intake.IntakeDecision;
import org.nmcpye.datarun.jpa.intake.IntakeMode;
import org.nmcpye.datarun.jpa.intake.SubmissionIntake;
import org.nmcpye.datarun.jpa.reference.ReferenceSubmissionResolver;
import org.nmcpye.datarun.jpa.user.repository.UserRepository;
import org.nmcpye.datarun.security.CurrentUserDetails;
import org.nmcpye.datarun.security.CurrentUserDetailsService;
import org.nmcpye.datarun.security.DomainUserDetailsService;
import org.nmcpye.datarun.web.rest.v1.datasubmission.dto.DataSubmissionUploadV1Dto;
import org.nmcpye.datarun.web.rest.v1.datasubmission.mapper.DataSubmissionUploadV1Mapper;
import org.nmcpye.datarun.web.rest.v1.intake.HeldDecisionRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.nmcpye.datarun.characterization.a2.A2PhoneContract.referenceCatalogPage;
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
import static org.nmcpye.datarun.characterization.a2.A2Seed.VILLAGE_4;
import static org.nmcpye.datarun.characterization.a2.A2Seed.version;

/** Tier 2, sequences: delete then re-send, the release races, and the catalog's second page. */
class A2SequencesTier2IT extends A2GoldenSupport {

    private static final String FUTURE = "2099-01-01T00:00:00Z";

    @Autowired
    private HeldSubmissionRepository held;

    @Autowired
    private GoLiveRuleService goLiveRuleService;

    @Autowired
    private DataSubmissionRepository submissions;

    @Autowired
    private SubmissionUploads uploads;

    @Autowired
    private DomainUserDetailsService users;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SubmissionCommandService commands;

    @Autowired
    private DataSubmissionService submissionService;

    @Autowired
    private DataSubmissionUploadV1Mapper uploadMapper;

    @Autowired
    private AssignmentRepository assignments;

    @Autowired
    private AssignmentFormAccessService formAccess;

    @Autowired
    private TemplateVersionResolver templateVersions;

    @Autowired
    private ReferenceSubmissionResolver references;

    @Autowired
    private StoredSubmissionGuard storedSubmissionGuard;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CurrentUserDetailsService currentUserDetailsService;

    private static Item formA(String uid, String name) {
        return item(G3, uid, FORM_A, version("FORM_A.v1"), ASSIGN_T1_V1, ordinary(G3, uid, name, "r1", "s1", 1));
    }

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @PinnedBug(changeBy = "E3/E4",
        what = "a phone re-send after an admin delete is absorbed into the deleted record")
    void s3_deleteResendDelete() {
        String uid = uid("s3");
        scenario("S3", "Admin delete, a phone re-send absorbed, a second admin delete")
            .upload("field1 creates", G3, FIELD_1, formA(uid, "first"))
            .call("admin deletes", "DELETE", "/api/v1/dataSubmission/" + uid, ADMIN, null)
            .upload("field1 re-sends changed", G3, FIELD_1, formA(uid, "changed"))
            .call("admin deletes again", "DELETE", "/api/v1/dataSubmission/" + uid, ADMIN, null)
            .verify();
    }

    /**
     * Fixed in B1: a release saves an upload only together with the decision that released it, so the re-send's
     * newer copy is not lost (it was until B1: the old copy was saved and the next release marked the newer one
     * superseded).
     */
    @Test
    void s4_resendDuringARelease() {
        String uid = uid("s4");
        Scenario scenario = scenario("S4",
            "A re-send lands between a release's read and its replay: that release saves nothing and reports it "
                + "changed; the next release applies the newer copy");
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
        HeldSubmissionDecisions decisions =
            new HeldSubmissionDecisions(hooked, goLiveRuleService, submissions, uploads, users, objectMapper);
        ObjectNode release = NODES.objectNode();
        release.set("uids", NODES.arrayNode().add(uid));

        scenario
            .goLive("campaign", null, null, FUTURE)
            .upload("held before go-live", G3, FIELD_1, formA(uid, "first"))
            .action("admin releases it (service call, hooked read)", () -> {
                asAdmin();
                return decisions.release(SubmissionActor.current(),
                    new HeldDecisionRequest(List.of(uid), null, null, null));
            })
            .call("admin releases it again", "POST", "/api/v1/admin/heldSubmissions/release", ADMIN, release)
            .verify();
    }

    /**
     * Fixed in B1: a re-send whose checked save ran while its earlier copy was held, and whose hold came after the
     * admin's release of that copy had committed, was dropped (the released copy is final), while the phone was told
     * it was saved. Now the re-send is saved as the update of real data it has become.
     */
    @Test
    void s5_reSendKeptAfterItsHeldCopyWasReleased() {
        String uid = uid("s5");
        Scenario scenario = scenario("S5", "A re-send checked while its earlier copy was held, kept after the admin's "
            + "release of that copy: saved as an update of the released record");
        SubmissionActor admin = SubmissionActor.of((CurrentUserDetails) users.loadUserByUsername(ADMIN));
        SubmissionActor field1 = SubmissionActor.of((CurrentUserDetails) users.loadUserByUsername(FIELD_1));
        SubmissionIntake releasedMeanwhile = new SubmissionIntake(goLiveRules, held, submissions) {
            @Override
            public boolean hold(String heldUid, JsonNode payload, IntakeDecision decision) {
                scenario.action("meanwhile the admin releases the held copy (service call)", () ->
                    commands.releaseHeld(new SubmissionCommand.ReleaseHeld(admin,
                        new HeldDecisionRequest(List.of(uid), null, null, null))));
                return super.hold(heldUid, payload, decision);
            }
        };
        SubmissionUploads hookedUploads = new SubmissionUploads(submissionService, uploadMapper, objectMapper,
            assignments, formAccess, templateVersions, references, transactionManager, releasedMeanwhile,
            storedSubmissionGuard, submissions);

        scenario
            .goLive("campaign", null, null, FUTURE)
            .upload("held before go-live", G3, FIELD_1, formA(uid, "first"))
            .action("the phone re-sends a changed copy (service call; its hold comes after the release)", () -> {
                asUser(FIELD_1);
                return hookedUploads.save(List.of(dto(formA(uid, "changed"))), field1, IntakeMode.LIVE);
            })
            .verify();
    }

    /**
     * Fixed in B1: a live upload of a held uid that landed after the release checked the uid and before its replay
     * saved the held copy was overwritten by that older copy. Now the replay's save sees the uid is real data, the
     * replay is rolled back and the held copy is superseded.
     */
    @Test
    void s6_liveUploadLandsBetweenTheReleaseCheckAndTheReplay() {
        String uid = uid("s6");
        Scenario scenario = scenario("S6", "A live re-send lands after the release checked the uid and before the "
            + "replay: the held copy is superseded and the newer data stays");
        DomainUserDetailsService lookupHooked = new DomainUserDetailsService(userRepository, currentUserDetailsService) {
            @Override
            public UserDetails loadUserByUsername(String login) {
                scenario.upload("meanwhile the phone re-sends a changed copy, saved: no go-live rule now", G3, FIELD_1,
                    formA(uid, "changed"));
                return users.loadUserByUsername(login);
            }
        };
        HeldSubmissionDecisions decisions =
            new HeldSubmissionDecisions(held, goLiveRuleService, submissions, uploads, lookupHooked, objectMapper);

        scenario
            .goLive("campaign", null, null, FUTURE)
            .upload("held before go-live", G3, FIELD_1, formA(uid, "first"))
            .sql("the go-live rule is removed", "DELETE FROM go_live_rule WHERE activity_uid = :activity",
                Map.of("activity", A2Seed.ACTIVITY))
            .action("admin releases it (service call; the submitter's lookup is hooked)", () -> {
                asAdmin();
                return decisions.release(SubmissionActor.current(),
                    new HeldDecisionRequest(List.of(uid), null, null, null));
            })
            .verify();
    }

    @Test
    @PinnedBug(changeBy = "none planned",
        what = "the catalog's nextPage link repeats page (?page=0&page=1&size=500); phones compute page numbers "
            + "themselves and do not follow it")
    void r2b_catalogPages() {
        scenario("R2b", "A 501-entry catalog is two pages of 500 and 1; the phone walks totalPages")
            .summarize("referenceEntries")
            .read("page 0", referenceCatalogPage(0, VILLAGE_4),
                "/api/v1/assignments/" + ASSIGN_T1_V4 + "/referenceEntries?page=0&size=500", FIELD_1)
            .read("page 1", referenceCatalogPage(1, VILLAGE_4),
                "/api/v1/assignments/" + ASSIGN_T1_V4 + "/referenceEntries?page=1&size=500", FIELD_1)
            .verify();
    }

    private void asAdmin() {
        asUser(ADMIN);
    }

    /** The security context a request by {@code login} would have, for direct service calls. */
    private void asUser(String login) {
        UserDetails user = users.loadUserByUsername(login);
        SecurityContextHolder.getContext().setAuthentication(
            UsernamePasswordAuthenticationToken.authenticated(user, null, user.getAuthorities()));
    }

    /** An upload item as the versioned route binds it. */
    private DataSubmissionUploadV1Dto dto(Item item) {
        try {
            return objectMapper.treeToValue(item.node, DataSubmissionUploadV1Dto.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }
}
