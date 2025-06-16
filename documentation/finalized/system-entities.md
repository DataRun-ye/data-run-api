# Enhanced Multi-Step System Model with Dynamic Scope Handling

We are building a metadata-driven workflow system designed to handle multi-stage data entry across diverse domains such
as inventory management, healthcare, and surveys. The system addresses the need for flexible scoping at both flow and
stage levels, allowing contextual data (like organization unit, team, date, and entity) to be captured dynamically. It
supports repeatable stages and entity binding, enabling complex processes like campaign distributions or inventory
receipts.

## Purpose & Capabilities

**Note:**
**Purpose**

The system's core purpose is to provide a configurable framework for defining and executing domain-specific workflows
without requiring code changes for each new use case. It separates workflow configuration (metadata) from runtime
execution, allowing non-technical users to model processes.

1. **Flexible Dimensional Context Configuration**: Captures a workflow context at flow and stage levels (e.g., warehouse
   for inventory, health, facility for patient intake, a team, ...) without hardcoding (used for tracking and filtering
   workflow data by a group of Dimensional elements).

2. **Multi-Stage Support**: Handles linear or repeatable stages (e.g., registering multiple households in a campaign).
3. **Flexible DataTemplate**: links to stages to define a row of data data elements' configuration.
4. **Domain Entity Binding to Context**: link domain entities (e.g., items, patients) to workflow Context.
5. **Core Entities Binding to Context**: link a core Elements (e.g., Team, OrgUnit, Activity) to workflow Context.
6. **Schema Evolution**: Starts minimal and evolves via configuration, avoiding complex migrations.
7. **Context Preservation**: Metadata changes don't compromise historical context integrity, allowing querying
   historical context independently.

**Core Functionality**

- **Metadata-Driven Configuration**: Define workflows via `FlowType` (process template) and `StageDefinition` (steps),
  `DataTemplate` (a row of data template).
- **Flexible Scoping**:
    - Fixed dimensions: `OrgUnit`, `Team`, `Activity` (predefined tables)
    - Dynamic entities: `Household`, `Item`, `Patient` (runtime-configurable)
    - Flow and Stage: each can define a Scope and have their own context with stage's grouped by scope of the parent's
      containing them.
    - stage can have data defined, configured using `DataTemplate`, and captured separately from its scope.
- **Runtime Execution**:
    - Create `FlowInstance` with `FlowScope`, submit `StageSubmission` with `StageScope`.
    - `StageSubmission` comprise of a a scope data `StageScope`, and a data row `data`, both defined separately and both
      can be captured Separately.
    - A Stage's dataRow is scoped or anchored by stage's and flow's captured contexts.

### Supported Scenarios

| **Domain** | **Use Case**        | **Key Scope Elements**                              |  
|------------|---------------------|-----------------------------------------------------|  
| Inventory  | Receiving shipments | `OrgUnit` (warehouse), `Item` (dynamic entity)      |  
| Healthcare | Patient intake      | `OrgUnit` (clinic), `Patient` (dynamic entity)      |  
| Campaigns  | ITNS distribution   | `Activity` (campaign), `Household` (dynamic entity) |  
| Surveys    | Crop assessment     | `Team` (field team), `Farm` (dynamic entity)        |  

**Reporting Capabilities**

- Filter by core dimensions: `OrgUnit`, `date`, `Team`
- Aggregate by dynamic entities: "Total nets distributed per household"
- Join flow/stage scopes: "Items received in WH_MAIN with quality issues"

## Conceptual Model

- **Fixed System Entities**: The system's regularly used Concrete entities `User`, `OrgUnit`, `Team`, `Activity`...etc.
- **FlowType**: Template for workflows (e.g., "Inventory Receiving"), defining:
    - `flowScopeDefinition`: Core/dynamic attributes at flow level (e.g., warehouse, invoice number).
    - `stages`: Sequence of steps (StageDefinitions).
- **StageDefinition**: Step in a workflow (e.g., "Unpack Items"), with:
    - `stageScopeDefinition`: Stage-specific context (e.g., item batch).
    - `repeatable`: Whether the step can be executed multiple times.
    - `dataTemplate`: defines and configure `DataElement`a used to power the dataRow captured in a stage.
- **Scope Architecture**:
    - **Core Elements**: Fixed entities (OrgUnit, Team, Activity) and dynamic entities (Household, Item) captured in
      flows via `ScopeElementValue`.
- **Dynamic Attributes**: Key-value pairs for less common dimensions (`ScopeAttribute`).
- **Dynamic Entities**:
    - `EntityType`: Defines domain objects (e.g., "Patient") and their attributes.
    - `EntityInstance`: Runtime instances (e.g., "Patient John Doe") with values for the defined attributes
