## Appendix
### 1. **System Architecture Diagrams**
Shows how components interact at a high level:
```mermaid
graph TB
    A[Submission JSON] --> B[TransformServiceRobust]
    B --> C[TransformServiceV2]
    C --> D[RefResolutionService]
    C --> E[SubmissionKeysService]
    C --> F[EventEntityService]

    D --> G[ref_resolution table]
    D --> H[dim_* tables]

    E --> I[submission_keys table]
    F --> J[events table]
    C --> K[tall_canonical table]

    L[Analytics API] --> M[Query Engine]
    M --> I
    M --> J
    M --> K
    M --> H

    N[Caching Layer<br/>Ehcache/Hibernate L2] -.-> M

    style A fill:#26551e
    style L fill:#1385f5
```

## 2. **Core Database Schema Relationships**
Visualizes how main tables relate:
```mermaid
erDiagram
    submission_keys {
        varchar11 submission_uid PK
        varchar11 assignment_uid
        varchar11 activity_uid
        varchar11 org_unit_uid
        varchar11 team_uid
        timestamp last_seen
    }

    events {
        varchar26 event_uid PK
        varchar26 instance_key UK
        varchar11 submission_uid FK
        varchar26 anchor_ce_id
        varchar11 anchor_ref_uid
    }

    tall_canonical {
        varchar26 instance_key FK
        varchar11 submission_uid FK
        uuid canonical_element_id FK
        varchar26 repeat_instance_id
        varchar26 parent_instance_id
        varchar11 value_ref_uid
        text value_text
        numeric value_number
    }

    canonical_element {
        uuid canonical_element_id PK
        varchar50 semantic_type
        varchar11 option_set_uid
        text canonical_path
    }

    canonical_element_anchor {
        uuid canonical_element_id PK,FK
        boolean anchor_allowed
        int anchor_priority
    }

    ref_resolution {
        varchar26 ref_resolution_uid PK
        text raw_value
        varchar50 ref_type
        varchar11 resolved_uid FK
        numeric confidence
    }

    dim_option ||--o{ ref_resolution : resolved_uid
    dim_org_unit ||--o{ ref_resolution : resolved_uid
    dim_team ||--o{ ref_resolution : resolved_uid

    submission_keys ||--o{ events : "submission_uid"
    events ||--o{ tall_canonical : "instance_key"
    canonical_element ||--o{ tall_canonical : "canonical_element_id"
    canonical_element ||--|| canonical_element_anchor : "canonical_element_id"
    ref_resolution }o--|| tall_canonical : "value_ref_uid"
```

## 3. **Instance Identity Model**
Clarifies the `instance_key` concept and relationships:
```mermaid
graph TD
    subgraph "Root Instance (Submission)"
        A[submission_uid: abc123<br/>instance_key = abc123] --> B[tall_canonical row 1]
        A --> C[tall_canonical row 2]
        A --> D[tall_canonical row 3]
    end

    subgraph "Repeat Instance 1"
        E[repeat_instance_id: def456<br/>instance_key = def456] --> F[tall_canonical row 4]
        E --> G[tall_canonical row 5]
        A -.-> E
    end

    subgraph "Repeat Instance 2"
        H[repeat_instance_id: ghi789<br/>instance_key = ghi789] --> I[tall_canonical row 6]
        A -.-> H
    end

    J[events table row 1<br/>instance_key: abc123] --- A
    K[events table row 2<br/>instance_key: def456] --- E
    L[events table row 3<br/>instance_key: ghi789] --- H

    style A fill:#3856c9
    style E fill:#6083e0
    style H fill:#0883e0
```

## 4. **Reference Resolution Flow**
Shows how tokens get resolved to canonical UIDs:
```mermaid
flowchart TD
    A[Raw JSON Value<br/>e.g. Nairobi] --> B{RefResolutionService}
    B --> C[Check ref_resolution table]
    C --> D{Found?}
    D -- Yes --> E[Return cached resolved_uid]
    D -- No --> F[Resolve via dim_* tables]
    F --> G[Deterministic algorithm]
    G --> H[Store in ref_resolution]
    H --> I[Cache result]
    I --> J[Return resolved_uid]

    K[tall_canonical.value_text] -.-> A
    J -.-> L[tall_canonical.value_ref_uid]

    style A fill:#4040bc
    style L fill:#0550c9
```

