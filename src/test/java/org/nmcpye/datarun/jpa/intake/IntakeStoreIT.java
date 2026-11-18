package org.nmcpye.datarun.jpa.intake;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.nmcpye.datarun.IntegrationTest;
import org.nmcpye.datarun.security.AuthoritiesConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The go-live and held-upload tables against a real PostgreSQL, migrated by Liquibase (#42). */
@AutoConfigureMockMvc
@IntegrationTest
class IntakeStoreIT {

    private static final Instant FRIDAY = Instant.parse("2026-11-20T00:00:00Z");
    private static final Instant SATURDAY = Instant.parse("2026-11-21T00:00:00Z");
    private static final Instant SUNDAY = Instant.parse("2026-11-22T00:00:00Z");
    private static final Instant MONDAY = Instant.parse("2026-11-23T00:00:00Z");

    @Autowired
    private GoLiveRuleRepository rules;

    @Autowired
    private HeldSubmissionRepository held;

    @Autowired
    private NamedParameterJdbcTemplate jdbc;

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private PlatformTransactionManager transactionManager;

    @AfterEach
    void clean() {
        // The pool does not auto-commit: outside a transaction these deletes would be rolled back.
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            jdbc.update("DELETE FROM held_submission", Map.of());
            jdbc.update("DELETE FROM go_live_rule", Map.of());
        });
    }

    @Test
    void thereIsOneRulePerScopeAndSettingAScopeAgainMovesItsGoLive() {
        GoLiveRule campaign = rules.put("campaign001", null, null, SATURDAY, "national", "admin");
        GoLiveRule moved = rules.put("campaign001", null, null, MONDAY, "postponed", "admin2");
        GoLiveRule district = rules.put("campaign001", "districtX01", null, SUNDAY, null, "admin");
        GoLiveRule warehouses = rules.put("campaign001", null, "warehouse01", FRIDAY, null, "admin");
        rules.put("otherCamp01", null, null, FRIDAY, null, "admin");

        assertThat(moved.uid()).isEqualTo(campaign.uid());
        assertThat(moved.liveFrom()).isEqualTo(MONDAY);
        assertThat(moved.note()).isEqualTo("postponed");
        assertThat(moved.lastModifiedBy()).isEqualTo("admin2");
        assertThat(rules.findByActivity("campaign001")).extracting(GoLiveRule::uid)
            .containsExactly(campaign.uid(), warehouses.uid(), district.uid());

        assertThat(rules.delete(warehouses.uid())).isTrue();
        assertThat(rules.delete(warehouses.uid())).isFalse();
        assertThat(rules.findByActivity("campaign001")).hasSize(2);
        assertThat(rules.findByUid(district.uid())).map(GoLiveRule::orgUnit).contains("districtX01");
    }

    @Test
    void aHeldUploadIsKeptRefreshedWhenSentAgainAndKeepsAnAdminsDiscard() {
        held.hold("heldSub0001", HoldReason.PRACTICE, null, context("village00X1", ",country0001,districtX01,village00X1", FRIDAY), payload(1));
        held.hold("heldSub0001", HoldReason.PRACTICE, null, context("village00X1", ",country0001,districtX01,village00X1", FRIDAY), payload(2));

        HeldSubmissionRow row = held.findByUid("heldSub0001").orElseThrow();
        assertThat(row.state()).isEqualTo(HeldState.HELD);
        assertThat(row.payload().get("edit").asInt()).isEqualTo(2);
        assertThat(row.startedAt()).isEqualTo(FRIDAY);
        assertThat(row.submittedBy()).isEqualTo("field-user");

        decide("heldSub0001", HeldState.DISCARDED, "training");
        held.hold("heldSub0001", HoldReason.PRACTICE, null, context("village00X1", ",country0001,districtX01,village00X1", FRIDAY), payload(3));
        row = held.findByUid("heldSub0001").orElseThrow();
        assertThat(row.state()).isEqualTo(HeldState.DISCARDED);
        assertThat(row.note()).isEqualTo("training");
        assertThat(row.payload().get("edit").asInt()).isEqualTo(3);

        decide("heldSub0001", HeldState.RELEASED, null);
        held.hold("heldSub0001", HoldReason.PRACTICE, null, context("village00X1", ",country0001,districtX01,village00X1", FRIDAY), payload(4));
        row = held.findByUid("heldSub0001").orElseThrow();
        assertThat(row.state()).as("a released upload is final, even when sent again").isEqualTo(HeldState.RELEASED);
        assertThat(row.payload().get("edit").asInt()).as("the released payload is kept").isEqualTo(3);
        assertThat(row.decidedBy()).isEqualTo("admin");
    }

    @Test
    void aDecisionAppliesOnlyToTheUploadAsItWasRead() {
        held.hold("heldSub0001", HoldReason.BEFORE_GO_LIVE, null, context("village00X1", ",country0001,districtX01,village00X1", FRIDAY), payload(1));
        HeldSubmissionRow read = held.findByUid("heldSub0001").orElseThrow();
        held.hold("heldSub0001", HoldReason.BEFORE_GO_LIVE, null, context("village00X1", ",country0001,districtX01,village00X1", FRIDAY), payload(2));

        assertThat(held.markDecided("heldSub0001", HeldState.RELEASED, EnumSet.of(HeldState.HELD),
            read.lastModifiedDate(), "admin", null))
            .as("sent again after it was read").isFalse();
        decide("heldSub0001", HeldState.RELEASED, null);
        HeldSubmissionRow released = held.findByUid("heldSub0001").orElseThrow();
        assertThat(held.markDecided("heldSub0001", HeldState.SUPERSEDED, EnumSet.of(HeldState.HELD, HeldState.DISCARDED),
            released.lastModifiedDate(), "admin", null))
            .as("a released upload is not relabelled").isFalse();
        assertThat(held.findByUid("heldSub0001").orElseThrow().state()).isEqualTo(HeldState.RELEASED);
    }

    @Test
    void heldUploadsAreFoundByAreaReasonStateAndEntryTime() {
        GoLiveRule rule = rules.put("campaign001", null, null, SATURDAY, null, "admin");
        held.hold("inXFriday01", HoldReason.BEFORE_GO_LIVE, rule,
            context("village00X1", ",country0001,districtX01,village00X1", FRIDAY), payload(1));
        held.hold("inXFriday02", HoldReason.BEFORE_GO_LIVE, rule,
            context("village00X2", ",country0001,districtX01,village00X2", FRIDAY.plusSeconds(3600)), payload(1));
        held.hold("inYFriday01", HoldReason.BEFORE_GO_LIVE, rule,
            context("village00Y1", ",country0001,districtY01,village00Y1", FRIDAY), payload(1));
        held.hold("practice001", HoldReason.PRACTICE, null,
            context("village00X1", ",country0001,districtX01,village00X1", SUNDAY), payload(1));
        decide("inXFriday02", HeldState.DISCARDED, null);

        assertThat(held.findAll(filter("districtX01", null, null, null, null)))
            .extracting(HeldSubmissionRow::uid)
            .containsExactlyInAnyOrder("inXFriday01", "inXFriday02", "practice001");
        assertThat(held.findAll(filter("districtX01", HoldReason.BEFORE_GO_LIVE, EnumSet.of(HeldState.HELD), null, null)))
            .extracting(HeldSubmissionRow::uid).containsExactly("inXFriday01");
        assertThat(held.findAll(filter(null, null, null, SATURDAY, null)))
            .extracting(HeldSubmissionRow::uid).containsExactly("practice001");
        assertThat(held.findAll(filter("village00X", null, null, null, null)))
            .as("an org unit uid is matched whole, never as a prefix").isEmpty();
        assertThat(held.find(filter(null, null, null, null, null), 0, 2)).hasSize(2);
        assertThat(held.findWithPayload(List.of("inXFriday02", "inYFriday01"), EnumSet.of(HeldState.HELD)))
            .extracting(HeldSubmissionRow::uid).containsExactly("inYFriday01");

        List<HeldSummaryRow> summary = held.summary(filter("districtX01", HoldReason.BEFORE_GO_LIVE, null, null, null));
        assertThat(summary).extracting(HeldSummaryRow::state, HeldSummaryRow::count)
            .containsExactlyInAnyOrder(
                org.assertj.core.groups.Tuple.tuple(HeldState.HELD, 1L),
                org.assertj.core.groups.Tuple.tuple(HeldState.DISCARDED, 1L));
    }

    @Test
    @WithMockUser(authorities = AuthoritiesConstants.USER)
    void fieldUsersCannotReachTheAdminRoutes() throws Exception {
        mockMvc.perform(get("/api/v1/admin/goLiveRules").param("activity", "campaign001"))
            .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/admin/heldSubmissions/summary"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = AuthoritiesConstants.ADMIN)
    void adminsCanReadHeldUploads() throws Exception {
        mockMvc.perform(get("/api/v1/admin/heldSubmissions/summary")).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/admin/heldSubmissions").param("state", "HELD", "DISCARDED"))
            .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/admin/heldSubmissions/unknown0001")).andExpect(status().isNotFound());
    }

    /** Records a decision as an admin would, against the row as it is now. */
    private void decide(String uid, HeldState state, String note) {
        HeldSubmissionRow row = held.findByUid(uid).orElseThrow();
        assertThat(held.markDecided(uid, state, EnumSet.of(HeldState.HELD, HeldState.DISCARDED),
            row.lastModifiedDate(), "admin", note)).isTrue();
    }

    private static HeldFilter filter(
        String orgUnit, HoldReason reason, EnumSet<HeldState> states, Instant enteredFrom, Instant enteredTo) {
        return new HeldFilter("campaign001", orgUnit, null, reason, states, enteredFrom, enteredTo);
    }

    private static HeldContext context(String orgUnit, String path, Instant startedAt) {
        return new HeldContext("campaign001", orgUnit, path, "household1", "version0001",
            "team0000001", "assignment1", "field-user", startedAt, startedAt.plusSeconds(600));
    }

    private ObjectNode payload(int edit) {
        return objectMapper.createObjectNode().put("uid", "heldSub0001").put("edit", edit);
    }
}
