# Core Concepts

1. **FlowType**

    * Defines **the shape** of a process:

        * `planningMode` (PLANNED | LOG\_AS\_YOU\_GO)
        * `submissionMode` (SINGLE | MULTI\_STAGE)
        * **Scopes** (ScopeDefinition, contains `ScopeElement` which are the dimensions like: team, orgUnit, entityType,
          Or an Attribute i.e DATE, STRING, NUMBER e.g invoiceNumber )
        * **Stages** (a StageDefinition each linked to a formTemplate, repeatable flag, optional entityBinding)
    * Example (Receive Inventory Flow type)
2. **FlowInstance**

    * A runtime instantiation of a FlowType, with:
        * `scopeInstance` (map of the chosen team/orgUnit/date/… values)
        * `status` (PLANNED → IN\_PROGRESS → COMPLETED | CANCELLED)
        * `stageStates` (map of stageId → list of submissionIds, for repeatable stages)

3. **StageSubmission**

    * One row per form submit, tied to a FlowInstance and (optionally) a StageDefinition.
    * ScopeInstance if the stage definition define an entity-bound, or it would be scoped only by parent scopeInstance
      elements.

4. **EntityInstance**

    * Spawned only when a stage or a stage's form section is **entity-bound**; upserted post-submission and linked back
      to its FlowInstance (and stageInstance).
5. ScopeDefinition:
    * Contains different `ScopeElement`s.
6. ScopeElement: ScopeElementTypes: [ORG_UNIT, TEAM, ENTITY, ATTRIBUTE]
    * define a scope Element configuration, for example:
        * TEAM,ORG_UNIT: `{ "key":"team","type":"TEAM","required":true,"multiple":false }`.
        * ATTRIBUTE: `{ "key":"invoiceNumber", "type":"NUMBER", "required":true, "multiple":false }`
7. **ScopeInstance**
    * every flow would at least be scoped by `ORGUNIT,` and `DATE`, other optional scoping elements: `TEAM`, `ACTIVITY`,
      `ENTITY`. its the values of the `ScopeDefinition`'s Elements.
    * A single, shareable record that captures “all the context” for either a whole flow (root scope instance) or an
      individual stage submission.

---

## Some Key Details About Scoping

### Formal Definition:

**`ScopeDefinition` → `ScopeInstance`** can be the context of a data, the something(s) that a flow, or stage(s) are
associated with and grouped by, can be one or more of:

- A system core entity `ORGUNIT (location)`, `USER`, `TEAM`, `ACTIVITY`.
- An `ENTITY` restricted to a certain `entity_type_id`.
- A `DATE`.
- Or a free typed-attribute like `INVOICE_No, DATE...`.

**Scope Instance Table Fields:**

* `flow_instance_id`: always points back to the parent flow.
* `stage_submission_id`: null when this SI was created at flow start; set when created by a stage.
* A JSONB scope defined dims `scopeData` (entity_instance_id, team_id, org_unit_id, date…).
* **Why it helps**
    - **Single join point** for any reporting or grouping you do by “scope.”
    - You never need to remember “is the entity in FlowInstance.scopes or StageSubmission.scopes?” — everything is in
      one table.
    - Easy to query: “show me all stage submissions for Household HH123” ⇒ join `stage_submissions → scope_instances`
      where `scope_data->>entity_instance_id = HH123`.

### Scoping Dimension Flow

```mermaid
flowchart TD
    A[Flow Start] --> B{Has Scopes?}
    B -->|Yes| C[Create Root ScopeInstance]
    B -->|No| D[Proceed without Scope]
    C --> E[Process Stages]
    E --> F{Stage has scope?}
    F -->|Yes| G[Create new ScopeInstance\extract ou,team,ei reference]
    F -->|No| H[Use root ScopeInstance]
    G --> I[Create/Update EntityInstance]
    H --> J[Persist StageSubmission]
    I --> J
```

### 2. How It Works in Each Scenario

#### 2.1 One‐Off Form

