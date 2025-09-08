# Analytics API: A Front-end Developer's Guide

## 1. Introduction & Getting Started

This guide provides all the necessary information to build a rich, interactive pivot table and analytics UI using the
Datarun API. The API is designed to be consumed by modern web frameworks like Angular, using component libraries such as
ag-Grid.

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

* **Base URL**: All API paths in this guide are relative to your deployed instance's base URL (e.g.,
  `https://your-datarun-instance.com`).
* **Authentication** All requests to `/api/v1/analytics/` endpoints must include an `Authorization: Bearer <TOKEN>`
  header. The JWT token is obtained by posting user credentials to the `POST /api/v1/auth/login` endpoint. The token is
  short-lived and should be refreshed using the refresh token provided in the login response. For full details, see the
  separate Authentication API Guide.
* **Identifiers**: The API uses a standardized, namespaced identifier for all queryable fields. This is the ID you will
  receive in the metadata and send back in all query requests.
    * **Format:** `namespace:value`
    * **Examples:** `core:team_uid`, `etc:CiEZemZ7mlg`

---

## The 3-Step Workflow

The core principle is a simple, stateful flow: **Discover** available data fields, **Query** for aggregated results, and
**Render & Interact** with the data.

1. **Discover:** The user selects a form. You fetch its metadata to see what Fields are available.
2. **Query:** You build a request to fetch data for specific Dimensions and Measures.
3. **Render:** You display the returned data in a grid.

### Step 1: Discover Available Fields

When a user selects a form template and version, call the metadata endpoint:
**Endpoint:** `GET /api/v1/analytics/pivot/metadata`

```http
GET /api/v1/analytics/pivot/metadata?templateId=Tcf3Ks9ZRpB&templateVersionId=fb2GC7FInSu
```

**Response (`PivotMetadataResponse`)**
The API returns a single, unified `availableFields` list. Each object in this list is a self-describing field that
provides all the information needed to drive the UI.

```json-lines
{
  "templateId": "Tcf3Ks9ZRpB",
  "templateVersionId": "fb2GC7FInSu",
  "availableFields": [
    {
      "id": "etc:etcAbc12345",
      "label": "Household Category",
      "dataType": "UID",
      "isDimension": false,
      "isSortable": true,
      "aggregationModes": ["COUNT", "COUNT_DISTINCT"],
      "displayGroup": "Household Demographics",
      "extras": {
           "optionSetId": "opsQWERTY11",
           "resolution": {
            "type": "API_ENDPOINT",
            "endpoint": "/api/v1/optionSets/{optionSetId}/options"
          }
      }
    },
    "id": "core:parent_category_uid",
    "label": "Parent Category",
    "dataType": "UID",
    "isDimension": true,
    "isSortable": false,
    "displayGroup": "System Fields",
    "extras": {
    "resolution": {
      "type": "HIERARCHICAL",
      "childDimensionId": "core:child_category_uid" // This new field links the hierarchy
      }
    },
    {
      "id": "core:org_unit_uid",
      "label": "Organization Unit",
      "dataType": "UID",
      "isDimension": true,
      "isSortable": true,
      "aggregationModes": [],
      "displayGroup": "System Fields",
      "extras": {
           "resolution": {
            "type": "API_ENDPOINT",
            "endpoint": "/api/v1/orgUnits"
          }
      }
    },
    {
      "id": "core:team_uid",
      "label": "Team",
      "dataType": "UID",
      "isDimension": true,
      "isSortable": true,
      "aggregationModes": [],
      "displayGroup": "System Fields",
      "extras": {
           "resolution": {
            "type": "API_ENDPOINT",
            "endpoint": "/api/v1/teams"
          }
      }
    },
    {
      "id": "core:submission_completed_at",
      "label": "Submission Date",
      "dataType": "TIMESTAMP",
      "isDimension": true,
      "isSortable": true,
      "aggregationModes": ["MIN", "MAX", "COUNT"],
      "displayGroup": "System Fields",
      "extras": {
        "formatHint": "SHORT_DATE"
      }
    },
    {
      "id": "etc:income_main_earner",
      "label": "Household Income",
      "dataType": "NUMERIC",
      "isDimension": false,
      "isSortable": true,
      "aggregationModes": ["SUM", "AVG", "MIN", "MAX"],
      "displayGroup": "Household Financials",
      "extras": {
        "formatHint": "CURRENCY_USD"
      }
    },
    {
      "id": "de:completion_rate",
      "label": "Completion Rate",
      "dataType": "NUMERIC",
      "isDimension": false,
      "isSortable": true,
      "aggregationModes": ["AVG"],
      "displayGroup": "Submission Metrics",
      "extras": {
        "formatHint": "PERCENT"
      }
    }
  ]
}
```

