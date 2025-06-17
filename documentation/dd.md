### How It Works

1. **Root Scope Creation** (at flow start):
    - When a flow instance is created, we also create a `ScopeInstance` row:
    - `flow_instance_id` = the new flow instance ID
    - `stage_submission_id` = null
    - `custom_dimensions` = JSON with the root scopes (orgUnit, date, etc.)
2. **Stage Scope Creation** (when a stage is submitted and it defines a scope):
    - When a stage submission occurs, if the stage has a scope definition, we create a `ScopeInstance` row:
    - `flow_instance_id` = null
    - `stage_submission_id` = the new stage submission ID
    - `custom_dimensions` = the stage-specific scopes (like itemId, batch, etc.)
    - Optionally, if the stage is entity-bound, we set `entity_instance_id` to the entity ID (which might be existing or
      newly created).

- To prevent duplicate scopes for the same stage submission and ensure one root per flow:

```sql
-- Add constraints
ALTER TABLE scope_instance
    ADD CONSTRAINT scope_owner_check CHECK (
        (flow_instance_id IS NOT NULL AND stage_submission_id IS NULL) OR
        (flow_instance_id IS NULL AND stage_submission_id IS NOT NULL)
        );

-- One root scope per flow
CREATE UNIQUE INDEX unique_root_scope_per_flow ON scope_instance (flow_instance_id)
    WHERE stage_submission_id IS NULL;

-- One scope per stage submission (if any)
CREATE UNIQUE INDEX unique_stage_scope
    ON scope_instance (stage_submission_id)
    WHERE stage_submission_id IS NOT NULL;
```

### Reporting Example

**Goal**: Get all stage submissions for a particular item (entity) with their flow context.

```sql
SELECT ss.id                      AS stage_submission_id,
       fi.id                      AS flow_instance_id,
       si_stage.custom_dimensions AS stage_scope,
       si_root.custom_dimensions  AS flow_scope

FROM stage_submission ss
         JOIN scope_instance si_stage ON ss.id = si_stage.stage_submission_id
         JOIN flow_instance fi ON ss.flow_instance_id = fi.id
         JOIN scope_instance si_root ON fi.id = si_root.flow_instance_id AND si_root.stage_submission_id IS NULL
WHERE si_stage.entity_instance_id = 'item-123'; -- or use custom_dimensions if multiple entities
```

Unified Reporting:

```sql
/* Simple unified scope query */
SELECT COALESCE(si_stage.scope_data, si_root.scope_data)
FROM flow_instances fi
         JOIN scope_instance si_root ON fi.id = si_root.flow_instance_id
         LEFT JOIN stage_submissions ss ON fi.id = ss.flow_instance_id
         LEFT JOIN scope_instance si_stage ON ss.id = si_stage.stage_submission_id
```

**Audit Trail Implementation**

```sql
CREATE TABLE scope_history
(
    id         BIGSERIAL PRIMARY KEY,
    scope_id   UUID NOT NULL,
    old_data   JSONB,
    new_data   JSONB,
    changed_at TIMESTAMPTZ DEFAULT NOW(),
    changed_by UUID
);

CREATE FUNCTION log_scope_change() RETURNS TRIGGER AS
$$
BEGIN
    IF OLD.scope_data IS DISTINCT FROM NEW.scope_data THEN
        INSERT INTO scope_history(scope_id, old_data, new_data)
        VALUES (OLD.id, OLD.scope_data, NEW.scope_data);
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER scope_audit
    BEFORE UPDATE
    ON scope_instance
    FOR EACH ROW
EXECUTE FUNCTION log_scope_change();
```

Reporting Query Example

