
## Some Ideas for an abstraction level that might be good to think about:

**EntityInstance:**

- entity_instance_table: one record per entity created with some rarely changing attributes as jsonb column.
- an entity instance record is created by dedicated flow scope instantiation form providing the entity attribute
  along any other optional flow scope properties defined in the same scope.
- the dedicated flow instantiation creates a new flowInstance linked to a scope instance which is json:
  A flowInstance post request with a new Entity Instance created at the flowScope level:
    ```json
    {
        "flowInstanceId": "FI-001",
        "flowTypeId": "FT-001",
        "stageStats": "...",
        "scopeInstance": {
            "id": "SI-001",
            "flowInstanceId": "FI-001",
            "stageSubmissionId": null, // when the scope instance is create or linked from a stage submission
            "team": "teamId",
            "orgUnit": "ouId",
            "entityInstance": {
                "Id": "EI-001",
                "eiAttribute1": "--",
                "eiAttribute2": "--"
            }
        }
    }
    ```

      - flowInstance post request linked to a pre-existing entity instance: `{"flowInstance": {"flowInstanceId": FI-001, scopes: {"team": teamId, …, "entityInstance": EI-001}, …, "stageStats":…,}, "entityInstanceData": null}` just linking to it.
      - the post request would save one record into the flow_instance table and if has a new entity it would upsert a new one record into entity_instances table.
      - a one stageSubmission post request : `{"stage": {flowInstanceId": FI-001, "stageId": SS-001, "entityInstanceId": EI-001, "testResuleDataElement": …, "anotherStageDataElement": …}, entityCreated: {}}` 
      - posting flow would add a new 
    - also an entity instance can be created/or linked from a flow stage submission, when the stage is entity-bound and
      the
      entity is new.
    - flow scope / stageSubmission creating the entity would upsert a new flow instance, and entity instance, the flow
      instance will be linked to the entity.
    - if the entity instantiation was from withing an entity bound stage (not the flow instance) the stage would be
      linked
      to the entity instance and the over all flow of the stage) but stored separately,
    - flow instances and Stage Submissions are grouped/link back to the scope they create

2. **Flow limiting using Scopes:** by a specific scope attribute on the flowType level, for Example:

|   #   | Scenario                                                 | FlowType Configuration                                         | FlowInstance Example                                                                                                    | EntityInstance Behavior                                     |
|:-----:|----------------------------------------------------------|----------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------|
| **3** | **Planned Visit to Existing Entity**                     | Scopes: `"entityInstanceType: HH, and entityInstance":"HH999"` | `{id:"FI-3001", flowType:"householdSurvey", scopes:{…,"entityInstance":"HH999"}, status:IN_PROGRESS}`                   | Stage 1 submission grouped by existing HH999                |
| **3** | **Planned Visit to Existing Entity, by a specific team** | Scopes: `"team: teamId, and entityInstance":"HH999"`           | `{id:"FI-3001", flowType:"householdSurvey", scopes:{…,"team": "teamId", "entityInstance":"HH999"}, status:IN_PROGRESS}` | Stage submissions linked (or grouped) per HH999, and teamId |

---

now entity instances can be bound on flow instance level scope, or on a stage level. I feel this part have some edge wierdness that would surface when I want group stage submissions, per some parent flow scope, plus enitity instance table should have one record per unique entity_instance, and the stage submissions can be about them grouped by an entity when its on a flow instance scope, by other elements in same flow instance scope, but how about when we start having entities bound to stages themselves, (now we have entities that might be bound to parent scope and an entity that might be bound to stage in same flow, are'nt we? what's the relation, maybe the entity creation/or linking to pre-existing process whether in flow scope or in stage needs to be abstracted away and be in a dedicated form that fills/update the identifying attributes of the entity which are rarely change, and referencing pre-existing shouldn't effect them only link to the entity id.
do we have another dedicated level of abstraction: i.e `ScopeInstance` which then can be created from either a parent flow instance (entity is an element of the flow scope) and/or link to it at the sametime, or from a stageSubmission for entity bound stages (which can be repeatable (differnt enities per row)). maybe when created from stage it fills the scopeinstance properties from the parent context i.e scope elements plus the entity, here we would have stage submissions also scoped by parent and the entity. Scope instances would refernce (flow Instance, stageSubmission or null, enity, other scope elements).
for stage submissions bound to an entity (new and pre-existing) then i have:
- a stage submission always scoped by its parent flow instance scope (including enity if in scope).
- the stage submission would: link back to its parent scope elements, and reference the entity from anywhere (not bound to parent flow because we might have repeated stages referencing many entities)

But what about if in parent there are an entity? creating scope instance record would require properties, do we stick to jsonb scopeInstance? or do we prevent entity-bound stages to parents that has entities in scope (inconsistency or a room of an edge issues).

```json
{
    "flowInstance": {
        "flowInstanceId": "FI-001",
        "flowTypeId": "FT-001",
        "stageStats": "...",
		// flow scope bound entity and other elements (i.e TEAM, ORGUNIT, ACTIVITY, DATE)
        "scopeInstance": {
            "id": "SI-001",
            "flowInstanceId": "FI-001",
            "stageSubmissionId": null,  // null , not the stage that created or referenced it
            "team": "teamId",
            "orgUnit": "ouId",
			// flow scope entity (new or pre-existing) 
            "entityInstance": {
                "Id": "EI-001",
                "eiAttribute1": "--",
                "eiAttribute2": "--"
            }
        }
    },
    "stageSubmissions": [
        {
            "stageId": "SS-001",
            "stageTypeId": "ST-0001",
            "flowInstanceId": "FI-001",
            "flowTypeId": "FT-001",
            "scopeInstance": {			
                "id": "SI-002",
				"stageSubmissionId": "SS-001", // not null 
				"flowInstanceId": "FI-001",
				// filled from context of this stage, from parent's scope elements team, orgUnit ... and other but then what about if in parent there are an entity,                 
                "team": "teamId",
                "orgUnit": "ouId",
				// pre-existing or a new one, this stage submissions could then be grouped by its overall context (parent and its entity)
                "entityInstance": {
                    "Id": "EI-001",
                    "eiAttribute1": "--",
                    "eiAttribute2": "--"
                }
            }
        }
    ]
}
```

---

First, please briefly restate your understanding of the requirements (in 2–3 sentences) to confirm alignment. Then list any assumptions you make. Next, provide the design or code in clearly labeled sections (e.g., “Design Proposal”, “Code Example”, “Testing”). Finally, end with “Next Steps / Questions” that I should consider. Please do not add extra topics beyond the scope asked

