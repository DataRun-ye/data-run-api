I understand. It seems that we're both getting confused about our current standing due to the length of this chat and the numerous design changes. To get us back on track, I'll share some quick snippets of the current flow components.

From there, could you please concisely summarize our current standing point? I'd like to know what's confirmed and working, what's not yet working, and what's left over from previous design choices that needs to be cleaned up.

Please provide a short, bulleted list of our current design choices. I will then add this to my prompts as a reminder of the context. Don't worry about spitting out complete code snippets yet—just point out the key things. Based on this, we'll prioritize our next steps and go through them one at a time.

## Architectural Considerations

* **Lack of Abstraction**: If you notice a "smell" of a lack of abstraction that could be better handled later through dedicated modeling (e.g., materialized views or ETL processes), please point it out. This will help us avoid architectural debt.
* **Premature Optimization**: I may be falling into the trap of "premature optimization" with some of the analytical design choices. If you see this happening and understand my goal, please bring it to my attention.
* **Frontend compatibility**: We don't need to consider frontend backward compatibility. The frontend isn't yet implemented and will use production-grade tools. We should prioritize a scalable model from the start.
* **Mixing Semantics**: If global queries become complex (e.g., cross-template canonicalization, different aggregations), we should consider creating a higher-level abstraction (e.g., separate services or optimized materialized views) flag it to avoid baking in technical debt.
* **Professional **: If global queries become complex (e.g., cross-template canonicalization, different aggregations), we should consider creating a higher-level abstraction (e.g., separate services or optimized materialized views) flag it to avoid baking in technical debt.


attached the code snippets for our standing point.

## test payloads that already passed with our current standing point in codes attatched (“All green”, all passed when practically sent to the api end points: `POST /api/v1/analytics/pivot/query?format=TABLE_ROWS&paging=true`)

Basic total (template mode):

```json
{
  "templateId": "Tcf3Ks9ZRpB",
  "templateVersionId": "fb2GC7FInSu",
  "measures": [
    { "elementIdOrUid": "etc:KiHwmLKUo9j", "aggregation": "COUNT", "alias": "count_day" }
  ]
}
```

Simple group by:

```json
{
  "templateId": "Tcf3Ks9ZRpB",
  "templateVersionId": "fb2GC7FInSu",
  "dimensions": ["team_uid"],
  "measures": [
    { "elementIdOrUid": "etc:CiEZemZ7mlg", "aggregation": "SUM", "alias": "sum_completion" }
  ],
  "limit": 50
}
```

Ordering + deterministic pagination test:

```json
{
  "templateId": "Tcf3Ks9ZRpB",
  "templateVersionId": "fb2GC7FInSu",
  "dimensions": ["element_template_config_uid"],
  "measures": [
    { "elementIdOrUid": "etc:CiEZemZ7mlg", "aggregation": "SUM", "alias": "sum_completion" }
  ],
  "sorts": [{ "fieldOrAlias": "sum_completion", "desc": true }],
  "limit": 10,
  "offset": 0
}
```

Count groups:

```json
POST /api/v1/analytics/pivot/query?format=TABLE_ROWS
{
  "templateId":"Tcf3Ks9ZRpB",
  "templateVersionId":"fb2GC7FInSu",
  "dimensions":["team_uid"],
  "measures":[]
}
```

---


## 1) Empty `IN` list → no matches (policy = `falseCondition()`) passed

* Test name: `filter_emptyIn_returnsNoRows`
* Type: **Integration**
* Request JSON:

