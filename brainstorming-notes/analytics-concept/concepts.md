# concepts and design choices (AI discussions)

## Key Principle

1. **One Materialized View per Grain:** A "grain" is what a single row in your table represents. In a form with
   repeating
   sections, you have multiple grains.
2. The key is to see the approaches not as mutually exclusive, but as two layers of an analytics stack.

**Key Strengths:**

* **Source of Truth:** The use of `DataSubmission.formData` as an immutable raw data archive is a critical best
  practice. It ensures you can always re-process data if business logic changes or bugs are found in the ETL.
* **Flexibility and Scalability:** The "tall" `element_data_value` table is extremely flexible, allowing you to add new
  data elements without changing the database schema. This is a great advantage for a platform with evolving form
  templates.
* **Data Integrity and Traceability:** The use of immutable `DataTemplateVersion` and linking submissions directly to a
  version is excellent for ensuring reproducible and historically accurate analytics.
* **Clear Identifiers:** The distinction between internal ULIDs and external, stable `uid` business keys is a
  sophisticated approach that benefits API design and system integrations.
* **Handling of Hierarchies:** The `repeat_instance` table and the `org_unit_hierarchy` closure table show a mature
  understanding of how to model and query complex hierarchical data efficiently.

The current `pivot_grid_facts` materialized view (MV) is a good step towards simplification. It denormalizes and
pre-joins data, which is the right instinct. However, as you've hinted, for a truly "dumb" frontend (like a simple
grid-building UI or a standard BI tool), it can be made even better. The current MV is very wide and still requires the
frontend to understand concepts like `child_category_kind` and to pick the right value column (`value_num`,
`value_text`, etc.).

### Enhancements for "Dumb" Frontend Analytics

The goal is to move from a model that is *queryable* to one that is *intuitive*. A frontend developer or data analyst
should be able to get meaningful data with simple `SELECT`, `GROUP BY`, and `WHERE` clauses, without needing to
understand the underlying EAV (Entity-Attribute-Value) structure.

Here are several strategies, from foundational to more advanced, to achieve this.

#### 1. Introduce a Dedicated Analytics Schema with a Star Schema Model

Instead of a single, all-encompassing materialized view, create a dedicated `analytics` schema with multiple,
purpose-built tables and views that follow a classic star schema pattern. This model is the industry standard for
analytics because it is simple, performant, and easy for BI tools to understand.

**A. Create Purpose-Built Fact Tables:**

Create separate fact tables for different grains of data. The "grain" refers to what a single row in the table
represents.

* **`analytics.fact_submissions`**:
* **Grain:** One row per submission.
* **Purpose:** For high-level reporting on submission volume, timeliness, and data from the *root (non-repeating)* part
  of your forms.
* **Example DDL:**
  ```sql
  CREATE TABLE analytics.fact_submissions (
      submission_uid          VARCHAR(11) PRIMARY KEY,
      assignment_uid          VARCHAR(11),
      team_uid                VARCHAR(11),
      org_unit_uid            VARCHAR(11),
      activity_uid            VARCHAR(11),
      form_template_uid       VARCHAR(11),
      form_version_uid        VARCHAR(11),
      submission_completed_at TIMESTAMP,
      -- Measures from non-repeating elements pivoted into columns
      household_size          NUMERIC,     -- from element 'household_size'
      has_electricity         BOOLEAN,     -- from element 'has_electricity'
      -- Add other key non-repeating measures here
      created_date            TIMESTAMP
  );
  ```

* **`analytics.fact_repeat_instances`**:
* **Grain:** One row per *repeat instance* (e.g., one row per child in a household).
* **Purpose:** For detailed analysis of repeated data.
* **Example DDL:**
  ```sql
  CREATE TABLE analytics.fact_repeat_instances (
      repeat_instance_id        VARCHAR(26) PRIMARY KEY,
      parent_repeat_instance_id VARCHAR(26),
      submission_uid            VARCHAR(11),
      category_uid              VARCHAR(11), -- The resolved category for this instance
      category_kind             VARCHAR(200),
      -- Measures from repeating elements pivoted into columns
      child_age                 NUMERIC,      -- from element 'age' inside the repeat group
      child_gender_option_uid   VARCHAR(11),  -- from element 'gender'
      -- Add other key repeating measures here
      created_date              TIMESTAMP
  );
  ```