- **DataTemplate**:
    - `DataElements`:

### Workflow Elements

1. **Configuration Parts (metadata definition)**:
    - FlowType
    - StageDefinition: A Flow Stage configuration,
    - DimensionalContext: A Define a context grouping one or more Dimensionals.
    - Dimensional: a Dimensional Element definition in a `DimensionalContext`.
    - DataElement: an Data element definition
    - DataTemplate: configuration of a row of `DataElement`s values.
    - EntityType: a definition of a domain object or Entity with Attributes.
    - EntityAttribute: a definition of an EntityType's attribute.
2. **Runtime Parts**:
    - FlowInstance
    - StageInstance
    - FlowContext
    - StageContext
    - DimensionalValue
    - EntityInstance
    - EntityAttributeValue
3. Fixed System Parts
    * **Dimensional Elements**: can be used in a DimensionalContext
        * OrgUnit: hierarchical orgUnits (districts, villages, facilities...etc)
        * Team: a Contract in a Workflow, a dimensional that can link someone(s), party, people, position, or a user to
          a workflow.
        * Activity: optional dimensional to group workflows data
        * OptionSet: a group of `Options` (e.i predefined select options)
        * Option: can be used as a dimensional, an entityAttribute, or a data element value.

### 1. Entity Relationship Diagram (ERD) Core System Entities

```mermaid
erDiagram
    FlowType ||--o{ StageDefinition: "stages"
    FlowType ||--|| ScopeDefinition: "flow context"
    FlowInstance ||--|| FlowScope: "captured context"
    FlowInstance ||--o{ StageSubmission: "submissions"
    StageSubmission ||--o| StageScope: "captured context"
    StageSubmission ||--o| DataRow: "captured data"
    EntityType ||--o{ EntityAttributeType: "attributes definitions"
    EntityInstance ||--o{ EntityAttributeValue: "attributes values"
    StageDefinition ||--|| DataTemplate: "dataTemplate"
    StageDefinition ||--o| ScopeDefinition: "stage context"
    ScopeDefinition ||--|{ ScopeElement: "dimensions definitions"
    DataTemplate ||--|{ DataElement: "element definitions"
```

### 2. System Class Diagram

```mermaid
classDiagram
    class ScopeDefinition {
        +String id
        +List~ScopeElement~ elements
    }

    class ScopeElement {
        +String id
        +String name
        +boolean required
        +boolean multi
        +String entityTypeId
        +ScopeElementType type
    }

%%    Captured
    class BaseScope {
        <<Abstract>>
        +String id
        +List~ScopeElementValue~ elementValues
    }

%%    Captured
    class FlowScope {
        +FlowInstance flowInstance
        +LocalDate scopeDate
    }

%%    Captured
    class StageScope {
        +StageSubmission stageSubmission
        +LocalDate scopeDate
    }

%%    Captured
    class ScopeElementValue {
        +String key
        +ScopeElement scopeElement
        +OrgUnit orgUnitRef
        +Team teamRef
        +Activity activityRef
        +EntityInstance entityRef
        +String stringValue
        +LocalDate dateValue
        +BigDecimal numberValue
    }

%%    Captured
    class StageSubmission {
        +String id
        +JsonNode dataRow
        +StageScope stageScope
        +StageDefinition stageDefinition
        +SubmissionStatus status
    }

    class FlowType {
        +String id
        +boolean foceStageOrder
        +ScopeDefinition flowScopeDefinition
        +List~StageDefinition~ stages
    }

%%    Captured
    class FlowInstance {
        +String id
        +FlowType flowType
        +FlowScope flowScope
        +List~StageSubmission~ submissions
        +FlowStatus status
    }

    class StageDefinition {
        +String id
        +String name
        +boolean repeatable
        +int order
        +ScopeDefinition stageScopeDefinition
        +DataTemplate dataTemplate
    }

    class OrgUnit {
        +String id
        +String name
        +String code
    }

    class Team {
        +String id
        +String name
        +OrgUnit orgUnit
    }

    class Activity {
        +String id
        +String name
        +LocalDate startDate
    }

    class EntityType {
        +String id
        +String name
    }

    class EntityAttributeType {
        +String id
        +String name
        +AttributeType type
        +boolean required
    }

    class EntityInstance {
        +String id
        +EntityType entityType
        +List~EntityAttributeValue~ attributeValues
    }

    class DataTemplate {
        +String id
        +String name
        +List~DataElement~ elements
    }

    class DataElement {
        +String id
        +String name
        +ValueType type
        +required
    }

    class EntityAttributeValue {
        +Long id
        +String value
    }

    BaseScope <|-- FlowScope
    BaseScope <|-- StageScope
    FlowType "1" *-- "1" ScopeDefinition: flowScopeDefinition
    DataTemplate "1" *-- "*" DataElement: elements
    FlowType "1" *-- "*" StageDefinition: stages
    ScopeDefinition "1" *-- "*" ScopeElement: elements
    FlowInstance "1" *-- "1" FlowScope: flowScope
    StageDefinition "1" *-- "0..1" ScopeDefinition: stageScopeDefinition
    DataTemplate "1" *-- "*" StageDefinition: dataTemplate
    StageSubmission "1" *-- "0..1" StageScope: stageScope
    BaseScope "1" *-- "*" ScopeElementValue: elementValues
    EntityType "1" *-- "*" EntityAttributeType: attributeTypes
    EntityInstance "1" *-- "*" EntityAttributeValue: attributeValues
    FlowInstance "1" *-- "*" StageSubmission
    ScopeElementValue "1" *-- "0..1" OrgUnit
    ScopeElementValue "1" *-- "0..1" Team
    ScopeElementValue "1" *-- "0..1" Activity
    ScopeElementValue "1" *-- "0..1" EntityInstance
    EntityType "1" *-- "*" EntityInstance
```

