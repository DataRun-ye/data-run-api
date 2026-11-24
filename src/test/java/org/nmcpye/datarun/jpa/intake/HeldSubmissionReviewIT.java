package org.nmcpye.datarun.jpa.intake;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.nmcpye.datarun.IntegrationTest;
import org.nmcpye.datarun.common.EntitySaveSummaryVM;
import org.nmcpye.datarun.jpa.datasubmission.command.HeldDecisionResult;
import org.nmcpye.datarun.jpa.datasubmission.command.HeldSubmissionDecisions;
import org.nmcpye.datarun.jpa.datasubmission.command.SubmissionActor;
import org.nmcpye.datarun.jpa.datasubmission.command.SubmissionUploads;
import org.nmcpye.datarun.jpa.datasubmission.repository.DataSubmissionRepository;
import org.nmcpye.datarun.jpa.orgunit.OrgUnit;
import org.nmcpye.datarun.jpa.orgunit.repository.OrgUnitRepository;
import org.nmcpye.datarun.security.CurrentUserDetails;
import org.nmcpye.datarun.security.CurrentUserDetailsImpl;
import org.nmcpye.datarun.security.DomainUserDetailsService;
import org.nmcpye.datarun.web.rest.v1.datasubmission.dto.DataSubmissionUploadV1Dto;
import org.nmcpye.datarun.web.rest.v1.intake.HeldDecisionRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Release and discard against real PostgreSQL, on the scenarios an independent
 * review of #42 found: bulk releases must not revive discarded or practice
 * uploads, stale or overlapping decisions must not relabel uploads, and
 * reading an assignment's area must not write its org unit.
 */
@IntegrationTest
class HeldSubmissionReviewIT {

    private static final Instant SUNDAY_0800 = Instant.parse("2026-11-22T05:00:00Z");
    private static final Instant SUNDAY_1000 = Instant.parse("2026-11-22T07:00:00Z");
    private static final Instant MONDAY = Instant.parse("2026-11-22T21:00:00Z");
    private static final Instant TUESDAY = Instant.parse("2026-11-24T07:00:00Z");

    @Autowired
    private HeldSubmissionRepository held;

    @Autowired
    private GoLiveRuleRepository rules;

    @Autowired
    private GoLiveRuleService goLiveRuleService;

    @Autowired
    private NamedParameterJdbcTemplate jdbc;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private OrgUnitRepository orgUnits;

    @Autowired
    private EntityManager entityManager;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final List<String> replayed = new ArrayList<>();
    private SubmissionUploads uploads;
    private DomainUserDetailsService users;
    private DataSubmissionRepository submissions;

    @BeforeEach
    void setUp() {
        uploads = mock(SubmissionUploads.class);
        when(uploads.saveReleased(any(), any(), any())).thenAnswer(invocation -> {
            replayed.add(invocation.<DataSubmissionUploadV1Dto>getArgument(0).getUid());
            return new SubmissionUploads.Released(null,
                invocation.<java.util.function.BooleanSupplier>getArgument(2).getAsBoolean());
        });
        users = mock(DomainUserDetailsService.class);
        when(users.loadUserByUsername(any())).thenAnswer(invocation -> user(invocation.getArgument(0)));
        submissions = mock(DataSubmissionRepository.class);
        asUser("admin");
    }

