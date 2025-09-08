# Analytics API: A Front-end Developer's Guide

## Platform and technical deps

- Angular v20.0.0,

## 1. Introduction & Core Concepts

This guide provides everything you need to build a rich, interactive analytics and pivot table UI using the Datarun API.
It is designed to be a practical, step-by-step manual that takes you from a simple data table to a full-featured pivot
grid.

### Core Concepts

Before you begin, understand these key terms:

* **Field:** The central concept in the API. A Field is any piece of data you can query, group by, or filter on. It can
  be a system-level attribute like "Team" or a question from a form like "Age of Household Head".
* **Dimension:** A Field used to group or categorize your data (e.g., grouping by `Team` or `Org Unit`). Dimensions are
  the "by" in your analysis (e.g., "show me results *by* Team").
* **Measure:** A calculation performed on a Field (e.g., the `SUM` of "Quantity Issued" or the `COUNT` of submissions).
* **Standardized ID:** The unique identifier for every Field. You will get this from the metadata and use it in all your
  query requests.
    * **Format:** `namespace:value`
    * **Examples:** `core:team_uid`, `etc:CiEZemZ7mlg`

### API Fundamentals

* **Base URL**: All paths are relative to your deployed instance (e.g., `https://your-datarun-instance.com`).
* **Authentication**: All `/api/v1/analytics/` endpoints require a JWT token in the `Authorization` header.
  ```http
  Authorization: Bearer <your_jwt_token>
  ```

---

## 2. Your First Query: Building a Simple Data Table

We'll start by fetching and displaying a basic, non-interactive table. This covers the fundamental workflow.

### The 3-Step Workflow

1. **Discover:** The user selects a form. You fetch its metadata to see what Fields are available.
2. **Query:** You build a request to fetch data for specific Dimensions and Measures.
3. **Render:** You display the returned data in a grid.

#### Step 1: Discover Available Fields

When a user selects a form template and version, call the metadata endpoint:

* `GET /api/v1/analytics/pivot/metadata?templateId=...`
* Metadata drives all UI controls (fields, labels, allowed aggregations, `factColumn`, `dataType`, `extras`).

**Endpoint:** `GET /api/v1/analytics/pivot/metadata`

```http
GET /api/v1/analytics/pivot/metadata?templateId=Tcf3Ks9ZRpB&templateVersionId=fb2GC7FInSu
```

**Response (`PivotMetadataResponse`)**
The API returns a single, unified `availableFields` list. This is the source of truth for building your entire UI.

```json-lines
{
  "availableFields": [
    {
      "id": "core:team_uid",
      "label": "Team",
      "category": "CORE_DIMENSION",
      "dataType": "UID",
      "factColumn": "team_uid",
      "extras": { /* ... */ }
    },
    {
      "id": "etc:CiEZemZ7mlg",
      "label": "Age of Household Head",
      "category": "DYNAMIC_MEASURE",
      "dataType": "NUMERIC",
      "factColumn": "etc_uid",
      "aggregationModes": ["SUM", "AVG", "MIN", "MAX", "COUNT"],
      "extras": { /* ... */ }
    }
  ],
  "hints": { /* ... */ }
}
```

**Frontend Action:**

* Iterate over the `availableFields` list to populate your UI pickers for both Dimensions and Measures.
* Use `label` for display.
* Use `id` as the unique identifier you'll send back to the server.
* Use `aggregationModes` to know which functions to offer for a given measure.

#### Step 2: Execute a Simple Query

Let's request the total age, grouped by team.

**Endpoint:** `POST /api/v1/analytics/pivot/query?format=TABLE_ROWS`

**Request Body (`PivotQueryRequest`)**
Use the `id` values from the metadata response.

```json-lines
{
  "templateId": "Tcf3Ks9ZRpB",
  "templateVersionId": "fb2GC7FInSu",
  "dimensions": ["core:team_uid"],
  "measures": [
    {
      "id": "etc:CiEZemZ7mlg",
      "aggregation": "SUM",
      "alias": "total_age"
    }
  ],
  "limit": 50,
  "offset": 0
}
```

#### Step 3: Render the Table

The `TABLE_ROWS` format is designed to map directly to a data grid.

**Response (`PivotQueryResponse`)**

```json-lines
{
  "columns": [
    { "id": "core:team_uid", "label": "Team", "dataType": "UID" },
    { "id": "total_age", "label": "total_age", "dataType": "NUMERIC" }
  ],
  "rows": [
    { "core:team_uid": "tm12345abc", "total_age": 2450 },
    { "core:team_uid": "tm67890xyz", "total_age": 3123 }
  ],
  "total": 24
}
```

**Frontend Action:**

* Use the `columns` array to configure your grid's column definitions (e.g., ag-Grid's `columnDefs`).
* Use the `rows` array as the data source (e.g., `rowData`).
* Use `total`, `limit`, and `offset` for pagination controls.

---

## 3. Making the Table Interactive

Now let's add filtering, sorting, and other user interactions.

### Filtering Your Data

The API makes filtering dynamic and data-driven. You don't need to hardcode any logic.

**How it Works:** The `resolution` object in a field's metadata tells you how to build its filter control.

**Example `PivotFieldDto` for "Team":**

```json-lines
{
  "id": "core:team_uid",
  "label": "Team",
  // ...
  "extras": {
    "resolution": {
      "type": "API_ENDPOINT",
      "endpoint": "/api/v1/teams"
    }
  }
}
```

**Frontend Action:**

1. When a user wants to filter by "Team," check its `extras.resolution` object.
2. The `endpoint` property gives you the exact URL to call (`/api/v1/teams`) to get the list of available teams.
3. Populate a dropdown or multi-select with the results.
4. Once the user makes a selection, add a `FilterDto` to your query request:

