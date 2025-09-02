/// # Pivot output formats — TABLE_ROWS vs PIVOT_MATRIX (design, SQL impact, API shapes)
///
/// Nice — this is the exact place where requirements, performance and implementation trade-offs meet. Below I explain the two output formats, what they mean for the server-side query vs post-processing, recommended defaults, how to express them in the request/response DTOs, sorting/pagination implications, and a short implementation plan + tests you should add.
///
/// ---
///
/// # 1) Meaning & high-level difference
///
/// **TABLE_ROWS**
///
/// * Flat, row-oriented result. Each row is one group (one tuple of grouping dimension values) with measure columns.
/// * Equivalent to a standard SQL `SELECT <group dims>, <agg1> AS a1, <agg2> AS a2 ... FROM pivot_grid_facts WHERE ... GROUP BY <group dims> ORDER BY ... LIMIT/OFFSET`.
/// * Simple to implement and paginate. Works for arbitrary dimension cardinalities.
///
/// **PIVOT_MATRIX**
///
/// * Two-dimensional cross-tab: rows = one or more `rowDimensions`, columns = a `columnDimension` (or composite), cells = aggregated measure(s) for that row×column cell.
/// * Produces a matrix-like structure the frontend can render as pivot table.
/// * Two implementation strategies:
///
///   1. **Server-side SQL pivot (conditional aggregate)** — push pivot to DB via dynamic SQL with `FILTER (WHERE columnDim = value)` or `SUM(CASE WHEN columnDim = value THEN measure ELSE NULL END)`. Requires a first-pass to discover distinct column keys (or user-supplied column keys), then build dynamic SELECT. Best when column cardinality is small/stable.
///   2. **App-side pivot (post-process TABLE_ROWS)** — run a GROUP BY on `(rowDims + columnDim)` to get flat rows then pivot in Java into matrix. Simpler, safer, and general-purpose; may fetch more rows and higher memory use.
///
/// ---
///
/// # 2) Does format change how we build the SQL?
///
/// **TABLE_ROWS**
///
/// * SQL: one-pass, single GROUP BY. Exactly what `PivotQueryBuilder.buildSelect(...)` already does.
/// * Pagination and ordering are straightforward and deterministic (tie-breaker `MIN(value_id)` applied when grouping).
///
/// **PIVOT_MATRIX**
///
/// * Options:
///
///   **A. App-side pivot (recommended default)**
///
///   * Build SQL like TABLE_ROWS but grouping on `rowDims + columnDim`.
///   * Example:
///
///     ```sql
///     SELECT row_dim1, row_dim2, column_dim,
///            SUM(value_num) AS sum_measure
///     FROM pivot_grid_facts
///     WHERE ... GROUP BY row_dim1, row_dim2, column_dim
///     ORDER BY ...;
///```
///   * Then in Java transform the flat rows into a matrix keyed by `rowDims` × `column` values.
///   * Pros: no dynamic SQL, no two-phase queries, flexible, easier to paginate rows (apply limit/offset to grouped rows after pivoting by row keys).
///   * Cons: transfers more rows when the cross product is large.
///
///   **B. Server-side pivot (conditional aggregates via dynamic SQL)**
///
///   * Two-phase:
///
///     1. `SELECT DISTINCT column_dim FROM pivot_grid_facts WHERE ... ORDER BY ... LIMIT N` to decide column keys (or ask client to supply column values).
///     2. Build `SELECT row_dims, SUM(value_num) FILTER (WHERE column_dim = 'key1') AS "key1", SUM(...) FILTER (WHERE column_dim='key2') AS "key2", ... FROM pivot_grid_facts WHERE ... GROUP BY row_dims` and execute.
///   * Pros: returns compact matrix from DB, less data movement when column cardinality is small.
///   * Cons: dynamic SQL complexity, dynamic columns, harder to type/serialize, requires two queries (one to collect keys), pagination semantics on row dimension must be applied after grouping results.
///
/// **Recommendation**
///
/// * Start with **App-side pivot** as default `PIVOT_MATRIX` behavior (easy, robust).
/// * Provide an opt-in parameter `pivotServerSide=true|false` (default `false`) — when true, do server-side pivot only if `columnDimension` cardinality is below a configured threshold or if client supplied explicit column values.
///
/// ---
///
/// # 3) API: request & response DTOs (concrete)
///
/// You already have `PivotQueryRequest`. Add/clarify these fields for pivoting:
///
/// ```java
/// public enum PivotOutputFormat { TABLE_ROWS, PIVOT_MATRIX }
///
///@Data@Builder
///publicclass PivotQueryRequest {///    @NonNll String templ;
///@NonNllStringtemplateVersionId;
///
///     // For TABLE_ROWS:
///     private List<String> dimensions; // fallback / backwards-compatible
///
///     // For PIVOT_MATRIX:
///     private List<String> rowDimensions;    // required for matrix
///     private List<String> columnDimensions; // usually 1, can be composite
///
///     private List<MeasureRequest> measures;
///     private List<FilterDto> filters;
///     private LocalDateTime from;
///     private LocalDateTime to;
///     private List<SortDto> sorts;
///     private Integer limit; // rows limit (for TABLE_ROWS or for matrix rows)
///     private Integer offset;
///
///     // server-side pivot hint (optional)
///@Builefault///privateBooleanpivotServerSide= false;
///
///     // If pivotServerSide==true, client may optionally provide columnValues for columnDimension(s):
///     // e.g. Map<String, List<String>> columnValues = Map.of("columnDimension", List.of("A","B","C"));
///     private Map<String, List<String>> coumnVaues;
///}
///```
///
/// **PivotQueryResponse** — two variants under one wrapper:
/// /// ```java
///@Data@Builder
///publicclass PivotQueryResponse {
///     private long total; // number of groups (for pagination)
///     private Map<String,Object> meta; // template UIDs, format, etc.
///     private List<ColumnDto> columns; // for TABLE_ROWS: describes columns
///     private List<Map<String, Object>> rows; // for TABLE_ROWS: list of maps row->value
///
///     // Matrix-specific:
///     private List<RowHeaderDto> rowHeaders;       // describing each matrix row (keys)
///     private List<String> columnHeaders;         // column keys (distinct columnDimension values)
///     private List<List<Object>> cells;           // matrix [rowIndex][colIndex] = vale (ornull)
///}
///```
///
/// Examples:
///
/// **TABLE_ROWS response (JSON)**:
/// /// ```json
///{
///   "total": 61,
///   "meta": {...},
///   "columns":[{"id":"team_uid","label":"Team","dataType":"team_uid"},{"id":"sum_completion","label":"...","dataType":"value_num"}],
///   "rows":[{"team_uid":"A0zx1...","sum_completion"200}, ... ]
///}
///```
///
/// **PIVOT_MATRIX response (app-side pivot)**:
///
/// ```json
///{
///   "meta": {...},
///   "rowHeaders": [{"key":{"region_uid":"R1"}},{"key":{"region_uid":"R2"}}],
///   "columnHeaders": ["setA","setB","setC"],
///   "cells": [
///[ 10, 20, null ],   // row0
///[ 5, nul, 12 ]     // row1
///]
///}
///```
///
/// Or an alternative representation `rows` where each row is `{ rowKey: {...}, cells: { "coKey1": val, "colKey2": val ... }}` — choose whichever is easier for your front-end; both are fine.
///
/// ---
///
/// # 4) Sorting & Pagination differences
///
/// **TABLE_ROWS**
///
/// * Sorting as you already support: by dimension columns or by aggregate aliases.
/// * Pagination via `LIMIT`/`OFFSET` on grouped rows. Tie-breaker `MIN(value_id)` guarantees deterministic order when sorting by aggregates.
///
/// **PIVOT_MATRIX (app-side pivot)**
///
/// * We paginate **rows** (the row dimension axis). Sorting must be defined on row headers. Sorting options:
///
///   * sort by a row dimension value (e.g., region name),
///   * sort by an aggregate total across columns (server computes sum of the measures across all column keys for that row),
///   * sort by a specific column cell value (row's value for column key X).
/// * Implementation choices:
///
///   * If doing app-side pivot, compute ordering in Java using aggregated numbers; then paginate row headers accordingly.
///   * If doing server-side pivot, you must include ordering in SQL (either `ORDER BY <sum of filters>` or `ORDER BY <column-specific FILTER aggregate>`), then apply `LIMIT/OFFSET`.
///
/// **Recommendation:** Keep pagination behavior consistent: `limit`/`offset` always apply to row axis (not to number of columns). Document this clearly.
///
/// ---
///
/// # 5) Edge cases & type handling
///
/// * **Missing cells** → return `null` or `0` depending on measure semantics. Recommend `null` (no data), let front-end decide to display `0` if desired.
/// * **Multiple measures** → each cell could be object with multiple measure values. For simplicity, when measures>1 produce cells as maps: `{"measureAlias1": 10, "measureAlias2": 2}` or provide parallel matrices per measure.
/// * **High cardinality columnDimension** → server-side pivot is dangerous (explosion). Enforce a configurable cap (e.g., 200 unique column keys), or require client to supply explicit `columnValues` and refuse otherwise.
/// * **Security / resource safety** → always validate requested dims and measures against `PivotMetadataService` to ensure they map to valid `factColumn` and allowed aggregations.
///
/// ---
///
/// # 6) Implementation plan (practical incremental steps)
///
/// 1. **API contract**: extend `PivotQueryRequest` and `PivotQueryResponse` as above.
/// 2. **Default behavior**: implement `PIVOT_MATRIX` as **app-side pivot**.
///
///    * Query: `GROUP BY rowDims + columnDim` (use existing builder by passing `dimensions = concat(rowDims, columnDim)` and measures aggregated accordingly).
///    * Post-process: group flat rows by rowKey into matrix.
/// 3. **Server-side pivot opt-in**:
///
///    * Add `pivotServerSide` flag.
///    * If true:
///
///      * Run `SELECT DISTINCT columnDim FROM ...` to get `columnKeys` (respect limit or columnValues if provided).
///      * Build dynamic `Select` with `FILTER` aggregates for each columnKey using jOOQ.
/// 4. **Sorting/paging**:
///
///    * For app-side pivot, apply ordering in Java before pagination (compute totals as needed).
///    * For server-side pivot, incorporate ORDER BY into generated SQL using conditional aggregates.
/// 5. **Tests**:
///
///    * Unit tests: small synthetic data to verify app-side pivot produces expected matrix shape and values.
///    * Integration tests (Testcontainers): verify server-side pivot SQL generation yields correct results for small cardinalities and verify safety cap triggers when too many column values.
///    * Tests for sorting by row total and by specific column cell.
///
/// ---
///
/// # 7) Quick examples (SQL snippets)
///
/// **App-side pivot (query builder call)** — Group rows then pivot in Java:
///
/// ```sql
/// SELECT region_uid, settlement_type, SUM(value_num) AS sum_nets
/// FROM pivot_grid_facts
/// WHERE etc_uid = 'etc:Ci...'
///   AND deleted_at IS NULL
/// GROUP BY region_uid, settlementtype
/// ORDER BY region_uid
///```
///
/// Post-process this set into matrix rows keyed by `region_uid` with columns `settlement_type` values.
///
/// **Server-side pivot (dynamic SQL)** — after discovering column values `['A','B','C']`:
///
/// ```sql
/// SELECT region_uid,
///   SUM(value_num) FILTER (WHERE settlement_type = 'A') AS "A",
///   SUM(value_num) FILTER (WHERE settlement_type = 'B') AS "B",
///   SUM(value_num) FILTER (WHERE settlement_type = 'C') AS "C"
/// FROM pivot_grid_facts
/// WHERE deleted_at IS NULL
/// GROUP BY regio_uid
/// ORDER BY region_uid
///```
///
/// ---
///
/// # 8) Tests to add right away (minimum set)
///
/// * TABLE_ROWS:
///
///   * group by single dim, multiple measures; ordering & deterministic pagination.
///   * `IN` filters (including empty IN => falseCondition).
///   * invalid type in filter -> IllegalArgumentException.
///
/// * PIVOT_MATRIX (app-side):
///
///   * pivot by 1 column, single measure — verify matrix matches expected (rows, columns, nulls).
///   * pivot by composite columnDimensions -> verify composite keys handled.
///
/// * PIVOT_MATRIX (server-side):
///
///   * request with `pivotServerSide=true` and `columnValues` provided -> correct shaped SQL executed and results OK.
///   * request with `pivotServerSide=true` and high distinct count -> error / refused (safety cap).
///
/// * Sorting:
///
///   * PIVOT_MATRIX sort by row total (descending) yields correct top rows.
///   * Sorting by specific column cell (e.g., column 'A') works.
///
/// ---
///
/// # 9) Final recommendation & next action
///
/// * Implement `PIVOT_MATRIX` initially with **app-side pivot**. It's robust, quick, and avoids dynamic SQL complexity.
/// * Add `pivotServerSide` (opt-in) later if you find frontend needs server-produced wide pivots and the column dimension cardinality is low.
/// * Update `PivotQueryService` to:
///
///   * detect requested `format`,
///   * call `PivotQueryBuilder` to get flat rows for matrix (group by rowDims+columnDim),
///   * pivot in Java and produce the `PivotQueryResponse` matrix structure.
/// @author Hamza Assada
/// @since 01/09/2025
package org.nmcpye.datarun.analytics.pivotg;