```log
[  XNIO-1 task-2] org.jooq.tools.LoggerListener            : Executing query          : select count(*) from (select public.pivot_grid_facts.team_uid from public.pivot_grid_facts where (public.pivot_grid_facts.deleted_at is null and false) group by public.pivot_grid_facts.team_uid) as g 
[  XNIO-1 task-2] org.jooq.tools.LoggerListener            : Fetched result           : +-----+ 
[  XNIO-1 task-2] org.jooq.tools.LoggerListener            :                          : |count| 
[  XNIO-1 task-2] org.jooq.tools.LoggerListener            :                          : +-----+ 
[  XNIO-1 task-2] org.jooq.tools.LoggerListener            :                          : |    0| 
[  XNIO-1 task-2] org.jooq.tools.LoggerListener            :                          : +-----+ 
[  XNIO-1 task-2] org.jooq.tools.LoggerListener            : Fetched row(s)           : 1 
[  XNIO-1 task-2] org.jooq.tools.LoggerListener            : Executing query          : select public.pivot_grid_facts.team_uid, sum(public.pivot_grid_facts.value_num) filter (where public.pivot_grid_facts.etc_uid = ?) as sum_completion from public.pivot_grid_facts where (public.pivot_grid_facts.deleted_at is null and false) group by public.pivot_grid_facts.team_uid order by public.pivot_grid_facts.team_uid asc, min(public.pivot_grid_facts.value_id) asc offset ? rows fetch next ? rows only 
[  XNIO-1 task-2] org.jooq.tools.LoggerListener            : -> with bind values      : select public.pivot_grid_facts.team_uid, sum(public.pivot_grid_facts.value_num) filter (where public.pivot_grid_facts.etc_uid = 'CiEZemZ7mlg') as sum_completion from public.pivot_grid_facts where (public.pivot_grid_facts.deleted_at is null and false) group by public.pivot_grid_facts.team_uid order by public.pivot_grid_facts.team_uid asc, min(public.pivot_grid_facts.value_id) asc offset 0 rows fetch next 10 rows only 
[  XNIO-1 task-2] org.jooq.tools.LoggerListener            : Fetched result           : +--------+--------------+ 
[  XNIO-1 task-2] org.jooq.tools.LoggerListener            :                          : |team_uid|sum_completion| 
[  XNIO-1 task-2] org.jooq.tools.LoggerListener            :                          : +--------+--------------+ 
[  XNIO-1 task-2] org.jooq.tools.LoggerListener            : Fetched row(s)  
```

* Expected: passed with the above logs

    * No rows returned (empty `rows` list) and `total = 0`.
    * Generated WHERE must not include `IN ()`. Instead builder should translate to `... AND (FALSE)` or `... AND false`.
* Assertions:

    * `response.status == 200`
    * `response.total == 0`
    * `response.rows.isEmpty() == true`
    * Optional SQL assertion if testing builder: `dsl.renderInlined(query)` contains `FALSE` or `false_condition` (or contains `WHERE ... AND (FALSE)`).

---

## 2) Invalid numeric filter value → validation error (coercion failure), t

* Test name: `filter_invalidNumeric_throwsBadRequest`
* Type: **Unit** (test `translateFilter` / `PivotQueryBuilder`) or **Integration**
* Request JSON:

Error returned
```json
{
	"type": "https://datarun.nmcpye.org/problem/problem-with-message",
	"title": "Internal Server Error",
	"status": 500,
	"detail": "Character n is neither a decimal digit number, decimal point, nor \"e\" notation exponential mark.",
	"instance": "/api/v1/analytics/pivot/query",
	"message": "error.http.500",
	"path": "/api/v1/analytics/pivot/query"
}
```

* Expected:

    * Validation/coercion fails → server responds `400 Bad Request` (or the service throws `IllegalArgumentException` which controller maps to 400).
    * Error message contains `"Cannot coerce to numeric"` or `"Cannot coerce to BigDecimal"` (based on `coerceToType`).
* Assertions:

    * `response.status == 400`
    * `response.body.error.contains("Cannot coerce to numeric")` (or match your exception mapping)

---

## 3) LIKE vs ILIKE semantics (case-sensitive vs case-insensitive) passed

* Test name: `filter_like_and_ilike_behavior`
* Type: **Integration**
* Setup: Make sure `pivot_grid_facts` contains rows with `value_text` values like `"FooBar"`, `"foobar"`.
* Request A — case-sensitive LIKE:

```log
[  XNIO-1 task-2] org.jooq.tools.LoggerListener            : Executing query          : select 1 from public.pivot_grid_facts where (public.pivot_grid_facts.deleted_at is null and public.pivot_grid_facts.value_text like ?) fetch next ? rows only 
[  XNIO-1 task-2] org.jooq.tools.LoggerListener            : -> with bind values      : select 1 from public.pivot_grid_facts where (public.pivot_grid_facts.deleted_at is null and public.pivot_grid_facts.value_text like 'Foo%') fetch next 1 rows only 
[  XNIO-1 task-2] org.jooq.tools.LoggerListener            : Fetched result           : +----+ 
[  XNIO-1 task-2] org.jooq.tools.LoggerListener            :                          : |   1| 
[  XNIO-1 task-2] org.jooq.tools.LoggerListener            :                          : +----+ 
[  XNIO-1 task-2] org.jooq.tools.LoggerListener            : Fetched row(s)           : 0 
[  XNIO-1 task-2] org.jooq.tools.LoggerListener            : Executing query          : select count(*) filter (where public.pivot_grid_facts.etc_uid = ?) as cnt from public.pivot_grid_facts where (public.pivot_grid_facts.deleted_at is null and public.pivot_grid_facts.value_text like ?) offset ? rows fetch next ? rows only 
[  XNIO-1 task-2] org.jooq.tools.LoggerListener            : -> with bind values      : select count(*) filter (where public.pivot_grid_facts.etc_uid = 'OGnFB2FI6Zz') as cnt from public.pivot_grid_facts where (public.pivot_grid_facts.deleted_at is null and public.pivot_grid_facts.value_text like 'Foo%') offset 0 rows fetch next 100 rows only 
[  XNIO-1 task-2] org.jooq.tools.LoggerListener            : Fetched result           : +----+ 
[  XNIO-1 task-2] org.jooq.tools.LoggerListener            :                          : | cnt| 
[  XNIO-1 task-2] org.jooq.tools.LoggerListener            :                          : +----+ 
[  XNIO-1 task-2] org.jooq.tools.LoggerListener            :                          : |   0| 
[  XNIO-1 task-2] org.jooq.tools.LoggerListener            :                          : +----+ 
[  XNIO-1 task-2] org.jooq.tools.LoggerListener            : Fetched row(s)           : 1 
```

* Request B — case-insensitive ILIKE:

Debug log, shortened for brevity:
```log
DEBUG 26900 --- [  XNIO-1 task-2] org.jooq.tools.LoggerListener            : Executing query          : select 1 from public.pivot_grid_facts where (public.pivot_grid_facts.deleted_at is null and public.pivot_grid_facts.value_text ilike ?) fetch next ? rows only 
DEBUG 26900 --- [  XNIO-1 task-2] org.jooq.tools.LoggerListener            : -> with bind values      : select 1 from public.pivot_grid_facts where (public.pivot_grid_facts.deleted_at is null and public.pivot_grid_facts.value_text ilike 'foo%') fetch next 1 rows only 
[  XNIO-1 task-2] org.jooq.tools.LoggerListener            : Fetched result           : +----+ 
[  XNIO-1 task-2] org.jooq.tools.LoggerListener            :                          : |   1| 
[  XNIO-1 task-2] org.jooq.tools.LoggerListener            :                          : +----+ 
[  XNIO-1 task-2] org.jooq.tools.LoggerListener            : Fetched row(s)           : 0 
[  XNIO-1 task-2] org.jooq.tools.LoggerListener            : Executing query          : select count(*) filter (where public.pivot_grid_facts.etc_uid = ?) as cnt from public.pivot_grid_facts where (public.pivot_grid_facts.deleted_at is null and public.pivot_grid_facts.value_text ilike ?) offset ? rows fetch next ? rows only 
[  XNIO-1 task-2] org.jooq.tools.LoggerListener            : -> with bind values      : select count(*) filter (where public.pivot_grid_facts.etc_uid = 'OGnFB2FI6Zz') as cnt from public.pivot_grid_facts where (public.pivot_grid_facts.deleted_at is null and public.pivot_grid_facts.value_text ilike 'foo%') offset 0 rows fetch next 100 rows only 
[  XNIO-1 task-2] org.jooq.tools.LoggerListener            : Fetched result           : +----+ 
[  XNIO-1 task-2] org.jooq.tools.LoggerListener            :                          : | cnt| 
[  XNIO-1 task-2] org.jooq.tools.LoggerListener            :                          : +----+ 
[  XNIO-1 task-2] org.jooq.tools.LoggerListener            :                          : |   0| 
[  XNIO-1 task-2] org.jooq.tools.LoggerListener            :                          : +----+ 
[  XNIO-1 task-2] org.jooq.tools.LoggerListener            : Fetched row(s)           : 1 
```

