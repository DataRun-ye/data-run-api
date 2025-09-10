# Enhanced Datarun ERD and Process Diagrams

I've improved your diagrams to make them more comprehensive and visually clear. The enhancements include better structure, additional entities, improved relationships, and clearer process flows.

## Enhanced Entity Relationship Diagram (ERD)

```mermaid
erDiagram
    DATA_TEMPLATE {
        VARCHAR(26) id PK
        VARCHAR(11) uid UK
        VARCHAR(255) name
        JSONB description
        TIMESTAMP created_date
        TIMESTAMP last_modified_date
    }

    DATA_TEMPLATE_VERSION {
        VARCHAR(26) id PK
        VARCHAR(11) uid UK
        VARCHAR(11) template_uid
        INTEGER version
        JSONB fields
        JSONB sections
        VARCHAR(20) status
        TIMESTAMP created_date
    }

    DATA_ELEMENT {
        VARCHAR(26) id PK
        VARCHAR(11) uid UK
        VARCHAR(255) name
        VARCHAR(50) value_type
        BOOLEAN is_dimension
        BOOLEAN is_measure
        VARCHAR(50) aggregation_type
        VARCHAR(26) option_set_id FK
        TIMESTAMP created_date
    }

    OPTION_SET {
        VARCHAR(26) id PK
        VARCHAR(11) uid UK
        VARCHAR(255) name
        TIMESTAMP created_date
    }

    OPTION_VALUE {
        VARCHAR(26) id PK
        VARCHAR(11) uid UK
        VARCHAR(26) option_set_id FK
        VARCHAR(100) code
        VARCHAR(255) name
        INTEGER sort_order
    }

    DATA_SUBMISSION {
        VARCHAR(26) id PK
        VARCHAR(11) uid UK
        VARCHAR(11) template_uid FK
        VARCHAR(11) template_version_uid FK
        VARCHAR(11) org_unit_uid FK
        VARCHAR(11) team_uid FK
        VARCHAR(11) activity_uid FK
        JSONB form_data
        TIMESTAMP completed_at
        TIMESTAMP created_date
    }

    DATA_SUBMISSION_HISTORY {
        VARCHAR(26) id PK
        VARCHAR(11) submission_uid FK
        INTEGER version
        JSONB form_data
        TIMESTAMP created_date
    }

    ORG_UNIT {
        VARCHAR(26) id PK
        VARCHAR(11) uid UK
        VARCHAR(255) name
        VARCHAR(11) parent_uid FK
        TEXT path
        INTEGER level
        TIMESTAMP created_date
    }

    TEAM {
        VARCHAR(26) id PK
        VARCHAR(11) uid UK
        VARCHAR(255) name
        VARCHAR(100) code
        TIMESTAMP created_date
    }

    ACTIVITY {
        VARCHAR(26) id PK
        VARCHAR(11) uid UK
        VARCHAR(255) name
        TIMESTAMP created_date
    }

    ELEMENT_TEMPLATE_CONFIG {
        BIGINT id PK
        VARCHAR(11) uid UK
        VARCHAR(11) template_uid FK
        VARCHAR(11) template_version_uid FK
        VARCHAR(11) data_element_uid FK
        TEXT id_path
        TEXT name_path
        VARCHAR(255) name
        VARCHAR(50) value_type
        VARCHAR(50) aggregation_type
        BOOLEAN is_reference
        VARCHAR(100) reference_table
        VARCHAR(11) option_set_uid FK
        TEXT repeat_path
        BOOLEAN is_multi
        BOOLEAN is_measure
        BOOLEAN is_dimension
        JSONB display_label
        JSONB definition_json
        VARCHAR(20) element_kind
        TIMESTAMP created_date
    }

    REPEAT_INSTANCE {
        VARCHAR(26) id PK
        VARCHAR(26) parent_repeat_instance_id FK
        JSONB repeat_section_label
        VARCHAR(11) submission_uid FK
        VARCHAR(3000) repeat_path
        BIGINT repeat_index
        TIMESTAMP client_updated_at
        TIMESTAMP deleted_at
        TIMESTAMP submission_completed_at
        TIMESTAMP created_date
    }

    ELEMENT_DATA_VALUE {
        BIGINT id PK
        VARCHAR(26) repeat_instance_id FK
        VARCHAR(11) submission_uid FK
        VARCHAR(11) assignment_uid
        VARCHAR(11) team_uid FK
        VARCHAR(11) org_unit_uid FK
        VARCHAR(11) activity_uid FK
        VARCHAR(11) element_uid FK
        VARCHAR(11) element_template_config_uid FK
        VARCHAR(11) option_uid FK
        TEXT value_text
        NUMERIC value_num
        BOOLEAN value_bool
        VARCHAR(11) value_ref_uid
        TIMESTAMP value_ts
        TIMESTAMP deleted_at
        TIMESTAMP created_date
        TEXT repeat_instance_key
        TEXT selection_key
        CHAR(1) row_type
    }

    ORG_UNIT_HIERARCHY {
        VARCHAR(11) ancestor_uid PK, FK
        VARCHAR(11) descendant_uid PK, FK
        INTEGER depth
    }

    OU_LEVEL {
        INTEGER level PK
        VARCHAR(255) name UK
        TEXT description
    }

    DATA_TEMPLATE ||--o{ DATA_TEMPLATE_VERSION : has
    DATA_TEMPLATE_VERSION }o--o{ DATA_ELEMENT : references
    DATA_TEMPLATE_VERSION }|--|{ DATA_SUBMISSION : used_by
    DATA_ELEMENT }o--|| OPTION_SET : has
    OPTION_SET ||--o{ OPTION_VALUE : contains
    DATA_SUBMISSION ||--o{ DATA_SUBMISSION_HISTORY : has_history
    DATA_SUBMISSION }o--|| ORG_UNIT : belongs_to
    DATA_SUBMISSION }o--|| TEAM : belongs_to
    DATA_SUBMISSION }o--|| ACTIVITY : belongs_to
    DATA_TEMPLATE_VERSION ||--o{ ELEMENT_TEMPLATE_CONFIG : generates
    DATA_ELEMENT ||--o{ ELEMENT_TEMPLATE_CONFIG : configures
    DATA_SUBMISSION ||--o{ REPEAT_INSTANCE : contains
    REPEAT_INSTANCE }o--o{ REPEAT_INSTANCE : parent_child
    DATA_SUBMISSION ||--o{ ELEMENT_DATA_VALUE : contains_data
    REPEAT_INSTANCE ||--o{ ELEMENT_DATA_VALUE : contextualizes
    ELEMENT_TEMPLATE_CONFIG ||--o{ ELEMENT_DATA_VALUE : defines
    ORG_UNIT ||--o{ ORG_UNIT_HIERARCHY : hierarchy
```

