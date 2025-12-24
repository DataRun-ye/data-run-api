
## 1. **Anchor Concept: Within-Submission Grouping Elements**
```mermaid
graph TD
    subgraph "Submission Root"
        A[Root Instance] --> B[Element A: text]
        A --> C[Element B: number]
        A --> D[Anchor Element: org_unit_ref]
        A --> E[Element C: option_ref]
        A --> F[Element D: date]
    end
    
    subgraph "Repeat Group 1"
        G[Repeat Instance 1] --> H[Element A: text]
        G --> I[Anchor Element: activity_ref]
        G --> J[Element B: number]
        G --> K[Element C: option_ref]
    end
    
    subgraph "Repeat Group 2"
        L[Repeat Instance 2] --> M[Element A: text]
        L --> N[Anchor Element: activity_ref]
        L --> O[Element B: number]
        L --> P[Element C: option_ref]
    end
    
    A --> G
    A --> L
    
    D -.-> Q[Groups all root elements<br/>by org_unit_ref]
    I -.-> R[Groups repeat elements<br/>by activity_ref]
    N -.-> R
    
    style D fill:#5050e0
    style I fill:#5050e0
    style N fill:#5050e0
    style Q fill:#5050fe
    style R fill:#5050fe
```

### 2. **Anchor Configuration & Selection Flow**
```mermaid
flowchart TD
    subgraph "Template Configuration"
        A[Template Elements]
        A --> B[Element 1: text_field]
        A --> C[Element 2: org_unit_ref<br/>marked as anchor]
        A --> D[Element 3: activity_ref<br/>marked as anchor]
        A --> E[Element 4: option_ref]
    end
    
    subgraph "Data Submission"
        F[Submission JSON] --> G[TransformServiceV2]
        G --> H{Check canonical_element_anchor}
        
        H -- Anchor allowed --> I[Add to anchor candidates]
        H -- Not anchor --> J[Process normally]
        
        I --> K[Evaluate anchor_priority]
        K --> L[Select best anchor per instance]
    end
    
    subgraph "Persist Anchor Context"
        L --> M[Update events.anchor_ce_id]
        L --> N[Update events.anchor_ref_uid]
        M --> O[Anchor available for grouping]
        N --> P[Link to dim_* tables]
    end
    
    style C fill:#4040e0
    style D fill:#4040e0
    style O fill:#4040fe
```

## 3. **Hierarchical Anchor Grouping**
```mermaid
graph TB
    subgraph "Submission with Nested Repeats"
        R[Root Instance<br/>Anchor: org_unit_uid]
        
        R --> G1[Repeat Group A<br/>Anchor: team_uid]
        R --> G2[Repeat Group B<br/>Anchor: team_uid]
        
        G1 --> S1[Repeat Item A1<br/>Anchor: activity_uid]
        G1 --> S2[Repeat Item A2<br/>Anchor: activity_uid]
        G2 --> S3[Repeat Item B1<br/>Anchor: activity_uid]
        
        S1 --> T1[Values: q1, q2, q3]
        S2 --> T2[Values: q1, q2, q3]
        S3 --> T3[Values: q1, q2, q3]
    end
    
    subgraph "Analytics Grouping"
        U[Group by org_unit_uid] --> V[All root-level analysis]
        
        W[Group by team_uid] --> X[Within each team<br/>see nested repeats]
        
        Y[Group by activity_uid] --> Z[Drill down to<br/>specific activities]
    end
    
    R -.-> U
    G1 -.-> W
    G2 -.-> W
    S1 -.-> Y
    S2 -.-> Y
    S3 -.-> Y
    
    style R fill:#5050e0
    style G1 fill:#5050e0
    style G2 fill:#5050e0
    style S1 fill:#5050e0
    style S2 fill:#5050e0
    style S3 fill:#5050e0
```

## **`ref_resolution` And Anchors**

