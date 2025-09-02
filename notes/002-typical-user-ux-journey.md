# Front-end user flow for a pivot table (functionality only)

Below is a concise, step-by-step user/UX flow for a pivot table UI that plugs into your backend (template-first / UID-native model). 
I focus only on **functional** behavior — what the UI does and why — 
and how it uses the metadata your backend returns.

---

## 0) Landing on the Pivot page

* User chooses a **form template** and **template version** (required).

    * UI calls `GET /api/v1/analytics/pivot/metadata?templateId=...&templateVersionId=...`
    * Metadata drives all UI controls (fields, labels, allowed aggregations, `factColumn`, `dataType`, `extras`).

---

## 1) Build the query via metadata-driven controls

Use the metadata to populate and constrain the UI:

* **Field / Measure picker**

    * Show available *measures* (template fields) and *core dimensions* from `PivotFieldDto`.
    * For each field show: label, dataType, `aggregationModes` (allowed), `factColumn`.
    * Measure builder UI:

        * Choose an element (e.g. from measures list). Internally this is `etc:<uid>` (for template fields) or `de:<uid>` (if frontend exposes global).
        * Choose aggregation (only those in `aggregationModes`).
        * Optional alias input (UI shows auto-rename policy or validation if duplicate).
        * Optional `distinct` toggle when applicable (COUNT).
        * If the field is an option/reference, show an **Option value selector** populated from `extras.optionSetUid` (to allow per-measure scoping by an option).
* **Dimension picker**

    * Pick 0..N dimensions (flat list), or pick row / column dimension sets for `PIVOT_MATRIX`.
    * Show label and `factColumn` for each dimension (e.g., `team_uid`, `etc_uid`, `submission_completed_at`).
* **Filter builder**

    * Field selector populated by metadata (reuse measures & dimensions plus aliases).
    * Operator dropdown driven by `dataType`:

        * `value_num` → `=, !=, >, <, >=, <=, IN`
        * `value_text` → `=, !=, LIKE, ILIKE, IN`
        * `value_bool` → `=, !=`
        * `value_ts` → `between, >=, <=, =`
        * `option_uid` / `value_ref_uid` → `=, !=, IN`
    * Value input UI depends on dataType:

        * Numeric input for `value_num`
        * Date/time picker for `value_ts`
        * Multi-select dropdown for `IN` on option/value types
        * For `IN` the picker should enforce non-empty lists (or warn user)
    * Per-filter validation:

        * Validate UIDs where required (use same validation rule as backend).
        * Prevent invalid operator/dataType combos client-side.
* **Time window** (submission\_completed\_at): date range pickers.
* **Sort builder**

    * Choose field or measure alias; choose asc/desc.
    * If sort references aggregated alias, UI should ensure the alias exists (or show an error).
* **Pagination & format**

    * Limit & offset controls (or page size + page number).
    * Format selector: `TABLE_ROWS` vs `PIVOT_MATRIX`. (See section below about differences.)

---

## 2) Local validation before send

* Ensure:

    * TemplateId & templateVersionId present.
    * Every `Measure` has allowed aggregation.
    * Aliases are unique (or apply auto-rename policy).
    * All UID inputs are valid format.
    * Filter values match expected type (numbers parse, dates valid).
* Show user-friendly messages for all problems.

---

## 3) Request construction

* Construct `PivotQueryRequest`:

    * `templateId`, `templateVersionId`
    * `dimensions` or `rowDimensions`/`columnDimensions`
    * `measures` (each with `elementIdOrUid` like `etc:<uid>` or `de:<uid>`, aggregation, alias, distinct, optional `optionId`)
    * `filters` (each `{ field: <fact or alias>, op: <op>, value: <value> }`)
    * `from` / `to`, `sorts`, `limit`, `offset`, other flags (e.g., `autoRenameAliases`)
* `format` query param (TABLE\_ROWS or PIVOT\_MATRIX).

---

## 4) Submit & receive response

* POST `/api/v1/analytics/pivot/query?format=TABLE_ROWS` (or `PIVOT_MATRIX`)
* Backend returns `PivotQueryResponse`:

    * For **TABLE\_ROWS**: a tabular result set (columns metadata + rows).
    * For **PIVOT\_MATRIX**: structured representation (row headers, column headers, matrix of cell values) or a canonical object your front-end knows how to render.