* **On Start**:

    * Create `FlowInstance` (FI-001).
    * Create `ScopeInstance` (SI-001) with `flow_instance_id = FI-001`, one default `stage_submission_id` linking back
      `=null`
      and a `scope_data` JSONB with the least defined scope dims, no `entity_instance_id` (unless the flow’s scope
      deliberately included an entity).

**One‐Off Form Flow Diagram**

This diagram shows the steps when a user submits a one-off form (SINGLE submission mode).

```mermaid

sequenceDiagram
    actor User
    participant App as Application
    participant DB as Database
    User ->> App: Start new Flow (one-off form)
    App ->> DB: Create FlowInstance (status=IN_PROGRESS)
    DB -->> App: FI-001
    App ->> DB: Create ScopeInstance (for FI-001, no stageSubmission)
    DB -->> App: SI-001
    App ->> User: Show form
    User ->> App: Submit form data
    App ->> DB: Create StageSubmission (for FI-001, no entity bound, data=formData)
    DB -->> App: SS-001
    App ->> DB: Update FlowInstance status to COMPLETED
    App ->> User: Success
```

#### 2.2 Multi‐Stage (ordered), No Repeats

* **Flow Start**: SI-001 created.
* **Each Stage Submit**: ordered (no stage is accepted if previous is not already exist and valid)

    * SS-N is created. `stageSubmission.stageScope=null`
    * process in sequence (first or check previous if exist in system or in same payload)

**This diagram shows a multi-stage flow without repeatable stages**

```mermaid

sequenceDiagram
    actor User
    participant App
    participant DB
    User ->> App: Start new Flow (multi-stage)
    App ->> DB: Create FlowInstance (status=IN_PROGRESS)
    DB -->> App: FI-002
    App ->> DB: Create ScopeInstance (for FI-002, root scope)
    DB -->> App: SI-002

    loop For each stage (in order)
        App ->> User: Show stage form
        User ->> App: Submit stage data
        App ->> DB: Create StageSubmission (for current stage, no new ScopeInstance)
        DB -->> App: SS-00X

    end

    App ->> DB: Update FlowInstance status to COMPLETED
    App ->> User: Flow completed

```

#### 2.3 Multi‐Stage, Scope with Entity at One Stage (entities pre exist in the system 'dropdown selection')

* **Flow Start**: SI-001 , flow scope with the least flow level's required dims `OrgUnit, and scopeDate` .
* **Stage “Enroll”**: for each repeated submission:

    1. Create SS-2a.
    2. Create *new* SI-002 with `flow_instance_id=FI`, `stage_submission_id=SS-2a`, and
       `scope_data={"entity_instance_id": P001}`.
    3. Next repeat ⇒ SI-003 for P002, etc.

* **This diagram shows a multi-stage flow where one stage is repeatable and entity-bound**

    ```mermaid
    sequenceDiagram
        actor User
        participant App
        participant DB
        User ->> App: Start new Flow
        App ->> DB: Create FlowInstance (FI-003, status=IN_PROGRESS)
        DB -->> App: FI-003
        App ->> DB: Create ScopeInstance (root scope for FI-003: SI-003)
        DB -->> App: SI-003
        Note over User, DB: Stage 1: Entity-bound and repeatable
    
        loop For each entity (e.g., each item)
            User ->> App: Start new repeat for Stage1
            App ->> User: Show stage1 form
            User ->> App: Submit form for one entity (e.g., item)
            App ->> DB: Create StageSubmission (SS-00Y for FI-003, stage1)
            DB -->> App: SS-00Y
            App ->> DB: Create ScopeInstance (for SS-00Y, with entityId=...)
            DB -->> App: SI-00Y
            App ->> DB: Create/Update EntityInstance (if new entity, or update attributes)
    
        end
    
        Note over User, DB: Next stage (non-repeatable)
        User ->> App: Submit next stage
        App ->> DB: Create StageSubmission (SS-00Z for FI-003, stage2, no new ScopeInstance)
        DB -->> App: SS-00Z
        App ->> DB: Update FlowInstance status to COMPLETED
        App ->> User: Flow completed
    ```

#### 2.4 Planned Visit to Existing Entity

