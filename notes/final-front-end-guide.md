# Developer's Guide: Building the Analytics UI**

## 1. Introduction

This guide provides front-end engineers with all the necessary information to build a rich, interactive pivot table
experience using the Datarun analytics API.
You will select a form, discover its available fields, and then construct queries to slice and dice the data. It is
designed to be used with modern web frameworks like Angular and UI component libraries such as `ag-Grid`.

## 2. Getting Started: API Fundamentals

*   **Authentication**: All requests to the `/api/v1/analytics/` endpoints must be authenticated. Include an `Authorization` header with a valid JWT token.
    ```
    Authorization: Bearer <your_jwt_token>
    ```
*   **Base URL**: All API paths in this guide are relative to your deployed instance's base URL (e.g., `https://your-datarun-instance.com`).
*   **Identifiers**: The API exclusively uses the 11-character `uid` for all entities in requests and responses. The internal 26-character ULID is never exposed to the client.


## 3. The Core Workflow: A Three-Step Process

Building a pivot table follows a simple, stateful flow:

1.  **Discover**: Fetch metadata for a selected form template. This tells the UI what fields, dimensions, and operations are available.
2.  **Query**: Construct and execute a query based on user selections. The backend processes the request and returns a structured data set.
3.  **Render & Interact**: Display the data. Handle interactions like sorting, pagination, or drilling down by re-querying with modified parameters.

---

### 3. Step 1: Metadata Discovery

To build the UI controls for the pivot table, you first need to know what data is available for a specific form.

#### **Endpoint: Get Template Metadata**

This is the starting point of the entire process. Call this endpoint whenever the user selects a form template and version.

```
GET /api/v1/analytics/pivot/metadata?templateId={uid}&templateVersionId={uid}
```

**Response Payload (`PivotMetadataResponse`)**

The response contains two primary lists: `fields` (data collected in the form) and `coreDimensions` (system-level data like team, org unit, etc.).

```json
{
  "templateUid": "dt123abc456",
  "templateVersionUid": "dtv987zyx321",
  "fields": [
    {
      "uid": "etcAbc12345", 
      "dataElementUid": "deXYZ67890",
      "factColumn": "etc_uid", 
      "name": "Age of Household Head",
      "dataType": "NUMERIC",
      "aggregationModes": ["SUM","AVG","MIN","MAX","COUNT"],
      "isDimension": true,
      "isMeasure": true,
      "extras": { "optionSetUid": null, "isMulti": false, "referenceTable": null
      }
    }
  ],
  "coreDimensions": [
    { "factColumn": "team_uid", "name": "Team", "dataType": "UID" },
    { "factColumn": "org_unit_uid", "name": "Org Unit", "dataType": "UID" },
    { "factColumn": "submission_completed_at", "name": "Submission Date", "dataType": "TIMESTAMP" }
  ]
}
```

**How the Frontend Uses This Metadata:**

*   **`fields` Array**: Use this to populate the "Measures" and template-specific "Dimensions" pickers.
    *   `name`: The display label for the field in the UI.
    *   `dataType`: Drives which filter operators (`=`, `>`, `IN`) and input controls (date picker, number input, dropdown) to show.
    *   `aggregationModes`: The list of allowed aggregations for a measure. Disable or hide any unsupported options.
    *   `uid`: The identifier for this template field. When creating a `MeasureRequest`, prefix this with `etc:` (e.g., `"fieldId": "etc:etcAbc12345"`).
    *   `extras.optionSetUid`: If present, use the `/api/v1/optionSets/{uid}/values` endpoint to fetch the available options for dropdowns.
*   **`coreDimensions` Array**: Use this to populate the "Dimensions" picker for system-level groupings.
    *   `factColumn`: The identifier to be used in the `dimensions`, `rowDimensions`, `columnDimensions`, and `filters` arrays of your query.
    *   `name`: The display label for the dimension.

---

### 4. Step 2: Pivot Query Execution

Once the user has configured their report, you will construct and send a `PivotQueryRequest`.

#### **Endpoint: Execute Pivot Query**

```
POST /api/v1/analytics/pivot/query?format={TABLE_ROWS | PIVOT_MATRIX}
```

**Request Body (`PivotQueryRequest`)**
This is the main object you will build from the UI state. 

```json
{
  "templateId": "dt123abc456",
  "templateVersionId": "dtv987zyx321",
  "dimensions": ["team_uid"],
  "measures": [
    { "fieldId": "etc:etcAbc12345", "aggregation": "SUM", "alias": "total_age" }
  ],
  "filters": [
    { "field": "submission_completed_at", "op": ">=", "value": "2025-01-01T00:00:00Z" }
  ],
  "sorts": [ { "fieldOrAlias": "total_age", "desc": true } ],
  "limit": 50,
  "offset": 0
}
```

**Response (`format=TABLE_ROWS`)**

This format is ideal for standard data grids.