### How It Works in Each Scenario

#### Scenario 1: Inventory Receiving (Repeatable Stage)

**FlowType Configuration**

```bash
{
  "id": "INV_RECEIVE",
  "name": "Inventory Receiving",
  "flowScopeDefinition": {
    "elements": [
      {"id": "warehouse", "type": "ORG_UNIT", "required": true},
      {"id": "receivingTeam", "type": "TEAM", "required": true},
      {"id": "invoiceNumber", "type": "STRING", "required": true}
    ]
  },
  "stages": [
    {
      "id": "unpack-verify",
      "name": "Unpack & Verify",
      "repeatable": true,  // Key for repeatable stage
      "stageScopeDefinition": {
        "elements": [
          {"id": "item", "type": "ENTITY", "name": "Item", "entityTypeId": "ITEM", "required": true},
          {"id": "batch", "type": "STRING", "name": "Batch", "required": false}
        ]
      },
      "dataTemplate": {
        "id": "unpackItems",
        "name": "unpack-verify Items Template",
        "elements": [
          {"id": "quantity", "type": "Number", "name": "Quantity", "required": true},
          {"id": "condition", "type": "Text", "name": "Condition", "required": true}         
        ]
      }
    }
  ]
}
```

**Flow Creation**

```bash
POST /flows
{
  "flowTypeId": "INV_RECEIVE",
  "scope": {
    "warehouse": {"id": "WH_MAIN"},       // OrgUnit ref
    "receivingTeam": {"id": "TEAM_RECV1"}, // Team ref
    "invoiceNumber": "INV-2024-001"       // Primitive value
  }
}
```

**Stage Submission (Repeatable)**

```bash
# First item
POST /stages
{
  "flowInstanceId": "FLOW_001",
  "stageDefinitionId": "unpack-verify",
  "scope": {
    "item": {"id": "ITEM_PARACETAMOL"}, // EntityInstance ref
    "batch": "BATCH-0424A"
  },
  "data": {
    "quantity": 100,
    "condition": "GOOD"
  }
}

# Second item
POST /stages
{
  "flowInstanceId": "FLOW_001",
  "stageDefinitionId": "unpack-verify",
  "scope": {
    "item": {"id": "ITEM_VITAMINC"},
    "batch": "BATCH-0424B"
  },
  "data": {
    "quantity": 50,
    "condition": "DAMAGED"
  }
}
```

---

#### Scenario 2: Patient Intake (Healthcare)

**FlowType Configuration**

```json-sample
{
  "id": "PATIENT_INTAKE",
  "name": "Patient Registration & Vitals",
  "flowScopeDefinition": {
    "elements": [
      {"id": "facilityScopeElementId", "type": "ORG_UNIT", "name": "Facility", "required": true},
      {"id": "providerScopeElementId", "type": "ENTITY", "name": "Staff", "entityTypeId": "STAFF", "required": true},
      {"id": "patientScopeElementId", "type": "ENTITY", "name": "Patient", "entityTypeId": "PATIENT", "required": true}     
    ]
  },
  "stages": [
    {
      "id": "vitals",
      "name": "Vital Signs",
      "stageScopeDefinition": {},
      "dataTemplate": {
        "id": "vitalTemplateId",
        "name": "vital data collection Template",
        "elements": [
          {"id": "bpDataElementId", "type": "Text", "name": "Pb", "required": true},
          {"id": "pulseDataElementId", "type": "Number", "name": "Pulse", "required": true}
        ]
      }
    }
  ]
}
```

