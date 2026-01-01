package org.nmcpye.datarun.party.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.JSONB;
import org.jooq.SelectConditionStep;
import org.jooq.impl.DSL;
import org.nmcpye.datarun.party.dto.ResolvedParty;
import org.nmcpye.datarun.party.entities.PartySet.PartySetSpec;
import org.nmcpye.datarun.party.entities.PartySetKind;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.UUID;

import static org.jooq.impl.DSL.*;
import static org.nmcpye.datarun.jooq.public_.tables.Party.PARTY;

// PartyQueryRepository.java
@Repository
@RequiredArgsConstructor
public class PartyQueryRepository {

    private final DSLContext dsl;

    /**
     * The Switchboard: Decides which query strategy to run based on the Spec.
     */
    public List<ResolvedParty> fetchParties(PartySetKind kind, PartySetSpec spec, String searchQuery) {
        var baseQuery = switch (kind) {
            case STATIC -> staticQuery(spec);
            case ORG_TREE -> orgTreeQuery(spec);
            case TAG_FILTER -> tagFilterQuery(spec);
            // Add QUERY/EXTERNAL cases later
            default -> throw new UnsupportedOperationException("Kind not implemented: " + kind);
        };

        // Apply Search Filter (if user is typing)
        if (StringUtils.hasText(searchQuery)) {
            baseQuery.and(PARTY.NAME.containsIgnoreCase(searchQuery));
        }

        return baseQuery
            .limit(20) // Always page results
            .fetchInto(ResolvedParty.class);
    }

    // Strategy 1: Static List
    private SelectConditionStep<?> staticQuery(PartySetSpec spec) {
//        return dsl.selectFrom(PARTY)
//            .where(PARTY.ID.in(spec.getMembers()));
        return dsl.selectFrom(PARTY)
            .where(trueCondition());
    }

    // Strategy 2: Recursive Org Tree
    private SelectConditionStep<?> orgTreeQuery(PartySetSpec spec) {
        // Define the recursive CTE
        // "Find root, then find all children where parent_id matches previous level"
        var partyId = PARTY.ID;
        var parentId = PARTY.PARENT_ID;

        var cteName = name("org_tree_cte");
        var id = name("id");
        var cte = dsl.withRecursive(cteName)
            .as(
                dsl.select(partyId, parentId).from(PARTY).where(partyId.eq(spec.getRootId()))
                    .unionAll(
                        dsl.select(PARTY.ID, PARTY.PARENT_ID)
                            .from(PARTY)
                            .join(cteName).on(PARTY.PARENT_ID.eq(field(name(cteName, id), UUID.class)))
                    )
            );

        return cte.select(PARTY.asterisk())
            .from(PARTY)
            .join(cteName).on(PARTY.ID.eq(field(name(cteName, id), UUID.class)))
            .where(trueCondition()); // Placeholder for subsequent .and() calls
    }

    // Strategy 3: JSONB Tag Filter
    private SelectConditionStep<?> tagFilterQuery(PartySetSpec spec) {
        // Postgres JSONB containment operator: tags @> '["cold_chain"]'
        Condition tagCondition = null;
        try {
            tagCondition = DSL.condition("tags @> ?::jsonb",
                JSONB.valueOf(new ObjectMapper().writeValueAsString(spec.getTags()))
            );
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        return dsl.selectFrom(PARTY)
            .where(tagCondition)
            .and(PARTY.SOURCE_TYPE.in(spec.getTypes()));
    }
}