**Why this helps:** Queries become dramatically simpler. To count submissions per team, the frontend executes
`SELECT team_uid, COUNT(*) FROM analytics.fact_submissions GROUP BY team_uid;`. This is trivial compared to querying the
EAV table or the complex `pivot_grid_facts` MV.

**B. Use Existing Tables as Dimension Tables:**

Your existing tables (`Team`, `OrgUnit`, `Activity`, `DataElement`, `OptionSet`, `Options`) are already perfect
dimension tables. You can create views in the `analytics` schema that point to them, providing a clean interface for the
frontend.

* `analytics.dim_teams` (view on `Team`)
* `analytics.dim_org_units` (view on `OrgUnit`)
* `analytics.dim_options` (view on `OptionValue`)

#### 2. Create Template-Specific "Wide" Materialized Views

For your most important or most frequently analyzed data templates, you can take the star schema concept a step further
by creating dedicated MVs that are fully "pivoted" into a wide, flat table.

Imagine a "Health Facility Visit" template. You could create an MV where each row is a visit and each column is a
specific answer from the form.

* **`analytics.mv_health_facility_visits`**:
  ```sql
  CREATE MATERIALIZED VIEW analytics.mv_health_facility_visits AS
  SELECT
      sub.uid AS submission_uid,
      team.name AS team_name,
      ou.name AS org_unit_name,
      -- Pivoted values from the element_data_value table
      MAX(sub.finished_entry_time) as finished_entry_time,
      MAX(CASE WHEN de.name = 'patient_age' THEN ev.value_num END) AS patient_age,
      MAX(CASE WHEN de.name = 'visit_reason' THEN ev.value_text END) AS visit_reason,
      MAX(CASE WHEN de.name = 'is_emergency' THEN ev.value_bool END) AS is_emergency,
      -- ... other fields
  FROM data_submission sub
  JOIN element_data_value ev ON sub.uid = ev.submission_uid
  JOIN data_element de ON ev.element_uid = de.uid
  JOIN team ON sub.team_uid = team.uid
  JOIN org_unit ou ON sub.org_unit_uid = ou.uid
  WHERE sub.team_uid = 'UID_OF_HEALTH_FACILITY_FORM'
  GROUP BY sub.uid, team.name, ou.name;
  ```

**Why this helps:** This is the ultimate simplification for a frontend. The data is presented exactly as a business user
thinks about it. It requires zero transformation or logic on the client side. While this requires more setup per
template, it provides the best possible performance and ease of use for key reports.

#### 3. Enhance Incremental and Efficient Data Processing

Your ETL populates the "tall" tables. The new analytics tables and MVs would be populated from those. To ensure this is
scalable, consider the following:

* **Incremental MV Refresh:** For your materialized views (`pivot_grid_facts` or the new ones suggested), ensure they
  can be refreshed concurrently and, if possible, incrementally. This is crucial to avoid locking and long refresh times
  as your data grows. You may need a robust ETL process (using tools like dbt or custom Spring Batch jobs) that tracks
  changes since the last run and updates the analytics tables accordingly.
* **Use `GENERATED` Columns Strategically:** Your use of generated columns in `element_data_value` is smart. You could
  extend this. For instance, you could add a generated column that coalesces all the `value_*` columns into a single
  `value_display` text column, which can simplify some downstream logic.

  ```sql
  ALTER TABLE element_data_value
  ADD COLUMN value_as_text TEXT GENERATED ALWAYS AS (
      COALESCE(
          value_text,
          value_num::text,
          value_bool::text,
          value_ref_uid,
          value_ts::text,
          option_uid
      )
  ) STORED;
  ```

#### 4. Leverage PostgreSQL's JSONB Power for Exploration

While a structured star schema is best for routine dashboards, don't forget the power of your raw
`DataSubmission.formData` JSONB column. For data exploration or answering one-off questions without waiting for an ETL
cycle, you can query it directly.

* **Create GIN Indexes:** Ensure you have a GIN index on the `formData` column to make these queries fast.
  ```sql
  CREATE INDEX idx_datasubmission_formdata_gin ON data_submission USING GIN (form_data);
  ```
* **Empower Power Users:** This approach is not for a "dumb frontend" but is a massive opportunity for power users, data
  scientists, or even a backend API that needs to quickly pull a specific field that hasn't been modeled in the
  analytics schema yet.

### Summary of Recommendations

