package org.nmcpye.etl.inventory;

import lombok.AllArgsConstructor;
import org.nmcpye.etl.translation.SubmissionContext;

import java.util.function.Function;

@AllArgsConstructor
public enum FlowStrategy {
    // Routine: Team is Actor, OrgUnit is Context (Stocktake/Usage)
    ROUTINE_CONTEXT_ONLY((ctx) -> ctx.getTeam(), (ctx) -> ctx.getOrgUnit()),

    // Campaign: Team issues to OrgUnit
    CAMPAIGN_ISSUANCE((ctx) -> ctx.getTeam(), (ctx) -> ctx.getOrgUnit()),

    // Campaign: MU issues to Team (Team UID is inside JSON)
    MU_TO_TEAM_IN_FORM((ctx) -> ctx.getTeam(), (ctx) -> ctx.extractJsonField("team_uid")),

    // Campaign: Team returns to MU
    TEAM_TO_MU_RETURN((ctx) -> ctx.extractJsonField("team_uid"), (ctx) -> ctx.getTeam());

    private final Function<SubmissionContext, Actor> sourceResolver;
    private final Function<SubmissionContext, Actor> destResolver;
}
