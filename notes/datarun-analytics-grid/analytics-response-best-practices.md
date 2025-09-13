# Core principles (always)

1. **Schema-first**: Every response must include a machine-readable schema describing columns/fields (id, label, type, format). Do not rely on client-side guesswork.
2. **Stable IDs vs SQL keys**: Separate the *logical id* (stable API id used by clients) from internal SQL/sanitized alias. If you must include SQL alias, mark it internal or write-only. Prefer returning rows keyed by logical ids.
3. **Explicit nulls**: Include keys for every column in each row; set `null` for missing values. Don’t omit keys — omission is ambiguous.
4. **Typed values**: Provide `dataType` for each column (NUMERIC, TEXT, DATE, UID, BOOLEAN, GEO, etc.) and serialize values consistently (e.g., ISO8601 for timestamps).
5. **Metadata block**: Include query echo (template/version, dimensions/measures used), execution stats (duration, rowCount, totalCount), and provenance (who/when).
6. **Pagination & totals**: If results are paged, return `limit`, `offset`, and `total` (or `hasMore`). `total` should be authoritative or clearly best-effort.
7. **Human-friendly labels & locales**: Provide `label` fields and optional `labels` map keyed by locale (`labels: { en: "...", ar: "..." }`).
8. **Aggregation/mode hints**: For measure columns include `aggregation` used and acceptable `aggregationModes` for metadata.
9. **Deterministic ordering**: Return `columns` array for column order — do not rely on map ordering.
10. **Errors & validation**: Use structured error objects with machine codes and human messages; for validation errors return details about which request field failed.
11. **Compact vs verbose**: Offer both compact (array-of-arrays) and verbose (array-of-objects) formats; choose one per API or permit `format=` param. Always document which is default.

---

# Common response shapes

### 1) TABLE\_ROWS (preferred for simple tabular exports)

* **When**: simple result set: dimensions + measures.
* **Schema**: `columns[]` (ordered), each `{ id, label, dataType, formatHint?, aggregation? }`.
* **Rows**: either `rows: Array<Record<string, any>>` (object per row with keys=column ids) **or** `rows: Array<Array<any>>` (array form matching columns order). Prefer object rows for readability; provide array form for compactness.
* **Meta**: `total`, `limit`, `offset`, `query`, `executionMs`, `warnings`.

Example (object rows):

```json
{
  "meta": { "format":"TABLE_ROWS", "executionMs": 120, "query": { "templateId":"T1" } },
  "columns": [
    {"id":"core:org_unit_uid","label":"Org Unit","dataType":"UID"},
    {"id":"OPe8SPUU2AI_sum","label":"Total Population (SUM)","dataType":"NUMERIC","aggregation":"SUM"}
  ],
  "rows": [
    {"core:org_unit_uid":"ou_123","OPe8SPUU2AI_sum":81},
    {"core:org_unit_uid":"ou_456","OPe8SPUU2AI_sum":400},
    {"core:org_unit_uid":null,"OPe8SPUU2AI_sum":115}
  ],
  "total": 3503,
  "limit": 50,
  "offset": 0
}
```

Notes:

* Clients should always use `columns` to derive display order and keys.
* Provide both optional `rowsAsArrays: true` or `format=ROWS_ARRAYS` if bandwidth matters.

---

### 2) PIVOT\_MATRIX (multi-dimensional pivot)

* **When**: user asks for matrix / pivot (row dims × column dims × measures).
* **Schema**: rowHeaders (ordered lists per row), columnHeaders, `measureAliases`, `cells` as nested arrays of objects (or values).
  Example:

```json
{
  "meta": {"format":"PIVOT_MATRIX"},
  "rowDimensionNames":["region"],
  "columnDimensionNames":["year"],
  "measureAliases":["pop_sum"],
  "rowHeaders":[["North"],["South"]],
  "columnHeaders":[["2021"],["2022"]],
  "cells":[
    [ {"pop_sum":100}, {"pop_sum":120} ],
    [ {"pop_sum":200}, {"pop_sum":180} ]
  ]
}
```

Notes:

* Keep headers and cells in sync; use empty object `{}` when a cell has no value.
* Provide explicit measure aliases so clients can map cells to measure names.

---

### 3) TIME SERIES / LINEAR AGGREGATES

* **When**: single-dimension (time) + measure.
* **Schema**: `series[]` each `{ name, points: [{ ts: ISO8601, value: number|null }] }`.
  Example:

