# Analytics API: A Front-end Developer's Guide

## 0. Appendix: Data Types and Operators

* The `dataType` from the metadata endpoint determines which operators are available in the filter builder. The
  following table provides the expected mappings.

| dataType      | Supported Operators              | UI Control Suggestion    |
|:--------------|:---------------------------------|:-------------------------|
| **NUMERIC**   | `=`, `!=`, `>`, `<`, `>=`, `<=`  | Number Input             |
| **TEXT**      | `=`, `!=`, `LIKE`, `ILIKE`, `IN` | Text Input               |
| **BOOLEAN**   | `=`, `!=`                        | Toggle Switch / Dropdown |
| **TIMESTAMP** | `BETWEEN`, `>=`, `<=`            | Date/Time Range Picker   |
| **DATE**      | `BETWEEN`, `>=`, `<=`            | Date Range Picker        |
| **UID**       | `=`, `!=`, `IN`                  | Searchable Dropdown      |
| **OPTION**    | `=`, `!=`, `IN`                  | Multi-select Dropdown    |

**Supported Operator Formats**: (`=` | `eq`), (`!=` | `<>` | `neq`), (`>` | `gt`), (`<` | `lt`), (`>=` | `gte`), (`<=` |
`lte`).

### Abbreviations used throughout the system

* `act`: Activity.
* `asi`: Assignment (relation between team, activity, orgUnit + other dynamic attributes).
* `de`: Data Element.
* `di`: Data Instance.
* `dt`: Data Template (the form template).
* `dtv`: Data Template Version.
* `dv`: etc/de Data Value.
* `etc`: Element Template Configuration (the mapping between the system-wide Canonical Data Element and a particular
  Data Template Version).
* `exp`: expression.
* `ops`: option set.
* `ou`: Org Unit.
* `ov`: option value.
* `prj`: Project.
* `tm`: Team.
* `vref`: reference value.

---

## 1. Introduction & Getting Started

This guide provides all the necessary information to build a rich, interactive pivot table and analytics UI using the
Datarun API. The API is designed to be consumed by modern web frameworks like Angular, using component libraries such as
ag-Grid or Ignite UI for Angular.

The core principle is a simple, stateful flow: **Discover** available data fields, **Query** for aggregated results, and
**Render & Interact** with the data.

### API Fundamentals

* **Base URL**: All API paths in this guide are relative to the deployed instance's base URL (e.g.,
  `https://drun.org`).
* **Authentication**: All requests to `/api/v1/analytics/` endpoints must be authenticated. Include an `Authorization`
  header with a valid JWT token.
  ```http
  Authorization: Bearer <jwt_token>
  ```
* **Identifiers**: The API exclusively uses the 11-character `uid` for all entities in requests and responses.
* **Common Attributes**: `{uid, name}` are common attributes for All standard entities e.g. `DataElement`, `Activity`,
  `OrgUnit`, `Team`, `Option`, `OptionSet`, `DataTemplate`.

---

## 2. The Core Workflow

Building any report follows three main steps:

1. **Discover**: The user selects a form template. You fetch its metadata to learn what fields, dimensions, and
   operations are available. This populates your UI controls (dropdowns, pickers, etc.).
2. **Query**: The user configures their report (selects measures, dimensions, filters). You construct a
   `PivotQueryRequest` object from the UI state and send it to the API.
3. **Render & Interact**: The API returns a structured data set. You render it in a grid or chart and handle user
   interactions like sorting, pagination, or drill-downs by re-submitting modified queries.

---

## 3. API Endpoints & Usage

### Step 1: Metadata Discovery

This is the starting point. Call this endpoint whenever the user selects a form template and version.
Before a client can build a query, it needs to know what **fields/dimensions/measures** are available. This is exposed
via metadata endpoints.

**Endpoint Get template metadata:**

```
GET /api/v1/analytics/pivot/metadata?templateId={uid}&templateVersionId={uid}
```

