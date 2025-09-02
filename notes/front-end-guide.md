**Title: Developer's Guide: Building the Analytics UI**

**1. Introduction & Getting Started**
*   **Brief Conceptual Overview (Optional but Recommended):** A single paragraph explaining the goal: "This API allows you to build a powerful pivot table experience by querying pre-aggregated data. You will select a form, discover its available fields, and then construct queries to slice and dice the data." (This gives context without the clutter).
*   **API Fundamentals:**
*   Base URL
*   Authentication (JWT Bearer Token)
*   Identifiers (Always use the 11-character `uid`)

**2. The Core Workflow: Discover, Query, Interact**
*   A high-level summary of the three-step process to frame the developer's thinking.

**3. API Endpoints & Usage**
*   **(Step 1) Metadata Discovery:** `GET /api/v1/analytics/pivot/metadata`
*   **(Step 2) Query Execution:** `POST /api/v1/analytics/pivot/query`
*   Explain the `format` parameter (`TABLE_ROWS` vs. `PIVOT_MATRIX`).
*   Provide clear examples for both request bodies and their corresponding response structures. *This is where you should add the section on handling the `PIVOT_MATRIX` response.*
*   **(Step 3) Interaction & Auxiliary Endpoints:**
*   **UID Resolution:** `POST /api/v1/resolve-uids` (Crucial for a good UX).
*   **Populating Filter Pickers:** Explain how to use `GET /api/v1/teams`, `GET /api/v1/org-units/tree`, etc. *This covers the "Advanced Filtering" point.*
*   **Saving & Loading Views:** The `GET` and `POST` endpoints for `/api/v1/analytics/views`. *This covers the "Saved Views" point.*

**4. Advanced Features & Recipes**
*   **Drill-Down Pattern:** A concrete example of transforming a clicked row into a new, filtered `PivotQueryRequest`.
*   **Filtering by Measure Aliases:** Show an example of a filter using a measure's alias (the "HAVING" clause).
*   **Global (Cross-Template) Queries:** Briefly explain the use case for `de:<uid>` and the different metadata endpoint. *This covers the "Global Queries" point.*

**5. Error Handling**
*   Provide the standardized JSON error structure and explain how the front-end should parse it to display user-friendly messages.

---

By structuring the guide this way, you are giving the front-end engineer **everything they need, and nothing they don't.** The document becomes a practical tool that they can have open while they code, rather than a dense system architecture document they have to sift through.

You can now confidently finalize the developer guide with this focused structure. It will be more than enough to get them started immediately and effectively.

---

Excellent. The enhanced guide is comprehensive and covers the critical path for a front-end developer. Before you finalize it, here are a few final points of clarification that will address potential edge cases and advanced features. Adding these will make the guide virtually foolproof and will preempt questions from the engineer.

Here are the details that are still missing or could be a source of confusion:

***

### Final Polish: Addressing Advanced Features and Potential Ambiguities

While the current guide covers the primary `TABLE_ROWS` workflow perfectly, an engineer might still have questions about the following advanced capabilities mentioned in the source documents.

---

#### **1. Handling the `PIVOT_MATRIX` Response Format**

The guide focuses on `TABLE_ROWS`, which is great for standard grids. However, if the UI needs to render a true pivot table (with row and column headers), the `PIVOT_MATRIX` format is more efficient. A developer needs to know how to parse this specific structure.

**Recommendation:** Add a subsection explaining how to render the `PIVOT_MATRIX` response.

**Endpoint: Execute Pivot Query (Matrix Format)**

```
POST /api/v1/analytics/pivot/query?format=PIVOT_MATRIX
```

**Response (`format=PIVOT_MATRIX`)**