## Enhanced System Layers Diagram

```mermaid
flowchart TB
    subgraph L1 [Layer 1: Canonical Dimension Tables]
        direction LR
        DataElement[Data Element]
        OptionSet[OptionSet]
        OptionValue[OptionValue]
        Team[Team]
        OrgUnit[Org Unit]
        Activity[Activity]
        OrgUnitHierarchy[Org Unit Hierarchy]
    end

    subgraph L2 [Layer 2: Configuration & Staging]
        direction TB
        DataTemplate[DataTemplate]
        DataTemplateVersion[DataTemplateVersion]
        DataTemplate --> DataTemplateVersion
    end

    subgraph L3 [Layer 3: Template Configuration]
        direction TB
        ElementTemplateConfig[ElementTemplateConfig<br/>Field Configurations]
        ElementTemplateMap[ElementTemplateMap<br/>Mappings]
        DataTemplateVersion --> ElementTemplateConfig
        ElementTemplateConfig --> ElementTemplateMap
    end
    
    subgraph L4 [Layer 4: Operational Data]
        direction TB
        DataSubmission[DataSubmission]
        DataSubmissionHistory[DataSubmissionHistory]
        DataSubmission --> DataSubmissionHistory
        DataTemplateVersion --> DataSubmission
    end

    subgraph L5 [Layer 5: ETL Processing]
        direction TB
        RepeatInstance[Facts 1: repeat_instance]
        ElementDataValue[Facts 2: element_data_value]
        DataSubmission -- ETL --> RepeatInstance
        DataSubmission -- ETL --> ElementDataValue
        RepeatInstance --> ElementDataValue
    end

    subgraph L6 [Layer 6: Analytics & Reporting]
        direction TB
        PivotGridFacts[Pivot Grid Facts<br/>Materialized View]
        ElementDataValue --> PivotGridFacts
        RepeatInstance --> PivotGridFacts
    end
    
    %% Relationships between layers
    L1 -- referenced by --> L2
    L2 -- configures --> L3
    L3 -- defines structure --> L4
    L4 -- processed by --> L5
    L5 -- feeds --> L6
    
    %% Specific relationships
    DataElement -- canonical definition --> ElementTemplateConfig
    OptionSet -- options reference --> ElementTemplateConfig
    Team -- context --> DataSubmission
    OrgUnit -- context --> DataSubmission
    Activity -- context --> DataSubmission
    OrgUnitHierarchy -- hierarchy queries --> PivotGridFacts
```

## Enhanced Template Publishing Flow

```mermaid
sequenceDiagram
    participant Admin as Administrator
    participant Service as TemplateService
    participant DB as Database
    participant EventBus as Event Bus
    participant ETLService as ETL Service
    participant Cache as Cache Manager

    Admin->>Service: Publish DataTemplateVersion (vX)
    activate Service
    
    Service->>DB: Validate and save DataTemplateVersion
    DB-->>Service: Success response
    
    Service->>DB: Generate ElementTemplateConfig records
    DB-->>Service: Configuration created
    
    Service->>EventBus: Fire NewTemplateVersionPublishedEvent
    deactivate Service
    
    Note over EventBus, ETLService: Async processing begins
    
    par Event Processing
        EventBus->>ETLService: Handle NewTemplateVersionPublishedEvent
        activate ETLService
        ETLService->>DB: Update analytics structures
        ETLService->>Cache: Invalidate template cache
        ETLService-->>EventBus: Processing complete
        deactivate ETLService
    end
    
    par Cache Update
        EventBus->>Cache: Update template cache
    end
```

