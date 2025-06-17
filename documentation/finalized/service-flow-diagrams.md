Based on your requirements, here are Mermaid diagrams visualizing key flows through the system layers, including
validation and processing patterns:

### 1. End-to-End Flow: User Submission to Persistence

```mermaid
sequenceDiagram
    actor User
    participant REST API
    participant Controller
    participant Service
    participant Validator
    participant Repository
    participant DB[(PostgreSQL)]
    User ->> REST API: POST /flows (with scope data)
    activate REST API
    REST API ->> Controller: parseRequest()
    Controller ->> Validator: validateScope(flowType, scopeDTO)
    activate Validator
    Validator -->> Controller: ValidationResult
    deactivate Validator

    alt Validation failed
        Controller -->> REST API: 400 Bad Request
        REST API -->> User: Error details
    else Validation passed
        Controller ->> Service: createFlowInstance(flowTypeId, scopeDTO)
        activate Service
        Service ->> Service: resolveEntityInstance(scopeDTO)
        Service ->> Repository: save(flowInstance)
        activate Repository
        Repository ->> DB: INSERT flow_instance, flow_scope
        DB -->> Repository: IDs
        deactivate Repository
        Service -->> Controller: FlowInstance
        deactivate Service
        Controller -->> REST API: 201 Created
        REST API -->> User: FlowInstance ID + status
    end
    deactivate REST API
```

### 2. Stage Submission Flow with Validation

```mermaid
sequenceDiagram
    actor Field Agent
    participant Mobile API
    participant StageController
    participant StageService
    participant ScopeValidator
    participant EntityResolver
    participant StageRepo
    Field Agent ->> Mobile API: POST /stages (JSON data)
    activate Mobile API
    Mobile API ->> StageController: handleSubmission()
    StageController ->> ScopeValidator: validateStageScope(stageDef, scopeData)
    activate ScopeValidator
    ScopeValidator ->> EntityResolver: resolveEntity(scopeData)
    EntityResolver -->> ScopeValidator: EntityInstance
    ScopeValidator -->> StageController: ValidationStatus
    deactivate ScopeValidator

alt Invalid entity/scope
StageController-->>Mobile API: 422 Unprocessable
Mobile API-->>Field Agent: Error message
else Valid
StageController->>StageService: submitStage(flowId, stageDefId, data)
StageService->>StageRepo: saveStageSubmission()
StageRepo->>StageRepo: Transaction:
- Create StageSubmission
- Create StageScope (if bound)
- Insert scope_attributes
StageService-->>StageController: SubmissionReceipt
StageController-->>Mobile API: 202 Accepted
Mobile API-->>Field Agent: Success + timestamps
end
deactivate Mobile API
```

### 3. Service Layer Processing Pattern

```mermaid
flowchart TD
    A[Service Method] --> B{Has Entity Binding?}
    B -->|Yes| C[Resolve EntityInstance]
    C --> D[Create StageScope]
    D --> E[Add ScopeAttributes]
    B -->|No| F[Use FlowScope]
    F --> G[Validate Against Metadata]
    G --> H[Persist StageSubmission]
    H --> I[Update Flow Status]
    I --> J[Publish Domain Event]
    J --> K[(Async Handlers)]
```

### 4. Validation Flow Through Layers

```mermaid
journey
    title Data Validation Journey
    section User Input
        REST API: 1. Schema Validation
    section Business Layer
        Service: 2. Scope Rules Check
        Validator: 3. Entity Exists?
        Validator: 4. Dynamic Attr Allowed?
    section Persistence
        Repository: 5. Constraint Checks
        DB: 6. Foreign Key Validation
    section Response
        Success: 7. Return 202 Accepted
        Failure: 7. Return 422 with Errors
```

### 5. Repository Save Sequence

```mermaid
sequenceDiagram
    participant S as StageService
    participant R as StageRepository
    participant E as EntityManager
    participant D as DB
    S ->> R: save(stageSubmission)
    R ->> E: beginTransaction()
    R ->> E: persist(stageSubmission)
    alt Has StageScope
        R ->> E: persist(stageScope)
        loop For each dynamic attribute
            R ->> E: persist(scopeAttribute)
        end
    end
    R ->> E: commit()
    E ->> D: INSERT statements
    D -->> E: Success
    E -->> R: Managed entities
    R -->> S: Saved entity graph
```

### Key Design Notes:

1. **Validation Layering**:
    - REST: Schema integrity
    - Service: Business rules/metadata compliance
    - Repository: Data integrity constraints
    - DB: Final safeguard

2. **Entity Resolution**:
   ```mermaid
   flowchart LR
       Request-->EntityResolver
       EntityResolver-->|Existing|EntityRepo[Entity Repository]
       EntityResolver-->|New|EntityFactory
       EntityFactory-->EntityService
   ```

3. **Transaction Boundaries**:
    - Flow creation: Single transaction (instance + scope)
    - Stage submission: Nested transaction with rollback
    - EAV attributes: Batched inserts

4. **Error Handling**:
    - Validation failures: Return 422 with error paths
    - DB errors: Spring DataExceptions translated to 503

These diagrams follow the explicit scope pattern with:

- Clear separation between flow-level and stage-level scoping
- Metadata-driven validation
- Entity binding resolution
- Progressive persistence through layers
- EAV attributes as extension points

Would you like me to elaborate on any specific flow or add diagrams for reporting/evolution patterns?