**Flow Creation**

```bash
# Registration and enrollment Stage
POST /flows
{
  "flowTypeId": "PATIENT_INTAKE",
  "scope": {
    "facilityScopeElementId": {"id": "CLINIC_A"},  // OrgUnit ref
    "providerScopeElementId": {"id": "DR_SMITH"}   // EntityInstance ref 
    "patientScopeElementId": {  // new EntityInstance with attributes, backend lookup or create)
        "id": "PT_JOHNDOE",
        "nameDataElementId": "John Doe",
        "dobDataElementId": "1985-04-12"
        } 
  }
}
```

**Stage Submissions**

```bash
# Vitals Stage (no scope)
POST /stages
{
  "flowInstanceId": "FLOW_002",
  "stageDefinitionId": "vitals",
  "data": {
    "bpDataElementId": "120/80",
    "pulseDataElementId": 72
  }
}
```

---

#### Scenario 3: ITNS Campaign Distribution (Repeatable + Multi-Stage)

**FlowType Configuration**

```json-sample
{
  "id": "ITNS_CAMPAIGN",
  "name": "Mosquito Net Distribution",
  "flowScopeDefinition": {
    "elements": [
      {"id": "campaignElementId", "name": "Campaign", "type": "ACTIVITY", "required": true},
      {"id": "villageElementId", "name": "Village", "type": "ORG_UNIT", "required": true}      
    ]
  },
  "stages": [
    {
      "id": "hh-enrollment-and-distribution",
      "name": "Household Net Distribution",
      "repeatable": true,
      "stageScopeDefinition": {
        "elements": [          
          {"id": "householdElementId", "type": "ENTITY", "entityTypeId": "HOUSEHOLD", "required": true}
        ]
      },
      "dataTemplate": {
        "id": "netDistribution",
        "name": "Net Distribution Template",
        "elements": [
          {"id": "hhSizeElement", "type": "Number", "name": "HH Size", "required": true},
          {"id": "gpsIdElement", "type": "Text", "name": "GPS", "required": true},
          {"id": "hhSizeElement", "type": "Number", "name": "HH Size", "required": true},
          {"id": "netsDistributedElement", "type": "Number", "name": "Nets", "required": true},
          {"id": "recipientElement", "type": "Text", "name": "Recipient Name", "required": true}
        ]
      }
    }
  ]
}
```

**Flow Creation**

```bash
POST /flows
{
  "flowTypeId": "ITNS_CAMPAIGN",
  "scope": {
    "campaignElementId": {"id": "CAMP_2024_MAL"}, // Activity ref
    "villageElementId": {"id": "VILLAGE_ALPHA"} // OrgUnit ref
  }
}
```

**Stage Submissions**

```bash
# Household Net Distribution (repeatable)
POST /stages
{
  "flowInstanceId": "FLOW_003",
  "stageDefinitionId": "hh-enrollment-and-distribution",
  "scope": {
    "householdElementId": {"id": "HH_123"}       // EntityInstance ref
  },
  "data": {
    "hhSizeElement": 5,
    "gpsElement": "-1.234,36.789",
    "netsDistributedElement": 3,
    "recipientElement": "Jane Doe"
  }
}
```

---

#### Key Patterns Demonstrated:

1. **Repeatable Stages**
    - `INV_RECEIVE`/`unpack-verify`: Multiple items in one flow
    - `ITNS_CAMPAIGN`/`hh-registration-and-distribution`: Multiple households
2. **Mixed Scope Types**
    - **Fixed Entities**: `OrgUnit` (warehouse/facility/district), `Team`, `Activity`
    - **Dynamic Entities**: `ITEM`, `PATIENT`, `HOUSEHOLD`
    - **Primitives**: `invoiceNumber` (string)

3. **Stage-Scoped Entities**
    - Household in `hh-registration` stage
    - Item in `unpack-verify` stage

---

## UI Concept

a highly configurable UI approach that aligns with the above model.

**Goal:**

1. a unified, configurable way to handle scope capture across all domains while maintaining domain-specific flexibility.
2. a clear separation between scope capture and data entry to creates a consistent user experience whether working with
   campaigns, inventory, or healthcare workflows.

### Unified Scope Capturing UI Concept

