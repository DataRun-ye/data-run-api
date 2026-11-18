package org.nmcpye.datarun.jpa.intake;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nmcpye.datarun.common.exceptions.IllegalQueryException;
import org.nmcpye.datarun.jpa.activity.repository.ActivityRepository;
import org.nmcpye.datarun.jpa.datatemplate.repository.DataTemplateRepository;
import org.nmcpye.datarun.jpa.orgunit.repository.OrgUnitRepository;
import org.nmcpye.datarun.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeSet;
import java.util.function.Predicate;

/**
 * Admin operations on go-live rules (DataRun-ye/data-run-api#42). Changing a
 * rule affects uploads from then on; uploads already held stay held until an
 * admin releases them, for example all those the current rules now accept.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GoLiveRuleService {

    private final GoLiveRuleRepository rules;
    private final HeldSubmissionRepository held;
    private final ActivityRepository activities;
    private final OrgUnitRepository orgUnits;
    private final DataTemplateRepository forms;

    /** A rule as saved, and how many held uploads of its campaign the rules now accept. */
    public record PutResult(GoLiveRule rule, long heldNowAccepted) {
    }

    /** The go-live that applies to one form of a campaign in an area, and the rule that sets it. */
    public record EffectiveGoLive(String form, Instant liveFrom, String rule, String ruleOrgUnit, String ruleForm) {
    }

    public List<GoLiveRule> list(String activity) {
        return rules.findByActivity(requireActivity(activity));
    }

    public PutResult put(String activity, String orgUnit, String form, Instant liveFrom, String note) {
        requireActivity(activity);
        if (orgUnit != null && !Boolean.TRUE.equals(orgUnits.existsByUid(orgUnit))) {
            throw new IllegalQueryException("Org unit not found: " + orgUnit);
        }
        if (form != null && forms.findByUid(form).isEmpty()) {
            throw new IllegalQueryException("Form not found: " + form);
        }
        if (liveFrom == null) {
            throw new IllegalQueryException("liveFrom is required");
        }
        String modifiedBy = SecurityUtils.getCurrentUserLogin().orElse("system");
        GoLiveRule rule = rules.put(activity, orgUnit, form, liveFrom, note, modifiedBy);
        log.info("Go-live rule {} set by {}: activity {}, org unit {}, form {}, live from {}",
            rule.uid(), modifiedBy, activity, orgUnit, form, liveFrom);
        return new PutResult(rule, countHeldNowAccepted(activity));
    }

    public boolean delete(String uid) {
        Optional<GoLiveRule> rule = rules.findByUid(uid);
        boolean deleted = rules.delete(uid);
        rule.ifPresent(removed -> log.info("Go-live rule {} of activity {} removed by {}",
            uid, removed.activity(), SecurityUtils.getCurrentUserLogin().orElse("system")));
        return deleted;
    }

    /**
     * For every form of the campaign, the go-live that applies in {@code orgUnit}
     * (null: anywhere without an area rule). Read-only transaction: an org
     * unit's ancestors are its lazily loaded parents.
     */
    @Transactional(readOnly = true)
    public List<EffectiveGoLive> effective(String activity, String orgUnit) {
        requireActivity(activity);
        List<String> areaChain = List.of();
        if (orgUnit != null) {
            areaChain = orgUnits.findByUid(orgUnit).map(SubmissionIntake::areaChainOf)
                .orElseThrow(() -> new IllegalQueryException("Org unit not found: " + orgUnit));
        }
        List<GoLiveRule> campaignRules = rules.findByActivity(activity);
        TreeSet<String> campaignForms = new TreeSet<>(rules.formsOfActivity(activity));
        campaignRules.stream().map(GoLiveRule::form).filter(f -> f != null).forEach(campaignForms::add);

        List<String> chain = areaChain;
        return campaignForms.stream()
            .map(form -> GoLivePolicy.applicableRule(campaignRules, chain, form)
                .map(rule -> new EffectiveGoLive(form, rule.liveFrom(), rule.uid(), rule.orgUnit(), rule.form()))
                .orElse(new EffectiveGoLive(form, null, null, null, null)))
            .toList();
    }

    /**
     * Held uploads the current rules would now save: held before a go-live
     * that has since moved or been removed. Practice uploads never qualify.
     */
    public Predicate<HeldSubmissionRow> acceptedByCurrentRules() {
        Map<String, List<GoLiveRule>> rulesByActivity = new HashMap<>();
        return row -> {
            if (row.reason() != HoldReason.BEFORE_GO_LIVE) {
                return false;
            }
            List<GoLiveRule> campaignRules = rulesByActivity.computeIfAbsent(row.activity(), rules::findByActivity);
            return GoLivePolicy.applicableRule(
                    campaignRules,
                    SubmissionIntake.areaChain(row.orgUnit(), row.orgUnitPath()),
                    row.form())
                .map(rule -> !GoLivePolicy.beforeGoLive(rule, row.startedAt(), row.receivedAt()))
                .orElse(true);
        };
    }

    private long countHeldNowAccepted(String activity) {
        HeldFilter waiting = new HeldFilter(
            activity, null, null, HoldReason.BEFORE_GO_LIVE, EnumSet.of(HeldState.HELD), null, null);
        return held.findAll(waiting).stream().filter(acceptedByCurrentRules()).count();
    }

    private String requireActivity(String activity) {
        if (activity == null || !Boolean.TRUE.equals(activities.existsByUid(activity))) {
            throw new IllegalQueryException("Activity not found: " + activity);
        }
        return activity;
    }
}