| Strategy                              | How It Helps the Frontend                                                                                                         | Scalability / Implementation Notes                                                                                                   |
|:--------------------------------------|:----------------------------------------------------------------------------------------------------------------------------------|:-------------------------------------------------------------------------------------------------------------------------------------|
| **1. Star Schema**                    | Provides simple, predictable `fact` and `dimension` tables. Eliminates complex joins and the need to understand the EAV model.    | Highly scalable. Requires a robust ETL process to transform EAV data into the star schema. Best for long-term, structured analytics. |
| **2. Template-Specific Wide MVs**     | The absolute simplest model for the frontend. Data is perfectly shaped for a specific report or dashboard with zero logic needed. | Less flexible than a star schema. Best for high-priority templates. The ETL to create these MVs can be complex.                      |
| **3. Incremental Processing**         | Ensures the analytics data is kept up-to-date without performance degradation as data volume grows.                               | A technical necessity for any large-scale analytics platform. Requires careful ETL design and job scheduling.                        |
| **4. Leverage JSONB for Exploration** | Offers a flexible, schema-on-read approach for ad-hoc questions and exploring new data before it's formally modeled.              | Requires knowledge of PostgreSQL's JSON functions. GIN indexes are essential for performance. Complements the structured models.     |

By layering these approaches, you can build an analytics layer on your already excellent foundation that is not only
powerful and scalable but also exceptionally easy to consume, enabling your frontend teams to build rich, performant
analytics experiences with minimal effort.

---

## Point 1 Expanded: The Generalized Star Schema (For ALL Templates)

Your concern about an explosion of pivoted columns is absolutely valid. A naive approach where every data element
becomes a column in one giant table would be unmanageable.

That is **not** what a generalized star schema does. Instead, it maintains a "tall" fact table, but surrounds it with
rich dimension tables for context. It is designed precisely to avoid the "infinite columns" problem.

**How it Works (No Column Explosion):**

The central table is **not wide**. It remains a "tall" table of facts, very similar to your existing
`element_data_value`, but we'll formally call it a fact table.

* **Fact Table: `analytics.fact_data_values`**
* **Grain:** One row per data point recorded.
* **What it contains:**
  * **Foreign Keys (UIDs):** These are pointers to the dimension tables (e.g., `submission_uid`, `element_uid`,
    `team_uid`, `org_unit_uid`, `repeat_instance_id`).
  * **Measures:** The actual values (`value_num`, `value_text`, `value_bool`, etc.).
* This table can grow to billions of rows, but its *width* is constant and small.

* **Dimension Tables: `analytics.dim_*`**
* These are your context tables, often views on your existing operational tables.
* `dim_submissions`: Contains metadata about the submission (`submission_uid`, `completion_date`, `form_template_name`).
* `dim_data_elements`: Contains metadata about the question (`element_uid`, `element_name`, `label`, `value_type`).
* `dim_teams`: (`team_uid`, `team_name`, `team_code`).
* `dim_org_units`: (`org_unit_uid`, `org_unit_name`, `level_name`). You can pre-join your `org_unit_hierarchy` here to
  add columns like `region_name`, `country_name` etc., for easy filtering.
* `dim_date`: A standard data warehousing utility table with columns like `date`, `day_of_week`, `month`, `year`,
  `quarter`. This makes time-based queries trivial.

**How This Simplifies Frontend Queries (Example):**

**Business Question:** "Show me the average numeric value for the element named 'household_size', grouped by
organization unit, for all submissions completed in Q2 2025."

**Frontend Query using this model:**

```sql
SELECT
  dou.org_unit_name,
  AVG(fdv.value_num) AS average_household_size
FROM analytics.fact_data_values AS fdv
JOIN analytics.dim_data_elements AS dde ON fdv.element_uid = dde.element_uid
JOIN analytics.dim_submissions   AS ds  ON fdv.submission_uid = ds.submission_uid
JOIN analytics.dim_org_units     AS dou ON ds.org_unit_uid = dou.org_unit_uid
JOIN analytics.dim_date          AS dd  ON ds.completion_date = dd.date
WHERE
  dde.element_name = 'household_size'
  AND dd.year = 2025
  AND dd.quarter = 2
GROUP BY
  dou.org_unit_name;
```

**Analysis of the Query:**

* **Simple for the Frontend:** The query is just a series of `JOIN`s on UIDs, a `WHERE` clause on human-readable text (
  `element_name`, `year`, `quarter`), and a `GROUP BY`. A UI could easily construct this.
* **Scalable:** This scales beautifully. Adding a new template with new elements just adds rows to `fact_data_values`
  and `dim_data_elements`. No schema changes are needed. The database is highly optimized for these types of join
  operations.