```mermaid
flowchart TD
    A[Start Flow/Stage] --> B{Has ScopeDefinition?}
    B -->|Yes| C[Render Scope Capturing Form]
    B -->|No| D[Skip to Data Form]
    C --> E[Capture Scope Values]
    E --> F[Save Scope Instance]
    F --> G[Render Data Template Form]
    G --> H[Save StageSubmission]
```

### Scenario Implementations:

#### 1. Campaign Distribution (ITNs)

**Scope Capture Flow**:

```mermaid
sequenceDiagram
    User ->> UI: Create New Campaign Distribution
    UI ->> Backend: GET /flow-types/ITNS_CAMPAIGN
    Backend -->> UI: FlowType with scopeDefinition
    UI ->> User: Show Scope Form (Campaign, Region, Period)
    User ->> UI: Fill scope values
    UI ->> Backend: POST /flow-instances {scope: {...}}
    Backend ->> DB: Save FlowScope
    Backend -->> UI: FlowInstance ID
    UI ->> User: Show Stage Selection

    loop For Each Household
        User ->> UI: Select "Register Household"
        UI ->> Backend: GET /stage-definitions/hh-registration
        Backend -->> UI: StageDefinition with scopeDefinition
        UI ->> User: Show Scope Form (Village, Household)
        User ->> UI: Fill household details
        UI ->> Backend: POST /stage-submissions {scope: {...}, data: {...}}
    end
```

**Sample UI Flow**:

1. Campaign-level scope form:
   ```
   [ Campaign: ______________ ]
   [ Region:   ______________ ]
   [ Period:   ▁▁▁▁▁▁▁▁▁▁▁▁▁▁ ]
   ```
2. Household registration scope form:
   ```
   [ Village:   ______________ ]
   [ Household: ______________ ]
   [ HH Size:   ▁▁▁▁▁▁▁▁▁▁▁▁▁▁ ]
   ```
3. Data form (appears after scope capture):
   ```
   [ GPS Coordinates: ______________ ]
   [ Notes:           ______________ ]
   ```

#### 2. Inventory Receiving

**Scope Capture Flow**:

```mermaid
sequenceDiagram
    User ->> UI: Start Inventory Receiving
    UI ->> Backend: GET /flow-types/INV_RECEIVE
    Backend -->> UI: FlowType with scopeDefinition
    UI ->> User: Show Scope Form (Warehouse, Team, Invoice)
    User ->> UI: Fill values
    UI ->> Backend: POST /flow-instances {scope: {...}}

    loop For Each Item
        User ->> UI: Click "Add Item"
        UI ->> Backend: GET /stage-definitions/unpack-verify
        Backend -->> UI: StageDefinition with scopeDefinition
        UI ->> User: Show Scope Form (Item, Batch, Expiry)
        User ->> UI: Fill values
        UI ->> Backend: POST /stage-submissions {scope: {...}, data: {quantity, condition}}
    end
```

**Sample UI Flow**:

1. Flow-level scope:
   ```
   [ Warehouse:  ▾ Warehouse A ]
   [ Team:       ▾ Receiving Team 1 ]
   [ Invoice #:  INV-2024-001 ]
   ```
2. Item-level scope:
   ```
   [ Item:      ▾ Paracetamol 500mg ]
   [ Batch #:   BATCH-0424A ]
   [ Expiry:    2025-12-31 ]
   ```
3. Data form:
   ```
   [ Quantity: 100 ]
   [ Condition: ▾ Good ]
   ```

#### 3. Patient Intake (Healthcare)

**Scope Capture Flow**:

```mermaid
sequenceDiagram
    User ->> UI: Start New Patient Intake
    UI ->> Backend: GET /flow-types/PATIENT_INTAKE
    Backend -->> UI: FlowType with scopeDefinition
    UI ->> User: Show Scope Form (Facility, Date, Provider)
    User ->> UI: Fill values
    UI ->> Backend: POST /flow-instances {scope: {...}}
    User ->> UI: Click "Registration"
    UI ->> Backend: GET /stage-definitions/registration
    Backend -->> UI: StageDefinition (no scope)
    UI ->> User: Show Data Form (Patient Details)
    User ->> UI: Click "Vital Signs"
    UI ->> Backend: GET /stage-definitions/vital-signs
    Backend -->> UI: StageDefinition (no scope)
    UI ->> User: Show Data Form (Vitals)
```

**Sample UI Flow**:

1. Facility-level scope:
   ```
   [ Facility: ▾ Main Clinic ]
   [ Date:     2024-06-18 ]
   [ Provider: ▾ Dr. Smith ]
   ```