**Request with Filter:**

```json-lines
{
  // ...
  "filters": [
    {
      "field": "core:team_uid",
      "op": "IN",
      "value": ["tm12345abc", "tm67890xyz"]
    }
  ]
}
```

### Sorting & Pagination

These are straightforward modifications to the query request.

* **Sorting:** To sort by a column, add a `SortDto` object to the `sorts` array.
  ```json-lines
  "sorts": [ { "fieldOrAlias": "total_age", "desc": true } ]
  ```
* **Pagination:** To navigate to the next page, simply update the `offset`.
  ```json-lines
  "limit": 50,
  "offset": 50 // Now requesting page 2
  ```

---

## 4. The Full Pivot Experience

Let's move beyond a simple table to a true pivot grid with advanced interactions.

### Building a Pivot Matrix

To create a pivot grid with row and column headers, change the format and request structure.

**Endpoint:** `POST /api/v1/analytics/pivot/query?format=PIVOT_MATRIX`

**Request with `rowDimensions` and `columnDimensions`:**

```json-lines
{
  "templateId": "dt123abc456",
  "templateVersionId": "dtv987zyx321",
  "autoRenameAliases": false, 
  "rowDimensions": ["core:team_uid", "core:team_code"],
  "columnDimensions": ["core:activity_name"],
  "measures": [
    { "fieldId": "etc:etcZyx12346", "aggregation": "SUM", "alias": "total_sum" },
    { "fieldId": "etc:etcZemZ7mlg", "aggregation": "COUNT", "alias": "household_count" }
  ]
}
```

**Response (`PIVOT_MATRIX` format)**

```json-lines
{
    "meta": { "format": "PIVOT_MATRIX", "templateId": "dt123abc456", "templateVersionId": "dtv987zyx321" },
    "matrix": {
        "rowDimensionNames": ["core:team_uid", "core:team_code"],
        "columnDimensionNames": ["core:activity_name"],
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

**Frontend Action:**
"The `matrix` object gives a pre-pivoted dataset. To render it:

1. The `rowHeaders` array gives the values for the row axis. The number of nested arrays corresponds to the number
   of `rowDimensions`.
2. The `columnHeaders` array gives the values for the column axis.
3. The `cells` data is a 2D array where `cells[i][j]` contains the measure values for the intersection of
   `rowHeaders[i]` and `columnHeaders[j]`.
4. Each cell object contains key-value pairs where the keys match the `measureAliases`.


## 5. Interaction & Auxiliary Endpoints

These endpoints help build a polished and user-friendly interface.

### UID-to-Name Resolution
For columns with `dataType: "UID"`, you will receive UIDs in the response. To display friendly names, use this batch endpoint.

**Endpoint:** `POST /api/v1/resolveUids`

**Usage:** After a query returns, collect all unique UIDs from the result set, make a single batch request to this endpoint, and use the returned map to format the display values in your grid.

## 6. Advanced Recipes & Patterns

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
        "dimensions": ["core:team_uid"],
        "measures": [{"fieldId": "etc:etcAbc12345", "aggregation": "SUM", "alias": "population_reached"}]
      }
      ```
    * **New Drill-Down Request:** (Notice the added filter and new dimension)
      ```json-lines
      {
        "templateId": "dt123abc456",
        "dimensions": ["core:team_uid", "core:submission_uid"], // Show individual submissions
        "measures": [{"fieldId": "etc:etcAbc12345", "aggregation": "SUM", "alias": "population_reached"}],
        "filters": [
          { "field": "core:team_uid", "op": "=", "value": "tm12345abc" } // The new drill-down filter
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
  prefixed with `de:` (e.g., `"fieldId": "de:deXYZ67890"`).

---

## 6. Guided Analytics: An Intelligent UI

Some measures only make sense when grouped by a specific dimension. The API provides hints to help you guide the user.

### The `recommendedDimensions` Hint

When you see this hint in a field's metadata, it means the measure is more meaningful when grouped by the recommended
dimension.

**Example `PivotFieldDto` for "Quantity Issued":**

```json-lines
{
  "id": "etc:qty_issued_uid",
  "label": "Quantity Issued",
  // ...
  "extras": {
    "recommendedDimensions": [ "core:child_category_name" ]
  }
}
```

**Recommended Frontend UX:**

1. When a user adds "Quantity Issued" as a measure, check for the `recommendedDimensions` hint.
2. If it exists, display an icon (e.g., 💡) next to the measure in the UI.
3. On hover, show a tooltip: **"For a meaningful result, group this measure by: 'Child Category Name'."**
4. (Optional) Provide a button to automatically add the recommended dimension to the query.

---

## 7. Error Handling & Appendix

### Error Handling

If a request is invalid, the API will return a `400 Bad Request` with a structured error.

**Example Error Response:**

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

**Frontend Action:**

* Display the top-level `message` in a general notification.
* Use the `details` array to highlight the specific UI control that caused the error.

### Appendix: Data Types and Operators

| `dataType`    | Supported Operators              | UI Control Suggestion  |
|:--------------|:---------------------------------|:-----------------------|
| **NUMERIC**   | `=`, `!=`, `>`, `<`, `>=`, `<=`  | Number Input           |
| **TEXT**      | `=`, `!=`, `LIKE`, `ILIKE`, `IN` | Text Input             |
| **BOOLEAN**   | `=`, `!=`                        | Toggle / Dropdown      |
| **TIMESTAMP** | `BETWEEN`, `>=`, `<=`            | Date/Time Range Picker |
| **DATE**      | `BETWEEN`, `>=`, `<=`            | Date Range Picker      |
| **UID**       | `=`, `!=`, `IN`                  | Searchable Dropdown    |
| **OPTION**    | `=`, `!=`, `IN`                  | Multi-select Dropdown  |
