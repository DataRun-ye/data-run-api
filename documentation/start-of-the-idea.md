## Schema Recap

## Scenario Walkthroughs

|   #   | Scenario                                          | FlowType Configuration                                                                                                                             | FlowInstance Example                                                                                                                               | EntityInstance Behavior                                      |
|:-----:|---------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------|
| **1** | **One-Off Form** (no repeats, one implicit stage) | `submissionMode=SINGLE`, `scope=[{orgUnit}]`, `stages=[defauleStage]`                                                                              | `{id:"FI-1001", flowType:"simpleSurvey", scopeInstance:{orgUnit}, status:IN_PROGRESS}` → submit → status → COMPLETED                               | None                                                         |
| **2** | **Multi-Stage + Entity-Bound**                    | `submissionMode=MULTI_STAGE`,`scope=[{type: ORG_UNIT}, {type: ENTITY}]` `stages=[…,{"id":"enroll",…,"repeatable":true,"entityBound":"Patient"},…]` | `{id:"FI-2001", flowType:"householdSurvey", scopeInstance:{…}, status:IN_PROGRESS, stageStates:{}}` → submit each stage → final status → COMPLETED | On each “enroll” submit, upsert a new Patient EntityInstance |
| **3** | **Planned Visit to Existing Entity**              | Same as #2, plus in scopes include `"entityInstance":"HH999"`                                                                                      | `{id:"FI-3001", flowType:"householdSurvey", scopeInstance:{…,"entityInstance":"HH999"}, status:IN_PROGRESS}`                                       | Stage 1 submission link to existing HH999                    |
| **4** | **Ad-Hoc (“Log-As-You-Go”)**                      | `planningMode=LOG_AS_YOU_GO`, `submissionMode=SINGLE`, `stages=[]`                                                                                 | UI creates `{id:"FI-4001",flowType:"mobileCheckin",scopeInstance:{…},status:IN_PROGRESS}`, submit → status → COMPLETED                             | None                                                         |
| **5** | **Cross-Campaign Entity Reuse**                   | New FlowType with scope `entityInstance` required; single “visit” is data scoped by flow instance scope                                            | Bulk-create FlowInstances per existing `P001–P100`, each with `scopeInstance:{entityInstance=P00x}`, then submit → stages submissions              | add new rows to stage submissions                            |

* **StageDefinition**

    * `repeatable: Boolean` → allows **multiple** StageSubmissions for that stage.
    * `data_template_id` → points at the template.

* **StageSubmission**

    * One row per submission per stage (or one if single-stage).
    * Only if stages are bound to an entity, One row of ScopeInstance per stage with fks to both the `flow_instance_id`
      and the `stage_submission_id` (do we need the stageSubmission to fk its scope too?)

---

## Scenario 1: One-off form (no repeat, no stages)

* **Config**:

    * `FlowDefinition.submission_mode = SINGLE`
    * scope `at least orgUnit is in scope`
    * `stages = []` (empty → implicit single stage)
    * DataTemplate has no repeatable sections.

* **Flow**:

    1. User submits form → one `StageSubmission` row (with `stage_definition_id = NULL`).
    2. No entity logic invoked.

* **Result**:

    * Exactly as today—your legacy JSON-only behavior.
    * **No change** in your UI or backend except new table `stage_submissions`.

---

## Scenario 2: Single-stage, entity-bound repeats

* **Config**:

    * `FlowDefinition.submission_mode = SINGLE, scopeDefinitions = (at least orgUnit is in scope)`
    * DataTemplate has a **repeatable, entity-bound** section.

* **Flow**:

    1. User fills form → one `StageSubmission` row.
    2. **Post-hook** (or DB transaction) scans that submission for the entity-bound section.
    3. For each entry in the JSON array, **upsert** an `EntityInstance`, setting:

        * `entity_definition_id`
        * `flow_instance_id`
        * `stage_submission_id`
        * `identity_attributes` & `properties` from that row.