## 5. **Idempotent Write Process**
Visualizes the upsert logic:
```mermaid
sequenceDiagram
    participant T as TransformServiceV2
    participant TC as tall_canonical
    participant E as events
    participant SK as submission_keys

    T->>TC: UPSERT ON CONFLICT<br/>(instance_key, canonical_element_id)<br/>DO UPDATE SET...

    T->>E: UPSERT ON CONFLICT (instance_key)<br/>DO UPDATE CASE:<br/>- Only update anchor if<br/>  new confidence > old<br/>- OR new resolved_at > old

    T->>SK: UPSERT ON CONFLICT (submission_uid)<br/>DO UPDATE SET last_seen = NOW()

    Note over T,SK: All writes are idempotent<br/>and safe for replay
```

```mermaid
---
title: Analytics Query Pattern with Superset Filters
---
flowchart LR
    Superset[Superset UI] --> API[API Parameters]
    
    subgraph "Filter Dimensions"
        API --> OU[org_unit_uid]
        API --> OUG[org_unit_group_uid]
        API --> TU[team_uid]
        API --> AU[activity_uid]
        API --> ASU[assignment_uid]
        API --> TR[time/time-range]
        API --> TMPU[template_uid]
        API --> TVU[template_version_uid]
        API --> ANC[anchor from canonical_anchors]
        API --> RPT[repeat canonical_element]
    end
    
    subgraph "Aggregation Layer"
        CE[canonical_element] --> DT[data_type]
        DT --> AT[Aggregation Types<br/>count, sum, avg, min, max, etc.]
        User[User Selection] --> SA[Specific Aggregation]
    end
    
    subgraph "Query Execution"
        Filters --> SKQ[Query submission_keys]
        Filters --> EJQ[Join events + tall_canonical]
        EJQ --> J1[Filter by instance_key]
        J1 --> J2[Join dim_* tables<br/>via value_ref_uid]
        J2 --> AG[Aggregate by selected element]
    end
    
    FilterDimensions --> Filters
    AggregationLayer --> AG
```
## 6. **Analytics Query Pattern**
Shows how typical queries join tables:
```mermaid
graph LR
    subgraph "Filtering Layer"
        A[API Parameters] --> B(submission_keys)
        A --> C(events via anchors)
    end

    B --> D{tall_canonical}
    C --> D

    D --> E[dim_* tables]
    D --> F[canonical_element]

    E --> G[Aggregation]
    F --> G

    G --> H[Results]

    subgraph "Superset Filters → API Params"
        I[ou_uid] --> A
        J[ou_group_uid] --> A
        K[team_uid] --> A
        L[activity_uid] --> A
        M[assignment_uid] --> A
        N[time_range] --> A
        O[template_uid] --> A
        P[anchor_ref_uid] --> A
        Q[repeat_ce_id] --> A
    end

    style I fill:#6060fe
    style P fill:#3030e0
    style Q fill:#4040e0
```

## 7. **Technology Stack Layers**
```mermaid
graph TB
    subgraph "Application Layer"
        A[Spring Boot 3.4.2<br/>Java 17+] --> B[Services]
        B --> C[Repositories]
    end

    subgraph "Persistence Layer"
        D[jOOQ / JdbcTemplate] --> E[Liquibase Migrations]
        C --> F[Hibernate 2nd-level Cache]
    end

    subgraph "Database Layer"
        G[PostgreSQL 16.x] --> H[Core Tables]
        G --> I[Analytics Schema]
        G --> J[Dimension Tables]
    end

    subgraph "Code Generation"
        K[Lombok] --> B
        L[MapStruct] --> B
        M[jOOQ Codegen] --> D
    end

    style A fill:#1010f5
    style G fill:#1111e8
```