```json
{
  "meta":{"format":"TIME_SERIES","timezone":"UTC"},
  "series":[
    {"name":"Total Population","points":[{"ts":"2024-01-01T00:00:00Z","value":120}, ...]}
  ]
}
```

Notes:

* Use consistent timestamps, explicitly document timezone.
* Include `interval`/`resolution` if bucketed (daily/monthly).

---

### 4) AGGREGATE SUMMARY (single number or small set)

* **When**: KPI cards (totals, average).
* **Format**:

```json
{ "meta":{"format":"SUMMARY"}, "values":[{"id":"total_population","label":"Total population","value":12345}] }
```

---

### 5) TOP-N / GROUPED LIST

* **When**: top categories by measure.
* **Format**:

```json
{
  "meta":{"format":"TOP_N","total":100},
  "columns":[{"id":"org","label":"Org Unit"},{"id":"pop","label":"Population"}],
  "rows":[{"org":"ou_1","pop":1000}, {"org":"ou_2","pop":900}]
}
```

---

### 6) GEO / SPATIAL RESPONSES

* **When**: geometry/polygons or lat/lng points.
* **Format**: include `geoType`, `crs`, and `geometry` (WKT or GeoJSON) and optionally `properties`.

```json
{ "columns":[{"id":"geometry","dataType":"GEO","formatHint":"GeoJSON"}], "rows":[{"geometry":{ "type":"Point","coordinates":[35.3,15.3]}}] }
```

Notes:

* Use GeoJSON for compatibility and include `crs` if non-default.

---

# Metadata fields to include (recommended)

* `columns[]` with fields:

    * `id` (logical id), `label`, `dataType` (enum), `formatHint` (currency/date/percent), `aggregation` (if measure), `isDimension` (bool), `displayGroup` (optional), `extras` (free-form).
* `meta` object:

    * `format`, `templateId`, `templateVersionId`, `executionMs`, `generatedAt` (ISO), `queryHash` (for caching), `warnings[]`, `traceId` (for logs).
* Pagination: `limit`, `offset`, `total`, `hasMore`.
* Provenance / Security: `requestedBy`, `rolesMask` (optional), `acl` (if necessary).
* Statistical hints: `sampled: true/false`, `sampleFraction`, `estimateConfidence` (if approximate).

---

# Contract & naming advice

* Keep `columns[].id` stable and URL/JSON-safe (you may keep colons `:` but then ensure server and client can use them reliably; many systems prefer safe names like `core:org_unit_uid` but be careful with SQL aliasing). If you must sanitize for SQL, either:

    * add a `key`/`alias` server-side (non-serialized or write-only) that server uses for lookup; or
    * set `columns[].id` to the sanitized alias and provide `columns[].logicalId` for client-friendly stable id.
* Document ordering semantics clearly (client must follow `columns` order; `rows` arrays must match columns when using array format).

---

# Error format (structured)

Use a predictable, machine-friendly error object:

```json
{
  "error": {
    "code":"INVALID_QUERY",
    "message":"Invalid aggregation provided for field X",
    "details":[{"field":"measures[0].aggregation","reason":"notSupported"}],
    "traceId":"abcd-1234"
  }
}
```

Status codes: 400 for bad request, 422 for validation, 401/403 for auth/perm, 500 for server.

---

# Performance & operational considerations

* **Stream large results**: support `limit`/`offset` and optionally cursor-based pagination. For huge exports provide async job API with download link.
* **Execution metrics**: include `executionMs` and `rowsScanned` so consumers can observe cost.
* **Caching**: return `queryHash` to let clients cache identical queries.
* **Sampling flags**: if results are approximate (pre-aggregated), indicate `sampled: true` and `sampleRate`.
* **Rate limiting & throttling hints**: provide `retryAfter` in HTTP headers on throttling.

---

# Small checklist for API correctness (tests)

1. For any query, `columns` length equals number of keys used in `rows` objects and each row contains all column ids (maybe null).
2. `rows` ordering corresponds to `columns` ordering when array-form is used.
3. `dataType` values match produced JS types (NUMERIC → number, DATE/TIMESTAMP → string formatted ISO).
4. Pagination `total` is correct or clearly labeled as estimate.
5. Error responses follow structured error schema.

---

# Quick recommendation for your project

* Use `TABLE_ROWS` object-rows as default. It’s safe and readable.
* Always return `columns` with `id`, `label`, `dataType`, and keep `id` stable. If you must alias for SQL, hide the `key` from clients (`@JsonIgnore`) and use it server-side only.
* Provide `meta.executionMs` and `meta.generatedAt` — super helpful for debugging.