**1. Frontend Usage:**

* **Populating Pickers:**
* Iterate over the single `availableFields` list to populate all of your UI pickers for both dimensions and measures.
  Use the `label` for display and the `id` for API requests.

### Step 2: Query Execution

Once the user has configured their report, Construct and send a query based on the user's selections.

**Endpoint:** `POST /api/v1/analytics/pivot/query`

The `format` query parameter determines the shape of the response.

#### Format 1: `TABLE_ROWS` (For Standard Grids)

**Request Body (`PivotQueryRequest`)**
All field references in `dimensions`, `filters`, `sorts`, and `measures` **must** use the standardized `id` received
from the metadata endpoint.

```json-lines
{
  "templateId": "Tcf3Ks9ZRpB",
  "templateVersionId": "fb2GC7FInSu",
  "dimensions": ["core:team_uid", "core:org_unit_name"],
  "measures": [
    { "fieldId": "etc:age_of_head_uid", "aggregation": "AVG", "alias": "avg_age"}
  ],
  "filters": [
    { "field": "core:team_uid", "op": "IN", "value": ["tm12345abc", "tm67890xyz"] },
    { "field": "etc:age_of_head_uid", "op": ">=", "value": 18 }
  ],
  "sorts": [
    { "fieldOrAlias": "avg_age", "desc": true }
  ],
  "limit": 50,
  "offset": 0
}
```

**Response (`TABLE_ROWS` format)**

```json-lines
{
  "columns": [
    { "id": "core:team_uid", "label": "Team", "dataType": "UID" },
    { "id": "core:org_unit_name", "label": "Org Unit Name", "dataType": "TEXT" },
    { "id": "avg_age", "label": "Avg Age", "dataType": "NUMERIC" }
  ],
  "rows": [
    { "core:team_uid": "tm12345abc", "core:org_unit_name": "Org Unit X1", "avg_age": 35.4 },
    { "core:team_uid": "tm67890xyz", "core:org_unit_name": "Org Unit X2", "avg_age": 41.2 }
  ],
  "total": 24
}
```

**Frontend Usage:**

* Map the `columns` array to your grid's column definitions. The `id` property corresponds to the key in each `rows`
  object.
* Use the `rows` array as the data source for the grid.
* Use `total`, `limit`, and `offset` to configure pagination controls.

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

1. `dataType`: Drives which filter operators (`=`, `>`, `IN`) and input controls (date picker, number input,
   dropdown) to show.
2. When a user wants to filter by "Team," check its `extras.resolution` object.
3. The `endpoint` property gives you the exact URL to call (`/api/v1/teams`) to get the list of available teams.
4. Populate a dropdown or multi-select with the results.
5. Once the user makes a selection, add a `filter` to your query request:

**Request with Filter:**