The response contains two primary lists: `fields` (data collected in the form) and `coreDimensions` (system-level data
like team, org unit, etc.).

```json-lines
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
      "extras": {
        "optionSetUid": null,
        "isMulti": false,
        "referenceTable": null
      }
    },
    {
      "uid": "etcDef98765",
      "dataElementUid": "deLMN22222",
      "factColumn": "etc_uid", 
      "name": "Household Category",
      "dataType": "OPTION",
      "aggregationModes": ["COUNT","COUNT_DISTINCT"],
      "isDimension": true,
      "isMeasure": false,
      "extras": {
        "optionSetUid": "opsQWERTY11",
        "isMulti": false
      }
    }
  ],
  "coreDimensions": [
    { "factColumn": "team_uid", "name": "Team", "dataType": "UID" },
    { "factColumn": "org_unit_uid", "name": "Org Unit", "dataType": "UID" },
    { "factColumn": "activity_uid", "name": "Activity", "dataType": "UID" },
    { "factColumn": "submission_completed_at", "name": "Submission Date", "dataType": "TIMESTAMP" }
  ]
}
```

**Frontend Usage:**

* **`fields` Array**: Use this to populate the "Measures" and template-specific "Dimensions" pickers.
    * `name`: The display label for the field in the UI.
    * `dataType`: Drives which filter operators (`=`, `>`, `IN`) and input controls (date picker, number input,
      dropdown) to show.
    * `aggregationModes`: The list of allowed aggregations for a measure. Disable or hide any unsupported options.
    * `uid`: The identifier for this template field. When creating a `MeasureRequest`, prefix this with `etc:` (e.g.,
      `"elementIdOrUid": "etc:etcAbc12345"`).
    * `extras.optionSetUid`: If present, use the `/api/v1/optionSets/{uid}/values` endpoint to fetch the available
      options for dropdowns.
* **`coreDimensions` Array**: Use this to populate the "Dimensions" picker for system-level groupings.
    * `factColumn`: The identifier to be used in the `dimensions`, `rowDimensions`, `columnDimensions`, and `filters`
      arrays of your query.
    * `name`: The display label for the dimension.

### Step 2: Query Execution

Once the user has configured their report, Construct and send a query based on the user's selections.

**Endpoint:** `POST /api/v1/analytics/pivot/query`

The `format` query parameter determines the shape of the response.

#### Format 1: `TABLE_ROWS` (For Standard Grids)

**Request (`?format=TABLE_ROWS`)**

```json-lines
{
  "templateId": "Tcf3Ks9ZRpB",
  "templateVersionId": "fb2GC7FInSu",
  "dimensions": ["team_uid", "org_unit_name"],
  "measures": [
    { "elementIdOrUid": "etc:etcAbc12345", "aggregation": "SUM", "alias": "population_reached" }
  ],
  "filters": [
    { "field": "submission_completed_at", "op": ">=", "value": "2025-01-01T00:00:00Z" },
    { "field": "team_uid", "op": "IN", "value": ["tm12345abc", "tm67890xyz"] }
  ],
  "sorts": [ { "fieldOrAlias": "population_reached", "desc": true } ],
  "limit": 50,
  "offset": 0
}
```

**Response (`TABLE_ROWS`)**

```json-lines
{
  "columns": [
    { "id": "team_uid", "label": "Team", "dataType": "UID" },
    { "id": "org_unit_name", "label": "Org Unit Name", "dataType": "TEXT" },
    { "id": "population_reached", "label": "Total Age", "dataType": "NUMERIC" }
  ],
  "rows": [
    { "team_uid": "tm12345abc", "org_unit_name": "Org Unit X1", "population_reached": 2450 },
    { "team_uid": "tm67890xyz", "org_unit_name": "Org Unit X2", "population_reached": 3123 }
  ],
  "total": 24 
}
```

**Frontend Usage:**