```sql
WITH unified_scopes AS (SELECT fi.id                                             AS flow_id,
                               ss.id                                             AS stage_id,
                               COALESCE(si_stage.scope_data, si_root.scope_data) AS effective_scope
                        FROM flow_instances fi
                                 JOIN scope_instance si_root ON fi.id = si_root.flow_instance_id
                                 LEFT JOIN stage_submissions ss ON fi.id = ss.flow_instance_id
                                 LEFT JOIN scope_instance si_stage ON ss.id = si_stage.stage_submission_id)
SELECT effective_scope ->> 'orgUnit'                   AS facility,
       effective_scope ->> 'entity'                    AS patient_id,
       COUNT(*) FILTER (WHERE ss.stage_def = 'triage') AS triage_count
FROM unified_scopes
WHERE effective_scope @> '{"program": "HIV"}'
GROUP BY 1, 2;
```

---

### Polymorphic Scope Model: Insert Examples

#### Schema Definition

```sql
CREATE TABLE scope_instance
(
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    flow_instance_id    UUID REFERENCES flow_instances (id) ON DELETE CASCADE,
    stage_submission_id UUID    REFERENCES stage_submissions (id) ON DELETE SET NULL,
    entity_instance_id  UUID    REFERENCES entity_instances (id) ON DELETE SET NULL,
    scope_data          JSONB   NOT NULL,
    created_at          TIMESTAMPTZ      DEFAULT NOW(),
    version             INTEGER NOT NULL DEFAULT 0
);

-- Constraints
ALTER TABLE scope_instance
    ADD CONSTRAINT scope_owner_check CHECK (
        (flow_instance_id IS NOT NULL AND stage_submission_id IS NULL) OR
        (flow_instance_id IS NULL AND stage_submission_id IS NOT NULL)
        );

CREATE UNIQUE INDEX one_root_per_flow ON scope_instance (flow_instance_id)
    WHERE stage_submission_id IS NULL;
```

---

### Scenario 1: Simple Form Submission (No Stages)

**Flow**: Patient Registration (single-step)  
**Context**: Clinic A, Team Blue, 2025-06-15

```sql
-- Flow Instance
INSERT INTO flow_instances (id, flow_type_id, status)
VALUES ('fi_001', 'patient_reg', 'COMPLETED');

-- Root Scope (Clinic/Team/Date context)
INSERT INTO scope_instance (id, flow_instance_id, scope_data)
VALUES ('sc_root_001',
        'fi_001',
        '{
            "orgUnit": "clinic_a",
            "team": "team_blue",
            "date": "2025-06-15"
        }');
```

---

### Scenario 2: Multi-Stage Flow (Inventory Receive)

**Flow**: Receive Inventory (planned)  
**Context**: Warehouse 5, Team Red, 2025-06-20, Invoice INV-100

```sql
-- Flow Instance
INSERT INTO flow_instances (id, flow_type_id, status)
VALUES ('fi_200', 'receive_inv', 'IN_PROGRESS');

-- Root Scope
INSERT INTO scope_instance (id, flow_instance_id, scope_data)
VALUES ('sc_root_200',
        'fi_200',
        '{
            "orgUnit": "warehouse_5",
            "team": "team_red",
            "date": "2025-06-20",
            "invoice": "INV-100",
            "supplier": "sup_456"
        }');

-- Stage 1: Unpack Item 1 (entity-bound)
INSERT INTO stage_submissions (id, flow_instance_id, stage_definition_id, data)
VALUES ('ss_unpack_201',
        'fi_200',
        'unpack_stage',
        '{"qty": 50, "quality": "GOOD"}');

INSERT INTO scope_instance (id,
                            stage_submission_id,
                            entity_instance_id,
                            scope_data)
VALUES ('sc_stage_201',
        'ss_unpack_201',
        'item_789', -- Entity reference
        '{"item": "item_789", "batch": "BATCH-A", "expiry": "2026-12-31"}');

-- Stage 1: Unpack Item 2
INSERT INTO stage_submissions (id, flow_instance_id, stage_definition_id, data)
VALUES ('ss_unpack_202',
        'fi_200',
        'unpack_stage',
        '{"qty": 30, "quality": "DAMAGED"}');

INSERT INTO scope_instance (id,
                            stage_submission_id,
                            entity_instance_id,
                            scope_data)
VALUES ('sc_stage_202',
        'ss_unpack_202',
        'item_101',
        '{"item": "item_101", "batch": "BATCH-B", "expiry": "2025-11-30"}');

-- Stage 2: Storage (non-entity-bound)
INSERT INTO stage_submissions (id, flow_instance_id, stage_definition_id, data)
VALUES ('ss_store_203',
        'fi_200',
        'store_stage',
        '{"location": "SHELF-A4", "notes": "Damaged items quarantined"}');

-- Uses root scope (no new scope instance)
```