2. Registration (no scope, direct to data):
   ```
   [ Patient Name: ______________ ]
   [ Date of Birth: ▁▁▁▁▁▁▁▁▁▁▁▁▁▁ ]
   ```

### Submission API Contracts

**1. Create Flow Instance**:

```json-sample
POST /api/flow-instances
{
  "flowTypeId": "INV_RECEIVE",
  "scope": {
    "warehouse": "WH_MAIN",
    "team": "TEAM_RECV_1",
    "invoiceNumber": "INV-2024-001"
  }
}

// Response
{
  "id": "FLOW_01H...",
  "status": "IN_PROGRESS",
  "scope": { ... } // created scope
}
```

**2. Create Stage Submission (with scope)**:

```json-sample
POST /api/stage-submissions
{
  "flowInstanceId": "FLOW_01H...",
  "stageDefinitionId": "unpack-verify",
  "scope": {
    "item": "ITEM_PARACETAMOL",
    "batch": "BATCH-0424A",
    "expiry": "2025-12-31"
  },
  "data": {
    "quantityReceived": 100,
    "condition": "GOOD"
  }
}
```

**3. Create Stage Submission (no scope)**:

```json-sample
POST /api/stage-submissions
{
  "flowInstanceId": "FLOW_01H...",
  "stageDefinitionId": "vital-signs",
  "data": {
    "bp": "120/80",
    "pulse": 72
  }
}
```

### Configuration for Different Domains

**Healthcare (Patient Referral)**:

```json-sample
{
  "id": "PATIENT_REFERRAL",
  "flowScopeDefinition": {
    "elements": [
      {"key": "referringFacility", "type": "ORG_UNIT", "label": "Referring Facility"},
      {"key": "referralDate", "type": "DATE", "label": "Referral Date"},
      {"key": "urgency", "type": "OPTION", "options": ["Emergency", "Urgent", "Routine"]}
    ]
  },
  "stages": [
    {
      "id": "clinical-summary",
      "stageScopeDefinition": {
        "elements": [
          {"key": "diagnosis", "type": "ENTITY", "entityTypeId": "DIAGNOSIS", "label": "Primary Diagnosis"}
        ]
      }
    }
  ]
}
```

**Education (Student Enrollment)**:

```json-sample
{
  "id": "STUDENT_ENROLLMENT",
  "flowScopeDefinition": {
    "elements": [
      {"key": "school", "type": "ORG_UNIT", "label": "School"},
      {"key": "academicYear", "type": "STRING", "label": "Academic Year"}
    ]
  },
  "stages": [
    {
      "id": "student-details",
      "stageScopeDefinition": {
        "elements": [
          {"key": "student", "type": "ENTITY", "entityTypeId": "STUDENT", "label": "Student"}
        ]
      }
    },
    {
      "id": "guardian-info",
      "repeatable": true,
      "stageScopeDefinition": {
        "elements": [
          {"key": "guardian", "type": "ENTITY", "entityTypeId": "GUARDIAN", "label": "Guardian"},
          {"key": "relationship", "type": "STRING", "label": "Relationship"}
        ]
      }
    }
  ]
}
```

### Benefits of This Approach

1. **Consistent UX Pattern**:
    - Scope form → Data form sequence works for all domains
    - Same UI components for scope capture across workflows

2. **Progressive Disclosure**:
   ```mermaid
   journey
       title Form Progression
       section Flow Initiation
         Scope Capture: 5: User
         Data Templates: 0
       section Stage Execution
         Scope Capture: 3: User
         Data Capture: 5: User
   ```


- Only show relevant scope elements per context
- Separate scope concerns from transactional data
- Add new scope elements without UI changes
- Domain-specific labels and input types

### 1. Entity Relationship Diagram (ERD) Scope handling

```mermaid
erDiagram
    BaseScope ||--o{ ScopeElementValue: "elementValues"
    BaseScope {
        string id PK
        string scope_type
    }

    FlowScope ||--|| FlowInstance: "flowInstance"
    FlowScope {
        LocalDate scopeDate
    }

    StageScope ||--|| StageSubmission: "stageSubmission"
    StageScope {
        LocalDate scopeDate
    }

    ScopeElementValue {
        string id PK
        string scope_id FK
        String scope_element_id FK
        string entity_ref_id FK
        string org_unit_ref_id FK
        string team_ref_id FK
        string activity_ref_id FK
        string string_value
        date date_value
        number number_value
    }

    FlowInstance ||--o{ StageSubmission: "submissions"
    OrgUnit ||--o{ ScopeElementValue: "elementValues"
    Team ||--o{ ScopeElementValue: "elementValues"
    Activity ||--o{ ScopeElementValue: "elementValues"
    EntityType ||--o{ EntityInstance: "instances"
    EntityInstance ||--o{ ScopeElementValue: "entityRefs"
    BaseScope }|--|| FlowScope: "FLOW"
    BaseScope }|--|| StageScope: "STAGE"
    ScopeElementValue }|--|| OrgUnit: "orgUnitRef"
    ScopeElementValue }|--|| Team: "teamRef"
    ScopeElementValue }|--|| Activity: "activityRef"
    ScopeElementValue }|--|| EntityInstance: "entityRef"
```