```json
{
  "columns": [
    { "id": "team_uid", "label": "Team", "dataType": "UID" },
    { "id": "total_age", "label": "Total Age", "dataType": "NUMERIC" }
  ],
  "rows": [
    { "team_uid": "tm12345abc", "total_age": 2450 },
    { "team_uid": "tm67890xyz", "total_age": 3123 }
  ],
  "total": 24 
}
```

*   **Frontend Action**:
    1.  Map the `columns` array to your grid's column definitions. The `id` field corresponds to the key in each object in the `rows` array.
    2.  Use the `rows` array as the data source for the grid.
    3.  Use `total`, `limit`, and `offset` to configure your pagination controls. Re-query with a new `offset` when the user changes pages.

---

##### **Handling the `PIVOT_MATRIX` Response Format**
* **preferred request format for `PIVOT_MATRIX` request:** request's attribute `dimensions` in `PIVOT_MATRIX` is `rowDimensions`, preferred `PIVOT_MATRIX` request format would use `rowDimensions`, and `columnDimensions`, Example:
```
POST /api/v1/analytics/pivot/query?format=PIVOT_MATRIX
```
**Preferred `PIVOT_MATRIX` Request Body**
```json
{
  "templateId": "dt123abc456",
  "templateVersionId": "dtv987zyx321",
  "autoRenameAliases": false, 
  "rowDimensions": ["team_uid", "team_code"],
  "columnDimensions": ["activity_name"],
  "measures": [
    { "fieldId": "etc:etcZyx12346", "aggregation": "SUM", "alias": "total_sum" },
    { "fieldId": "etc:etcZemZ7mlg", "aggregation": "COUNT", "alias": "household_count" }
  ]
}
```
**Response (`format=PIVOT_MATRIX`)**
```json
{
    "total": 2,
    "matrix": {
        "rowDimensionNames": ["team_uid", "team_code"],
        "columnDimensionNames": ["activity_name"],
        "measureAliases": ["total_sum", "household_count"],
        "rowHeaders": [ [ "tm12345abc", "TMA" ], [ "tm67890xyz", "TMB" ] ],
        "columnHeaders": [ [ "Activity A" ], [ "Activity B" ] ],
        "cells": [
            [ {"total_sum": 200, "household_count": 10}, {"total_sum": 321, "household_count": 14} ],
            [ {"total_sum": 150, "household_count": 8},  {"total_sum": 400, "household_count": 20} ]
        ]
    }
}
```
**Frontend Developer Guide:**

"The `matrix` object gives you a pre-pivoted dataset. To render it:

1.  The `rowHeaders` array gives you the values for the row axis. The number of nested arrays corresponds to the number of `rowDimensions`.
2.  The `columnHeaders` array gives you the values for the column axis.
3.  The `cells` data is a 2D array where `cells[i][j]` contains the measure values for the intersection of `rowHeaders[i]` and `columnHeaders[j]`.
4.  Each cell object contains key-value pairs where the keys match the `measureAliases`."

---

### 5. Step 3: Interactions and Follow-up Actions

A static table is good, but an interactive one is better. Here’s how to handle common user actions.

#### **Sorting and Pagination**

When a user clicks a column header to sort or navigates to a new page:

1.  Modify the `sorts` or `offset` properties in your stored `PivotQueryRequest` object.
2.  Resubmit the request to the `POST /query` endpoint.
3.  Update the grid with the new response.

#### **Drill-Down: From Aggregation to Detail**

When a user clicks a cell or row, they often want to see the underlying data that produced that result.

**Example Scenario:** The table shows `team_uid: "tm12345abc"` has a `total_age: 2450`. The user clicks this row to investigate.

1.  **Get Context from the Clicked Row**: The data for the clicked row provides the context for the drill-down. In this case, the context is `team_uid = "tm12345abc"`.
2.  **Construct a New Query**: Create a new `PivotQueryRequest` by cloning the original one and adding the context as a new filter. You might also want to change the dimensions to see more detail.

    *   **Original Request:**
        ```json
        {
          "templateId": "dt123abc456",
          "dimensions": ["team_uid"],
          "measures": [{"fieldId": "etc:etcAbc12345", "aggregation": "SUM", "alias": "total_age"}]
        }
        ```
    *   **New Drill-Down Request:** (Notice the added filter and new dimension)
        ```json
        {
          "templateId": "dt123abc456",
          "dimensions": ["team_uid", "submission_uid"], // Show individual submissions
          "measures": [{"fieldId": "etc:etcAbc12345", "aggregation": "SUM", "alias": "total_age"}],
          "filters": [
            { "field": "team_uid", "op": "=", "value": "tm12345abc" } // The new drill-down filter
          ]
        }
        ```
3.  **Execute and Render**: Execute this new query and display the results in a new tab, a modal, or by replacing the current view.

---

### 6. Auxiliary Endpoints and UI Helpers

#### **Resolving UIDs to Readable Names**

Your grid will often receive UIDs (e.g., `team_uid`, `org_unit_uid`). To display friendly names, use this efficient batch endpoint.

**Endpoint: Batch Resolve UIDs**

```
POST /api/v1/resolveUids
```

**Request Body**
```json
{
  "uids": ["tm12345abc", "ou77777", "tm67890xyz"]
}
```