---

# Principles (short)

1. **Schema-first & explicit:** Every response ships a machine-readable schema (column list) so clients never guess structure.
2. **Stable public contract:** Keep the API’s logical IDs stable. Internals (SQL aliases) must be separate and not required for clients.
3. **Deterministic order + full keys:** Return `columns[]` (ordered) and ensure each row contains *all* column keys (use `null` not omission).
4. **Typed & serialised values:** Include `dataType` and `formatHint`. Dates → ISO8601, numbers → JSON numbers, booleans → JSON booleans.
5. **Small, composable shapes:** Offer a small set of canonical response shapes (TABLE\_ROWS, PIVOT\_MATRIX, TIMESERIES, SUMMARY). Keep them consistent.
6. **Evolve safely:** Add new fields only in a backward-compatible way; use API versioning and `meta.version` for contract changes.
7. **Observable & testable:** Include execution metadata (timing, traceId) to make debugging and regression tests deterministic.

---

# Canonical response shapes (recommended defaults + minimal required fields)

## TABLE\_ROWS (default — human & program friendly)

* Use this as your primary tabular format.
* Required: `meta`, `columns[]`, `rows[]`.
* `columns[]` items: `{ id, label, dataType, formatHint?, isDimension?, aggregation? }`
* `rows[]` options:

    * **Verbose (recommended default):** `rows: Array<Record<string, any>>` — each row is an object keyed by `columns[].id`.
    * **Compact (optional):** `rows: Array<Array<any>>` — arrays must strictly match `columns` order; include `rowsAsArrays: true` in `meta`.
* Pagination: include `limit`, `offset`, `total` or `cursor` for cursor-based pages.

Example:

```json
{
  "meta": { "format":"TABLE_ROWS", "executionMs": 34, "generatedAt":"2025-09-11T12:00:00Z", "queryHash":"abc123" },
  "columns":[
    {"id":"org:uid","label":"Org Unit","dataType":"UID"},
    {"id":"pop_sum","label":"Population (SUM)","dataType":"NUMERIC","aggregation":"SUM"}
  ],
  "rows":[
    {"org:uid":"ou_1","pop_sum":81},
    {"org:uid":"ou_2","pop_sum":400},
    {"org:uid":null,"pop_sum":115}
  ],
  "limit":50,"offset":0,"total":3503
}
```

## PIVOT\_MATRIX (multi-dim)

* Good when UI needs row × column matrix.
* Minimal fields: `meta`, `rowDimensionNames`, `columnDimensionNames`, `measureAliases`, `rowHeaders`, `columnHeaders`, `cells`.
* `cells` should mirror headers exactly; use empty object `{}` for missing cell.

## TIME\_SERIES

* Use `series[]` as `{ name, points:[{ts:ISO,value}] }`
* Include `timezone`, `interval` (bucket size), `sampled` flag if aggregated.

## SUMMARY / KPI

* Compact `{ meta, values:[{id,label,value,units}] }` for cards or single metrics.

---

# Metadata & tracing (must-have)

Always include a `meta` object containing:

* `format` (TABLE\_ROWS/PIVOT\_MATRIX/TIMESERIES)
* `generatedAt` (ISO timestamp)
* `executionMs` (server-side execution time)
* `query` or `queryHash` (echo/ident) for caching & debugging
* `limit`/`offset` or `cursor`/`hasMore`
* `traceId` (for correlating logs/traces)
* `version` (API schema version)
* `warnings[]` (non-blocking issues)

Why: traceability, reproducible debugging, caching, client-side telemetry.

---

# IDs, aliases, and keys — contract rules

* Public `columns[].id` = logical, stable identifier (used by clients and for row keys).
* Internals: if SQL requires sanitized aliases, keep them server-side only. Two safe patterns:

    1. **Hidden key field**: `ColumnDto.key` present in server object but annotated `@JsonIgnore` or write-only so not exposed. Server uses it to read DB; client sees only `id`.
    2. **Expose alias separately**: `columns[].alias` (documented). Prefer hiding internals for clean contracts.
* **Never** let DB-specific naming leak into the logical API ids clients use.

---

# Nulls vs omissions

* **Always include every column key in each row**, with `null` for missing data. Omitting keys makes client logic fragile.

---

# Data typing & format hints