---

### Scenario 3: Healthcare Encounter Flow

**Flow**: Patient Visit (log-as-you-go)  
**Context**: Clinic B, Team Green, Patient P-888

```sql
-- Flow Instance
INSERT INTO flow_instances (id, flow_type_id, status)
VALUES ('fi_300', 'patient_visit', 'IN_PROGRESS');

-- Root Scope (Patient context)
INSERT INTO scope_instance (id,
                            flow_instance_id,
                            entity_instance_id,
                            scope_data)
VALUES ('sc_root_300',
        'fi_300',
        'pat_888', -- Patient entity
        '{
            "orgUnit": "clinic_b",
            "team": "team_green",
            "date": "2025-06-18",
            "visitType": "CHECKUP"
        }');

-- Stage 1: Triage (entity-bound to encounter)
INSERT INTO stage_submissions (id, flow_instance_id, stage_definition_id, data)
VALUES ('ss_triage_301',
        'fi_300',
        'triage_stage',
        '{"bp": "120/80", "temp": 98.6}');

INSERT INTO scope_instance (id,
                            stage_submission_id,
                            entity_instance_id,
                            scope_data)
VALUES ('sc_stage_301',
        'ss_triage_301',
        'enc_777', -- New encounter entity
        '{"encounter": "enc_777", "department": "ER"}');

-- Stage 2: Diagnosis (uses patient scope)
INSERT INTO stage_submissions (id, flow_instance_id, stage_definition_id, data)
VALUES ('ss_diag_302',
        'fi_300',
        'diagnosis_stage',
        '{"diagnosis": "A15.0", "severity": "MODERATE"}');
-- No new scope (inherits root)
```

---

### Scenario 4: Campaign Registration Flow

**Flow**: Vaccination Campaign (multi-stage)  
**Context**: District X, Campaign C-2025, Team Mobile-1

```sql
-- Flow Instance
INSERT INTO flow_instances (id, flow_type_id, status)
VALUES ('fi_400', 'vax_campaign', 'COMPLETED');

-- Root Scope
INSERT INTO scope_instance (id, flow_instance_id, scope_data)
VALUES ('sc_root_400',
        'fi_400',
        '{
            "orgUnit": "district_x",
            "campaign": "c_2025",
            "team": "mobile_1",
            "date": "2025-06-10"
        }');

-- Stage 1: Household Registration (entity-bound)
INSERT INTO stage_submissions (id, flow_instance_id, stage_definition_id, data)
VALUES ('ss_hhreg_401',
        'fi_400',
        'hh_reg_stage',
        '{"hhSize": 5, "waterSource": "PIPED"}');

INSERT INTO scope_instance (id,
                            stage_submission_id,
                            entity_instance_id,
                            scope_data)
VALUES ('sc_stage_401',
        'ss_hhreg_401',
        'hh_12345',
        '{"household": "hh_12345", "village": "VILL-A"}');

-- Stage 2: Member Enrollment (entity-bound to person)
INSERT INTO stage_submissions (id, flow_instance_id, stage_definition_id, data)
VALUES ('ss_member_402',
        'fi_400',
        'member_stage',
        '{"age": 12, "gender": "F"}');

INSERT INTO scope_instance (id,
                            stage_submission_id,
                            entity_instance_id,
                            scope_data)
VALUES ('sc_stage_402',
        'ss_member_402',
        'pers_678',
        '{"household": "hh_12345", "individual": "pers_678"}');

-- Stage 3: Vaccination (entity-bound to dose)
INSERT INTO stage_submissions (id, flow_instance_id, stage_definition_id, data)
VALUES ('ss_vax_403',
        'fi_400',
        'vax_stage',
        '{"vaccine": "Measles", "lot": "MMR-2025Q1"}');

INSERT INTO scope_instance (id,
                            stage_submission_id,
                            entity_instance_id,
                            scope_data)
VALUES ('sc_stage_403',
        'ss_vax_403',
        'dose_xyz',
        '{"individual": "pers_678", "dose": 1}');
```

