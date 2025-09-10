# Datarun Platform: Key Architectural Principles

## Foundational Design Principles

### 1. Immutability as the Bedrock of Integrity

**Principle:** Critical entities are immutable once published to prevent canonical drift

- **DataTemplateVersion:** Schema is locked upon publication
- **DataElement.valueType:** Semantic definition cannot change once in use
- **DataSubmission context:** template_id and template_version_id are immutable after creation

**How it's implemented:**

- Database constraints prevent updates to published versions
- Event sourcing pattern captures all changes as new versions
- Historical data is always interpreted using original context

### 2. Strict Separation of Concerns

**Principle:** Clear distinction between timeless concepts and contextual applications

- **Canonical (The "What"):** DataElement represents pure abstract definition
- **Contextual (The "How"):** ElementTemplateConfig represents specific usage in a template

**Benefits:**

- Enables stable longitudinal analysis across template versions
- Prevents breaking changes from affecting historical data
- Allows flexible UI design without compromising data integrity

### 3. Idempotent, Transactional Processes

**Principle:** All data processing operations are designed to be safely repeatable

- **ETL Process:** Uses "sweep-and-update" pattern within transactions
- **Metadata Generation:** Event-driven with proper transaction boundaries
- **Materialized View Refresh:** Scheduled and versioned for consistency

## Schema Evolution & Drift Management

### Handling Data Element Changes

**Scenario:** Changing a DataElement name or definition

- **Existing submissions:** Continue to use original element definition
- **New submissions:** Can use updated element definition
- **Analytics:** Both versions remain queryable with appropriate metadata

### Template Versioning Strategy

**Approach:** Superset schema with deprecation tracking

- Analytics entities include all fields that ever existed in any version
- `is_deprecated` flag indicates fields not in current version
- Backward compatibility maintained for existing queries

### Deterministic Identity Management

**Repeat Instance Identification:**

- Client-generated `_id` captured for traceability
- Server-generated composite key used for internal consistency
- Pattern: `submission_uid + semantic_path + index`

## Data Integrity Mechanisms

### Transactional Boundaries

**All-or-nothing processing** for:

- Template publishing and metadata generation
- Submission ETL processing
- Materialized view refresh operations

### Consistency Enforcement

**Database-level guarantees:**

- Unique constraints prevent duplicate data
- Foreign key constraints maintain referential integrity
- Check constraints validate data quality rules

### Auditability & Traceability

**Comprehensive change tracking:**

- DataSubmissionHistory for submission evolution
- ETL version metadata for processing traceability
- Materialized view refresh history

## Performance & Scalability Considerations

### Layered Architecture Benefits

**Separation of concerns enables optimization:**

- Operational database optimized for transactional workloads
- Analytics layer optimized for read performance and aggregation
- Materialized views pre-compute expensive joins and calculations

### Indexing Strategy

**Comprehensive coverage for common query patterns:**

- Organizational hierarchy queries
- Template-specific analytics
- Temporal analysis patterns

### Cache Management

**Strategic caching at multiple levels:**

- Metadata caching for frequently accessed definitions
- Query result caching for common analytical queries
- Localization caching for multi-language support

## Key System Properties

| Property            | Implementation                          | Benefit                   |
|---------------------|-----------------------------------------|---------------------------|
| **Idempotency**     | Transactional sweep-and-update ETL      | Safe retries, consistency |
| **Immutability**    | Versioned templates and elements        | Historical accuracy       |
| **Extensibility**   | Metadata-driven architecture            | Configuration over code   |
| **Performance**     | Layered storage with materialized views | Scalable analytics        |
| **Maintainability** | Clear separation of concerns            | Easier evolution          |