* **Flow Start**: FI linked to existing flowScope `SI-001` with `{entity_instance_id=HH999}`.
* **Stage 1**: SS-001 is just data that is grouped by parent root scope instance.
* **No extra** SI needed unless a stage binds *another* entity (e.g. enrolling a member).

#### 2.5 Ad‐Hoc (“Log‐As‐You‐Go”)

* **FlowInstance** created on‐demand ⇒ SI-001 created at same time.
* **Stage Submission** ⇒ null.
* **When to create**

    * **Always on flow start** (tie the flow’s scope).
    * **Also** when a stage binds a *new* entity → new SI with `stage_submission_id`.

---

This is supposed to cover different workflows scenarios: campaign-data, inventory, health facility cases, surveys ...

## Example OF WorkFlow Configuration for “Receive Inventory”:

**User sees “Receive Inventory” on their FlowInstance List**

### **FlowInstance**

* **Flow Instance Scope dims Capturing**
    * Form fields:

        1. **ScopeDefinition elements:**
            * `supplierId` (core dim `ENTITY`: `EntityType` “Supplier” dropdown)
            * `teamId` (core dim `TEAM`: automatically injected, or selected, current user’s team) or select
            * `orgUnitId` (core dim `ORG_UNIT`: warehouse location for this assignment)
            * `invoiceNumber` (extra dim `STRING`) value will be stored in `ScopeAttributes`
            * `receiveDate` (extra dim `DATE`) value will be stored in `ScopeAttributes`
        2. **dataTemplate elements**:{`quantityReceived` (number), `qualityStatus` (Good/Damaged)}

    * On “Save,” we insert a `FlowInstance` row, and `FlowScope` row with core attributes and extra dim
      scopeAttributes.
    * No EntityInstances created, just suppliers are selected from a pre-existing entities.

    * **Stage 1: “Unpack & Quality Check”**
        * Stage definition fields:
            1. **ScopeDefinition elements:**
                * `itemId` (core dim `ENTITY`: EntityType “Item” dropdown)
                * `batchNumber` (extra dim `STRING`:)
                * `expirationDate` (extra dim `DATE`)
            2. **dataTemplate elements**:{`quantityReceived` (number), `qualityStatus` (Good/Damaged)}
            3. repeatable = true

    * **Stage 3: “Store in Warehouse” Single‐submission stage:**
        * Stage definition fields:
            1. **ScopeDefinition elements:**
                * `storageLocationId` (core dim `ENTITY`: “Location/Shelf” or simple dropdown item)
            2. **dataTemplate elements**:{`quantityReceived` (number), `qualityStatus` (Good/Damaged)}
                * `storageLocationId` (EntityType “Location” or simple dropdown)
                * Optionally, a multi‐select of which `EntityInstance` items (from Stage 2) go to that location—though
                  we can also assume “all from Stage 2” if our process dictates.
            3. repeatable = false.

### INSERTS SAMPLES

Below is a concrete **Warehouse Inventory** example showing how we’d model **Receive**, **Issue**, and **Discard** flows
with the metadata‐driven engine. It includes:

1. **EntityType** definitions for the core “things” (Item, Supplier).
2. **FlowType** definitions for the three key workflows.
3. Example **FlowInstance** JSON for each.
4. Sketches of **StageSubmission** and **EntityInstance** behavior.

Receive Inventory Flow Example

---

### 1. EntityType Definitions

#### 1.1. `Item`

```jsonc
// POST /api/entity-types
{
  "id": "itemEntityTypeId",
  "name": "Inventory Item",
  "attributes": [
    { "id":"itemId",        "type":"string", "required":true },
    { "id":"itemName",      "type":"string", "required":true },
    { "id":"unitOfMeasure", "type":"string", "required":true }
  ]
}
```

#### 1.2. `Supplier`

```jsonc
{
  "id": "supplierEntityTypeId",
  "name": "Supplier",
  "attributes": [
    { "id":"supplierId",   "type":"string", "required":true },
    { "id":"supplierName", "type":"string", "required":true }
  ]
}
```
// other entityTypes definition as needed

#### 2. FlowType Definitions

##### 2.1. Receive Inventory Flow type

