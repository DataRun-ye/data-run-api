# 🛠 Frontend Dev Roadmap for Analytics Grid

## Phase 0 — Setup & Orientation

* Connect to the API with JWT auth.
* Test a **metadata call** manually (e.g. using Postman or cURL).
* Goal: *You can see `availableFields` coming back from the server.*

---

## Phase 1 — Static Table

1. **Discover fields**

    * Render a list of available dimensions and measures from `availableFields`.
    * Start with a hardcoded templateId/templateVersionId.
2. **Build & send a query** (`POST /query?format=TABLE_ROWS`).

    * Hardcode one dimension + one measure for now.
3. **Render raw table**

    * Columns from `response.columns`.
    * Rows from `response.rows`.
    * Just dump it into your grid (e.g. ag-Grid, MUI DataGrid, TanStack Table).
      👉 At this point, you’ve got a working static table.

---

## Phase 2 — Interactive Basics

1. **Dimension/measure pickers**

    * Populate dropdowns with `availableFields`.
    * User chooses → build query dynamically.
2. **Pagination + sorting**

    * Add `limit`, `offset`, and `sorts` handling.
    * Wire them to grid controls.
      👉 Now the table feels alive, not just static.

---

## Phase 3 — Filtering

* Use `field.extras.resolution` to fetch filter options dynamically (e.g. `/api/v1/teams`).
* Add filter controls for UID/text/number/date based on `dataType`.
* Include chosen filters in your query’s `filters` array.
  👉 You now support full drill filtering.

---

## Phase 4 — Pivot Mode

1. Switch query to `format=PIVOT_MATRIX`.
2. Support **rowDimensions** and **columnDimensions**.
3. Render a pivot grid (row headers × column headers × cells).
   👉 This is the first “wow” moment: cross-tab views.

---

## Phase 5 — Advanced Interactivity

* **Drill-downs**: clicking a cell → add filter + requery.
* **Saved views**: POST/GET from `/views`.
* **Recommended dimensions**: UI hints from metadata.
  👉 This is where your grid becomes a *reporting tool*, not just a table.

---

## Phase 6 — Polish & Resilience

* Handle errors gracefully with the `details` array.
* Add tooltips, icons, empty states.
* Optimize with caching (don’t re-fetch metadata too often).
* Prep for advanced features: guided analytics, chart integration.

---

## 📊 Milestones for Analytics Grid

### **Milestone 1: Foundations (MVP Table)**

* [ ] Setup JWT auth for `/api/v1/analytics`.
* [ ] Fetch **metadata** → populate field pickers.
* [ ] Build simple query (`TABLE_ROWS`).
* [ ] Render static table (`columns` + `rows`).
* [ ] Add pagination (limit/offset).

---

### **Milestone 2: Interactivity**

* [ ] Implement filtering (dynamic via `resolution`).
* [ ] Add sorting (query `sorts`).
* [ ] Enhance pagination controls (UI navigation).

---

### **Milestone 3: Pivot Grid**

* [ ] Switch to `PIVOT_MATRIX` format.
* [ ] Render row + column headers.
* [ ] Map cells to intersections.

---

### **Milestone 4: Advanced UX**

* [ ] Drill-down on cell click (clone + add filters).
* [ ] Save / load views (`/analytics/views`).
* [ ] Guided analytics (detect & display `recommendedDimensions`).

---

### **Milestone 5: Polish**

* [ ] Error handling (map API `details` → UI).
* [ ] Consistent UI controls for data types (NUMERIC, TEXT, BOOLEAN, DATE, UID, OPTION).
* [ ] UX refinements (tooltips, loading states, empty states).

---

# 📂 Repository Proposal

**Name:**
`datarun-analytics-grid`
(short, scoped, explains it’s the grid/pivot component for Datarun)

---

# 📘 README (starter)

```markdown
# Datarun Analytics Grid

A front-end component for exploring analytics data from the **Datarun platform**.  
It provides a table and pivot-grid interface powered by the `/api/v1/analytics` endpoints.

## 🚀 Features
- Fetches metadata dynamically to build field pickers
- Query builder for dimensions, measures, filters, and sorts
- Table mode (`TABLE_ROWS`) and Pivot mode (`PIVOT_MATRIX`)
- Interactive features: filtering, sorting, pagination
- Advanced UX: drill-downs, saved views, guided analytics hints
- Strong error handling with UI feedback

## 🗂️ Roadmap
Development is organized into milestones:
1. **Foundations (MVP Table)** – metadata, basic table, pagination  
2. **Interactivity** – filters, sorting, navigation  
3. **Pivot Grid** – matrix rendering with row/column headers  
4. **Advanced UX** – drill-downs, saved views, guided analytics  
5. **Polish** – error handling, data-type UI mapping, refinements  

## 🔗 Integration
This module is designed to slot into the broader **Datarun** system.  
All queries and metadata come from the analytics API: `/api/v1/analytics`.

## 📄 License
MIT
```