    @AfterEach
    void clean() {
        SecurityContextHolder.clearContext();
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            jdbc.update("DELETE FROM held_submission", Map.of());
            jdbc.update("DELETE FROM go_live_rule", Map.of());
            jdbc.update("DELETE FROM org_unit WHERE uid IN ('reviewChild', 'reviewRoot1')", Map.of());
        });
    }

    @Test
    void releasingWhatTheRulesNowAcceptDoesNotBringBackDiscardedTraining() {
        GoLiveRule monday = rules.put("campaign001", null, null, MONDAY, null, "admin");
        held.hold("training001", HoldReason.BEFORE_GO_LIVE, monday, context(SUNDAY_1000), payload("training001"));
        decide("training001", HeldState.DISCARDED);

        rules.put("campaign001", null, null, SUNDAY_0800, "real work started Sunday 08:00", "admin");
        long nowAccepted = held.findAll(new HeldFilter("campaign001", null, null, HoldReason.BEFORE_GO_LIVE,
                EnumSet.of(HeldState.HELD), null, null))
            .stream().filter(goLiveRuleService.acceptedByCurrentRules()).count(); // as PutResult.heldNowAccepted
        HeldDecisionResult result = decisions(held).release(ADMIN, new HeldDecisionRequest(null,
            new HeldDecisionRequest.Filter("campaign001", null, null, null, null, null, true), null, null));

        assertThat(nowAccepted).isZero();
        assertThat(result.released()).isEmpty();
        assertThat(replayed).isEmpty();
        assertThat(held.findByUid("training001").orElseThrow().state()).isEqualTo(HeldState.DISCARDED);
    }

    @Test
    void aFilterReleaseAfterTheGoLiveDoesNotReplayPracticeUploads() {
        GoLiveRule monday = rules.put("campaign001", null, null, MONDAY, null, "admin");
        held.hold("realEarly01", HoldReason.BEFORE_GO_LIVE, monday, context(SUNDAY_1000), payload("realEarly01"));
        held.hold("practice001", HoldReason.PRACTICE, null, context(TUESDAY), payload("practice001"));

        decisions(held).release(ADMIN, new HeldDecisionRequest(null,
            new HeldDecisionRequest.Filter("campaign001", null, null, null, SUNDAY_0800, null, null), null, null));

        assertThat(replayed).containsExactly("realEarly01");
        assertThat(held.findByUid("practice001").orElseThrow().state()).isEqualTo(HeldState.HELD);
    }

    @Test
    void aRepeatedReleaseKeepsTheRowReleased() {
        held.hold("heldSub0001", HoldReason.BEFORE_GO_LIVE, null, context(SUNDAY_1000), payload("heldSub0001"));
        List<HeldSubmissionRow> secondRunSelection =
            held.findWithPayload(List.of("heldSub0001"), EnumSet.of(HeldState.HELD, HeldState.DISCARDED));

        decide("heldSub0001", HeldState.RELEASED); // the first run finishes this upload
        when(submissions.existsByUid("heldSub0001")).thenReturn(true);

        HeldSubmissionRepository staleSelection = new HeldSubmissionRepository(jdbc, objectMapper) {
            @Override
            public List<HeldSubmissionRow> findWithPayload(Collection<String> uids, Collection<HeldState> states) {
                return secondRunSelection; // read before the first run's decision
            }

            @Override
            public boolean markDecided(String uid, HeldState state, Collection<HeldState> from,
                                       Instant seenLastModified, String decidedBy, String note) {
                return held.markDecided(uid, state, from, seenLastModified, decidedBy, note);
            }
        };
        HeldDecisionResult second =
            decisions(staleSelection).release(ADMIN, new HeldDecisionRequest(List.of("heldSub0001"), null, null, null));

        assertThat(second.changed()).containsExactly("heldSub0001");
        assertThat(held.findByUid("heldSub0001").orElseThrow().state()).isEqualTo(HeldState.RELEASED);
    }

    @Test
    void readingAnAssignmentsAreaInAWriteTransactionDoesNotWriteItsOrgUnit() {
        asUser("importer");
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            OrgUnit root = new OrgUnit();
            root.setUid("reviewRoot1");
            root.setCode("reviewRoot1");
            root.setName("Review root");
            entityManager.persist(root);
            OrgUnit child = new OrgUnit();
            child.setUid("reviewChild");
            child.setCode("reviewChild");
            child.setName("Review village");
            child.setParent(root);
            entityManager.persist(child); // stored path stays NULL, as for units never passed through updatePaths
        });

        asUser("field-user");
        List<String> chain = new TransactionTemplate(transactionManager).execute(status ->
            SubmissionIntake.areaChainOf(orgUnits.findByUid("reviewChild").orElseThrow()));

        Map<String, Object> row = jdbc.queryForMap(
            "SELECT path, last_modified_by FROM org_unit WHERE uid = 'reviewChild'", Map.of());
        assertThat(chain).containsExactly("reviewChild", "reviewRoot1");
        assertThat(row.get("last_modified_by")).isEqualTo("importer");
        assertThat(row.get("path")).isNull();
    }

    @Test
    void aMissingStartTimeIsStoredAndEntryFallsBackToReceipt() {
        assertThat(rules.formsOfActivity("campaign001")).isEmpty();

        HeldContext noStart = new HeldContext("campaign001", "village00X1", ",country0001,districtX01,village00X1",
            "household1", "version0001", "team0000001", "assignment1", "field-user", null, TUESDAY);
        held.hold("noStart0001", HoldReason.BEFORE_GO_LIVE, null, noStart, payload("noStart0001"));
        held.hold("noStart0001", HoldReason.BEFORE_GO_LIVE, null, noStart, payload("noStart0001"));

        HeldSubmissionRow row = held.findByUid("noStart0001").orElseThrow();
        assertThat(row.startedAt()).isNull();
        assertThat(row.receivedAt()).isEqualTo(TUESDAY);
        assertThat(held.summary(new HeldFilter("campaign001", "districtX01", null, null, null, TUESDAY, null)))
            .singleElement().satisfies(s -> assertThat(s.firstEntered()).isEqualTo(TUESDAY));
    }

    private void decide(String uid, HeldState state) {
        HeldSubmissionRow row = held.findByUid(uid).orElseThrow();
        assertThat(held.markDecided(uid, state, EnumSet.of(HeldState.HELD, HeldState.DISCARDED),
            row.lastModifiedDate(), "admin", null)).isTrue();
    }

    private HeldSubmissionDecisions decisions(HeldSubmissionRepository repository) {
        return new HeldSubmissionDecisions(repository, goLiveRuleService, submissions, uploads, users, objectMapper);
    }

    private static final SubmissionActor ADMIN = SubmissionActor.of(user("admin"));

    private static CurrentUserDetails user(String login) {
        return CurrentUserDetailsImpl.builder()
            .id("01JTEST" + login)
            .username(login)
            .password("x")
            .enabled(true)
            .authorities(List.<GrantedAuthority>of(new SimpleGrantedAuthority("ROLE_USER")))
            .build();
    }

    private static void asUser(String login) {
        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
            User.withUsername(login).password("x").authorities("ROLE_ADMIN").build(), null, List.of()));
    }

    private static HeldContext context(Instant startedAt) {
        return new HeldContext("campaign001", "village00X1", ",country0001,districtX01,village00X1", "household1",
            "version0001", "team0000001", "assignment1", "field-user", startedAt, startedAt.plusSeconds(600));
    }

    private ObjectNode payload(String uid) {
        return objectMapper.createObjectNode().put("uid", uid).put("form", "household1");
    }
}