* Expected:

    * A (LIKE): matches `"FooBar"` but not `"foobar"`.
    * B (ILIKE): matches both `"FooBar"` and `"foobar"`.
* Assertions:

    * Compare counts returned by A and B as expected (A <= B, B >= A, B > 0).
    * SQL (optional): `LIKE` uses `LIKE`, `ILIKE` uses `ILIKE` or jOOQ `likeIgnoreCase`.

---

## 4) LIKE on non-string field → validation error

* Test name: `filter_like_on_numeric_throws`
* Type: **Unit**
* Request:

http Error returned
```json
{
	"type": "https://datarun.nmcpye.org/problem/problem-with-message",
	"title": "Internal Server Error",
	"status": 500,
	"detail": "Character % is neither a decimal digit number, decimal point, nor \"e\" notation exponential mark.",
	"instance": "/api/v1/analytics/pivot/query",
	"message": "error.http.500",
	"path": "/api/v1/analytics/pivot/query"
}
```

* Expected:

    * `400 Bad Request` or thrown `IllegalArgumentException` with message like `"LIKE is only valid on string fields"`.
* Assertions:

    * Exception type and message check.

---

## 5) Ordering by measure alias + deterministic pagination (tie-breaker)

* Test name: `orderBy_aggregate_alias_pagination_is_deterministic`
* Type: **Integration**
* Setup: Insert predictable rows in `pivot_grid_facts` such that two groups share same aggregate value to exercise tie-breaker. Example:

    * Group "A" sum = 100
    * Group "B" sum = 100 (tied)
    * Group "C" sum = 50
* Request JSON:

```log
[  XNIO-1 task-2] org.jooq.tools.LoggerListener            : Executing query          : select count(*) from (select public.pivot_grid_facts.etc_uid from public.pivot_grid_facts where public.pivot_grid_facts.deleted_at is null group by public.pivot_grid_facts.etc_uid) as g 
[  XNIO-1 task-2] org.jooq.tools.LoggerListener            : Fetched result           : +-----+ 
[  XNIO-1 task-2] org.jooq.tools.LoggerListener            :                          : |count| 
[  XNIO-1 task-2] org.jooq.tools.LoggerListener            :                          : +-----+ 
[  XNIO-1 task-2] org.jooq.tools.LoggerListener            :                          : |   17| 
[  XNIO-1 task-2] org.jooq.tools.LoggerListener            :                          : +-----+ 
[  XNIO-1 task-2] org.jooq.tools.LoggerListener            : Fetched row(s)           : 1 
[  XNIO-1 task-2] org.jooq.tools.LoggerListener            : Executing query          : select public.pivot_grid_facts.etc_uid, sum(public.pivot_grid_facts.value_num) filter (where public.pivot_grid_facts.etc_uid = ?) as sum_val from public.pivot_grid_facts where public.pivot_grid_facts.deleted_at is null group by public.pivot_grid_facts.etc_uid order by sum_val desc, min(public.pivot_grid_facts.value_id) asc offset ? rows fetch next ? rows only 
[  XNIO-1 task-2] org.jooq.tools.LoggerListener            : -> with bind values      : select public.pivot_grid_facts.etc_uid, sum(public.pivot_grid_facts.value_num) filter (where public.pivot_grid_facts.etc_uid = 'CiEZemZ7mlg') as sum_val from public.pivot_grid_facts where public.pivot_grid_facts.deleted_at is null group by public.pivot_grid_facts.etc_uid order by sum_val desc, min(public.pivot_grid_facts.value_id) asc offset 0 rows fetch next 1 rows only 
[  XNIO-1 task-2] org.jooq.tools.LoggerListener            : Fetched result           : +-----------+-------+ 
[  XNIO-1 task-2] org.jooq.tools.LoggerListener            :                          : |etc_uid    |sum_val| 
[  XNIO-1 task-2] org.jooq.tools.LoggerListener            :                          : +-----------+-------+ 
[  XNIO-1 task-2] org.jooq.tools.LoggerListener            :                          : |OGnFB2FI6Zz| {null}| 
[  XNIO-1 task-2] org.jooq.tools.LoggerListener            :                          : +-----------+-------+ 
[  XNIO-1 task-2] org.jooq.tools.LoggerListener            : Fetched row(s)           : 1 
```

* Steps/assertions:

    1. Fetch page0 (limit 1, offset 0) → expect first group e.g. "A" (highest).
    2. Fetch page1 (limit 1, offset 1) → deterministic second group: if tie between A and B, order must be deterministically resolved by tie-breaker (MIN(value\_id) or etc) — assert page1 != page0 and equals expected "B" or ordering you precomputed.

    * SQL check (optional): ORDER BY includes alias `sum_val` and the tie-breaker column: `ORDER BY "SUM_VAL" DESC, MIN("VALUE_ID") ASC` (or equivalent). Your builder implementation uses `MIN(value_id)` as group-aware tie-breaker — assert presence.
* Expected:

    * No flakiness across runs: same first and second returned consistently.

---

## 6) ORDER BY dimension + pagination determinism

* Test name: `orderBy_dimension_pagination_is_deterministic`
* Type: **Integration**
* Setup: Insert rows for dimensions `element_uid` values `el-A`, `el-B`, `el-C` (alphabetical ordering).
* Request JSON:

```json
{
  "templateId":"Tcf3Ks9ZRpB",
  "templateVersionId":"fb2GC7FInSu",
  "dimensions": ["etc_uid"],
  "measures": [],
  "sorts": [{ "fieldOrAlias": "etc_uid", "desc": false }],
  "limit": 1,
  "offset": 0
}
```

