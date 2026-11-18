package org.nmcpye.datarun.jpa.intake;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class GoLivePolicyTest {

    private static final Instant SATURDAY = Instant.parse("2026-11-21T00:00:00Z");
    private static final Instant FRIDAY = Instant.parse("2026-11-20T00:00:00Z");
    private static final Instant SUNDAY = Instant.parse("2026-11-22T00:00:00Z");
    private static final Instant MONDAY = Instant.parse("2026-11-23T00:00:00Z");

    private static final String HOUSEHOLD_FORM = "household1";
    private static final String WAREHOUSE_FORM = "warehouse01";

    /** Districts X and Y in one governorate: path root, governorate, district. */
    private static final List<String> IN_X = GoLivePolicy.areaChain(",country0001,governor001,districtX01");
    private static final List<String> IN_Y = GoLivePolicy.areaChain(",country0001,governor001,districtY01");

    private static GoLiveRule rule(String uid, String orgUnit, String form, Instant liveFrom) {
        return new GoLiveRule(uid, "campaign001", orgUnit, form, liveFrom, null, "admin", SATURDAY);
    }

    private static final GoLiveRule CAMPAIGN = rule("ruleCampain", null, null, SATURDAY);
    private static final GoLiveRule WAREHOUSES = rule("ruleWarehse", null, WAREHOUSE_FORM, FRIDAY);
    private static final GoLiveRule DISTRICT_X = rule("ruleDistrX1", "districtX01", null, MONDAY);
    private static final GoLiveRule WAREHOUSES_IN_X = rule("ruleWhInX01", "districtX01", WAREHOUSE_FORM, SUNDAY);

    private static Optional<String> applies(List<GoLiveRule> rules, List<String> area, String form) {
        return GoLivePolicy.applicableRule(rules, area, form).map(GoLiveRule::uid);
    }

    @Test
    void readsTheAreaChainNearestFirst() {
        assertThat(IN_X).containsExactly("districtX01", "governor001", "country0001");
        assertThat(GoLivePolicy.areaChain(null)).isEmpty();
        assertThat(GoLivePolicy.areaChain(",")).isEmpty();
    }

    @Test
    void noRuleMeansNothingIsHeld() {
        assertThat(applies(List.of(), IN_X, HOUSEHOLD_FORM)).isEmpty();
    }

    @Test
    void aFormRuleBeatsTheCampaignRuleAtTheSameLevel() {
        List<GoLiveRule> rules = List.of(CAMPAIGN, WAREHOUSES);

        assertThat(applies(rules, IN_Y, HOUSEHOLD_FORM)).contains("ruleCampain");
        assertThat(applies(rules, IN_Y, WAREHOUSE_FORM)).contains("ruleWarehse");
    }

    @Test
    void anAreaRuleCoversEveryFormOfThatArea() {
        List<GoLiveRule> rules = List.of(CAMPAIGN, WAREHOUSES, DISTRICT_X);

        assertThat(applies(rules, IN_X, HOUSEHOLD_FORM)).contains("ruleDistrX1");
        assertThat(applies(rules, IN_X, WAREHOUSE_FORM))
            .as("the district's exception applies to all its forms, the warehouse form too")
            .contains("ruleDistrX1");
        assertThat(applies(rules, IN_Y, WAREHOUSE_FORM)).contains("ruleWarehse");
    }

    @Test
    void aFormRuleInAnAreaBeatsThatAreasRule() {
        List<GoLiveRule> rules = List.of(CAMPAIGN, WAREHOUSES, DISTRICT_X, WAREHOUSES_IN_X);

        assertThat(applies(rules, IN_X, WAREHOUSE_FORM)).contains("ruleWhInX01");
        assertThat(applies(rules, IN_X, HOUSEHOLD_FORM)).contains("ruleDistrX1");
    }

    @Test
    void aParentAreasRuleAppliesBelowIt() {
        GoLiveRule governorate = rule("ruleGovern1", "governor001", null, SUNDAY);
        List<GoLiveRule> rules = List.of(CAMPAIGN, governorate, DISTRICT_X);

        assertThat(applies(rules, IN_X, HOUSEHOLD_FORM)).contains("ruleDistrX1");
        assertThat(applies(rules, IN_Y, HOUSEHOLD_FORM)).contains("ruleGovern1");
    }

    @Test
    void aFormRuleOfAnotherFormDoesNotApply() {
        List<GoLiveRule> rules = List.of(WAREHOUSES);

        assertThat(applies(rules, IN_Y, HOUSEHOLD_FORM)).isEmpty();
    }

    @Test
    void theEarlierOfPhoneStartAndServerReceiptIsCompared() {
        Instant thursday = Instant.parse("2026-11-19T10:00:00Z");

        assertThat(GoLivePolicy.effectiveTime(thursday, MONDAY)).isEqualTo(thursday);
        assertThat(GoLivePolicy.effectiveTime(null, MONDAY)).isEqualTo(MONDAY);
        assertThat(GoLivePolicy.effectiveTime(MONDAY.plusSeconds(86_400), MONDAY))
            .as("a phone clock set ahead cannot move data past the go-live")
            .isEqualTo(MONDAY);
    }

    @Test
    void dataStartedBeforeTheGoLiveIsHeldEvenWhenSentAfterIt() {
        Instant thursday = Instant.parse("2026-11-19T10:00:00Z");

        assertThat(GoLivePolicy.beforeGoLive(CAMPAIGN, thursday, MONDAY)).isTrue();
        assertThat(GoLivePolicy.beforeGoLive(CAMPAIGN, SUNDAY, MONDAY)).isFalse();
        assertThat(GoLivePolicy.beforeGoLive(CAMPAIGN, SATURDAY, SUNDAY))
            .as("the go-live instant itself is live")
            .isFalse();
        assertThat(GoLivePolicy.beforeGoLive(CAMPAIGN, SUNDAY, FRIDAY))
            .as("a clock set ahead on Friday is still before Saturday's go-live")
            .isTrue();
    }
}