* **Result**:

    * The JSON still carries the raw array (for audit), but your repeat data now lives in `entity_instances`.
    * You can query `entity_instances WHERE flow_instance_id = X` to get all “repeat rows” as flat records.

---

## Scenario 4: Multi-stage, no repeats

* **Config**:

    * `submission_mode = MULTI_STAGE`, `stages = [ A, B, C ]`
    * Each StageDefinition has `repeatable = false`.
    * DataTemplates contain no repeatable sections.

* **Flow**:

    1. User submits Stage A → `StageSubmission(stage_id = A.id)`.
    2. Then Stage B → one row.
    3. Then Stage C → one row.
    4. No entity logic invoked.

* **Result**:

    * Each logical “stage” is its own JSON row in `stage_submissions`.
    * No changes to legacy repeats.

---

## Scenario 5: Multi-stage, entity-bound at one stage

* **Config**:

    *
  `stages = [ Registration (repeatable=false), MemberEnrollment (repeatable=true, entity-bound), FollowUp (repeatable=false) ]`.

* **Flow**:

    1. **Registration** submission → `StageSubmission(stage_id=Registration)`; no entities.
    2. **MemberEnrollment**: user can add N entries → each submission of that stage:

        * Creates one `StageSubmission(stage_id=MemberEnrollment)` per entry (because `repeatable=true`).
        * **Post-hook** for each submissions upserts a **single** `EntityInstance` (e.g. each Patient).
    3. **FollowUp** → one `StageSubmission(stage_id=FollowUp)`; may read or update existing entities.

* **Result**:

    * **Entities** appear only for the MemberEnrollment stage.
    * You get both the JSON submissions and a clean `entity_instances` table.

---

## Scenario 6: Planned visit to existing entity

* **Config**:

    * An `FlowInstance` is pre created along with scopeInstance **pre-linked** to an existing `entity_instance_id` (e.g.
      a Household you created earlier).
    * StageDefinition = single‐stage or multi‐stage.

* **Flow**:

    1. When rendering the form, your UI loads the `EntityInstance.identity_attributes` as initial values.
    2. On submit → one `StageSubmission`, then **post-hook** upserts the a ScopeInstance with date and stage linked to
       this.

* **Result**:

    * You’re truly “following up” on that entity—no new instances created.

---

## Scenario 7: Ad-hoc (“log-as-you-go”) submissions

* **Config**:

    * `planning_mode = LOG_AS_YOU_GO` in `FlowDefinition`.
    * You may or may not pre-create an `FlowInstance` record; UI auto-generates a transient one.

* **Flow**:

    1. UI creates a new `FlowInstance(planning_mode=LOG_AS_YOU_GO)` on first form open.
    2. User submits one or multiple stages as above (depending on `submission_mode` & `stages`).
    3. Entities created only if template sections demand it.

* **Result**:

    * Flexibility: you get the same stage/entity machinery even without pre-planning.
    * Later, you can reconcile/log those flowInstances like planned ones, or archive them.

---

## Key Takeaways

1. **Free repeats** (legacy nested arrays) remain untouched unless you explicitly **entity-bind** them.
2. **StageDefinition.repeatable** controls **how many** times you can submit a stage—but **not** whether it creates
   entities.
3. **EntityInstance** logic only fires when a section in the DataTemplate is flagged **entity-bound**.
4. You can progressively migrate old repeats by back-filling `entity_instances` from existing JSON, without disrupting
   current forms.
5. **Planned vs log-as-you-go**, **single vs multi-stage**, and **entity-bound vs free** are orthogonal dimensions—any
   combination is supported by our schema.

With this mapping, you can be confident:

* **Stages** fully replace legacy repeats for any use-case you flag as “entity-bound.”
* **Legacy behavior** lives on for all others.
* You avoid confusion or conflicting flows by keeping “free repeats” and “entity repeats” separate in your DataTemplate
  metadata.