```log
[  XNIO-1 task-2] org.jooq.tools.LoggerListener            : Executing query          : select count(*) from (select public.pivot_grid_facts.etc_uid from public.pivot_grid_facts where public.pivot_grid_facts.deleted_at is null group by public.pivot_grid_facts.etc_uid) as g 
[  XNIO-1 task-2] org.jooq.tools.LoggerListener            : Fetched result           : +-----+ 
[  XNIO-1 task-2] org.jooq.tools.LoggerListener            :                          : |count| 
[  XNIO-1 task-2] org.jooq.tools.LoggerListener            :                          : +-----+ 
[  XNIO-1 task-2] org.jooq.tools.LoggerListener            :                          : |   17| 
[  XNIO-1 task-2] org.jooq.tools.LoggerListener            :                          : +-----+ 
[  XNIO-1 task-2] org.jooq.tools.LoggerListener            : Fetched row(s)           : 1 
[  XNIO-1 task-2] org.jooq.tools.LoggerListener            : Executing query          : select public.pivot_grid_facts.etc_uid from public.pivot_grid_facts where public.pivot_grid_facts.deleted_at is null group by public.pivot_grid_facts.etc_uid order by public.pivot_grid_facts.etc_uid asc, min(public.pivot_grid_facts.value_id) asc offset ? rows fetch next ? rows only 
[  XNIO-1 task-2] org.jooq.tools.LoggerListener            : -> with bind values      : select public.pivot_grid_facts.etc_uid from public.pivot_grid_facts where public.pivot_grid_facts.deleted_at is null group by public.pivot_grid_facts.etc_uid order by public.pivot_grid_facts.etc_uid asc, min(public.pivot_grid_facts.value_id) asc offset 0 rows fetch next 1 rows only 
[  XNIO-1 task-2] org.jooq.tools.LoggerListener            : Fetched result           : +-----------+ 
[  XNIO-1 task-2] org.jooq.tools.LoggerListener            :                          : |etc_uid    | 
[  XNIO-1 task-2] org.jooq.tools.LoggerListener            :                          : +-----------+ 
[  XNIO-1 task-2] org.jooq.tools.LoggerListener            :                          : |a1pyMmK0GJp| 
[  XNIO-1 task-2] org.jooq.tools.LoggerListener            :                          : +-----------+ 
[  XNIO-1 task-2] org.jooq.tools.LoggerListener            : Fetched row(s)           : 1 
```

* Assertions:

    * page0 returns `el-A`
    * page1 returns `el-B`
    * ensure `rows.size()` and values as expected

---

## 7) ORDER BY alias when alias name conflicts with MV column → alias precedence / auto-rename behavior

* Test name: `duplicate_alias_handling_autorename_or_error`
* Type: **Unit**
* Variants to test (based on your alias policy decision):

    * `autoRenameAliases = false` => `InvalidMeasureException` when two measures share the same alias.
    * `autoRenameAliases = true` => builder silently renames duplicate alias to `alias_1`, `alias_2` etc and query works.
* Request JSON (duplicate alias):

```json
{
  "templateId":"Tcf3Ks9ZRpB",
  "templateVersionId":"fb2GC7FInSu",
  "measures": [
    { "elementIdOrUid": "etc:CiEZemZ7mlg", "aggregation": "SUM", "alias": "total" },
    { "elementIdOrUid": "etc:CiEZemZ7mlg", "aggregation": "COUNT", "alias": "total" }
  ]
}
```