* Column-level `dataType` (enum): `NUMERIC`, `TEXT`, `DATE`, `TIMESTAMP`, `UID`, `BOOLEAN`, `GEO`, `PERCENT`, `CURRENCY`.
* Optional `formatHint`: `{ style: "currency", currency: "USD" }` or `"isoDate"`.
* Dates: use ISO8601 with timezone or with `Z`. Document timezone in `meta.timezone`.

---

# Pagination & large results

* Prefer **cursor-based** pagination for large or changing result sets; fallback to offset for simplicity.
* Return `limit`, `offset` and `total` for offset pagination; for cursor: `nextCursor`, `hasMore`.
* For very large exports: provide an async export job (POST query → returns job id → client polls or downloads file).
* Support HTTP `Range` or streaming NDJSON for massive streaming workloads.

---

# Error & validation model

Use structured errors:

```json
{
  "error": {
    "code":"INVALID_QUERY",
    "message":"Invalid aggregation for field 'age'",
    "details":[{"path":"measures[0].aggregation","reason":"notSupported"}],
    "traceId":"trace-123"
  }
}
```

* Use `400` for bad requests, `422` for validation, `403`/`401` for auth, `429` for rate limiting, `500` for server.
* Include `traceId` for cross-system debugging.

---

# Performance & operational best practices

* **Limit default result size** (e.g., default limit=100, max limit with guardrails).
* **Execution cost metadata**: `rowsScanned`, `executionMs`, `planCost` (optional).
* **Caching**: include `queryHash` so clients/proxies can cache responses.
* **Rate limiting & throttling headers**: `X-RateLimit-*`, `Retry-After`.
* **Async job pattern** for heavy queries (respond quickly with job id + status endpoint).
* **NDJSON** for stream-friendly large results; `Content-Type: application/x-ndjson`.
* **Compression**: enable gzip/brotli and document `Content-Encoding`.
* **Sampling flags**: if approximate, include `sampled: true` and `sampleFraction`.

---

# Security & governance

* Enforce row-level filters / ACLs server-side. Include `requestedBy` in `meta` only if permitted.
* PII masking: tag sensitive columns in metadata (`columns[].sensitivity: "PII"`) and refuse exports if policy blocks it.
* Auditing: log query, user, traceId, execution time for compliance.

---

# Versioning & evolution

* Version the API (e.g., `/v1/analytics/...`) and include `meta.version`.
* For schema changes prefer:

    * adding optional fields (backward-compatible),
    * or new endpoint/`version` bump for breaking changes.
* Maintain schema examples in OpenAPI + JSON Schema; run contract tests.

---

# Client-side guidance (what clients must do)

* Use `columns[]` for header labels and ordering; do not infer order from row object keys.
* Accept missing values as `null`.
* Prefer object-row format over array-rows unless bandwidth-critical.
* Respect `meta.executionMs` and `traceId` for diagnostics.
* Fail gracefully if unknown columns appear (ignore extra fields).

---

# Testing checklist (automated)

1. **Schema tests**: `columns` presence, `rows` keys cover all `columns[].id`.
2. **Type tests**: each column value matches `dataType` (or `null`).
3. **Pagination tests**: `limit/offset` round trips, cursors work.
4. **Streaming tests**: NDJSON integrity, gzipped response decoding.
5. **Error tests**: validation errors include path + reason.
6. **Regression test**: same query yields same `queryHash` and compatible shape.

---

# JSON Schema & OpenAPI snippet (minimal)

Use this in your OpenAPI `components.schemas` (sketch):

```yaml
TableRowsResponse:
  type: object
  properties:
    meta:
      type: object
      properties:
        format: { type: string }
        generatedAt: { type: string, format: date-time }
        executionMs: { type: integer }
    columns:
      type: array
      items:
        type: object
        properties:
          id: { type: string }
          label: { type: string }
          dataType: { type: string }
    rows:
      type: array
      items:
        type: object
        additionalProperties: true
    limit: { type: integer }
    offset: { type: integer }
    total: { type: integer }
```

---

# Migration & evolution advice

* Add `meta.version` with semantic versioning of the response contract.
* For big changes: run dual-write on server (old + new shape), toggle clients gradually.
* Keep a compatibility test suite that runs against the API on CI.

---

# Quick checklist you can adopt now

* [ ] Default to TABLE\_ROWS object rows.
* [ ] Ensure `columns[].id` is stable and rows include keys for each id (even `null`).
* [ ] Add `meta.{generatedAt,executionMs,traceId,queryHash}`.
* [ ] Implement strict JSON Schema and contract tests.
* [ ] Provide async job API and NDJSON for large exports.
* [ ] Add rate-limit & tracing headers.