```jsonb
// POST /api/flow-types
{
    "id": "receiveInventory",
    "name": "Receive Inventory",
    "forceStageOrder": true,
    "submissionMode": "MULTI_STAGE",
    "flowScopeDefinition": {
    "scopeElements": [
      {
        "key": "Warehouse",
        "type": "ORG_UNIT",
        "coreElement": true,
        "required": true
      },
      {
        "key": "Receiving Team",
        "type": "TEAM",
        "coreElement": true,
        "required": false
      },
      {
        "key": "Supplier",
        "type": "ENTITY",
        "coreElement": true,
        "required": true,
        "entityTypeId": "SUPPLIER"
      },
      {
        "key": "Invoice #",
        "type": "STRING",
        "coreElement": false,
        "required": true
      }
    ]
  },
    "flowScopeDefinition": {
        "scopeElements": [
            { "key":"supplier","type":"ENTITY",   "required":true,  "multiple":false, "entityTypeId":"Supplier" }
            { "key":"team",    "type":"TEAM",     "required":true,  "multiple":false },
            { "key":"orgUnit", "type":"ORG_UNIT", "required":true,  "multiple":false },
            { "key":"date",    "type":"DATE",     "required":true,  "multiple":false },
            { "key":"invoiceNumber",    "type":"NUMBER",     "required":true,  "multiple":false },
        ]  
  },
  "stages": [
    {
      "id":"unpackCheck",
      "name":"Unpack & Quality Check",
      "sortOrder": 2,
      "formTemplateId":"unpackForm", // {contains: quantityReceived, and qualityStatus elements definitions }
      "repeatable":true,
      "stageScopeDefinition": {
        "scopeElements": [
            { "key":"item","type":"ENTITY",   "required":true,  "multiple":false, "entityTypeId":"ItemEntityTypeId" }         
        ]
      }
    },
    {
      "id":"storeItem",
      "sortOrder": 3,
      "name":"Store Items",
      "formTemplateId":"storeForm",
      "repeatable":false,
      "stageScopeDefinition": {
        "scopeElements": [ // or it can be defined as an optionSet dropdown, not entity.
            { "key":"item","type":"ENTITY",   "required":true,  "multiple":false, "entityTypeId": "storageLocationEntityTypeId" }         
        ]
      }
    }
  ]
}
```

##### 2.2. Issue Inventory

```jsonc
{
  "id": "issueInventory",
  "name": "Issue Inventory",
  "forceStageOrder": true,
  "submissionMode": "MULTI_STAGE",
  "stageScopeDefinition": {
        "scopeElements": [ 
            { "key":"team","type":"TEAM","required":true,"multiple":false },
            { "key":"orgUnit","type":"ORG_UNIT","required":true,"multiple":false },
            { "key":"date","type":"DATE","required":true,"multiple":false }         
        ]
   }
  "stages":[
    {
      "id":"pickItems",
      "name":"Pick Items",
      "formTemplateId":"pickForm",
      "sortOrder": 1,
      "repeatable":true,
      "stageScopeDefinition": {
        "scopeElements": [
            { "key":"item","type":"ENTITY",   "required":true,  "multiple":false, "entityTypeId":"ItemEntityTypeId" }         
        ]
      }
    },
    {
      "id":"validateRecipient",
      "name":"Validate Recipient",
      "sortOrder": 2,
      "formTemplateId":"recipientForm",
      "repeatable":false
    },
    {
      "id":"finalizeIssue",
      "name":"Finalize Issue",
      "sortOrder": 3,
      "formTemplateId":"issueForm",
      "repeatable":false
    }
  ]
}
```

##### 2.3. Discard Inventory

```jsonc
{
  "id": "discardInventory",
  "name": "Discard Inventory",
  "planningMode": "PLANNED",
  "submissionMode": "SINGLE",
  "stageScopeDefinition": {
        "scopeElements": [ 
            { "key":"team","type":"TEAM","required":true,"multiple":false },
            { "key":"orgUnit","type":"ORG_UNIT","required":true,"multiple":false },
            { "key":"date","type":"DATE","required":true,"multiple":false }     
        ]
   }
  "stages":[...]
}
```

---

### 3. Example FlowInstance & Submissions

#### 3.1. Receive FlowInstance