---

## 5) Render results in UI

* **TABLE\_ROWS**

    * Render a grid/table using column metadata (id, label, dataType).
    * Support pagination controls using `total` and `limit/offset`.
    * Allow sorting by clicking column headers (re-issue query with updated `sorts`).
    * Where a column is an id (uid), optionally resolve label on the fly (via live lookup) to show friendly names.
* **PIVOT\_MATRIX**

    * Render axis headers (rows & columns), and numeric or value cells.
    * Support expanding/collapsing row groups (if hierarchical rows).
    * Provide export and drill-down actions from cells (see next).

---

## 6) Interactions / drill-downs & follow-ups

* **Drill-down**: click a row/cell → open a filtered query that includes that row's grouping values as additional filters (e.g., `team_uid = X` and `etc_uid = Y`) and either:

    * show raw submissions (navigation to a detail view), or
    * show a new pivot scoped to that selection.
* **Save view / share**: allow users to save query definition (templateUid + version + dims + measures + filters + sorts + format). Saved views store the request object (and alias policy).
* **Export**: CSV / Excel / JSON export of TABLE\_ROWS; for PIVOT\_MATRIX export as CSV with pivoted headers or as structured JSON.
* **Refresh / Live updates**: optional refresh to re-run (user triggers after ETL/MV refresh).
* **Validation errors from server**: display the message (e.g., invalid UID, invalid aggregation for field, ambiguous alias) and highlight offending controls.

---

## 7) Special UI considerations driven by backend model

* **Template mode vs Global lookups**

    * If user selects a template field (etc:<uid>) the UI should assume `factColumn = etc_uid`. If user wants global analysis, allow them to pick the canonical data element (de:<uid>) — UI should offer a way to switch to global view if metadata exposes `de` items.
* **Per-measure scoping**

    * If a measure needs scoping (e.g., count only option `O:xyz`), show that option input inside the measure builder and send `optionId` with the measure. Backend will apply `filterWhere(scope)` to the aggregate.
* **Category & repeats**

    * If a template field is a category for a repeat, expose UI that lets user choose to group by `child_category_uid` or `parent_category_uid` and show repeat path context (from `etc.template_repeat_path`).
* **Aliases**

    * Show user-friendly alias validation; if auto-rename is enabled explain how duplicates will be altered (append `_1`, `_2`, ...).
* **Filter by alias**

    * Allow filters to reference either `factColumn` or existing measure aliases; UI should disambiguate (e.g., a dropdown that lists `team_uid` and `sum_population (alias)`).

---

## 8) Error messages & helpful hints (UX)

* If server rejects because of invalid UID: show "Invalid UID: `<value>` — please select from the field picker."
* If user requests aggregation not supported: "Aggregation `<AGG>` not allowed on `<field>` — choose from: `<allowed list>`."
* If `IN` is empty: prevent send and show "Select at least one value".
* If no dimensions and only measures: show whether result is a single aggregated row (TOTAL) vs grouped rows (when dimensions present).

---

## Example quick happy-path

1. User picks template Tcf3Ks9ZRpB + version fb2... → UI fetches metadata.
2. User adds measure: pick `etc:CiEZemZ7mlg` → allowed aggs show SUM/AVG/… → user picks SUM and alias `sum_completion`.
3. User picks dimension `team_uid`.
4. User adds filter `submission_completed_at >= 2025-08-01`.
5. User selects format `TABLE_ROWS`, limit 50, sorts by `sum_completion` desc.
6. User clicks Run → UI sends request → receives rows and total → renders table.
7. User clicks a team row → UI runs a new query scoped to `team_uid = X` for drill-down.

---

## Final notes (functional scope)

* The metadata DTO (`PivotFieldDto`) is the single source for building UI controls: `dataType`, `factColumn`, `aggregationModes`, and `extras` drive the available operators, inputs, and option pickers.
* The front end needs only to validate input (UID shape, types, non-empty INs, alias uniqueness) and then submit the `PivotQueryRequest`. All enforcement (exact SQL mapping, per-measure `filterWhere`, typed binding) is server-side.
* Keep the UI controls conservative: disable unsupported ops, show allowed aggregations, and avoid exposing internal column names (use `factColumn` only internally).
