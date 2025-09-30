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
* **Mapping and Codegen Tools**: Lombok and MapStruct are used.
* **Testing**: Testcontainers (Postgres), JUnit 5, and AssertJ are used for testing.
* **User authentication**:  sending basic user's credentials and receiving Access/Refresh tokens.

## Key Architectural Principles

### 1. IDs, UIDs and business keys

* **id**: internal primary key (VARCHAR(26)) ULID format. Immutable, never recycled. Used for all foreign-key
  relationships.
* **uid**: short system generated business key (VARCHAR(11)), globally unique, stable across environments, used
  extensively in api client's requests and analytics for human-friendly references.

### 2. Immutability as the Bedrock of Integrity

**Principle:** Critical entities are immutable once published to prevent canonical drift

- **DataTemplateVersion:** Schema is locked upon publication
- **DataElement.valueType:** Semantic definition cannot change once in use
- **DataSubmission context:** template_uid and template_version_uid are immutable after creation

## System Architecture System Overview (Concept)

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

### Template Publishing Flow

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

### ETL Process Flow

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

## Dev Project Structure

Node is required for generation and recommended for development. `package.json` is always generated for a better
development experience with prettier, commit hooks, scripts and so on.

In the project root, generates configuration files for tools like git, prettier, eslint, husky, and others that are well
known and you can find references in the web.

`/src/*` structure follows default Java structure.

- `.yo-resolve` (optional) - Yeoman conflict resolver
  Allows to use a specific action when conflicts are found skipping prompts for files that matches a pattern. Each line
  should match `[pattern] [action]` with pattern been a [Minimatch](https://github.com/isaacs/minimatch#minimatch)
  pattern and action been one of skip (default if omitted) or force. Lines starting with `#` are considered comments and
  are ignored.
- `/src/main/docker` - Docker configurations for the application and services that the application depends on

## Development

To start your application in the dev profile, run:

```
./mvnw
```

## Building for production

### Packaging as jar

To build the final jar and optimize the dataRunApi application for production, run:

```
./mvnw -Pprod clean verify
```

To ensure everything worked, run:

```
java -jar target/*.jar
```

Refer to [Using Datarun in production][] for more details.

### Packaging as war

To package your application as a war in order to deploy it to an application server, run:

```
./mvnw -Pprod,war clean verify
```

## Testing

### Spring Boot tests

To launch your application's tests, run:

```
./mvnw verify
```

## Others

### Code quality using Sonar

Sonar is used to analyse code quality. You can start a local Sonar server (accessible on http://localhost:9001) with:

```
docker compose -f src/main/docker/sonar.yml up -d
```

Note: we have turned off forced authentication redirect for UI in [src/main/docker/sonar.yml](src/main/docker/sonar.yml)
for out of the box experience while trying out SonarQube, for real use cases turn it back on.

You can run a Sonar analysis with using
the [sonar-scanner](https://docs.sonarqube.org/display/SCAN/Analyzing+with+SonarQube+Scanner) or by using the maven
plugin.

Then, run a Sonar analysis:

```
./mvnw -Pprod clean verify sonar:sonar -Dsonar.login=admin -Dsonar.password=admin
```

If you need to re-run the Sonar phase, please be sure to specify at least the `initialize` phase since Sonar properties
are loaded from the sonar-project.properties file.

```
./mvnw initialize sonar:sonar -Dsonar.login=admin -Dsonar.password=admin
```

Additionally, Instead of passing `sonar.password` and `sonar.login` as CLI arguments, these parameters can be configured
from [sonar-project.properties](sonar-project.properties) as shown below:

```
sonar.login=admin
sonar.password=admin
```

For more information, refer to the [Code quality page][].

### Using Docker to simplify development (optional)

You can use Docker to improve Datarun development experience. A number of docker-compose configuration are available in
the [src/main/docker](src/main/docker) folder to launch required third party services.

For example, to start a postgresql database in a docker container, run:

```
docker compose -f src/main/docker/postgresql.yml up -d
```

To stop it and remove the container, run:

```
docker compose -f src/main/docker/postgresql.yml down
```

You can also fully dockerize your application and all the services that it depends on.
To achieve this, first build a docker image of your app by running:

```
npm run java:docker
```

Or build a arm64 docker image when using an arm64 processor os like MacOS with M1 processor family running:

```
npm run java:docker:arm64
```

Then run:

```
docker compose -f src/main/docker/app.yml up -d
```

When running Docker Desktop on MacOS Big Sur or later, consider enabling experimental
`Use the new Virtualization framework` for better processing
performance ([disk access performance is worse](https://github.com/docker/roadmap/issues/7)).

## Continuous Integration (optional)

To configure CI for your project, run the ci-cd sub-generator (`ci-cd`), this will let you generate configuration files
for a number of Continuous Integration systems. Consult the [Setting up Continuous Integration][] page for more
information.
