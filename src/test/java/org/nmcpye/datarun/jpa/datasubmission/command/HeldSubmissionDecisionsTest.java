package org.nmcpye.datarun.jpa.datasubmission.command;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.nmcpye.datarun.common.EntitySaveSummaryVM;
import org.nmcpye.datarun.common.exceptions.IllegalQueryException;
import org.nmcpye.datarun.jpa.datasubmission.repository.DataSubmissionRepository;
import org.nmcpye.datarun.jpa.intake.GoLiveRuleService;
import org.nmcpye.datarun.jpa.intake.HeldFilter;
import org.nmcpye.datarun.jpa.intake.HeldState;
import org.nmcpye.datarun.jpa.intake.HeldSubmissionRepository;
import org.nmcpye.datarun.jpa.intake.HeldSubmissionRow;
import org.nmcpye.datarun.jpa.intake.HoldReason;
import org.nmcpye.datarun.jpa.intake.IntakeMode;
import org.nmcpye.datarun.security.CurrentUserDetails;
import org.nmcpye.datarun.security.CurrentUserDetailsImpl;
import org.nmcpye.datarun.security.DomainUserDetailsService;
import org.nmcpye.datarun.web.rest.v1.datasubmission.dto.DataSubmissionUploadV1Dto;
import org.nmcpye.datarun.web.rest.v1.intake.HeldDecisionRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HeldSubmissionDecisionsTest {

    private static final EnumSet<HeldState> HELD_OR_DISCARDED = EnumSet.of(HeldState.HELD, HeldState.DISCARDED);
    private static final EnumSet<HeldState> HELD = EnumSet.of(HeldState.HELD);
    private static final Instant SEEN = Instant.parse("2026-11-20T08:10:00Z");

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private HeldSubmissionRepository held;
    private GoLiveRuleService goLiveRules;
    private DataSubmissionRepository submissions;
    private SubmissionUploads uploads;
    private DomainUserDetailsService users;
    private HeldSubmissionDecisions decisions;
    private SubmissionActor admin;

    @BeforeEach
    void setUp() {
        held = mock(HeldSubmissionRepository.class);
        goLiveRules = mock(GoLiveRuleService.class);
        submissions = mock(DataSubmissionRepository.class);
        uploads = mock(SubmissionUploads.class);
        users = mock(DomainUserDetailsService.class);
        decisions = new HeldSubmissionDecisions(held, goLiveRules, submissions, uploads, users, objectMapper);

        admin = SubmissionActor.of(user("admin", "ROLE_ADMIN"));
        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
            admin.access(), null, admin.access().getAuthorities()));
        when(users.loadUserByUsername("field-user")).thenReturn(user("field-user", "ROLE_USER"));
        when(held.markDecided(anyString(), any(), any(), any(), any(), any())).thenReturn(true);
    }

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void releaseReplaysTheUploadAsItsSubmitterAndRecordsTheDecision() {
        when(held.findWithPayload(List.of("heldSub0001"), HELD_OR_DISCARDED))
            .thenReturn(List.of(row("heldSub0001", HoldReason.BEFORE_GO_LIVE)));
        AtomicReference<String> replayedAs = new AtomicReference<>();
        AtomicReference<String> replayedFor = new AtomicReference<>();
        AtomicReference<String> replayedUid = new AtomicReference<>();
        when(uploads.saveReleased(any(), any(), any())).thenAnswer(invocation -> {
            replayedAs.set(SecurityContextHolder.getContext().getAuthentication().getName());
            replayedFor.set(invocation.<SubmissionActor>getArgument(1).login());
            replayedUid.set(invocation.<DataSubmissionUploadV1Dto>getArgument(0).getUid());
            return saved(invocation);
        });

        HeldDecisionResult result = decisions.release(admin,
            new HeldDecisionRequest(List.of("heldSub0001"), null, "district started early", null));

        assertThat(replayedAs.get()).as("the audit columns name the submitter").isEqualTo("field-user");
        assertThat(replayedFor.get()).as("the checks read the submitter's access").isEqualTo("field-user");
        assertThat(replayedUid.get()).isEqualTo("heldSub0001");
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName())
            .as("the admin's context is back after the replay").isEqualTo("admin");
        assertThat(result.released()).containsExactly("heldSub0001");
        verify(held).markDecided("heldSub0001", HeldState.RELEASED, HELD_OR_DISCARDED, SEEN,
            "admin", "district started early");
    }

    @Test
    void aDecisionThatLostARaceIsReportedAsChangedNotRelabelled() {
        when(held.findWithPayload(List.of("heldSub0001"), HELD_OR_DISCARDED))
            .thenReturn(List.of(row("heldSub0001", HoldReason.BEFORE_GO_LIVE)));
        when(submissions.existsByUid("heldSub0001")).thenReturn(true);
        when(held.markDecided(eq("heldSub0001"), eq(HeldState.SUPERSEDED), any(), any(), any(), any()))
            .thenReturn(false);

        HeldDecisionResult result =
            decisions.release(admin, new HeldDecisionRequest(List.of("heldSub0001"), null, null, null));

        assertThat(result.superseded()).isEmpty();
        assertThat(result.changed()).containsExactly("heldSub0001");
        verify(uploads, never()).saveReleased(any(), any(), any());
    }

    /** The release race (A2 S4): a re-send changed the held upload after the release read it. */
    @Test
    void aReleaseWhoseHeldUploadChangedMeanwhileIsReportedAsChanged() {
        when(held.findWithPayload(List.of("heldSub0001"), HELD_OR_DISCARDED))
            .thenReturn(List.of(row("heldSub0001", HoldReason.BEFORE_GO_LIVE)));
        when(held.markDecided(eq("heldSub0001"), eq(HeldState.RELEASED), any(), any(), any(), any())).thenReturn(false);
        when(uploads.saveReleased(any(), any(), any())).thenAnswer(HeldSubmissionDecisionsTest::saved);

        HeldDecisionResult result =
            decisions.release(admin, new HeldDecisionRequest(List.of("heldSub0001"), null, null, null));

        assertThat(result.released()).isEmpty();
        assertThat(result.changed()).containsExactly("heldSub0001");
        verify(held, never()).recordReleaseError(anyString(), anyString());
    }

    @Test
    void aRefusedReleaseKeepsTheUploadAndRecordsWhy() {
        when(held.findWithPayload(List.of("heldSub0001"), HELD_OR_DISCARDED))
            .thenReturn(List.of(row("heldSub0001", HoldReason.BEFORE_GO_LIVE)));
        when(uploads.saveReleased(any(), any(), any()))
            .thenReturn(new SubmissionUploads.Released("E4114: not in the team", false));

        HeldDecisionResult result =
            decisions.release(admin, new HeldDecisionRequest(List.of("heldSub0001"), null, null, null));

        assertThat(result.failed()).isEqualTo(Map.of("heldSub0001", "E4114: not in the team"));
        verify(held).recordReleaseError("heldSub0001", "E4114: not in the team");
        verify(held, never()).markDecided(anyString(), any(), any(), any(), any(), any());
    }

    @Test
    void aSubmitterWhoCannotLogInAnymoreFailsTheReleaseCleanly() {
        when(held.findWithPayload(List.of("heldSub0001"), HELD_OR_DISCARDED))
            .thenReturn(List.of(row("heldSub0001", HoldReason.BEFORE_GO_LIVE)));
        when(users.loadUserByUsername("field-user")).thenThrow(new UsernameNotFoundException("gone"));

        HeldDecisionResult result =
            decisions.release(admin, new HeldDecisionRequest(List.of("heldSub0001"), null, null, null));

        assertThat(result.failed().get("heldSub0001")).startsWith("Cannot release as field-user");
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo("admin");
    }

    /**
     * The admin and the API keep a failed release's full text; the log line keeps identifiers and shapes: no login
     * (the submitter's appears in that text) and no exception message.
     */
    @Test
    void aFailedReleaseLogsItsCodeOrTypeOnly() {
        when(held.findWithPayload(List.of("heldSub0001", "heldSub0002"), HELD_OR_DISCARDED))
            .thenReturn(List.of(row("heldSub0001", HoldReason.BEFORE_GO_LIVE),
                row("heldSub0002", HoldReason.BEFORE_GO_LIVE, "gone-user")));
        when(users.loadUserByUsername("gone-user"))
            .thenThrow(new UsernameNotFoundException("User gone-user was not found in the database"));
        when(uploads.saveReleased(any(), any(), any())).thenReturn(new SubmissionUploads.Released(
            "E4122: You may not submit form `household1` for assignment `assignment1`", false));

        try (LogCapture logs = new LogCapture(HeldSubmissionDecisions.class)) {
            HeldDecisionResult result = decisions.release(admin,
                new HeldDecisionRequest(List.of("heldSub0001", "heldSub0002"), null, null, null));

            assertThat(result.failed().get("heldSub0002")).startsWith("Cannot release as gone-user: ");
            logs.assertNothingLeaks("gone-user", "field-user", "admin", "You may not");
            assertThat(logs.lines()).anySatisfy(line -> assertThat(line)
                .contains("heldSub0001").contains("household1").contains(admin.userId()).endsWith(": E4122"));
            assertThat(logs.lines()).anySatisfy(line -> assertThat(line)
                .contains("heldSub0002").contains("UsernameNotFoundException"));
        }
    }

    @Test
    void aFilterReleaseTakesOnlyUploadsStillHeldBeforeAGoLive() {
        HeldFilter expected = new HeldFilter("campaign001", null, null, HoldReason.BEFORE_GO_LIVE, HELD, null, null);
        when(held.findAll(expected)).thenReturn(List.of(
            row("heldSub0001", HoldReason.BEFORE_GO_LIVE), row("heldSub0002", HoldReason.BEFORE_GO_LIVE)));
        when(goLiveRules.acceptedByCurrentRules()).thenReturn(r -> r.uid().equals("heldSub0001"));
        when(held.findWithPayload(List.of("heldSub0001"), HELD))
            .thenReturn(List.of(row("heldSub0001", HoldReason.BEFORE_GO_LIVE)));
        when(uploads.saveReleased(any(), any(), any())).thenAnswer(HeldSubmissionDecisionsTest::saved);

        HeldDecisionResult result = decisions.release(admin, new HeldDecisionRequest(null,
            new HeldDecisionRequest.Filter("campaign001", null, null, null, null, null, true), null, null));

        assertThat(result.released()).containsExactly("heldSub0001");
        verify(held).findAll(expected);
    }

    @Test
    void practiceUploadsAreNeverReleasedInBulk() {
        assertThatThrownBy(() -> decisions.release(admin, new HeldDecisionRequest(null,
            new HeldDecisionRequest.Filter("campaign001", null, null, HoldReason.PRACTICE, null, null, null),
            null, null)))
            .isInstanceOf(IllegalQueryException.class)
            .hasMessageContaining("only by uid");
    }

    @Test
    void aLargeSelectionIsWorkedInBatchesAndReportsWhatRemains() {
        List<HeldSubmissionRow> many = List.of(
            row("heldSub0001", HoldReason.BEFORE_GO_LIVE),
            row("heldSub0002", HoldReason.BEFORE_GO_LIVE),
            row("heldSub0003", HoldReason.BEFORE_GO_LIVE));
        when(held.findAll(any())).thenReturn(many);
        when(held.findWithPayload(List.of("heldSub0001", "heldSub0002"), HELD)).thenReturn(many.subList(0, 2));

        HeldDecisionResult result = decisions.discard(admin, new HeldDecisionRequest(null,
            new HeldDecisionRequest.Filter("campaign001", null, null, null, null, null, null), "training", 2));

        assertThat(result.discarded()).containsExactly("heldSub0001", "heldSub0002");
        assertThat(result.remaining()).isEqualTo(1);
    }

    @Test
    void discardLabelsHeldUploadsOfAnyReason() {
        when(held.findAll(new HeldFilter("campaign001", null, null, HoldReason.PRACTICE, HELD, null, null)))
            .thenReturn(List.of(row("heldSub0002", HoldReason.PRACTICE)));
        when(held.findWithPayload(List.of("heldSub0002"), HELD))
            .thenReturn(List.of(row("heldSub0002", HoldReason.PRACTICE)));

        HeldDecisionResult result = decisions.discard(admin, new HeldDecisionRequest(null,
            new HeldDecisionRequest.Filter("campaign001", null, null, HoldReason.PRACTICE, null, null, null),
            "training day", null));

        assertThat(result.discarded()).containsExactly("heldSub0002");
        verify(held).markDecided("heldSub0002", HeldState.DISCARDED, HELD_OR_DISCARDED, SEEN, "admin", "training day");
    }

    @Test
    void aSelectionIsEitherUidsOrAFilterOnOneCampaign() {
        assertThatThrownBy(() -> decisions.release(admin, new HeldDecisionRequest(null, null, null, null)))
            .isInstanceOf(IllegalQueryException.class);
        assertThatThrownBy(() -> decisions.release(admin, new HeldDecisionRequest(List.of("heldSub0001"),
            new HeldDecisionRequest.Filter("campaign001", null, null, null, null, null, null), null, null)))
            .isInstanceOf(IllegalQueryException.class);
        assertThatThrownBy(() -> decisions.discard(admin, new HeldDecisionRequest(null,
            new HeldDecisionRequest.Filter(null, null, null, null, null, null, null), null, null)))
            .isInstanceOf(IllegalQueryException.class);
    }

    private static CurrentUserDetails user(String login, String authority) {
        return CurrentUserDetailsImpl.builder()
            .id("01JTEST" + Integer.toHexString(login.hashCode()))
            .username(login)
            .password("x")
            .enabled(true)
            .authorities(List.<GrantedAuthority>of(new SimpleGrantedAuthority(authority)))
            .isSuper("ROLE_ADMIN".equals(authority))
            .build();
    }

    /** A release the checks accept: the decision it is given runs, as inside the release's transaction. */
    private static SubmissionUploads.Released saved(org.mockito.invocation.InvocationOnMock invocation) {
        return new SubmissionUploads.Released(null, invocation.<BooleanSupplier>getArgument(2).getAsBoolean());
    }

    private HeldSubmissionRow row(String uid, HoldReason reason) {
        return row(uid, reason, "field-user");
    }

    private HeldSubmissionRow row(String uid, HoldReason reason, String submittedBy) {
        ObjectNode payload = objectMapper.createObjectNode().put("uid", uid).put("form", "household1");
        Instant received = Instant.parse("2026-11-20T08:00:00Z");
        return new HeldSubmissionRow(uid, reason, HeldState.HELD, "campaign001", "village00001",
            ",country0001,village00001", "household1", "version0001", "team0000001", "assignment1",
            submittedBy, received, received, "goLiveRule1", Instant.parse("2026-11-21T00:00:00Z"),
            null, null, null, null, SEEN, payload);
    }
}