* Map the `columns` array to your grid's column definitions (e.g., `ag-Grid`'s `columnDefs`).
* Use the `rows` array as the data source (e.g., `rowData`).
* Use `total`, `limit`, and `offset` to configure pagination controls.

#### Format 2: `PIVOT_MATRIX` (For True Pivot Tables)

**Request (`?format=PIVOT_MATRIX`)**

```json-lines
{
  "templateId": "dt123abc456",
  "templateVersionId": "dtv987zyx321",
  "autoRenameAliases": false, 
  "rowDimensions": ["team_uid", "team_code"],
  "columnDimensions": ["activity_name"],
  "measures": [
    { "elementIdOrUid": "etc:etcZyx12346", "aggregation": "SUM", "alias": "total_sum" },
    { "elementIdOrUid": "etc:etcZemZ7mlg", "aggregation": "COUNT", "alias": "household_count" }
  ]
}
```

**Response (`PIVOT_MATRIX`)**

```json-lines
{
    "meta": { "format": "PIVOT_MATRIX", "templateId": "dt123abc456", "templateVersionId": "dtv987zyx321" },
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

**Frontend Usage:**

"The `matrix` object gives a pre-pivoted dataset. To render it:

1. The `rowHeaders` array gives the values for the row axis. The number of nested arrays corresponds to the number
   of `rowDimensions`.
2. The `columnHeaders` array gives the values for the column axis.
3. The `cells` data is a 2D array where `cells[i][j]` contains the measure values for the intersection of
   `rowHeaders[i]` and `columnHeaders[j]`.
4. Each cell object contains key-value pairs where the keys match the `measureAliases`."

---

## 4. Interaction & Auxiliary Endpoints

These endpoints help build a polished and user-friendly interface.

### UID-to-Name Resolution

To display human-readable names instead of raw UIDs in your grid.

**Endpoint:** `POST /api/v1/resolveUids`

**Request Body**

```json-lines
{ "uids": ["tm12345abc", "ou77777", "act67890xyz"] }
```

**Response**

```json-lines
{
  "tm12345abc": { "uid": "tm12345abc", "name": "Team Alpha" },
  "ou77777": { "uid": "ou77777", "name": "District West" }
}
```

**Usage:** After a query returns, collect all unique UIDs from the result set, make a single batch request to this
endpoint, and use the returned map to format the display values in your grid.

### **Building a Hierarchy Filter (e.g., for Org Units)**

To let users select from a tree of organizational units:

1. Fetch the hierarchy using `GET /api/v1/orgUnits/tree`.
2. Render this data using a tree component in your UI.
3. When the user selects one or more org units, collect their UIDs.
4. Construct a filter in your `PivotQueryRequest`:
   ```json-lines
   { "field": "org_unit_uid", "op": "IN", "value": ["ouChild123", "ouChild456"] }
   ```

### standard REST endpoints to Populate Filter Pickers for Core Dimensions

When a user wants to filter by a dimension (e.g. A user filtering by `team_uid` needs a dropdown of all available
teams), fetch the possible values from their respective REST endpoints.

* **Teams**: `GET /api/v1/teams?filter=...` -> returns `[{uid, name, label}, ...]`
* **Activities**: `GET /api/v1/activities?filter=...` -> returns `[{uid, name, label}, ...]`
* **Org Units (Hierarchy)**: `GET /api/v1/orgUnits?filter=...` -> returns `[{uid, name}, ...]`, &
  `GET /api/v1/orgUnits/tree` -> returns a tree structure. also available with `?root={uid}`.
* **Option values lookup**: `GET /api/v1/optionSets/{uid}/values` → returns list of `{uid, code, name, sortOrder}`.
* **DataElements lookup (for global)**: `GET /api/v1/dataElements?filter=...` -> returns `[{uid, name}, ...]`
* **DataTemplates**: `GET /api/v1/dataTemplates?filter=...`  -> returns `[{uid, name}, ...]`

### Saving & Loading Views

To allow users to save and share their query configurations.

* **Save View:** `POST /api/v1/analytics/views`
    * **Body:** `{ "name": "Monthly Household Report by Team", "queryRequest": { ... } }` (`queryRequest`: The full
      `PivotQueryRequest` object).
* **List Views:** `GET /api/v1/analytics/views`

```json-lines
[
    {
        "id": 123,
        "name": "Monthly Household Report by Team",
        "description": "...",
        "createdAt": "...",
        "owner": "user_a"
    }
]
```

* **Load View:** `GET /api/v1/analytics/views/{id}`
    * **Response:** The full view object, including the `queryRequest`. Use this to restore the UI state.

---

## 5. Advanced Recipes & Patterns

### Recipe: Implementing Drill-Down

When a user clicks a cell or row, they often want to see the underlying data that produced that result.

**Example Scenario:** The table shows `team_uid: "tm12345abc"` has a `population_reached: 2450`. The user clicks this
row to
investigate.

1. **Get Context from the Clicked Row**: The data for the clicked row provides the context for the drill-down. In this
   case, the context is `team_uid = "tm12345abc"`.
2. **Construct a New Query**: Create a new `PivotQueryRequest` by cloning the original one and adding the context as a
   new filter. You might also want to change the dimensions to see more detail.

    * **Original Request:**
      ```json-lines
      {
        "templateId": "dt123abc456",
        "dimensions": ["team_uid"],
        "measures": [{"elementIdOrUid": "etc:etcAbc12345", "aggregation": "SUM", "alias": "population_reached"}]
      }
      ```
    * **New Drill-Down Request:** (Notice the added filter and new dimension)
      ```json-lines
      {
        "templateId": "dt123abc456",
        "dimensions": ["team_uid", "submission_uid"], // Show individual submissions
        "measures": [{"elementIdOrUid": "etc:etcAbc12345", "aggregation": "SUM", "alias": "population_reached"}],
        "filters": [
          { "field": "team_uid", "op": "=", "value": "tm12345abc" } // The new drill-down filter
        ]
      }
      ```
3. **Execute and Render**: Execute this new query and display the results in a new tab, a modal, or by replacing the
   current view.

### Recipe: Filtering by Aggregated Values (HAVING)

To filter on a calculated result (e.g., show teams with more than 10 households).

* Simply use the measure's `alias` in the `field` property of a `FilterDto`. Your UI's field picker for filters should
  include both dimensions and any user-defined measure aliases.

```json-lines
{
  "measures": [{ "aggregation": "COUNT", "alias": "household_count", ... }],
  "filters": [
    { "field": "household_count", "op": ">", "value": 10 }
  ]
}
```

### Recipe: Global (Cross-Template) Queries

To query a single data element across all forms, your UI can provide a "global" mode.

* **Metadata:** Use `GET /api/v1/analytics/pivot/metadata/global` to discover available global data elements.
* **Querying:** Construct the `PivotQueryRequest` **without** `templateId` and `templateVersionId`. Measures must be
  prefixed with `de:` (e.g., `"elementIdOrUid": "de:deXYZ67890"`).

---

## 6. Error Handling

The API returns errors in a standardized format. A `400 Bad Request` indicates a user input error.

**Example Error Response (`400 Bad Request`)**

```json-lines
{
  "status": 400,
  "message": "Validation failed for the query request.",
  "details": [
    {
      "field": "measures[0].aggregation",
      "value": "SUM",
      "issue": "Aggregation 'SUM' is not allowed for data type 'OPTION'."
    }
  ]
}
```

**Usage:**

* Display the top-level `message` in a general notification.
* Use the `details` array to highlight the specific UI control that caused the error.

---


## **Future Enhancements:**

## Enhancements

1. make the front-end "dumber" It shouldn't need to know the difference between a "core dimension" and a "template field", to the ui they both just a querable id.
2. Standardize the Query Request (`POST /query`), same thing, + rename `elementIdOrUid` to just `id`