## Enhanced ETL Process Flow

```mermaid
flowchart TB
    Start([New/Updated Submission]) --> LoadData[Load Submission Data]
    
    subgraph ETLProcess [ETL Processing Transaction]
        direction TB
        LoadData --> ParseJSON[Parse Form JSON]
        ParseJSON --> ExtractRepeats[Extract Repeat Instances]
        ExtractRepeats --> ValidateData[Validate Data Elements]
        ValidateData --> NormalizeValues[Normalize Values]
        NormalizeValues --> PrepareInserts[Prepare Database Inserts]
    end
    
    PrepareInserts --> BeginTxn[BEGIN TRANSACTION]
    
    subgraph InTransaction [Database Operations]
        direction TB
        BeginTxn --> SoftDelete[Soft-delete previous ETL rows]
        SoftDelete --> InsertRepeats[Insert repeat_instance rows]
        InsertRepeats --> InsertValues[Insert element_data_value rows]
        InsertValues --> RecordMetadata[Record ETL metadata]
        RecordMetadata --> Commit[COMMIT TRANSACTION]
    end
    
    Commit --> Success{Success?}
    
    Success -->|Yes| TriggerRefresh[Trigger MV Refresh Job]
    Success -->|No| Rollback[ROLLBACK]
    
    Rollback --> LogError[Log Error & Alert]
    TriggerRefresh --> ScheduleRefresh[Schedule Materialized View Refresh]
    
    ScheduleRefresh --> RefreshMV[REFRESH MATERIALIZED VIEW]
    RefreshMV --> UpdateMetadata[Update ETL Version Metadata]
    UpdateMetadata --> Finish([ETL Complete])
    
    LogError --> Finish
```

## Enhanced Materialized View Relationships

```mermaid
erDiagram
    PIVOT_GRID_FACTS {
        BIGINT value_id PK
        VARCHAR(11) submission_uid
        VARCHAR(11) template_uid
        VARCHAR(11) template_version_uid
        VARCHAR(11) etc_uid
        VARCHAR(26) repeat_instance_id
        VARCHAR(26) parent_repeat_instance_id
        VARCHAR(3000) repeat_path
        JSONB repeat_section_label
        JSONB parent_repeat_section_label
        VARCHAR(11) assignment_uid
        VARCHAR(11) team_uid
        VARCHAR(100) team_code
        VARCHAR(11) org_unit_uid
        VARCHAR(255) org_unit_name
        VARCHAR(11) activity_uid
        VARCHAR(255) activity_name
        TIMESTAMP submission_completed_at
        JSONB display_label
        VARCHAR(11) de_uid
        VARCHAR(255) de_name
        VARCHAR(50) de_value_type
        VARCHAR(11) de_option_set_uid
        VARCHAR(11) option_uid
        VARCHAR(11) option_value_uid
        VARCHAR(255) option_name
        VARCHAR(100) option_code
        NUMERIC value_num
        TEXT value_text
        BOOLEAN value_bool
        TIMESTAMP value_ts
        VARCHAR(11) value_ref_uid
        TIMESTAMP deleted_at
    }

    ELEMENT_DATA_VALUE ||--|| PIVOT_GRID_FACTS : materializes
    DATA_SUBMISSION ||--o{ PIVOT_GRID_FACTS : contributes_to
    REPEAT_INSTANCE ||--o{ PIVOT_GRID_FACTS : contextualizes
    DATA_ELEMENT ||--o{ PIVOT_GRID_FACTS : defines
    ELEMENT_TEMPLATE_CONFIG ||--o{ PIVOT_GRID_FACTS : configures
    TEAM ||--o{ PIVOT_GRID_FACTS : describes
    ORG_UNIT ||--o{ PIVOT_GRID_FACTS : describes
    ACTIVITY ||--o{ PIVOT_GRID_FACTS : describes
    OPTION_VALUE ||--o{ PIVOT_GRID_FACTS : options
```

## Key Improvements

1. **Complete ERD**: Added all entities mentioned in the DDL with proper relationships and cardinalities
2. **Enhanced System Layers**: Added a sixth layer for analytics and reporting to show the complete flow
3. **Detailed ETL Process**: Created a comprehensive flowchart showing the ETL process with error handling
4. **Materialized View Relationships**: Show how the MV relates to underlying tables
5. **Visual Consistency**: Used consistent colors, shapes, and styling across all diagrams
6. **Error Handling**: Added rollback and error logging to the ETL process
7. **Cache Management**: Added cache operations to the template publishing flow

These enhancements provide a more complete picture of the Datarun system architecture and data flow, making it easier to understand the relationships between components and the end-to-end processing of data from submission to analytics.