**Conclusion for Point 1:** The generalized star schema is for **all templates**. It provides a single, unified, and
scalable way to query any piece of data. It does **not** explode into infinite columns because the facts remain in a
tall, narrow table.

---

### Point 2 Expanded: Template-Specific Wide Models (The Performance Layer)

This is where we address your second question directly. For your most important templates, you create fully pivoted,
wide models that are even easier to query. This is an *optimization* built on top of your raw data.

**Key Principle: One Materialized View per Grain.**

A "grain" is what a single row in your table represents. In a form with repeating sections, you have multiple grains.

Let's walk through a detailed example.

**Template Name:** "Household Health Survey"

* **Root Section (Grain: 1 row per Household/Submission):**
* `is_urban` (Boolean)
* `head_of_household_name` (Text)
* **Repeating Section: `children` (Grain: 1 row per Child):**
* `child_name` (Text)
* `child_age` (Integer)
* `vaccinations_up_to_date` (Boolean)
* `illnesses_last_month` (Multi-select from an OptionSet: "Fever", "Cough", "Diarrhea")

Here’s how you model this for analytics:

#### 1. The Root Section Model: `analytics.mv_household_surveys`

This is a straightforward pivoted view.

* **Grain:** One row per household survey.
* **DDL Idea:**
  ```sql
  CREATE MATERIALIZED VIEW analytics.mv_household_surveys AS
  SELECT
      sub.uid AS submission_uid,
      team.name AS team_name,
      ou.name AS org_unit_name,
      -- PIVOTED COLUMNS FROM THE ROOT SECTION
      MAX(sub.finished_entry_time) AS finished_entry_time,
      MAX(CASE WHEN de.name = 'is_urban' THEN ev.value_bool END) AS is_urban,
      MAX(CASE WHEN de.name = 'head_of_household_name' THEN ev.value_text END) AS head_of_household_name
  FROM data_submission sub
  JOIN element_data_value ev ON sub.uid = ev.submission_uid
  JOIN data_element de ON ev.element_uid = de.uid
  JOIN team ON sub.team_uid = team.uid
  JOIN org_unit ou ON sub.org_unit_uid = ou.uid
  WHERE
      sub.template_uid = 'UID_OF_HOUSEHOLD_SURVEY'
      AND ev.repeat_instance_id IS NULL -- IMPORTANT: Only get data from the root
  GROUP BY sub.uid, team.name, ou.name;
  ```

#### 2. The Repeating Section Model: `analytics.mv_household_children`

This is a separate view for the repeating data.

* **Grain:** One row per child.
* **DDL Idea:**
  ```sql
  CREATE MATERIALIZED VIEW analytics.mv_household_children AS
  SELECT
      ri.id AS repeat_instance_id,
      ri.submission_uid, -- Foreign key back to the household
      ri.repeat_index,
      -- PIVOTED COLUMNS FROM THE 'children' REPEATING SECTION
      MAX(CASE WHEN de.name = 'child_name' THEN ev.value_text END) AS child_name,
      MAX(CASE WHEN de.name = 'child_age' THEN ev.value_num END) AS child_age,
      MAX(CASE WHEN de.name = 'vaccinations_up_to_date' THEN ev.value_bool END) AS vaccinations_up_to_date
  FROM repeat_instance ri
  JOIN element_data_value ev ON ri.id = ev.repeat_instance_id
  JOIN data_element de ON ev.element_uid = de.uid
  WHERE
      ri.repeat_path = 'children' -- Filter for the correct repeating section
  GROUP BY ri.id, ri.submission_uid, ri.repeat_index;
  ```

#### 3. Handling the Multi-Select: The Bridge Table Model

This is the most robust way to handle many-to-many relationships like multi-selects. Do not use `array_agg` or string
concatenation if you want to filter and group by the selected options easily.

* **Grain:** One row per child per illness.
* **DDL Idea:** `analytics.bridge_child_illnesses`
  ```sql
  CREATE MATERIALIZED VIEW analytics.bridge_child_illnesses AS
  SELECT
      ev.repeat_instance_id, -- Foreign key back to the child
      ev.submission_uid,     -- Foreign key back to the household
      ev.option_uid AS illness_option_uid, -- The selected option
      ov.name AS illness_name -- Pre-join the name for convenience
  FROM element_data_value ev
  JOIN data_element de ON ev.element_uid = de.uid
  JOIN option_value ov ON ev.option_uid = ov.uid
  WHERE
      de.name = 'illnesses_last_month'
      AND ev.repeat_instance_id IS NOT NULL;
  ```

