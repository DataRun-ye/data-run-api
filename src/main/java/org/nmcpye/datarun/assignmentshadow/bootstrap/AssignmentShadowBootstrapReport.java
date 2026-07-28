package org.nmcpye.datarun.assignmentshadow.bootstrap;

import java.util.List;

public record AssignmentShadowBootstrapReport(
    long baselineTupleCount,
    long shadowTupleCount,
    long baselineOnlyCount,
    long shadowOnlyCount,
    List<MismatchSample> mismatchSamples,
    ItemCount actorAliases,
    ItemCount orgUnitAliases,
    ItemCount roles,
    ItemCount identities,
    ItemCount events,
    ItemCount activeGrants,
    ItemCount endedGrants,
    long retiredRows,
    long disabledRows,
    long noActorRows,
    long emptyFormSetRows,
    long nullScopeRows,
    long malformedRows,
    long rawActiveGrantCount,
    long distinctEffectiveAccessCount,
    long overlapCount,
    long retiredGrantMismatchCount
) {

    public AssignmentShadowBootstrapReport {
        mismatchSamples = List.copyOf(mismatchSamples).stream().limit(10).toList();
    }

    public boolean successful() {
        return baselineOnlyCount == 0
            && shadowOnlyCount == 0
            && retiredGrantMismatchCount == 0;
    }

    public String toOperatorText() {
        StringBuilder output = new StringBuilder();
        output.append("assignment-shadow-bootstrap/v1\n");
        output.append("status=").append(successful() ? "SUCCESS" : "MISMATCH").append('\n');
        output.append("comparison_scope=direct_team_assignment_capture_authority\n");
        output.append(
            "comparison_excludes=administrator_bypass,managed_team,user_group,view_only,delete_only\n"
        );
        append(output, "baseline_tuple_count", baselineTupleCount);
        append(output, "shadow_tuple_count", shadowTupleCount);
        append(output, "baseline_except_shadow_count", baselineOnlyCount);
        append(output, "shadow_except_baseline_count", shadowOnlyCount);
        append(output, "aliases_created", actorAliases.created() + orgUnitAliases.created());
        append(output, "aliases_existing", actorAliases.existing() + orgUnitAliases.existing());
        append(output, "actor_aliases_created", actorAliases.created());
        append(output, "actor_aliases_existing", actorAliases.existing());
        append(output, "org_unit_aliases_created", orgUnitAliases.created());
        append(output, "org_unit_aliases_existing", orgUnitAliases.existing());
        append(output, "roles_created", roles.created());
        append(output, "roles_existing", roles.existing());
        append(output, "identities_created", identities.created());
        append(output, "identities_existing", identities.existing());
        append(output, "events_created", events.created());
        append(output, "events_existing", events.existing());
        append(output, "active_grants_created", activeGrants.created());
        append(output, "active_grants_existing", activeGrants.existing());
        append(output, "ended_grants_created", endedGrants.created());
        append(output, "ended_grants_existing", endedGrants.existing());
        append(output, "observed_retired_rows", retiredRows);
        append(output, "excluded_disabled_rows", disabledRows);
        append(output, "excluded_no_actor_rows", noActorRows);
        append(output, "excluded_empty_form_set_rows", emptyFormSetRows);
        append(output, "excluded_null_scope_rows", nullScopeRows);
        append(output, "excluded_malformed_rows", malformedRows);
        append(output, "raw_active_grant_count", rawActiveGrantCount);
        append(output, "distinct_effective_access_count", distinctEffectiveAccessCount);
        append(output, "overlap_count", overlapCount);
        append(output, "retired_grant_mismatch_count", retiredGrantMismatchCount);
        mismatchSamples.forEach(sample -> output
            .append("mismatch_sample=")
            .append(sanitize(sample.side())).append('|')
            .append(sanitize(sample.userUid())).append('|')
            .append(sanitize(sample.activityUid())).append('|')
            .append(sanitize(sample.orgUnitUid())).append('|')
            .append(sanitize(sample.formUid())).append('\n'));
        return output.toString();
    }

    private static void append(StringBuilder output, String name, long value) {
        output.append(name).append('=').append(value).append('\n');
    }

    private static String sanitize(String value) {
        return value == null ? "<null>" : value.replaceAll("[\\p{Cntrl}|]", "?");
    }

    public record ItemCount(long created, long existing) {
    }

    public record MismatchSample(
        String side,
        String userUid,
        String activityUid,
        String orgUnitUid,
        String formUid
    ) {
    }
}