## System Components Explained

### 1. Scope Hierarchy

| **Component**       | **Description**                                 | **Relationships**              |
|---------------------|-------------------------------------------------|--------------------------------|
| `BaseScope`         | Abstract base for all scopes                    | Parent of FlowScope/StageScope |
| `FlowScope`         | Flow-level context (orgUnit, date, etc.)        | 1:1 with FlowInstance          |
| `StageScope`        | Stage-level context (entity binding, overrides) | 1:1 with StageSubmission       |
| `ScopeElementValue` | Configurable dimension with typed value storage | M:1 with BaseScope             |

### 2. Fixed Core Entities

| **Entity** | **Description**                                                      |
|------------|----------------------------------------------------------------------|
| `OrgUnit`  | Organizational units (health facilities, warehouses, districts)      |
| `Team`     | Teams executing workflows (clinical teams, inventory teams)          |
| `Activity` | Activities or campaigns (vaccination drives, distribution campaigns) |

### 3. Dynamic Entity System

| **Component**          | **Description**                                                  |
|------------------------|------------------------------------------------------------------|
| `EntityType`           | Definition of domain entities (Household, Item, Patient)         |
| `EntityInstance`       | Concrete instance of an entity (Household-123, Item-PARACETAMOL) |
| `EntityAttributeType`  | Attribute definition for entities (name, batch, expiry)          |
| `EntityAttributeValue` | Value storage for entity attributes                              |

### 4. Workflow Components

| **Component**     | **Description**                                  |
|-------------------|--------------------------------------------------|
| `FlowType`        | Workflow template (e.g., "Vaccination Campaign") |
| `FlowInstance`    | Runtime instance of a workflow                   |
| `StageDefinition` | Step template (e.g., "Patient Registration")     |
| `StageSubmission` | Actual execution of a stage with data            |
| `DataTemplate`    | Form configuration for stage data                |

## System Capabilities

### 1. Dynamic Scope Configuration

```mermaid
flowchart LR
    A[ScopeDefinition] --> B[CoreElements]
    B --> D[FixedCore: OrgUnit/Team/Activity]
    B --> E[DynamicEntity: EntityInstance]
    B --> F[PrimitiveValue: Date/Number/String]
```

### 2. Entity Lifecycle

```mermaid
sequenceDiagram
    Config ->> System: Define EntityType (e.g., "Household")
    System ->> DB: Create EntityType metadata
    User ->> System: Create EntityInstance
    System ->> DB: Store EntityInstance + AttributeValues
    Workflow ->> System: Reference EntityInstance in Scope
```

### 3. Workflow Execution

```mermaid
sequenceDiagram
    User ->> System: Start Flow (Select FlowType)
    System ->> UI: Render Scope Form
    User ->> System: Submit Scope Values
    System ->> DB: Create FlowInstance + FlowScope
    loop For Each Stage
        User ->> System: Start Stage
        alt Stage Requires Scope
            System ->> UI: Render Stage Scope Form
            User ->> System: Submit Scope Values
            System ->> DB: Create StageScope
        end
        System ->> UI: Render Data Form
        User ->> System: Submit Data
        System ->> DB: Create StageSubmission
    end
```

## Benefits of This Model

1. **Flexible Yet Structured**:
    - Fixed core entities for common dimensions (OrgUnit/Team/Activity)
    - Dynamic entities for domain-specific objects (Household/Item/Patient)
    - Primitive values for simple attributes

2. **Runtime Entity Management**:
    - Create new EntityTypes without schema changes
    - Define attributes through configuration
    - Maintain referential integrity

3. **Optimized Query Performance**:
    - Direct joins for fixed core entities
    - Indexed entity references
    - Materialized views for complex reports

4. **Consistent Scope Handling**:
    - Unified pattern for flow and stage scopes
    - Inheritable scope values
    - Configurable requirements per workflow

## Sample System Output

**FlowType Configuration (YAML)**