This produced an error in the log but still returned a result:
```log
[  XNIO-1 task-2] org.jooq.tools.LoggerListener            : Executing query          : select 1 from public.pivot_grid_facts where public.pivot_grid_facts.deleted_at is null fetch next ? rows only 
[  XNIO-1 task-2] org.jooq.tools.LoggerListener            : -> with bind values      : select 1 from public.pivot_grid_facts where public.pivot_grid_facts.deleted_at is null fetch next 1 rows only 
[  XNIO-1 task-2] org.jooq.tools.LoggerListener            : Fetched result           : +----+ 
[  XNIO-1 task-2] org.jooq.tools.LoggerListener            :                          : |   1| 
[  XNIO-1 task-2] org.jooq.tools.LoggerListener            :                          : +----+ 
[  XNIO-1 task-2] org.jooq.tools.LoggerListener            :                          : |   1| 
[  XNIO-1 task-2] org.jooq.tools.LoggerListener            :                          : +----+ 
[  XNIO-1 task-2] org.jooq.tools.LoggerListener            : Fetched row(s)           : 1 
[  XNIO-1 task-2] org.jooq.tools.LoggerListener            : Executing query          : select sum(public.pivot_grid_facts.value_num) filter (where public.pivot_grid_facts.etc_uid = ?) as total, count(*) filter (where public.pivot_grid_facts.etc_uid = ?) as total from public.pivot_grid_facts where public.pivot_grid_facts.deleted_at is null offset ? rows fetch next ? rows only 
[  XNIO-1 task-2] org.jooq.tools.LoggerListener            : -> with bind values      : select sum(public.pivot_grid_facts.value_num) filter (where public.pivot_grid_facts.etc_uid = 'CiEZemZ7mlg') as total, count(*) filter (where public.pivot_grid_facts.etc_uid = 'CiEZemZ7mlg') as total from public.pivot_grid_facts where public.pivot_grid_facts.deleted_at is null offset 0 rows fetch next 100 rows only 
[  XNIO-1 task-2] org.jooq.tools.LoggerListener            : Fetched result           : +-----+-----+ 
[  XNIO-1 task-2] org.jooq.tools.LoggerListener            :                          : |total|total| 
[  XNIO-1 task-2] org.jooq.tools.LoggerListener            :                          : +-----+-----+ 
[  XNIO-1 task-2] org.jooq.tools.LoggerListener            :                          : | 1225|   17| 
[  XNIO-1 task-2] org.jooq.tools.LoggerListener            :                          : +-----+-----+ 
[  XNIO-1 task-2] org.jooq.tools.LoggerListener            : Fetched row(s)           : 1 
[  XNIO-1 task-2] org.jooq.impl.FieldsImpl                 : Ambiguous match found for total. Both "total" and "total" match. 

java.sql.SQLWarning: null
	at org.jooq.impl.FieldsImpl.field0(FieldsImpl.java:359)
	at org.jooq.impl.FieldsImpl.indexOf(FieldsImpl.java:472)
	at org.jooq.impl.AbstractRow.indexOf(AbstractRow.java:423)
	at org.jooq.impl.Tools.indexOrFail(Tools.java:2266)
	at org.jooq.impl.AbstractRecord.get(AbstractRecord.java:368)
	at org.nmcpye.datarun.analytics.pivot.PivotQueryServiceImpl.mapResultToRows(PivotQueryServiceImpl.java:216)
	at org.nmcpye.datarun.analytics.pivot.PivotQueryServiceImpl.query(PivotQueryServiceImpl.java:114)
	at org.nmcpye.datarun.web.rest.v1.pivotgrid.PivotQueryController.query(PivotQueryController.java:70)
```

* Assertions:

    * If `autoRenameAliases=false` → assert thrown `InvalidMeasureException` / `400` with message about duplicate alias.
    * If `autoRenameAliases=true` → assert response contains columns `total` and `total_1` (or whichever renaming scheme).

---

## 8) Filter per-measure scoping (ensure `filterWhere(scope)` applied to aggregates)

* Test name: `measure_filter_scope_applies`
* Type: **Integration**
* Scenario:

    * Two measures on same element but one limited to a specific option (option\_uid) via `MeasureRequest.optionId`.
    * The aggregate for the measure with `optionId` must only count rows where `option_uid = <that uid>`.
* Request JSON:

```json
{
  "templateId":"Tcf3Ks9ZRpB",
  "templateVersionId":"fb2GC7FInSu",
  "measures": [
    { "elementIdOrUid": "etc:CiEZemZ7mlg", "aggregation": "SUM", "alias": "sum_all" },
    { "elementIdOrUid": "etc:CiEZemZ7mlg", "aggregation": "SUM", "alias": "sum_opt1", "optionId": "ov:abc123" }
  ]
}
```

