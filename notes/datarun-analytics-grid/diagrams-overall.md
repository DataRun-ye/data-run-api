# Datarun: Key Architectural Principles & Diagrams
## Key System Properties

| Property            | Implementation                          | Benefit                   |
|---------------------|-----------------------------------------|---------------------------|
| **Idempotency**     | Transactional sweep-and-update ETL      | Safe retries, consistency |
| **Immutability**    | Versioned templates and elements        | Historical accuracy       |
| **Extensibility**   | Metadata-driven architecture            | Configuration over code   |
| **Performance**     | Layered storage with materialized views | Scalable analytics        |
| **Maintainability** | Clear separation of concerns            | Easier evolution          |

## 1. Complete System Architecture with Analytics Layer

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
        RepeatInstance[Repeat Facts]
        ElementDataValue[Cell Value Facts]
        DataSubmission -- ETL --> RepeatInstance
        DataSubmission -- ETL --> ElementDataValue
        RepeatInstance --> ElementDataValue
    end

    subgraph L6 [Layer 6: Analytics Foundation]
        direction TB
        PivotGridFacts[Pivot Grid Facts<br/>Materialized View]
        ElementDataValue --> PivotGridFacts
        RepeatInstance --> PivotGridFacts
    end

    subgraph L7 [Layer 7: Analytics Metadata & Wide Models]
        direction TB
        AnalyticsMetadata[Analytics Metadata Service]
        WideModels[Template-Specific Wide Models]
        AnalyticsEntity[AnalyticsEntity]
        AnalyticsAttribute[AnalyticsAttribute]
        AnalyticsRelationship[AnalyticsRelationship]
        AnalyticsMetadata --> AnalyticsEntity
        AnalyticsMetadata --> AnalyticsAttribute
        AnalyticsMetadata --> AnalyticsRelationship
        AnalyticsMetadata -- configures --> WideModels
    end

    subgraph L8 [Layer 8: API & Query Layer]
        direction TB
        DynamicQueryEngine[Dynamic Query Engine]
        MetadataService[Analytics Metadata Service]
        MVManager[Materialized View Manager]
        DynamicQueryEngine -- uses --> MetadataService
        DynamicQueryEngine -- manages --> MVManager
    end

    subgraph L9 [Layer 9: Client Interface]
        direction TB
        ClientAPI[Client API]
        AdminUI[Admin UI]
        ReportUI[Report UI]
    end

%% Relationships between layers
    L1 -- referenced by --> L2
    L2 -- configures --> L3
    L3 -- defines structure --> L4
    L4 -- processed by --> L5
    L5 -- feeds --> L6
    L6 -- source for --> L7
    L7 -- drives --> L8
    L8 -- serves --> L9
%% Specific relationships
    DataElement -- canonical definition --> ElementTemplateConfig
    OptionSet -- options reference --> ElementTemplateConfig
    Team -- context --> DataSubmission
    OrgUnit -- context --> DataSubmission
    Activity -- context --> DataSubmission
    OrgUnitHierarchy -- hierarchy queries --> PivotGridFacts
    PivotGridFacts -- source --> WideModels
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
    Admin ->> Service: Publish DataTemplateVersion (vX)
    activate Service
    Service ->> DB: Validate and save DataTemplateVersion
    DB -->> Service: Success response
    Service ->> DB: Generate ElementTemplateConfig records
    DB -->> Service: Configuration created
    Service ->> EventBus: Fire NewTemplateVersionPublishedEvent
    deactivate Service
    Note over EventBus, ETLService: Async processing begins

    par Event Processing
        EventBus ->> ETLService: Handle NewTemplateVersionPublishedEvent
        activate ETLService
        ETLService ->> DB: Update analytics structures
        ETLService ->> Cache: Invalidate template cache
        ETLService -->> EventBus: Processing complete
        deactivate ETLService
    end

    par Cache Update
        EventBus ->> Cache: Update template cache
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
