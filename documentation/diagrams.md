Here's the revised model with stage-level scoping capabilities, maintaining your core concepts while adding the
flexibility for stages to define their own scoping dimensions:

### 1. Core ER Diagram with Stage Scoping

```mermaid
erDiagram
    FlowType ||--o{ FlowInstance: defines
    FlowType {
        string id PK
        string name
        enum planningMode "PLANNED | LOG_AS_YOU_GO"
        enum submissionMode "SINGLE | MULTI_STAGE"
        jsonb scopes "ScopeDefinition[]"
        jsonb stages "StageDefinition[]"
    }

    FlowInstance ||--o{ StageSubmission: contains
    FlowInstance ||--|| ScopeInstance: "root scope"
    FlowInstance {
        string id PK
        string flowTypeId FK
        string status
    }

    StageSubmission ||--|| ScopeInstance: "stage scope"
    StageSubmission {
        string id PK
        string flowInstanceId FK
        string stageDefinitionId
        jsonb data
    }

    ScopeInstance {
        string id PK
        string flowInstanceId FK
        string stageSubmissionId "nullable FK"
        jsonb scopeData "team,orgUnit,date,entity..."
    }

    EntityInstance {
        string id PK
        string entityTypeId
        jsonb attributes
    }

    StageDefinition {
        string id
        string name
        string formTemplateId
        boolean repeatable
        jsonb scopeDefinition "StageScopeDefinition[]"
        string entityBoundEntityTypeId "optional"
    }
```

Key changes:

1. Added `scopeDefinition` to `StageDefinition`
2. Made `ScopeInstance` relationship with `StageSubmission` mandatory
3. Removed entity-binding as special case (now just part of scopeDefinition)

### 2. Scope Inheritance Flow

```mermaid
flowchart TD
    A[Flow Start] --> B[Create Root ScopeInstance]
    B --> C[Process Stage]
    C --> D{Stage has\nscopeDefinition?}
    D -->|Yes| E[Create Stage ScopeInstance]
    D -->|No| F[no stageScope created]
    E --> H[Process Data]
    F --> H
    H --> I{More stages?}
    I -->|Yes| C
    I -->|No| J[Complete Flow]
```

### 3. Scope Resolution Hierarchy

```mermaid
classDiagram
    class FlowScope {
        <<abstract>>
        dimensions: JSON
    }

    class RootScope {
        team: ID
        orgUnit: ID
        date: Date
        customFields: JSON
    }

    class StageScope {
        entity: ID?
        batch: String?
        location: ID?
        customFields: JSON
    }

    class ResolvedScope {
        root: RootScope
        stage: StageScope?
        combined: JSON
    }

    FlowScope <|-- RootScope
    FlowScope <|-- StageScope
    RootScope "1" *-- "0..n" StageScope: children
    RootScope -- ResolvedScope: input
    StageScope -- ResolvedScope: input
    ResolvedScope ..> JSON: output
```

### 4. Inventory Receive Flow with Stage Scoping

```mermaid
sequenceDiagram
    participant U as User
    participant S as System
    participant DB as Database
    U ->> S: Start Receive Flow
    S ->> DB: Create FlowInstance (FI-6001)
    S ->> DB: Create ScopeInstance (SI-1001: root scopes)
    U ->> S: Submit Unpack Stage
    S ->> S: Check stage definition - has scope!
    S ->> DB: Create ScopeInstance (SI-1002: item/batch)
    S ->> DB: Create StageSubmission (SS-1001)
    U ->> S: Submit Store Stage
    S ->> S: Check stage definition - no scope
    S ->> DB: Create StageSubmission (SS-2001)
    S ->> DB: Update FlowStatus (COMPLETED)
```

### Implementation Logic for Stage Scoping

```python
def handle_stage_submission(flow_instance, stage_definition, form_data):
    # Resolve scope
    if stage_definition.scopeDefinition:
        # Create new scope instance for stage
        stage_scope = create_scope_instance(
            flow_instance.id,
            stage_submission_id=None,  # Set later
            scope_data=resolve_scope_values(stage_definition.scopeDefinition, form_data)
        )
    else:
        # Inherit from flow root scope
        stage_scope = flow_instance.root_scope
    
    # Create stage submission
    submission = create_stage_submission(
        flow_instance.id,
        stage_definition.id,
        data=form_data,
        scope_instance_id=stage_scope.id
    )
    
    # Update scope instance with submission reference
    if stage_definition.scopeDefinition:
        stage_scope.stage_submission_id = submission.id
        save(stage_scope)
    
    # Handle entity binding if defined in scope
    if entity_type := stage_definition.entityBoundEntityTypeId:
        entity_id = form_data.get(entity_type.identifier_field)
        upsert_entity(entity_type, entity_id, form_data)
```

### Scoping Configuration Examples

**FlowType Definition (Receive Inventory):**

```json
{
    "stages": [
        {
            "id": "unpackCheck",
            "name": "Unpack & Quality Check",
            "scopeDefinition": [
                {
                    "key": "item",
                    "type": "ENTITY",
                    "entityTypeId": "item"
                },
                {
                    "key": "batch",
                    "type": "TEXT",
                    "required": true
                }
            ],
            "entityBoundEntityTypeId": "item"
        },
        {
            "id": "storeItem",
            "name": "Store Items",
            "scopeDefinition": [
                {
                    "key": "storageLocation",
                    "type": "ENTITY",
                    "entityTypeId": "location"
                }
            ]
        }
    ]
}
```

**ScopeInstance Creation:**

1. Root scope: `{orgUnit: "WH-1", date: "2025-06-20", invoice: "INV-001"}`
2. Unpack stage: `{item: "ITEM-123", batch: "BATCH-A"}`
3. Store stage: `{storageLocation: "SHELF-A1"}`

### Query Advantages

```sql
/* Get all unpack submissions for specific item */
SELECT ss.*
FROM stage_submissions ss
         JOIN scope_instances si ON ss.scope_instance_id = si.id
WHERE si.scope_data ->> 'item' = 'ITEM-123'

/* Get all storage operations in location */
SELECT ss.*
FROM stage_submissions ss
         JOIN scope_instances si ON ss.scope_instance_id = si.id
WHERE si.scope_data ->> 'storageLocation' = 'SHELF-A1'
```

This revised model maintains your core concepts while:

1. Making stage scoping a first-class citizen (not just for entities)
2. Keeping entity binding as optional specialization
3. Preserving the ScopeInstance single-table advantage
4. Simplifying query patterns with direct stage-scope relationships
5. Allowing gradual complexity (stages can start without scopes)
6. Enabling cross-flow reporting through consistent dimensioning

The changes are minimal in implementation but significantly expand the flexibility for healthcare workflows where stages
might need to scope by patient/encounter while still inheriting facility/team context.
