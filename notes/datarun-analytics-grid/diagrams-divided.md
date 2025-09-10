# Modular Datarun Analytics System Diagrams

I'll create a set of focused diagrams that can be presented independently, each covering a specific logical component of the system. These diagrams will reference each other where needed, reducing cognitive load while maintaining system coherence.

## Master Diagram: System Overview

```mermaid
flowchart TB
    subgraph S1 [Template Configuration]
        TC[Data Template]
        TV[Data Template Version]
        ETC[Element Template Config]
        TC --> TV --> ETC
    end

    subgraph S2 [Operational Data]
        DS[Data Submission]
        DSH[Data Submission History]
        DS --> DSH
    end

    subgraph S3 [ETL Processing]
        RI[Repeat Instance]
        EDV[Element Data Value]
        DS -- ETL --> RI
        DS -- ETL --> EDV
        RI --> EDV
    end

    subgraph S4 [Analytics Foundation]
        PGF[Pivot Grid Facts MV]
        EDV --> PGF
        RI --> PGF
    end

    subgraph S5 [Analytics Metadata]
        AM[Analytics Metadata Service]
        AE[Analytics Entity]
        AA[Analytics Attribute]
        AR[Analytics Relationship]
        AM --> AE --> AA
        AE --> AR
    end

    subgraph S6 [Wide Models & Query]
        WM[Wide Models]
        DQE[Dynamic Query Engine]
        AM -- configures --> WM
        WM --> DQE
    end

    subgraph S7 [Client Interface]
        API[REST API]
        UI[User Interface]
        DQE --> API --> UI
    end

    %% Relationships
    ETC -- configures --> DS
    S1 -- feeds --> S2
    S2 -- processes --> S3
    S3 -- populates --> S4
    S4 -- source for --> S6
    S5 -- drives --> S6
    S6 -- serves --> S7
```

## 1. Template Configuration System

```mermaid
flowchart TB
    subgraph TemplateMgmt [Template Management]
        DT[DataTemplate]
        DTV[DataTemplateVersion]
        DT --> DTV
    end

    subgraph ElementConfig [Element Configuration]
        DE[DataElement]
        OS[OptionSet]
        OV[OptionValue]
        OS --> OV
        DE --> OS
    end

    subgraph TemplateConfig [Template Configuration]
        ETC[ElementTemplateConfig]
        ETM[ElementTemplateMap]
        DTV --> ETC
        DE --> ETC
        ETC --> ETM
    end

    TemplateMgmt -- versioning --> TemplateConfig
    ElementConfig -- references --> TemplateConfig
```

## 3. ETL Processing System

```mermaid
flowchart TB
    DS[DataSubmission] --> ETL[ETL Process]
    
    subgraph Normalization [Data Normalization]
        RI[Repeat Instance Processing]
        VN[Value Normalization]
        ETL --> RI
        ETL --> VN
    end

    subgraph Storage [ETL Storage]
        RI --> RIT[Repeat Instance Table]
        VN --> EDVT[Element Data Value Table]
    end

    Normalization -- creates --> Storage
```

**See Diagram 3.1: Data Normalization Details**


**See Diagram 4.1: Source Data Details**

## 5. Analytics Metadata System

```mermaid
flowchart TB
    subgraph MetadataCore [Metadata Core]
        AE[AnalyticsEntity]
        AA[AnalyticsAttribute]
        AR[AnalyticsRelationship]
        AE --> AA
        AE --> AR
    end

    subgraph MetadataService [Metadata Services]
        MDS[Metadata Discovery Service]
        LCS[Localization Service]
        RRS[Reference Resolution Service]
        MDS --> LCS
        MDS --> RRS
    end

    MetadataCore -- managed by --> MetadataService
```

**See Diagram 5.1: Metadata Core Details**
**See Diagram 5.2: Metadata Services Details**

## 6. Wide Models & Query System

```mermaid
flowchart LR
    subgraph WideModels [Wide Model Generation]
        SS[Superset Schema Discovery]
        DDL[DDL Generation]
        MVW[Materialized View Creation]
        SS --> DDL --> MVW
    end

    subgraph QueryProcessing [Query Processing]
        QP[Query Parser]
        SQLG[SQL Generator]
        QE[Query Executor]
        QP --> SQLG --> QE
    end

    WideModels -- creates --> QueryProcessing
    MetadataService -- informs --> QueryProcessing
```

**See Diagram 6.1: Wide Model Generation Details**
**See Diagram 6.2: Query Processing Details**

## 7. Client Interface System

```mermaid
flowchart TB
    subgraph API [API Layer]
        MAPI[Metadata API]
        QAPI[Query API]
        OAPI[Options API]
    end

    subgraph UI [User Interface]
        SM[Selection Manager]
        RM[Result Renderer]
        FM[Filter Manager]
        SM --> RM
        SM --> FM
    end

    API -- serves --> UI
    QueryProcessing -- powers --> API
```

**See Diagram 7.1: API Layer Details**
**See Diagram 7.2: User Interface Details**

## How to Use These Diagrams

1. **Start with the Master Diagram** to understand the overall system architecture
2. **Drill down into specific areas** using the linked diagrams
3. **Each diagram is self-contained** with minimal external dependencies
4. **References to other diagrams** are clearly marked for navigation
5. **Present one area at a time** without overwhelming the audience
