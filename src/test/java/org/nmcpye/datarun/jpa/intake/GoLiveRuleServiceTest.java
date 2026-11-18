package org.nmcpye.datarun.jpa.intake;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.nmcpye.datarun.common.exceptions.IllegalQueryException;
import org.nmcpye.datarun.jpa.activity.repository.ActivityRepository;
import org.nmcpye.datarun.jpa.datatemplate.DataTemplate;
import org.nmcpye.datarun.jpa.datatemplate.repository.DataTemplateRepository;
import org.nmcpye.datarun.jpa.orgunit.OrgUnit;
import org.nmcpye.datarun.jpa.orgunit.repository.OrgUnitRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GoLiveRuleServiceTest {

    private static final Instant THURSDAY = Instant.parse("2026-11-19T00:00:00Z");
    private static final Instant FRIDAY = Instant.parse("2026-11-20T00:00:00Z");
    private static final Instant SATURDAY = Instant.parse("2026-11-21T00:00:00Z");
    private static final Instant MONDAY = Instant.parse("2026-11-23T00:00:00Z");

    private GoLiveRuleRepository rules;
    private HeldSubmissionRepository held;
    private ActivityRepository activities;
    private OrgUnitRepository orgUnits;
    private DataTemplateRepository forms;
    private GoLiveRuleService service;

    @BeforeEach
    void setUp() {
        rules = mock(GoLiveRuleRepository.class);
        held = mock(HeldSubmissionRepository.class);
        activities = mock(ActivityRepository.class);
        orgUnits = mock(OrgUnitRepository.class);
        forms = mock(DataTemplateRepository.class);
        service = new GoLiveRuleService(rules, held, activities, orgUnits, forms);
        when(activities.existsByUid("campaign001")).thenReturn(true);
    }

    private static GoLiveRule rule(String uid, String orgUnit, String form, Instant liveFrom) {
        return new GoLiveRule(uid, "campaign001", orgUnit, form, liveFrom, null, "admin", SATURDAY);
    }

    @Test
    void aRuleNeedsAnExistingCampaignAreaAndFormAndAGoLive() {
        when(orgUnits.existsByUid("districtX01")).thenReturn(false);
        when(forms.findByUid("household1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.put("unknown0001", null, null, SATURDAY, null))
            .isInstanceOf(IllegalQueryException.class).hasMessageContaining("Activity not found");
        assertThatThrownBy(() -> service.put("campaign001", "districtX01", null, SATURDAY, null))
            .isInstanceOf(IllegalQueryException.class).hasMessageContaining("Org unit not found");
        assertThatThrownBy(() -> service.put("campaign001", null, "household1", SATURDAY, null))
            .isInstanceOf(IllegalQueryException.class).hasMessageContaining("Form not found");
        assertThatThrownBy(() -> service.put("campaign001", null, null, null, null))
            .isInstanceOf(IllegalQueryException.class).hasMessageContaining("liveFrom");
        verify(rules, never()).put(any(), any(), any(), any(), any(), any());
    }

    @Test
    void movingAGoLiveReportsHowManyHeldUploadsTheRulesNowAccept() {
        GoLiveRule moved = rule("ruleCampain", null, null, THURSDAY);
        when(rules.put("campaign001", null, null, THURSDAY, "started early", "system")).thenReturn(moved);
        when(rules.findByActivity("campaign001")).thenReturn(List.of(moved));
        when(held.findAll(any())).thenReturn(List.of(
            heldRow("startedFri01", HoldReason.BEFORE_GO_LIVE, FRIDAY),
            heldRow("startedWed01", HoldReason.BEFORE_GO_LIVE, THURSDAY.minusSeconds(86_400)),
            heldRow("practice001", HoldReason.PRACTICE, FRIDAY)));

        GoLiveRuleService.PutResult result = service.put("campaign001", null, null, THURSDAY, "started early");

        assertThat(result.rule()).isEqualTo(moved);
        assertThat(result.heldNowAccepted()).as("Friday's upload now falls after Thursday's go-live").isEqualTo(1);
    }

    @Test
    void removingTheOnlyRuleAcceptsWhatItHeldButNeverPractice() {
        when(rules.findByActivity("campaign001")).thenReturn(List.of());

        var accepted = service.acceptedByCurrentRules();

        assertThat(accepted.test(heldRow("startedFri01", HoldReason.BEFORE_GO_LIVE, FRIDAY))).isTrue();
        assertThat(accepted.test(heldRow("practice001", HoldReason.PRACTICE, FRIDAY))).isFalse();
    }

    @Test
    void theEffectiveGoLiveOfEveryFormInAnArea() {
        when(rules.findByActivity("campaign001")).thenReturn(List.of(
            rule("ruleCampain", null, null, SATURDAY),
            rule("ruleWarehse", null, "warehouse01", FRIDAY),
            rule("ruleDistrX1", "districtX01", null, MONDAY)));
        when(rules.formsOfActivity("campaign001")).thenReturn(List.of("household1", "warehouse01"));
        OrgUnit country = orgUnit("country0001", null);
        when(orgUnits.findByUid("village00X1"))
            .thenReturn(Optional.of(orgUnit("village00X1", orgUnit("districtX01", country))));
        when(orgUnits.findByUid("village00Y1"))
            .thenReturn(Optional.of(orgUnit("village00Y1", orgUnit("districtY01", country))));

        assertThat(service.effective("campaign001", "village00X1"))
            .extracting(GoLiveRuleService.EffectiveGoLive::form, GoLiveRuleService.EffectiveGoLive::liveFrom,
                GoLiveRuleService.EffectiveGoLive::rule)
            .containsExactly(
                tuple("household1", MONDAY, "ruleDistrX1"),
                tuple("warehouse01", MONDAY, "ruleDistrX1"));
        assertThat(service.effective("campaign001", "village00Y1"))
            .extracting(GoLiveRuleService.EffectiveGoLive::form, GoLiveRuleService.EffectiveGoLive::liveFrom)
            .containsExactly(tuple("household1", SATURDAY), tuple("warehouse01", FRIDAY));
        assertThat(service.effective("campaign001", null))
            .extracting(GoLiveRuleService.EffectiveGoLive::liveFrom)
            .containsExactly(SATURDAY, FRIDAY);
    }

    @Test
    void aFormWithoutAnyRuleHasNoGoLive() {
        when(rules.findByActivity("campaign001")).thenReturn(List.of(rule("ruleWarehse", null, "warehouse01", FRIDAY)));
        when(rules.formsOfActivity("campaign001")).thenReturn(List.of("household1"));
        when(forms.findByUid(anyString())).thenReturn(Optional.of(new DataTemplate()));

        assertThat(service.effective("campaign001", null))
            .extracting(GoLiveRuleService.EffectiveGoLive::form, GoLiveRuleService.EffectiveGoLive::liveFrom)
            .containsExactly(tuple("household1", null), tuple("warehouse01", FRIDAY));
    }

    /** An org unit's path is built from its parents, not set. */
    private static OrgUnit orgUnit(String uid, OrgUnit parent) {
        OrgUnit unit = new OrgUnit();
        unit.setUid(uid);
        unit.setParent(parent);
        return unit;
    }

    private static HeldSubmissionRow heldRow(String uid, HoldReason reason, Instant startedAt) {
        return new HeldSubmissionRow(uid, reason, HeldState.HELD, "campaign001", "village00X1",
            ",country0001,districtX01,village00X1", "household1", "version0001", "team0000001", "assignment1",
            "field-user", startedAt, startedAt.plusSeconds(600), null, null, null, null, null, null,
            startedAt, null);
    }
}