---

### Key Advantages in Inserts

1. **Consistent Pattern**:
   ```sql
   -- Root scope
   INSERT INTO scope_instance (flow_instance_id, scope_data)...
   
   -- Stage scope
   INSERT INTO scope_instance (stage_submission_id, scope_data)...
   ```
2. **Flexible Dimensioning**:
   ```jsonb
   // Healthcare
   {"patient": "P123", "encounter": "E456"}
   
   // Campaign
   {"campaign": "C2025", "village": "V789"}
   
   // Inventory
   {"item": "I101", "batch": "B202"}
   ```

3. **Entity Linking**:
   ```sql
   -- Explicit entity reference
   INSERT ... entity_instance_id = 'ent_123'
   -- Implicit in JSON
   scope_data = '{"entity": "ent_123"}'
   ```

4. **Concurrency Safe**:
   ```sql
   -- Optimistic locking
   UPDATE scope_instance SET 
     scope_data = '...',
     version = version + 1
   WHERE id = 'sc_001' AND version = 3
   ```

5. **Inheritance Handling**:
   ```sql
   -- Stage inheriting root scope
   INSERT INTO stage_submissions (..., scope_instance_id)
   VALUES (..., 'sc_root_400')  -- Reference root scope directly
   ```

This implementation maintains your "single join point" principle while providing:

- Explicit entity relationships through FKs
- Flexible dimensioning via JSONB
- Clear ownership semantics through constraints
- Optimistic locking for concurrency
- Uniform query patterns across domains

For reporting, all context is accessible via:

```sql
SELECT *
FROM scope_instance
WHERE scope_data @> '{"orgUnit": "warehouse_5"}'
   OR entity_instance_id = 'item_789'
```

---

### Benefits

1. **JPA Mapping**: Clean one-to-one mappings without enum-based hacks.
2. **Concurrency Safety**: The `version` column enables optimistic locking to prevent lost updates.
3. **Entity Binding**: Explicit FK to `entity_instance` simplifies queries for entity-bound data.
4. **Data Integrity**: The check constraint ensures that every scope is either for a flow or a stage, not both or none.

### Edge Cases Addressed

1. **Missing ROOT Scope**:
    - The root scope is created as part of the flow instance creation transaction. If the transaction fails, the flow
      instance isn't created. This ensures atomicity.
2. **Duplicate ROOT Scope**:
    - We can add a unique constraint on `flow_instance_id` (since only one root scope per flow is allowed) but note that
      the
      check constraint already ensures that a root scope must have `flow_instance_id` and `stage_submission_id` null.
      So:
        ```sql
        CREATE UNIQUE INDEX unique_root_scope_per_flow ON scope_instance (flow_instance_id) WHERE stage_submission_id IS NULL;
        ```
3. **Concurrent STAGE Scopes**:
    - Since each stage submission creates its own scope instance (with a unique stage_submission_id), concurrent
      creation is
      safe. The unique constraint on `stage_submission_id` (see below) ensures no duplicates.
4. **Data Consistency**:
    - The `custom_dimensions` JSONB is not updated once created? (We might enforce immutability for scopes to avoid
      mid-flow
      changes). If updates are needed, we can use a new version of the scope.
5. **Deeply Nested Scopes**:

    - We don't support nesting natively, but if a stage needs to reference multiple entities, we can store them as JSON
      array in `custom_dimensions` and also link the primary entity via `entity_instance_id`.