**How This All Comes Together for the Frontend:**

**Business Question:** "In the 'Northern Region', what is the average age of children who had a 'Fever' last month?"

**Frontend Query (Now incredibly simple):**

```sql
SELECT
  AVG(children.child_age)
FROM analytics.mv_household_surveys AS surveys
JOIN analytics.mv_household_children AS children ON surveys.submission_uid = children.submission_uid
JOIN analytics.bridge_child_illnesses AS illnesses ON children.repeat_instance_id = illnesses.repeat_instance_id
WHERE
  surveys.org_unit_name = 'Northern Region'
  AND illnesses.illness_name = 'Fever';
```

This is the ultimate goal. The frontend developer doesn't need to know about EAV, pivoting, or value types. They are
querying intuitive, well-structured tables (`surveys`, `children`, `illnesses`) with clear column names. This model
handles nesting (by joining from parent to child views) and multi-selects (by joining to the bridge table) with standard
SQL. It is both scalable (MVs are pre-calculated) and maximally simple for the consumer.

---

## Code abstraction of db

Excellent questions. You've hit the exact point where a great database design can fall apart if the software abstraction
isn't right. The feeling of being "lost and suffocated" is normal when bridging this gap.

Let's demystify this. The great model you should start with is a **Metadata-Driven Analytics Layer**.

Think of it this way: Your backend will serve as a "translator" that presents the complex analytics database as a
simple, logical set of "business objects" that the frontend can understand and request. The frontend will no longer ask
for `value_num` from a giant view; it will ask for the "Average Patient Age," and the backend will know how to get it.

Here is a breakdown of the key components and the model, designed for extensibility from the start.

---

### The Great Model: A Metadata-Driven Abstraction

The core idea is to create a layer in your Spring Boot application that defines the "shape" of your analytics data. This
metadata will drive everything: API responses, query generation, and even the UI components on the frontend.

#### Key Components in Your Backend (Spring Boot)

1. **The Analytics Metadata Service:** The brain of the operation.
2. **The Dynamic Query Engine:** The muscle that builds and runs the SQL.
3. **The Materialized View (MV) Manager:** The automated housekeeper.

Let's detail each one.

---

#### 1. The Analytics Metadata Service

This service's job is to describe your analytics models (the star schemas and wide MVs) in a consistent,
machine-readable way. You will define these models in your Java code.

**How it works:**

You create a set of DTOs (Data Transfer Objects) that represent your "analytics entities."

**Example: `AnalyticsEntity.java`**

```java
// This is a generic descriptor for an analytics model
public class AnalyticsEntity {
    private String uid; // e.g., "household_surveys" or "household_children"
    private String displayName; // "Household Surveys"
    private String description; // "Each row represents one household survey submission."
    private String underlyingViewName; // "analytics.mv_household_surveys"
    private List<AnalyticsAttribute> attributes;
    private List<AnalyticsRelationship> relationships;
}

public class AnalyticsAttribute {
    private String uid; // e.g., "is_urban"
    private String displayName; // "Is Urban"
    private DataType dataType; // ENUM: NUMERIC, BOOLEAN, TEXT, DATE
    private String underlyingColumnName; // "is_urban"
    private boolean isDimension; // Can you group/filter by this? (e.g., team_name)
    private boolean isMeasure;   // Can you aggregate this? (e.g., child_age)
}

public class AnalyticsRelationship {
    private String toEntityUid; // "household_children"
    private String joinFromAttribute; // "submission_uid"
    private String joinToAttribute; // "submission_uid"
}
```

Your `AnalyticsMetadataService` would have methods like `getEntityByUid(String uid)` and `listAllEntities()`. On
application startup, it can be populated by reading configuration files (YAML, JSON) or even by scanning annotations in
your code.

**This directly answers:**

* **"How the model is gonna look":** It looks like a collection of `AnalyticsEntity` objects. This is your clean,
  abstract view of the data.
* **"Extensibility":** To add a new report or analytics model, you simply define a new `AnalyticsEntity` in your
  configuration. No code changes are needed in the query engine itself.

---

#### 2. The Dynamic Query Engine

This is the service that receives a request from the frontend and uses the metadata to build a valid SQL query.

**How it works:**

The frontend sends a simple, abstract request.

**Example Frontend Request (JSON):**

