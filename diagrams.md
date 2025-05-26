
---

```mermaid
erDiagram
    ACTIVITY_DEFINITION ||--o{ ASSIGNMENT_TYPE       : defines
    ASSIGNMENT_TYPE     ||--o{ STAGE_DEFINITION      : has
    ASSIGNMENT_TYPE     ||--o{ FORM_TEMPLATE         : uses
    ASSIGNMENT_TYPE     ||--o{ ASSIGNMENT           : instantiates

    ASSIGNMENT          ||--o{ STAGE_SUBMISSION     : has
    ASSIGNMENT          ||--o{ ENTITY_INSTANCE      : links
    STAGE_DEFINITION    ||--o{ STAGE_SUBMISSION     : defines
    FORM_TEMPLATE       ||--o{ STAGE_DEFINITION     : powers
    FORM_TEMPLATE       ||--o{ STAGE_SUBMISSION     : records

    ENTITY_DEFINITION   ||--o{ ENTITY_INSTANCE      : defines
    ENTITY_INSTANCE     ||--o{ STAGE_SUBMISSION     : may_update

    %% Table-entity definitions
    ACTIVITY_DEFINITION {
        UUID id PK
        String name
        String description
        Timestamp created_at
    }
    ASSIGNMENT_TYPE {
        UUID id PK
        UUID activity_definition_id FK
        String name
    %% PLANNED LOG_AS_YOU_GO
        Enum planning_mode
    %% SINGLE MULTI_STAGE
        Enum submission_mode  
        Timestamp created_at
    }
    STAGE_DEFINITION {
        UUID id PK
        UUID assignment_type_id FK
        String name
        UUID form_template_id FK
        Boolean repeatable
        Integer stage_order
        Timestamp created_at
    }
    FORM_TEMPLATE {
        UUID id PK
        String name
    %% your existing template JSON
        JSONB structure        
        Timestamp created_at
    }
    ASSIGNMENT {
        UUID id PK
        UUID assignment_type_id FK
        UUID assigned_team_id 
        UUID org_unit_id     
        Date   assignment_date
        %% optional pre-link
        UUID entity_instance_id
        %% { stageId: [...submissionIds] }
        JSONB  stage_states
        %% PLANNED, IN_PROGRESS, COMPLETED, CANCELLED
        Enum   status 
        Timestamp created_at
    }
    STAGE_SUBMISSION {
        UUID id PK
        UUID assignment_id FK
        %% null if single-stage
        UUID stage_definition_id FK
        UUID form_template_id FK
        JSONB data
        %% PENDING, SUBMITTED, REJECTED
        Enum   status
        Timestamp submitted_at
    }
    ENTITY_DEFINITION {
        UUID id PK
        %% e.g. "Household", "Patient"
        String key
        String display_name
        %% list of { id, type, required }
        JSONB attributes
        Timestamp created_at
    }
    ENTITY_INSTANCE {
        UUID id PK
        UUID entity_definition_id FK
        %% the assignment that created it
        UUID assignment_id
        %% optional link
        UUID stage_submission_id  
        JSONB identity_attributes
        JSONB properties
        %% ACTIVE, INACTIVE, ARCHIVED
        Enum   status 
        Timestamp created_at
        Timestamp updated_at
    }
```

---

### How to migrate in stages

1. **Add metadata tables**

    * Create `activity_definitions`, `assignment_types`, `stage_definitions` and `form_templates` (if not already).
2. **Extend `assignments`**

    * Add `assignment_type_id`, `entity_instance_id`, `stage_states`, and `status`.
3. **Create submission table**

    * `stage_submissions` holds all form data, with an optional `stage_definition_id`.
4. **Introduce entity types**

    * `entity_definitions` → holds your repeat-entity schemas.
    * `entity_instances` → holds actual repeat data, linked to assignments/submissions.
5. **Wire up processing**

    * On form save: insert into `stage_submissions`; if template has an entity-bound section, upsert into `entity_instances`.
6. **Back-fill or migrate existing data**

    * For existing repeatable sections, you can run a one-off script that reads old JSON submissions and populates `entity_instances`, linking back to assignments.

This ER diagram and table list should give you a clear roadmap for expanding your schema—layering in stages and entities without breaking your current flow.