```mermaid
graph LR
    subgraph "Ref Resolution (Token → UID)"
        A[Raw value `Nairobi`] --> B[ref_resolution table]
        B --> C[Deterministic lookup]
        C --> D[resolved_uid: `ou_abc123`]
        D --> E[dim_org_unit.uid]
    end
    
    subgraph "Anchor System (Submission → Grouping)"
        F[Submission instance] --> G{Which element provides<br/>grouping context?}
        G --> H[Check canonical_element_anchor]
        H --> I[Select best anchor element]
        I --> J[Store anchor_ref_uid in events]
        J --> K[Group all instance elements<br/>by this anchor]
    end
    
    subgraph "Why different?"
        L[ref_resolution: Token-level<br/>resolves individual values]
        M[Anchors: Instance-level<br/>provides grouping context<br/>for ALL values in instance]
    end
    
    D -.-> J
```

## 4. **Anchor vs ref_resolution: Complementary Systems**
```mermaid
flowchart TD
    subgraph "Data Flow"
        A[Submission JSON] --> B[TransformServiceRobust]
        B --> C[tall_canonical rows]
        C --> D[RefResolutionService]
        D --> E[Resolve ref values]
        E --> F[Update tall.value_ref_uid]
        
        C --> G[TransformServiceV2]
        G --> H[Identify anchor candidates]
        H --> I[Select best anchor]
        I --> J[Update events.anchor_ref_uid]
    end
    
    subgraph "Anchor-Specific Processing"
        K[Check canonical_element_anchor] --> L[anchor_allowed?]
        L --> M[Yes: Consider for anchor]
        L --> N[No: Skip for anchor]
        
        M --> O[Evaluate anchor_priority]
        O --> P[Compare with other candidates]
        P --> Q[Select highest priority<br/>non-null anchor]
    end
    
    subgraph "Analytics Usage"
        R[Query for submissions] --> S{Group by?}
        S -- Standard --> T[submission_keys dims]
        S -- Anchor-based --> U[events.anchor_ref_uid]
        
        U --> V[Join to dim tables<br/>via anchor_ref_uid]
    end
    
    style Q fill:#1212e0
    style U fill:#2020fe
```

## **Key Differences Summary:**
```mermaid
graph TD
    subgraph "ref_resolution Table"
        A1[Purpose: Token resolution audit]
        A2[Scope: Individual values]
        A3[Function: Map text → uid]
        A4[Authority: Resolution history]
        A5[Use: Backfill tall.value_ref_uid]
    end
    
    subgraph "Anchor System"
        B1[Purpose: Grouping context]
        B2[Scope: Entire instance/level]
        B3[Function: Select grouping key]
        B4[Authority: Template configuration]
        B5[Use: Analytics grouping/filtering]
    end
    
    C[Both systems use dim_* tables]
    D[Anchors build ON TOP of ref_resolution]
    
    A3 --> C
    B3 --> C
    A5 -.-> D
```

## 5. **Analytics: Anchor vs Standard Filtering**
```mermaid
graph LR
    subgraph "Superset Filters → API"
        A[ou_uid] --> F(Query Builder)
        B[team_uid] --> F
        C[time_range] --> F
        D[anchor: org_unit] --> F
        E[anchor: activity] --> F
    end

    subgraph "Standard Query Path"
        F --> G[Filter submission_keys]
        G --> H[Join tall_canonical]
        H --> I[Group/Aggregate]
    end

    subgraph "Anchor Query Path"
        F --> J[Filter events by anchor_ref_uid]
        J --> K[Join tall_canonical on instance_key]
        K --> L[Group/Aggregate<br/>by anchor context]
    end

    subgraph "Result Differences"
        I --> M[All submissions<br/>matching filters]
        L --> N[Only submissions where<br/>specified anchor exists<br/>AND provides grouping]
    end

    style D fill:#3030e0
    style E fill:#2020e0
    style N fill:#5050fe
```