**Response**
```json
{
  "tm12345abc": { "uid": "tm12345abc", "name": "Team Alpha", "type": "Team" },
  "ou77777": { "uid": "ou77777", "name": "District West", "type": "OrgUnit" },
  "tm67890xyz": { "uid": "tm67890xyz", "name": "Team Bravo", "type": "Team" }
}
```
*   **Frontend Action**: After receiving a `TABLE_ROWS` response, collect all unique UIDs from the result set. Make a single call to this endpoint and use the returned map to display the names in your grid, for example by using an `ag-Grid` value formatter.

#### **Option values lookup**

```
GET /api/v1/optionSets/{uid}/values
```

→ returns list of `{uid, code, name, sortOrder}`.

#### Standard end points
* **OrgUnit hierarchy**
```
GET /api/v1/orgUnits/tree?root={uid}
```

→ returns hierarchical tree for filters.

* **Teams, Activities, Projects, form Template**

```
GET /api/v1/teams?filter=...
GET /api/v1/activities?filter=...
GET /api/v1/projects?filter=...
GET /api/v1/dataTemplates?filter=...
```

→ used to populate dropdowns in filter builders. 

* **Data element lookup (global)**

  ```
  GET /api/v1/dataElements/{uid}
  ```

  → metadata when building global (cross-template) queries using `de:<uid>`.

#### **Building a Hierarchy Filter (e.g., for Org Units)**

To let users select from a tree of organizational units:
1.  Fetch the hierarchy using `GET /api/v1/orgUnits/tree`.
2.  Render this data using a tree component in your UI.
3.  When the user selects one or more org units, collect their UIDs.
4.  Construct a filter in your `PivotQueryRequest`: ~~<TODO> show standard filter format~~
    ```json
    { "field": "org_unit_uid", "op": "IN", "value": ["ouChild123", "ouChild456"] }
    ```

#### Common attributes

- `{uid, code, name, label}` are common attributes between main auxiliary entities e.g. `DataElement`, `Activity`, `OrgUnit`, `Team`, `Option`, `OptionSet`, `DataTemplate`.
- label is a map of localized display labels formated `locale-key:value` e.g. `{"en": "..", "ar":".."}`.

#### **2. Advanced Filtering: By Measure Aliases and Populating Pickers**

The filter builder needs more detail on two fronts.

**A) How to Filter by a Measure's Result (e.g., a HAVING clause)**

A user might want to find all teams where the total number of households is greater than 10. This requires filtering by the *alias* of a measure.

**Example `PivotQueryRequest`:**

```json
{
  "dimensions": ["team_uid"],
  "measures": [
    { "fieldId": "etc:etcDef98765", "aggregation": "COUNT", "alias": "household_count" }
  ],
  "filters": [
    { "field": "household_count", "op": ">", "value": 10 }
  ]
}
```

**Frontend Developer Guide:**

"To filter on an aggregated value, simply use the measure's `alias` in the `field` property of a `FilterDto`. Your UI should populate the filter's field selector with both the available dimensions *and* the aliases of any measures the user has already defined."

**B) How to Populate Filter Dropdowns for Core Dimensions**

A user filtering by `team_uid` needs a dropdown of all available teams.

**Recommendation:** Explicitly state that the standard REST endpoints should be used for this.

**Frontend Developer Guide:**

"When a user wants to filter by a core dimension like 'Team' or 'Activity', use the existing top-level API endpoints to fetch a list of possible values.

*   For `team_uid`: call `GET /api/v1/teams` to get a list of teams (`{uid, name}`).
*   For `activity_uid`: call `GET /api/v1/activities` to get a list of activities.

Use the results to populate the dropdown or multi-select control in your filter builder."

---

* **(Step 3) Interaction & Auxiliary Endpoints:**
* **UID Resolution:** `POST /api/v1/resolveUids` (Crucial for a good UX).
* **Populating Filter Pickers:** Explain how to use `GET /api/v1/teams`, `GET /api/v1/orgUnits/tree`, etc. *This covers
  the "Advanced Filtering" point.*
* **Saving & Loading Views:** The `GET` and `POST` endpoints for `/api/v1/analytics/views`. *This covers the "Saved
  Views" point.*

---

**4. Advanced Features & Recipes**

* **Drill-Down Pattern:** A concrete example of transforming a clicked row into a new, filtered `PivotQueryRequest`.
* **Filtering by Measure Aliases:** Show an example of a filter using a measure's alias (the "HAVING" clause).
* **Global (Cross-Template) Queries:** Briefly explain the use case for `de:<uid>` and the different metadata endpoint.
  *This covers the "Global Queries" point.*

**5. Error Handling**

* Provide the standardized JSON error structure and explain how the front-end should parse it to display user-friendly
  messages.

---

By structuring the guide this way, you are giving the front-end engineer **everything they need, and nothing they don't.
** The document becomes a practical tool that they can have open while they code, rather than a dense system
architecture document they have to sift through.

You can now confidently finalize the developer guide with this focused structure. It will be more than enough to get
them started immediately and effectively.
