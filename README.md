# Datarun

This is a "microservice" application intended to be part of a microservice architecture.

This application is configured for Service Discovery and Configuration with Consul. On launch, it will refuse to start if it is not able to connect to Consul at [http://localhost:8500](http://localhost:8500). For more information, read our documentation on [Service Discovery and Configuration with Consul][].

## Build dependencies

The current system is built upon:

* **Java 17+ (Spring Boot 3.4.2)**: A Maven-based project, initially generated with JHipster and extended.
* **PostgreSQL (tested with v16.x)**: Utilizes a compatible PostgreSQL JDBC driver.
* **Liquibase (XML)**: Used for managing schema migrations.
* **Spring Security & Application-level ACLs**: Integrated for security.
* **`jOOQ` & `NamedParameterJdbcTemplate`/`JdbcTemplate`**: Available for analytical queries.
* **Caching**: Employs Ehcache and Hibernate 2nd-level cache annotations where appropriate.
* **Codegen Tools**: Lombok.
* **Testing**: Testcontainers (Postgres), JUnit 5, and AssertJ are used for testing.
* **User authentication (will be deprecated)**:  
    * sending basic user's credentials and receiving Access/Refresh tokens.
    * **Authentication for New Services (new):** All new services authenticate via this backend, which issues RS256-signed JWTs and exposes the public key at `/ .well-known/jwks.json`. New services simply configure a `JwtDecoder` with this public key to validate tokens—no private keys or user DB access needed. Clients authenticate once with `datarunapi`, receive a JWT, and call any service with `Authorization: Bearer <token>`; each service validates the token locally.

## Key Architectural Principles

### 1. IDs, UIDs and business keys

* **id**: internal primary key (VARCHAR(26)) ULID format. Immutable, never recycled. Used for all foreign-key
  relationships.
* **uid**: short system generated business key (VARCHAR(11)), globally unique, stable across environments, used
  extensively in api client's requests and analytics for human-friendly references.

## 2. High level view of the core archtircture


```mermaid
graph TB
    subgraph DatarunAPI_Frontend["DatarunAPI Web Frontend (Angular)"]
        direction TB
        DR_ADMIN["Admin Module<br/>──────────<br/>Template Designer<br/>User/Team/Assignment Mgmt"]
        DR_CAPTURE["Data Capture Module<br/>──────────<br/>Form Rendering Engine<br/>Normalized Submission"]
        DR_REVIEW["Review Module<br/>──────────<br/>Submission Browser<br/>Pivot / Analytics"]
        FORM_ENGINE["Headless Form Engine<br/>──────────<br/>State Store · Rule Evaluator<br/>Tree Registry · Collection Mgr"]
    end

    subgraph DatarunAPI_Backend["DatarunAPI Backend (Java · Spring Boot)"]
        direction TB
        V1_REST["V1 REST API<br/>──────────<br/>/api/v1/dataSubmission<br/>/api/v1/dataFormTemplates<br/>(mobile app uses this)"]
        V2_REST["V2 REST API<br/>──────────<br/>/api/v2/dataSubmission<br/>/api/v2/formTemplates<br/>(web frontend uses this)"]
        TRANSLATOR["Internal Translation<br/>──────────<br/>V1→Canonical<br/>Canonical→V1<br/>(not an API, not a BC)"]
        TREE_BUILDER["Template Transformer<br/>──────────<br/>HashMap Registry<br/>sections+fields → V2 Tree"]
        CANONICAL_STORE[("PostgreSQL<br/>──────────<br/>Canonical Normalized<br/>Submission Store<br/>(form_data JSONB)")]
    end

    subgraph Mobile["Flutter Mobile App"]
        MOB_APP["Data Collector App<br/>──────────<br/>Offline-first<br/>V1 API consumer"]
    end

    subgraph Consumer_Platform["Consumer / Domain Platform (Python · FastAPI)"]
        direction TB
        ADAPTER["Adapter BC (ACL)<br/>──────────<br/>Mapping Contracts<br/>Published Language → DomainCommand"]
        DOMAIN_BC["Domain BC (event-sourced or CRUD)<br/>──────────<br/>Inventory / Case Mgmt / Ledger (example)"]
        SHARED_KERNEL["Shared Kernel<br/>──────────<br/>Node Registry<br/>Commodity Registry<br/>Policy Engine"]
        BFF["Composition (BFF)<br/>──────────<br/>Multi-BC Read Aggregation"]
        SPA["Consumer SPA (Angular)<br/>──────────<br/>Dashboards · Adapter Monitoring · Approval Workflows"]
    end

    %% DatarunAPI Frontend → Backend
    DR_CAPTURE --> FORM_ENGINE
    DR_ADMIN -->|"CRUD"| V2_REST
    DR_CAPTURE -->|"GET tree / POST submission"| V2_REST
    DR_REVIEW -->|"GET submissions"| V2_REST

    %% Mobile → Backend
    MOB_APP -->|"V1 REST"| V1_REST

    %% Internal Backend Flow
    V1_REST -->|"normalize on write"| TRANSLATOR
    V1_REST -->|"denormalize on read"| TRANSLATOR
    V2_REST -->|"build tree"| TREE_BUILDER
    V2_REST -->|"passthrough"| CANONICAL_STORE
    TRANSLATOR --> CANONICAL_STORE

    %% DatarunAPI → Consumer Platform
    V1_REST -.->|"Published Language<br/>(OHS + PL)"| ADAPTER
    V2_REST -.->|"Future: V2 Published Language<br/>(when Adapter migrates)"| ADAPTER

    %% Consumer Platform Internal
    ADAPTER -->|"DomainCommand"| DOMAIN_BC
    SHARED_KERNEL -.-|"Shared Kernel<br/>(DB reads)"| DOMAIN_BC
    DOMAIN_BC -->|"Read Models"| BFF
    SHARED_KERNEL -->|"Read Models"| BFF
    SPA -->|"HTTP"| BFF
    SPA -->|"Direct single-BC ops"| DOMAIN_BC

    %% SSO
    DatarunAPI_Backend -.->|"JWKS (RS256)<br/>SSO for all"| Consumer_Platform
    DatarunAPI_Backend -.->|"JWKS"| DatarunAPI_Frontend

    %% Styling
    classDef external fill:#2d3748,stroke:#4a5568,color:#e2e8f0
    classDef datarun fill:#1a365d,stroke:#2b6cb0,color:#bee3f8
    classDef consumer fill:#22543d,stroke:#38a169,color:#c6f6d5
    classDef engine fill:#744210,stroke:#d69e2e,color:#fefcbf

    class MOB_APP external
    class V1_REST,V2_REST,TRANSLATOR,TREE_BUILDER,CANONICAL_STORE datarun
    class ADAPTER,DOMAIN_BC,SHARED_KERNEL,BFF,SPA consumer
    class FORM_ENGINE engine
```
* [Related discussion](docs\README.md)
* [DatarunAPI Frontend](docs\datarunapi\datarunapi-frontend\overview.md)