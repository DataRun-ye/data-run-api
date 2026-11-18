package org.nmcpye.datarun.jpa.intake;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Which go-live rule applies to a submission, and whether the submission came
 * before it (DataRun-ye/data-run-api#42). Pure: no I/O, no clock.
 *
 * <p>The scope picks the rule; time never takes part in matching. The
 * submission's own org unit is looked at first, then each parent up to the
 * whole campaign, and the first level with a matching rule decides. At one
 * level, a rule for the submission's form beats that level's rule for every
 * form. So an area's exception ("this district starts Monday") applies to all
 * of that area's forms, unless the area also has a rule for a specific form.
 *
 * <p>The time compared with the rule is the earlier of when the form was
 * started on the phone and when the server received it: a phone clock set
 * ahead cannot make training data count as real, and a clock set behind can
 * only hold data, which an admin can release.
 */
public final class GoLivePolicy {

    private GoLivePolicy() {
    }

    /**
     * The org unit of {@code orgUnitPath} and its ancestors, nearest first.
     * Paths list the uids from the root down, comma separated, as in
     * {@code ",root,region,district"}.
     */
    public static List<String> areaChain(String orgUnitPath) {
        if (orgUnitPath == null || orgUnitPath.isBlank()) {
            return List.of();
        }
        List<String> chain = new ArrayList<>(Arrays.stream(orgUnitPath.split(","))
            .map(String::trim)
            .filter(uid -> !uid.isEmpty())
            .toList());
        Collections.reverse(chain);
        return chain;
    }

    /**
     * The rule that applies to a submission of {@code form} in the area
     * {@code areaChain} (nearest first), among one campaign's rules.
     */
    public static Optional<GoLiveRule> applicableRule(
        Collection<GoLiveRule> campaignRules,
        List<String> areaChain,
        String form) {
        if (campaignRules.isEmpty()) {
            return Optional.empty();
        }
        List<String> levels = new ArrayList<>(areaChain);
        levels.add(null); // the whole campaign
        for (String area : levels) {
            GoLiveRule everyForm = null;
            for (GoLiveRule rule : campaignRules) {
                if (!Objects.equals(rule.orgUnit(), area)) {
                    continue;
                }
                if (rule.form() == null) {
                    everyForm = rule;
                } else if (rule.form().equals(form)) {
                    return Optional.of(rule);
                }
            }
            if (everyForm != null) {
                return Optional.of(everyForm);
            }
        }
        return Optional.empty();
    }

    /** The earlier of the phone's start time and the server's receive time. */
    public static Instant effectiveTime(Instant startedAt, Instant receivedAt) {
        Objects.requireNonNull(receivedAt, "receivedAt");
        if (startedAt == null || startedAt.isAfter(receivedAt)) {
            return receivedAt;
        }
        return startedAt;
    }

    /** Whether a submission comes before {@code rule}'s go-live; the go-live itself is live. */
    public static boolean beforeGoLive(GoLiveRule rule, Instant startedAt, Instant receivedAt) {
        return effectiveTime(startedAt, receivedAt).isBefore(rule.liveFrom());
    }
}