```json
{
    "total": 61,
    "matrix": {
        "rowDimensionNames": ["team_uid", "team_code"],
        "columnDimensionNames": ["activity_name"],
        "measureAliases": ["total_sum", "household_count"],
        "rowHeaders": [
            [ "tm12345abc", "TMA" ], 
            [ "tm67890xyz", "TMB" ]
        ],
        "columnHeaders": [
            [ "Activity A" ],
            [ "Activity B" ]
        ],
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

#### **2. Advanced Filtering: By Measure Aliases and Populating Pickers**

The filter builder needs more detail on two fronts.

**A) How to Filter by a Measure's Result (e.g., a HAVING clause)**

A user might want to find all teams where the total number of households is greater than 10. This requires filtering by the *alias* of a measure.

**Recommendation:** Add an example showing a filter that uses a measure alias.

**Example `PivotQueryRequest`:**

```json
{
  "dimensions": ["team_uid"],
  "measures": [
    { "elementIdOrUid": "etc:etcDef98765", "aggregation": "COUNT", "alias": "household_count" }
  ],
  "filters": [
    { "field": "household_count", "op": ">", "value": 10 }
  ]
}
```

**Frontend Developer Guide:**

"To filter on an aggregated value, simply use the measure's `alias` in the `field` property of a `FilterDto`. Your UI should populate the filter's field selector with both the available dimensions *and* the aliases of any measures the user has already defined."

**B) How to Populate Filter Dropdowns for Core Dimensions**



**Recommendation:** Explicitly state that the standard REST endpoints should be used for this.

**Frontend Developer Guide:**

"When a user wants to filter by a core dimension like 'Team' or 'Activity', use the existing top-level API endpoints to fetch a list of possible values.

*   For `team_uid`: call `GET /api/v1/teams` to get a list of teams (`{uid, name}`).
*   For `activity_uid`: call `GET /api/v1/activities` to get a list of activities.

Use the results to populate the dropdown or multi-select control in your filter builder."

---

#### **3. Saving and Loading Queries ("Views")**

The original UX document mentions saving and sharing views. The engineer will need to know which endpoints to call.

**Recommendation:** Define the endpoints for managing saved views.

**Endpoint: Save a New View**

```
POST /api/v1/analytics/views
```

**Request Body**

```json
{
  "name": "Monthly Household Report by Team",
  "description": "Shows the total age and household count for all teams in the last month.",
  "queryRequest": { ... } // The full PivotQueryRequest JSON object
}
```

**Endpoint: List Saved Views**

```
GET /api/v1/analytics/views
```

**Response**

```json
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

**Endpoint: Load a Saved View**

```
GET /api/v1/analytics/views/{id}
```

**Response**

Returns the full saved view object, including the `queryRequest`, which the frontend can then use to restore the UI state and re-execute the query.

---

#### **4. Clarifying Global (Cross-Template) vs. Template-Scoped Queries**

The original documentation mentions `de:<uid>` for global queries. This is a powerful feature, but the current guide assumes a template is always selected first.

**Recommendation:** Briefly explain the "global" query flow.

**Frontend Developer Guide:**

"The platform supports two query modes:

1.  **Template-Scoped (Standard):** This is the primary flow where you first select a template. All measures are prefixed with `etc:` (e.g., `"elementIdOrUid": "etc:KiHwmLKUo9j"`).
2.  **Global (Advanced):** This mode allows you to query a specific data element across *all* templates where it is used. To enable this, your UI would need a separate entry point or a toggle that does not require a template selection.

*   **Metadata:** To discover available global elements, call a global metadata endpoint: `GET /api/v1/analytics/pivot/metadata/global`. This would return a list of all unique data elements (`de:<uid>`) available for querying.
*   **Querying:** When building the `PivotQueryRequest`, you will **omit** the `templateId` and `templateVersionId` fields. Measures will use the `de:` prefix (e.g., `"elementIdOrUid": "de:deXYZ67890"`)."

By adding these four sections, your developer guide will be exceptionally thorough, covering not just the happy path but also the advanced features and potential complexities an engineer will encounter. This will significantly speed up development and reduce back-and-forth communication.