```json-lines
{
  // ...
  "filters": [
    {
      "field": "core:team_uid",
      "op": "IN",
      "value": ["resolved1.uid", "resolved2.uid"] 
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
    * **Explicit Sortability (`isSortable`)**
        * **Purpose:** To explicitly declare whether a field can be used in the `sorts` array of a query request.
        * **Implementation:** If `isSortable` is `true`, your grid UI should show sorting controls (e.g., clickable
          column headers) for that field. If `false`, those controls should be disabled to prevent the user from making
          an invalid request.

* **Pagination:** To navigate to the next page, simply update the `offset`.
  ```json-lines
  "limit": 50,
  "offset": 50 // Now requesting page 2
  ```

### aggregation rules:

* `aggregationModes` to know which functions to offer for a given field.
* A field's role is flexible. The `isDimension` property suggests its common use.
* Any field with a non-empty aggregationModes array can be used as a measure.
* UI should allow users to drag fields into either the 'Dimensions' or 'Measures' area based on these rules.
* **. Field Grouping & Organization (`displayGroup`):** To prevent overwhelming users with a long, flat list of fields,
  the metadata provides a `displayGroup` property.
    * **Purpose:** This property allows you to group related fields together in your UI's field pickers.
    * **Implementation:** Use this string to build a sectioned list, an accordion, or a tree view for selecting
      dimensions and measures. This dramatically improves usability, especially for forms with many questions.

      For example, you can group all fields with `"displayGroup": "System Fields"` under one heading and fields with
      `"displayGroup": "Household Demographics"` under another.

### Data Presentation & Formatting (`formatHint`)

The `dataType` tells you the type of data, while the optional `formatHint` in the `extras` object tells you how to
display it. This allows the UI to apply context-aware formatting without hardcoding rules.

* **Purpose:** To ensure values like currencies, percentages, and dates are displayed in a human-readable and consistent
  format.
* **Implementation:** Your rendering logic should check for this hint and apply the appropriate formatting mask.

| `formatHint` Value | Example Raw Value   | Suggested Rendering |
|:-------------------|:--------------------|:--------------------|
| `CURRENCY_USD`     | `1500.75`           | `$1,500.75`         |
| `PERCENT`          | `0.854`             | `85.4%`             |
| `SHORT_DATE`       | `2024-09-21T10:00Z` | `09/21/2024`        |
| *`(not provided)`* | `12345`             | `12,345` (default)  |

### Enabling Hierarchical Drill-Down (`childDimensionId`)

For the "Actionable Hierarchical Context" recipe to work, the front-end needs to know the relationship between parent
and child entities.

* **Purpose:** The `childDimensionId` property, found inside the `resolution` object for a `HIERARCHICAL_ENTITY`,
  provides the direct link to the next level down in a hierarchy.
* **Implementation:** When a user triggers a "Drill Down" action on a row, your application should:
    1. Identify the field the user clicked (e.g., `core:org_unit_uid`).
    2. Check its metadata for `extras.resolution.childDimensionId`.
    3. Construct a new query where the original dimension (`core:org_unit_uid`) is replaced by the child dimension (
       `core:sub_org_unit_uid`) in the `dimensions` array.
    4. Add a filter to the new query to only show results for the parent the user clicked on.

---

#### Format 2: `PIVOT_MATRIX` (For True Pivot Tables)

**Request (`?format=PIVOT_MATRIX`)**

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

**Response (`PIVOT_MATRIX`)**

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

For columns with `dataType: "UID""`, you will receive UIDs in the response. To display friendly names, use this batch
endpoint.

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
      "issue": "Aggregation 'SUM' is not allowed for data type 'UID'."
    }
  ]
}
```

**Usage:**

* Display the top-level `message` in a general notification.
* Use the `details` array to highlight the specific UI control that caused the error.

---

## 7. Appendix: Data Types and Operators

The `dataType` from the metadata endpoint determines which operators are available in the filter builder. The following
table provides the expected mappings.

| dataType      | Supported Operators              | UI Control Suggestion    |
|:--------------|:---------------------------------|:-------------------------|
| **NUMERIC**   | `=`, `!=`, `>`, `<`, `>=`, `<=`  | Number Input             |
| **TEXT**      | `=`, `!=`, `LIKE`, `ILIKE`, `IN` | Text Input               |
| **BOOLEAN**   | `=`, `!=`                        | Toggle Switch / Dropdown |
| **TIMESTAMP** | `BETWEEN`, `>=`, `<=`            | Date/Time Range Picker   |
| **DATE**      | `BETWEEN`, `=>`, `<=`            | Date Range Picker        |
| **UID**       | `=`, `!=`, `IN`                  | Searchable Dropdown      |
| **OPTION**    | `=`, `!=`, `IN`                  | Multi-select Dropdown    |
