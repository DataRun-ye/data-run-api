# Analytics API: A Front-end Developer's Guide

## 1. Introduction & Getting Started
(... no changes here ...)

### API Fundamentals

*   **Base URL**: (... no changes ...)
*   **Authentication**: (... no changes ...)
*   **[UPDATED] Identifiers**: The API now uses a standardized, namespaced identifier for all queryable fields. This is the ID you will receive in the metadata and send back in query requests.
    *   **Format:** `namespace:value`
    *   **Examples:** `core:team_uid`, `etc:CiEZemZ7mlg`

---

## 2. The Core Workflow
(... no changes here ...)

---

## 3. API Endpoints & Usage

### Step 1: Metadata Discovery

**Endpoint:** `GET /api/v1/analytics/pivot/metadata`

**[UPDATED] Response (`PivotMetadataResponse`)**
The response now contains a single, unified `availableFields` list. Each object in this list is a self-describing field that the UI can use to build its controls.

```json-lines
{
  "templateId": "Tcf3Ks9ZRpB",
  "templateId": "fb2GC7FInSu",
  "availableFields": [
    {
      "id": "etc:etcAbc12345", // Standardized ID
      "label": "Household Category",
      "dataType": "OPTION",
      "isDimension": false, // ui hint
      "aggregationModes": ["COUNT", "COUNT_DISTINCT"], // it hints it can be a measure 
      "extras": {
           "optionSetId": "opsQWERTY11",
           "resolution": {
            "type": "API_ENDPOINT",
            "endpoint": "/api/v1/optionSets/{optionSetId}/options", // where to get options
            "valueField": "uid",
            "labelField": "name"
          }
      }
    },
    {
      "id": "core:team_uid", // Standardized ID
      "label": "Team",
      "dataType": "UID",
      "isDimension": true,
      "aggregationModes": [],
      "extras": {
           "resolution": {
            "type": "API_ENDPOINT",
            "endpoint": "/api/v1/teams", // where to get the list of teams
            "valueField": "uid",
            "labelField": "name"
          }
      }
    },
    {
      "id": "core:org_unit_uid", // Standardized ID
      "label": "Organization Unit",
      "dataType": "UID",
      "isDimension": true,
      "aggregationModes": [],
      "extras": {
           "resolution": {
            "type": "API_ENDPOINT",
            "endpoint": "/api/v1/orgUnits", // where to get the list of teams
            "valueField": "uid",
            "labelField": "name"
          }
      }
    },
    {
      "id": "core:team_code", // Standardized ID
      "label": "Team Code",
      "dataType": "TEXT", // This field doesn't need name resolution
      "isDimension": true,
      "aggregationModes": [],
      "extras": {}
    }
    {
      "id": "core:submission_completed_at",
      "label": "Submission Date",
      "dataType": "TIMESTAMP",
      "isDimension": true,
      "aggregationModes": [],
      "extras": {}
    }
  ]
}
```

**[UPDATED] Frontend Usage:**

*   **Populating Pickers:** Iterate over the single `availableFields` list to populate all of your UI pickers for both dimensions and measures. Use the `label` for display and the `id` for API requests.
*   **Populating Filter Dropdowns:** For any field, check for an `extras.resolution` object. If it exists, its `endpoint` property gives you the exact API endpoint to call to fetch the list of selectable values (e.g., list of teams, list of options). This removes all guesswork.
* `dataType`: Drives which filter operators (`=`, `>`, `IN`) and input controls (date picker, number input, dropdown) to show.
* `aggregationModes`: The list of allowed aggregations for a measure. Disable or hide any unsupported options.
---

### Step 2: Query Execution

**Endpoint:** `POST /api/v1/analytics/pivot/query`

**[UPDATED] Request Body**
All field references in `dimensions`, `rowDimensions`, `columnDimensions`, `filters`, and `sorts` **must** now use the standardized `id` received from the metadata endpoint.

```json-lines
{
  "templateId": "Tcf3Ks9ZRpB",
  "templateVersionId": "fb2GC7FInSu",
  "dimensions": ["core:team_uid", "core:child_category_name"],
  "measures": [
    { "fieldId": "etc:qty_issued_uid", "aggregation": "SUM", "alias": "total_issued" }
  ],
  "filters": [
    { "field": "core:team_uid", "op": "IN", "value": ["tm12345abc"] }
  ]
}
```

(... Response format examples are unchanged ...)

---

## 4. Interaction & Auxiliary Endpoints
(... No changes to the endpoints themselves, but the guide text is simplified ...)

**[UPDATED] Populating Filter Pickers**
The API metadata makes populating filters simple and dynamic. For any field you want to use in a filter, inspect its object from the `/metadata` response:

1.  Look for an `extras.resolution` object.
2.  If it exists, its `endpoint` property will give you the exact API endpoint to call to fetch the list of selectable values.
3.  If the `resolution.type` is `DYNAMIC_ENDPOINT`, the UI should call the specified `endpoint` and pass the field's `id` as a query parameter (e.g., `?fieldId=core:child_category_uid`).

This means you no longer need to hardcode any logic for specific dimensions.

---

## 5. Advanced Recipes & Patterns
(... Drill-Down, Filtering by Alias, and Global Queries sections remain the same, just ensure they use the new standardized IDs in their examples ...)

**[NEW] Recipe: Guiding Users with resolution `type`**
Some
**[NEW] Recipe: Guiding Users with Recommended Dimensions**

Some measures, especially those from repeatable sections of a form, are only meaningful when grouped by a specific category (e.g., "Quantity Issued" should be grouped by "Product"). The API provides hints to help you guide the user toward creating these meaningful reports.

**The Metadata Hint:**
When you receive the `availableFields` from the `/metadata` endpoint, inspect the `extras` object for each measure. If you find a `recommendedDimensions` list, this is a hint from the backend.

```json-lines
{
  "id": "etc:qty_issued_uid",
  "label": "Quantity Issued",
  "extras": {
    "recommendedDimensions": [ "core:child_category_name" ]
  }
}
```

**Recommended UX Behavior:**

1.  When a user adds a measure that has `recommendedDimensions` to their query, the UI should provide a visual cue.
2.  Display a "lightbulb" icon 💡 or a similar indicator next to the measure in the query builder.
3.  On hover, show a tooltip with a helpful message: **"For a meaningful result, consider grouping this measure by: 'Child Category Name'."** (You can get the label "Child Category Name" by looking up the field `core:child_category_name` in the main `availableFields` list).
4.  Optionally, provide a button or link next to the hint that, when clicked, automatically adds the recommended dimension(s) to the query's `rowDimensions`.

This UX pattern educates the user and helps them build better reports without restricting their freedom or requiring complex server-side validation.

---

## 6. Error Handling
(... no changes here ...)

## 7. Appendix: Data Types and Operators
(... no changes here ...)

---

### **Final Result**

This finalized guide now perfectly reflects your new, robust, and scalable backend architecture. It provides clear, actionable instructions for the frontend developer, covering:
*   The simplified, unified data model (`availableFields`).
*   The new standardized ID format (`namespace:value`).
*   The explicit, data-driven way to populate filter controls (`resolution` object).
*   The new intelligent guidance system for building meaningful reports (`recommendedDimensions`).

You are now in an excellent position to hand this off and have the frontend work begin with confidence.