result
```log
[  XNIO-1 task-2] org.jooq.tools.LoggerListener            : Executing query          : select 1 from public.pivot_grid_facts where public.pivot_grid_facts.deleted_at is null fetch next ? rows only 
[  XNIO-1 task-2] org.jooq.tools.LoggerListener            : -> with bind values      : select 1 from public.pivot_grid_facts where public.pivot_grid_facts.deleted_at is null fetch next 1 rows only 
[  XNIO-1 task-2] org.jooq.tools.LoggerListener            : Fetched result           : +----+ 
[  XNIO-1 task-2] org.jooq.tools.LoggerListener            :                          : |   1| 
[  XNIO-1 task-2] org.jooq.tools.LoggerListener            :                          : +----+ 
[  XNIO-1 task-2] org.jooq.tools.LoggerListener            :                          : |   1| 
[  XNIO-1 task-2] org.jooq.tools.LoggerListener            :                          : +----+ 
[  XNIO-1 task-2] org.jooq.tools.LoggerListener            : Fetched row(s)           : 1 
[  XNIO-1 task-2] org.jooq.tools.LoggerListener            : Executing query          : select sum(public.pivot_grid_facts.value_num) filter (where public.pivot_grid_facts.etc_uid = ?) as sum_all, sum(public.pivot_grid_facts.value_num) filter (where (public.pivot_grid_facts.etc_uid = ? and public.pivot_grid_facts.option_uid = ?)) as sum_opt1 from public.pivot_grid_facts where public.pivot_grid_facts.deleted_at is null offset ? rows fetch next ? rows only 
[  XNIO-1 task-2] org.jooq.tools.LoggerListener            : -> with bind values      : select sum(public.pivot_grid_facts.value_num) filter (where public.pivot_grid_facts.etc_uid = 'CiEZemZ7mlg') as sum_all, sum(public.pivot_grid_facts.value_num) filter (where (public.pivot_grid_facts.etc_uid = 'CiEZemZ7mlg' and public.pivot_grid_facts.option_uid = 'ov:abc123')) as sum_opt1 from public.pivot_grid_facts where public.pivot_grid_facts.deleted_at is null offset 0 rows fetch next 100 rows only 
[  XNIO-1 task-2] org.jooq.tools.LoggerListener            : Fetched result           : +-------+--------+ 
[  XNIO-1 task-2] org.jooq.tools.LoggerListener            :                          : |sum_all|sum_opt1| 
[  XNIO-1 task-2] org.jooq.tools.LoggerListener            :                          : +-------+--------+ 
[  XNIO-1 task-2] org.jooq.tools.LoggerListener            :                          : |   1225|  {null}| 
[  XNIO-1 task-2] org.jooq.tools.LoggerListener            :                          : +-------+--------+ 
[  XNIO-1 task-2] org.jooq.tools.LoggerListener            : Fetched row(s)           : 1 
```

* Expected:

    * `sum_all >= sum_opt1`
    * SQL (optional): `SUM(value_num) FILTER (WHERE element_template_config_uid = 'etc:..' AND option_uid = 'ov:abc123')` exists — in your builder you use `filterWhere` so check for `.filterWhere` effect in rendered SQL: `SUM("VALUE_NUM") FILTER (WHERE ("OPTION_UID" = 'ov:abc123' AND ...))` or equivalent.
* Assertions:

    * `sum_all >= sum_opt1` and `sum_opt1` equals manual count for that option.

---

### Extra notes & test harness suggestions

* **Where to put tests**

    * Use **unit tests** for `translateFilter`, `coerceToType`, `buildAggregateField` (validate exceptions & Field typing).
    * Use **integration tests** with Testcontainers for end-to-end SQL rendering & execution (you already have this harness; reuse it).
* **SQL assertions**

    * For unit builder tests, call `dsl.renderInlined(select)` and assert presence/absence of fragments: e.g., `IN ('x','y')`, `ILIKE`, `SUM("VALUE_NUM")`, `ORDER BY "SUM_VAL" DESC, "MIN_VALUE_ID" ASC` (or your exact tie-breaker fragment).
* **UID validation**

    * Add tests that send an invalid uid (wrong length) in `filters` or `measures` and assert early rejection (400).
* **Test data**

    * Keep small deterministic datasets for each test to avoid flakiness. Seed known rows before each test and truncate afterwards.

---

If you want, I can:

* Produce ready-to-paste JUnit test methods (with your existing DSL/testcontainer setup) for each of the above tests; **or**
* Generate a smaller subset of ready-to-run integration tests first (e.g., empty-IN, invalid numeric, alias duplicate, order-by-alias pagination), and we iterate.

Which do you want me to generate now?