```json-
{
  "entity": "household_children",
  "dimensions": [ "surveys.team_name" ], // Attributes to GROUP BY
  "measures": [ "AVG(child_age)" ], // Aggregations
  "filters": [
    {
      "attribute": "illnesses.illness_name",
      "operator": "EQUALS",
      "value": "Fever"
    }
  ]
}
```

Your `DynamicQueryEngine` service would:

1. Receive this JSON.
2. Use the `AnalyticsMetadataService` to look up the entities: `household_children`, `household_surveys` (via
   relationship), and `bridge_child_illnesses` (via relationship).
3. **Translate:** It sees `surveys.team_name` and knows (from the metadata) that it needs to
   `JOIN analytics.mv_household_surveys AS surveys ON ...` and select the `team_name` column.
4. **Build:** It programmatically builds the SQL query using `jOOQ` or `NamedParameterJdbcTemplate`. The metadata tells
   it which tables to join and which columns to select, filter, and group by.
5. **Execute:** It runs the query against the database.
6. **Return:** It returns a clean JSON result, abstracting away the underlying database structure.

**This directly answers:**

* **"The transformation between FE and BE":** The transformation is this JSON request format. It's the simple, stable
  contract between your two applications.

---

#### 3. The Materialized View (MV) Manager

This component handles the "housekeeping" of your database views.

**How it works:**

* **Naming Convention (solves "do I utilize the UIDs"):** Yes, absolutely. A consistent naming scheme is critical.
* **Syntax:** `analytics.mv_{template_uid}_{grain}`
* **Example 1:** `analytics.mv_01HFB2J4WXYZ..._household` (for the root section of the Household Survey)
* **Example 2:** `analytics.mv_01HFB2J4WXYZ..._children` (for the `children` repeat group)
* **Why UIDs?** It guarantees no name collisions and programmatically links the MV to the `DataTemplate` it serves. You
  can store this MV name in your metadata.

* **Automation:**
* **Creation:** When a user "publishes" a new high-value `DataTemplate`, your system could have a process (e.g., a
  button in an admin UI or an automated job) that generates and executes the `CREATE MATERIALIZED VIEW` DDL for it.
* **Refresh:** You use a Spring `@Scheduled` job to periodically run `REFRESH MATERIALIZED VIEW CONCURRENTLY ...` on all
  your analytics MVs. The manager can query the database for all views matching the `analytics.mv_*` pattern.

**This directly answers:**

* **"Do I namespace things?":** Yes. Put everything in an `analytics` schema.
* **"How to manage the MVs?":** You automate their lifecycle based on your template definitions and a scheduler.

---

### How to Start (Ensuring Extensibility from Day 1)

You don't have to build all of this at once. Here is your incremental path:

**Step 1: The Foundation (Generalized Star Schema)**

* Forget template-specific MVs for now.
* Create your `analytics.fact_data_values` table and the core dimension views (`dim_teams`, `dim_org_units`, etc.). This
  is your catch-all model.
* Populate these tables with your ETL process.

**Step 2: Build the Backend Abstraction for the Foundation**

* Create the `AnalyticsMetadataService`. Define **one** `AnalyticsEntity` called "All Data Values" that describes your
  `fact_data_values` table and its related dimensions.
* Build V1 of your `DynamicQueryEngine`. It only needs to know how to query this single, foundational star schema.
* Create your first API endpoint: `/api/analytics/query`.

**Step 3: Build a Simple Frontend Consumer**

* Create a simple table or chart builder on the frontend. It will call `listAllEntities()` to get the "All Data Values"
  entity and its attributes.
* Allow the user to select dimensions and measures from this list.
* The frontend then constructs the simple JSON request and sends it to your API.

**You are now live!** You have a system that can query *any* data in the platform, is fully scalable, and is built on a
clean abstraction.

**Step 4: Introduce the First Template-Specific MV (The Optimization Layer)**

* Identify your most important form.
* Manually write the `CREATE MATERIALIZED VIEW` SQL for it. Follow your naming convention.
* **Crucially:** Go back to your `AnalyticsMetadataService` and simply *define a new `AnalyticsEntity`* that describes
  this new MV.
* **That's it!** Your `DynamicQueryEngine` and frontend don't need to change. The next time the frontend calls
  `listAllEntities()`, this new, user-friendly model ("Household Surveys") will simply appear as an option.

This approach ensures extensibility because adding new, highly-optimized analytics views requires **configuration, not
code changes** in your core query logic. You are simply teaching your existing engine about new "words" (entities and
attributes) it can use.