1. **FlowInstance: (two tables)**
    * Table 1 (FlowInstance=`FI-6001`):
        ```jsonc
        POST /api/flow-instances
        {
          "flowTypeId":"receiveInventory",
          "flowInstanceId": "FI-6001",
          "scopeInstance": { // "SI-1001", one to one relation to its scope (see next)
            "scopeDate": {"team": "teamA", "orgUnit":"warehouse1", "date":"2025-06-20", "invoiceNumber":"INV001", "supplier":"supplierX" }
          }, 
        }
        ```
    * Table 2 (`FI-6001` FlowInstance's ScopeInstance=`SI-1001`):
        ```jsonc
        {
            "scopeInstanceId": "SI-1001",
            "flowInstanceId": "FI-6001",
            "stageSubmissionId": null,
            "scopeData": { "team":"teamA", "orgUnit":"warehouse1", "date":"2025-06-20", "invoiceNumber":"INV001", "supplier":"supplierX"}
        }
        ```

2. **Unpack & Quality Check** (repeatable, entity‐bound) scopes only kicks in if there are an entity.

    * Entry 1:
        * Table 1 (StageSubmission=`SS-1001`):
          ```jsonc
          POST /api/stage-submissions
          {
            "stageDefinitionId": "unpackCheck", 
            "flowInstanceId": "FI-6001",
            "stageSubmissionId": "SS-1001",
            "formTemplateId": "unpackForm",
            "scopeInstance": { // `SI-1002`, one to one relation to its scope (see next)
                "scopeDate": {"itemId":"IT100"} 
            } 
            "data":{ "batchNumber":"B100","qty":300,"quality":"Good" } // submission's jsonb data
          }
          ```
        * Table 2 (`SS-1001` StageSubmission's ScopeInstance=`SI-1002`):
          ```jsonc 
          {
            "scopeInstanceId":"SI-1002",
            "flowInstanceId":"FI-6001",
            "stageSubmissionId":"SS-1001", // unpack
            "scopeData":{ "itemId":"IT100"}
          }
          ``` 
    * Entry 2: same for next batch=`SS-xxxx`.
3. **Store Items**
    * Table 1 (StageSubmission=`SS-2002`) :
       ```jsonc
       POST /api/stage-submissions
       {
         "flowInstanceId":"FI-6001",
         "stageSubmissionId": "SS-2002", 
         "stageDefinitionId":"storeItem",
         "scopeInstanceId": "SI-2002",
         "formTemplateId":"storeForm",
         "scopeInstance": null, // (null means: no soping of its own, and scoped by its parent flow scopeInstance)
         "data":{ "storageLocation":"ShelfA","notes":"Completed" }, // submission's jsonb data
       }
       ```
    * Table 2 (`SS-2002` StageSubmission's ScopeInstance) no upsert (means: scoped by its parent flow scope): (unless
      storageLocation is of entity type)

      → marks `FlowInstance.status = COMPLETED`

---

**Multi-Stage Inventory Flow (Receive Example)**

```mermaid
sequenceDiagram
    participant U as User
    participant S as System
    participant DB as Database
    U ->> S: Start Receive Flow
    S ->> DB: Create FlowInstance(FI-6001)
    S ->> DB: Create ScopeInstance(SI-1001: team/orgUnit/date/invoice)

    loop Each Item
        U ->> S: Submit Unpack Stage
        S ->> DB: Create StageSubmission(SS-100x)
        S ->> DB: Create ScopeInstance(SI-100y: itemId)
        S ->> DB: Upsert EntityInstance(Item)
    end

    U ->> S: Submit Store Stage
    S ->> DB: Create StageSubmission(SS-2001)
    S ->> DB: Update FlowStatus(COMPLETED)
```

---

### 4. Issue FlowInstance

1. **FlowInstance**
    * Table 1 (FlowInstance=`FI-7001`):
        ```jsonc
        POST /api/flow-instances
        {
          "flowTypeId":"issueInventory",
          "flowInstanceId": "FI-7001",
          "scopeInstance": { // SI-2001, one to one relation to its scope (see next)
               "scopeData": {"team":"teamB", "orgUnit":"warehouse1", "date":"2025-06-21"} }
        }
        ```

    * Table 2 (`FI-7001` FlowInstance's ScopeInstance=`SI-4001`):
        ```jsonc
        {
            "scopeInstanceId": "SI-4001",
            "flowInstanceId": "FI-7001",
            "stageSubmissionId": null,
            "scopeData": { "team":"teamB", "orgUnit":"warehouse1", "date":"2025-06-21" }
        }
        ```

* **Pick Items**: repeat to select batches/quantities → upsert `Item` ScopeInstance linked to this stage.
* **Validate Recipient** → submission with recipient info.
* **Finalize Issue** → final submission, then `status = COMPLETED`.

---

### 5. Discard FlowInstance

* Table 1 FlowInstance=`FI-8001`:
  ```jsonc
  POST /api/flow-instances
  {
    "flowTypeId":"discardInventory",
    "flowInstanceId": "FI-8001",
    "scopeInstance": { // one to one relation to its scope 
        "scopeData": { "team":"teamA", "orgUnit":"warehouse1", "date":"2025-06-22" } 
    }
  }
  ```
* Table 2 `FI-8001` FlowInstance's ScopeInstance=`SI-xxx`: goes the same as above

### How This Scales to Other Warehouse Flows

* **Transfer Inventory**: define a `transferInventory` FlowType with `scopes` including `fromOrgUnit` and `toOrgUnit`.
* **Returns or Repairs**: bind stages to an `ItemBatch` or `Equipment` EntityType for lifecycle tracking.

## Model Diagram

```mermaid
erDiagram
    FLOW_TYPE ||--|{ SCOPE_ELEMENT: has
    FLOW_TYPE ||--|{ STAGE_DEFINITION: has
    FLOW_TYPE ||--o{ FLOW_INSTANCE: instantiates
    FLOW_INSTANCE ||--o{ STAGE_SUBMISSION: has
    FLOW_INSTANCE ||--|| SCOPE_INSTANCE: scopedBy
    STAGE_DEFINITION ||--o{ STAGE_SUBMISSION: defines
    DATA_TEMPLATE ||--o{ STAGE_DEFINITION: powers
    STAGE_SUBMISSION ||--|| SCOPE_INSTANCE: scopedBy
    DATA_TEMPLATE ||--o{ DATA_ELEMENT: has
    ENTITY_DEFINITION ||--o{ ENTITY_INSTANCE: defines
    SCOPE_INSTANCE ||--o| ENTITY_INSTANCE: has_embeddedInJsonB
    SCOPE_INSTANCE ||--o| ENTITY_INSTANCE: has_embeddedInJsonB
    SCOPE_INSTANCE ||--o| OTHER_SYSTEM_ENTITIES: has_embeddedInJsonB
    SCOPE_ELEMENT ||--|{ ENTITY_DEFINITION: has
    SCOPE_ELEMENT ||--|{ SCOPE_ELEMENT_VALUE: has
    SCOPE_ELEMENT ||--|{ SCOPE_ELEMENT_VALUE: has
    SCOPE_ELEMENT ||--|{ SCOPE_ELEMENT_VALUE: has
    SCOPE_ELEMENT ||--|{ SCOPE_ELEMENT_VALUE: has
%% Table-entity definitions
    FLOW_TYPE {
        String id PK
        String activity_definition_id FK
        String name
    %% {}
        List scopeElementDefinitions
    %% PLANNED LOG_AS_YOU_GO
        Enum planning_mode
    %% SINGLE MULTI_STAGE
        Enum submission_mode
        Timestamp created_at
    }
%% Definition of a scope element
    SCOPE_ELEMENT {
        String id PK
        String key
    %% dataType.ORG_UNIT, dataType.TEAM, dataType.ACTIVITY, dataType.ENTITY, dataType.DATE, ...
        enum valueType
    %% when dataType.ENTITY, provide the id of its type, e.g. "Household", "Patient"
        String entityTypeId
        Timestamp created_at
    }
    STAGE_DEFINITION {
        String id PK
        String flow_type_id FK
        String name
        String data_template_id FK
        Boolean repeatable
        Integer stage_order
    %% if the
    %% where entity fits better, option1: bound entity to a stage instance in flow instance level
        String entity_definition_id FK
        Timestamp created_at
    }
    DATA_TEMPLATE {
        String id PK
        String name
    %% your existing template JSON (DataElements)
        List dataTemplateElements
        Timestamp created_at
    }
%% optional pre-created and pre-link scope for planned flow instances
    FLOW_INSTANCE {
        String id PK
        String flow_type_id FK
        Date flow_instance_date
    %% entity_instance_id {team: team_id, orgUnit: ou_id, entity: ei_id, ...}
        String scope_instance_id FK
    %% { stageId: [...submissionIds] }
        JSONB stage_states
    %% PLANNED, IN_PROGRESS, COMPLETED, CANCELLED
        Enum status
        Timestamp created_at
    }
%% default implicit single stage if no stages
    STAGE_SUBMISSION {
        String id PK
        String flow_instance_id FK
        String stage_definition_id FK
        String data_template_id FK
    %% nullable if scoped by parent
        String scope_instance_id
        JSONB data
    %% PENDING, SUBMITTED, REJECTED
        Enum status
        Timestamp submitted_at
    }
    SCOPE_INSTANCE {
        String id PK
        String flow_instance_id FK
    %% null if a scope is of flow instance
        String stage_submission_id FK
    %% a map of scope elements values
        JSONB scopeData
        Timestamp submitted_at
    }
    ENTITY_DEFINITION {
        String id PK
    %% e.g. "Household", "Patient"
        String key
        String display_name
    %% list of { id, type, required }
        JSONB attributeDefinitions
        Timestamp created_at
    }
    ENTITY_INSTANCE {
        String id PK
        String entity_definition_id FK
    %% e.g id, name, code
        JSONB entityAttributeValues
        Timestamp created_at
        Timestamp updated_at
    }
    DATA_ELEMENT {
        String id PK
        String name
        Enum valueType
    }

    OTHER_SYSTEM_ENTITIES {
    }

    SCOPE_ELEMENT_VALUE {
        String element_id
        Object element_value
    }
```

### 1. Overall System Sequence Diagram

```mermaid
sequenceDiagram
    participant User
    participant Controller
    participant Service
    participant Repository
    participant DB as Database
    User ->> Controller: HTTP Request (e.g., POST /flow-instances)
    Controller ->> Service: Process request
    Service ->> Repository: Call appropriate method
    Repository ->> DB: Execute database operation
    DB -->> Repository: Return result
    Repository -->> Service: Return domain object
    Service ->> Service: Perform business logic/validation
    Service -->> Controller: Return DTO
    Controller -->> User: Return API response
```

### 2. FlowInstance Creation Sequence

```mermaid
sequenceDiagram
    participant User
    participant FlowController
    participant FlowService
    participant ScopeService
    participant FlowRepo
    participant ScopeRepo
    User ->> FlowController: POST /flow-instances (with flowTypeId + scopeData)
    FlowController ->> FlowService: createFlowInstance(flowTypeId, scopeData)
    FlowService ->> FlowService: Validate flowType exists
    FlowService ->> ScopeService: createScopeInstance(flowType.scopeDefinition, scopeData)
    ScopeService ->> ScopeRepo: save(ScopeInstance)
    ScopeRepo -->> ScopeService: SavedScopeInstance
    FlowService ->> FlowRepo: save(new FlowInstance(scopeInstance))
    FlowRepo -->> FlowService: SavedFlowInstance
    FlowService -->> FlowController: FlowInstanceDTO
    FlowController -->> User: 201 Created with DTO
```

### 3. StageSubmission Sequence (Entity-Bound)

```mermaid
sequenceDiagram
    participant User
    participant StageController
    participant StageService
    participant EntityService
    participant ScopeService
    participant FlowService
    User ->> StageController: POST /stage-submissions (flowId, stageDefId, formData)
    StageController ->> StageService: submitStage(flowId, stageDefId, formData)
    StageService ->> FlowService: getFlowInstance(flowId)
    StageService ->> StageService: Validate stage definition
    alt Entity-bound stage
        StageService ->> EntityService: createOrUpdateEntity(formData.entityAttributes)
        EntityService -->> StageService: EntityInstance
        StageService ->> ScopeService: createScopeInstance(flow.scopeInstance + entity)
    else Non-entity stage
        StageService ->> ScopeService: getParentScope(flowId)
    end

    ScopeService -->> StageService: ScopeInstance
    StageService ->> StageService: Build StageSubmission object
    StageService ->> FlowService: updateFlowStageState(flowId, newSubmission)
    StageService -->> StageController: StageSubmissionDTO
    StageController -->> User: 201 Created with DTO
```

### 4. State Transition Diagram for FlowInstance

```mermaid
stateDiagram-v2
    [*] --> PLANNED: For PLANNED flows
    PLANNED --> IN_PROGRESS: First stage submission
    IN_PROGRESS --> COMPLETED: Final stage completed
    IN_PROGRESS --> CANCELLED: User cancellation
    COMPLETED --> [*]
    CANCELLED --> [*]
    [*] --> IN_PROGRESS: LOG_AS_YOU_GO flows
    IN_PROGRESS --> COMPLETED: Final stage completed
    IN_PROGRESS --> CANCELLED: User cancellation
```

### 5. ScopeInstance Resolution Process

```mermaid
flowchart TD
    A[Start Stage Submission] --> B{Stage entity-bound?}
    B -->|Yes| C[Create new ScopeInstance]
    C --> D[Bind entity to scopeData]
    D --> E[Link to stageSubmission]
    B -->|No| F[Use parent FlowInstance ScopeInstance]
    E --> G[Save ScopeInstance]
    F --> G
    G --> H[Proceed with submission]
```

---

### 1. Entity Relationships metadata

```mermaid
erDiagram
    FlowType ||--|| ScopeDefinition: "1:1 (root dims)"
    FlowType ||--|| ScopeDefinition: "1:1 (root dims)"
    FlowType ||--|{ StageDefinition: "1:M"
    StageDefinition ||--o| ScopeDefinition: "0:1 (stage dims)"
    StageDefinition ||--|| FormTemplate: "1:1 (stage form def)"
    FormTemplate ||--|{ DataElement: "1:M (form element def)"
```

### 1. Entity Relationships

```mermaid
erDiagram
    FlowInstance ||--o{ StageSubmission: "1:M"
    FlowInstance ||--|| ScopeInstance: "1:1 (root dims)"
    StageSubmission ||--o| ScopeInstance: "0..1 (stage dims)"
    ScopeInstance ||--|| orgUnit: "1:1 (ou dim)"
    ScopeInstance ||--o| Team: "0..1 (team dim)"
    ScopeInstance ||--o| EntityInstance: "0..1"
    FlowType ||--|{ StageDefinition: "1:M"
    FlowType ||--|{ ScopeDefinition: "1:M (root ctx defs)"
    StageDefinition ||--o| ScopeDefinition: "1:M (stage ctx defs)"
    ScopeDefinition ||--|{ ScopeElement: "1:M (scope element def)"
    StageDefinition ||--|| FormTemplate: "1:1 (stage form def)"
    FormTemplate ||--|{ DataElement: "1:M (form element def)"
```

#### 2. Key Operations Sequence

```mermaid
sequenceDiagram
    participant User
    participant Controller
    participant FlowService
    participant ScopeService
    participant EntityService
    User ->> Controller: POST /flow-instances (PLANNED)
    Controller ->> FlowService: createPlannedFlow(flowTypeId, scopes)
    FlowService ->> ScopeService: createRootScope(scopes)
    ScopeService ->> ScopeRepo: save(ScopeInstance)
    FlowService ->> FlowRepo: save(FlowInstance)
    User ->> Controller: POST /stages (entity-bound)
    Controller ->> StageService: submitStage(flowId, stageDefId, formData)
    StageService ->> EntityService: upsertEntity(entityType, attributes)
    StageService ->> ScopeService: createStageScope(flowId, entity)
    StageService ->> StageRepo: save(StageSubmission)
    StageService ->> FlowService: updateFlowStatus(flowId)
```
