### Current Status Specification

The platform, Datarun, is a meticulously architected data collection system built
on a **hybrid schema approach** with a strong **meta-data driven model**.

#### **1. Data Capture and Ingestion Layer**

This layer is responsible for accepting and immutably storing client submissions with minimal runtime semantics.

* **Technology Stack**: The system is built on Java 17+ (Spring Boot 3.4.2) and uses PostgreSQL (v16.x) for its
  database. Liquibase manages schema migrations.
* **Core Artifacts**: The primary artifacts in this layer are the `DATA_SUBMISSION` (DS), which contains the raw
  `form_data` in JSONB format, and `DATA_TEMPLATE_VERSION` (DTV), which stores a snapshot of a template's configuration,
  including fields and sections.
* **Per-Element Descriptors**: The system generates immutable `TemplateElement` (TE) records for each field and
  repeatable section within a `DATA_TEMPLATE_VERSION`. These descriptors are rich with metadata, including `namePath` (
  the physical storage path) and a `semantic_path` (the true semantic path).
* **Workflow**: The ingestion process is transactional and idempotent, involving:
    1. Soft-deleting previous extracted data.
    2. Inserting new `REPEAT_INSTANCE` and `ELEMENT_DATA_VALUE` rows.
    3. Recording processing metadata and committing the transaction.

#### **2. Data Transformation Layer (Structural ETL)**

This layer performs a structural, minimal-domain-semantics extraction of data from the raw JSON.

* **Methodology**: A dedicated ETL process uses the `semantic_path` from the `TemplateElement` to traverse the
  `form_data` JSON and extract individual values.
* **Data Structure**: The extracted values are stored in a normalized, key-value format across two tables:
    * **`REPEAT_INSTANCE`**: Stores metadata for each instance of a repeatable section (e.g., `medicines` or
      `householdnames`).
    * **`ELEMENT_DATA_VALUE`**: Stores the extracted values as text, numbers, booleans, etc., linked back to their
      respective `REPEAT_INSTANCE`.

***

### Goal of the Next Module

The next phase of your platform aims to build a **self-service authoring module** on top of this robust foundation. The
primary goal is to empower business-facing professionals to define, maintain, and manage the transformation of
collected, raw data into a canonical, business-oriented data model.

#### **Key Objectives:**

* **Authoring of a Unified Domain Model**: The module will provide a user-friendly interface to define core business
  entities (e.g., `Patient`, `Household`) and their attributes. This shifts the focus from managing templates to
  managing core domain concepts.
* **Self-Service Mapping**: Data managers will be able to map specific `TemplateElement`s and their `semantic_path` to
  the canonical attributes of the unified domain model. This process will include the ability to define simple
  transformation logic (e.g., `CONCAT` or date formatting) through a configuration-driven UI.
* **Automated Harmonization**: The system will use these user-authored mapping rules to dynamically transform the
  normalized `ELEMENT_DATA_VALUE` data into a denormalized, clean data structure, eliminating the need for manual,
  per-template view creation.
* **Centralized Business Logic**: This layer will serve as a single location for defining and applying business rules,
  ensuring that all downstream analytics and reports use consistent logic and definitions, regardless of which template
  collected the source data.
* **On-Demand View Creation**: The module will enable users to generate materialised views on demand from the unified
  domain model for faster, more predictable query performance for analytics and reporting .

### Appendix

#### 