```yaml
flowType:
    id: VACCINATION_CAMPAIGN
    name: Community Vaccination Drive
    forceStageOrder: true
    flowScopeDefinition:
        elements:
            -   id: healthFacility
                type: ORG_UNIT
                name: Facility
                required: true
            -   id: team
                type: TEAM
                required: true
            -   id: campaign
                type: ACTIVITY
                name: Campaign
                required: true

    stages:
        -   id: HOUSEHOLD_REGISTRATION
            stageScopeDefinition:
                elements:
                    -   id: household
                        type: ENTITY
                        entityTypeId: HOUSEHOLD
                        required: true
            dataTemplate: HOUSEHOLD_FORM

        -   order: 1
        -   id: VACCINATION_RECORD
            stageScopeDefinition:
                elements:
                    -   id: patient
                        type: ENTITY
                        name: Patient
                        entityTypeId: PATIENT
                        required: true
            dataTemplate: VACCINATION_FORM
```

**EntityType Configuration (JSON)**

```json
{
    "id": "HOUSEHOLD",
    "name": "Household",
    "attributes": [
        {
            "id": "hhId",
            "type": "STRING",
            "required": true
        },
        {
            "id": "location",
            "type": "GPS"
        },
        {
            "id": "members",
            "type": "INTEGER"
        }
    ]
}
```

---

### 3. Sequence Diagram: Stage Submission Flow

```mermaid
sequenceDiagram
    participant User
    participant Controller
    participant Service
    participant Validator
    participant Repository
    User ->> Controller: Submit Stage (JSON)
    Controller ->> Validator: Validate against StageDefinition
    Validator -->> Controller: Validation Result

    alt Validation Failed
        Controller -->> User: 400 Bad Request
    else Validation Passed
        Controller ->> Service: processSubmission()
        Service ->> Service: resolveEntityBinding()
        Service ->> Repository: findFlowInstance()
        Service ->> Repository: saveStageSubmission()

        alt Needs StageScope
            Service ->> Repository: createStageScope()
            loop Each dynamic attribute
                Service ->> Repository: addScopeAttribute()
            end
        end

        Repository -->> Service: Saved Entities
        Service -->> Controller: Submission Receipt
        Controller -->> User: 201 Created
    end
```

### 4. State Diagram: Flow Lifecycle

```mermaid
stateDiagram-v2
    [*] --> Planned
    Planned --> InProgress: Start
    InProgress --> Completed: Finish all stages
    InProgress --> Cancelled: Cancel
    Completed --> Archived
    Cancelled --> Archived
```

---

## Limits & Solutions

| **Limit**                          | **Solution**                                     |  
|------------------------------------|--------------------------------------------------|  
| Complex cross-entity queries       | Materialized views or analytics DB replication   |  
| Real-time reporting at scale       | Async aggregation jobs                           |  
| UI customization for dynamic forms | Flutter form engine with domain-specific widgets |  
| Validation of dynamic scope        | Metadata-driven rules engine                     |  

**Key Constraint**

- **No ad-hoc joins**: Cannot dynamically join arbitrary entity types.  
  *Solution*: Predefine reporting views for common entity combinations.

### Technical Scope

- **Included**:
    - Configurable scoping (flow/stage)
    - Dynamic entity binding
    - Repeatable stages
    - Primitive value capture (date/number/string)
- **Excluded**:
    - Real-time analytics
    - Ad-hoc relationship modeling
    - UI theme customization

## Summary

This engine solves domain-agnostic workflow execution with:

1. **Configurable scopes** mixing fixed dimensions and dynamic entities
2. **Repeatable stages** for bulk operations (e.g., item receiving)
3. **Extensible metadata** to avoid schema changes
4. **Cross-domain consistency** via unified scope/data models

**Problems Solved**

1. **Domain Rigidity**: Support healthcare, inventory, and surveys with same engine
2. **Scope Bloat**: Avoid custom columns for every new dimension
3. **Stage Flexibility**: Repeatable stages with entity binding (e.g., multiple items in one receipt)
4. **Evolution**: Add new scope dimensions without migrations

Reporting focuses on indexed core dimensions, with materialized views for dynamic entity aggregations. UI flexibility is
achieved through a metadata-driven Flutter form renderer.

## Next Steps:

1. **Validation Rules**:
    - Should we add `min/max` for numbers (e.g., `quantity > 0`)?
2. **Cross-Stage References**:
    - How to enforce that `net-distribution` references a household from `hh-registration`?
3. **Bulk Operations**:
    - API support for bulk stage submissions?
4. **Flutter UI Components**:
    - Prioritize widgets for:
        - Entity selectors (dynamic + fixed)
        - Repeatable stage controller
        - Scope/data form separator
