Nice — here’s a compact, accurate summary of the admin HTTP surface you **actually have implemented** (controllers + scheduler), what each endpoint does, example calls, and the full processing flow they trigger (so you — or an operator — can reason end-to-end).

I also mark which admin operations we discussed but haven’t implemented as endpoints yet and give the exact SQL you can use for them (so you can call them from psql or add endpoints later).

---

## Endpoints implemented (what they are & example calls)

### 1) `POST /admin/etl/enqueue-backfill`

**Controller:** `EnqueueBackfillController` (thin; no JDBC).
**What it does:** Idempotently enqueue BACKFILL outbox rows for submissions. It delegates to `BackfillService` → `OutboxRepository` that runs `INSERT ... ON CONFLICT ... DO NOTHING` using the partial unique index to avoid duplicate BACKFILLs per submission.

**Request body (two options):**

* By IDs:

```json
{ "submissionIds": ["01K9J8TW...","01K9J8TW..."] }
```

* By serial-range:

```json
{ "fromSerial": 72000, "toSerial": 72499 }
```

**Curl example:**

```sh
curl -X POST https://your-host/admin/etl/enqueue-backfill \
  -H "Content-Type: application/json" \
  -d '{"fromSerial":72000,"toSerial":72010}'
```

**Returns:** `{ "inserted": 10 }` (number actually inserted).

**DB tables touched:** `data_submission` (read), `outbox` (insert, partial-unique index prevents duplicates).

---

### 2) `POST /admin/etl/run`  *(synchronous)*

**Controller:** `EtlAdminController` (simple helper).
**What it does:** Calls `EtlOrchestrator.runOnce(pipelineName, batchSize)` synchronously and returns after completion. Good for short test runs.

**Query/form parameters:**

* `pipeline` (default `backfill`)
* `batchSize` (e.g., `50`)

**Curl example:**

```sh
curl -X POST "https://your-host/admin/etl/run?pipeline=backfill&batchSize=50"
```

**Returns:** simple success string when run completes.

**Note:** synchronous — not recommended for large batches because request will block until run finishes.

---

### 3) `POST /admin/etl/run-async`

**Controller:** `AsyncEtlController` (async runner).
**What it does:** Starts an asynchronous ETL run (background thread) that executes `EtlOrchestrator.runOnce`. Returns a `jobId` you can poll.

**Request body (example):**

```json
{ "pipeline": "backfill", "batchSize": 100 }
```

**Curl example:**

```sh
curl -X POST https://your-host/admin/etl/run-async \
  -H "Content-Type: application/json" \
  -d '{"pipeline":"backfill","batchSize":100}'
```

**Returns (accepted):**

```json
{ "jobId":"...", "status":"RUNNING", "startedAt":"2025-11-17T12:00:00Z" }
```

---

### 4) `GET /admin/etl/run-status/{jobId}`

**Controller:** `AsyncEtlController` (status endpoint).
**What it does:** Returns the in-memory status for the async job started with `run-async`.

**Curl example:**

```sh
curl https://your-host/admin/etl/run-status/3fa85f64-...
```

**Returns:** `{ "jobId":"...", "status":"RUNNING" }` or `SUCCESS` or `FAILED: <short-msg>`.

**Note:** job status is in-memory (non-persistent) — if app restarts you lose it. We can replace with a persistent job table later.

---

### 5) Scheduler (not an endpoint; scheduled bean)

**Class:** `ScheduledOrchestratorRunner` — runs periodically (default every 5 minutes).
**Behavior:** tries to acquire a Postgres advisory lock (`pg_try_advisory_lock`) and if successful calls `EtlOrchestrator.runOnce("scheduled", SCHEDULE_BATCH_SIZE)`. This enforces single-leader scheduled execution across clustered instances.

**Config:** enable scheduling with `@EnableScheduling`. Adjust cron and batch size as needed.

---

## What each endpoint triggers (end-to-end flow)

Take `POST /admin/etl/enqueue-backfill` → `POST /admin/etl/run-async` as an example.

1. **Enqueue backfill**

    * `EnqueueBackfillController` → `BackfillService.enqueueBySerialRange` → `SubmissionRepository.findBySerialRange` reads submissions.
    * `BackfillService` builds `OutboxRepository.OutboxInsert` entries and calls `OutboxRepository.insertBackfillIfNotExists`.
    * `OutboxRepository` executes a batch:

      ```sql
      INSERT INTO outbox (...)
      VALUES (...)
      ON CONFLICT (submission_id) WHERE (event_type = 'BACKFILL') DO NOTHING;
      ```
    * New outbox rows are now in `outbox` with `status='pending'`.

2. **Start run (async)**

    * `AsyncEtlController` enqueues a background job that calls `EtlOrchestrator.runOnce(pipeline, batchSize)`.

3. **Orchestrator claims a batch**

    * `EtlOrchestrator.runOnce` → `OutboxClaimService.claimBatchWithIngestId(ingestId, batchSize)` → `OutboxJdbcRepository.claimPendingBatch`:

        * `UPDATE outbox SET ingest_id = :ingestId WHERE status='pending' ... RETURNING ... FOR UPDATE SKIP LOCKED` or equivalent `UPDATE ... RETURNING`.
        * Claiming guarantees other consumers skip those rows.

4. **Per-outbox processing (each claimed row)**

    * For each OutboxDto, orchestrator starts a **per-outbox DB transaction** (TransactionTemplate).
    * `outboxProcessingService.startProcessing(runId, outbox)` inserts a processing row.
    * `EventProcessorService.processEvent(runId, outbox, ingestId)`:

        * If `DELETE` → `TallCanonicalJdbcRepository.deleteByRepeatInstanceId` or `deleteBySubmissionUid`.
        * Else (`SAVE`/`UPDATE`/`BACKFILL`):

            * `TransformService.transform(outbox)` → pure mapping → `List<TallCanonicalRow>`.
            * `tallRepo.upsertBatch(rows)` → single batch UPSERT:

              ```sql
              INSERT INTO tall_canonical (instance_key, canonical_element_uid, ...)
              VALUES (...)
              ON CONFLICT (instance_key, canonical_element_uid) DO UPDATE SET ...
              ```
            * On success: `outboxProcessingService.recordSuccess` (in same TX) → updates `outbox.status='success'` via `OutboxJdbcRepository.markOutboxSuccess` and `EtlRunService.incrementSuccess`.
    * If transform/upsert throws:

        * `processEvent` calls `recordFailureRequiresNewTx` which starts a **REQUIRES_NEW** transaction to persist a failure row into `outbox_processing` and call `OutboxJdbcRepository.markOutboxFailure(outboxId, error, nextAttemptAt)` (so `attempt` is incremented and `next_attempt_at` scheduled).
        * The per-outbox TX rolls back (no partial writes persisted).

5. **Finish run**

    * After processing the batch, `EtlOrchestrator` calls `EtlRunService.finishRun(runId, status, counts)` which sets `finished_at` and run status.

---

## Quick examples / sequences you (admin) will use

### A. One-off small backfill

1. `POST /admin/etl/enqueue-backfill` with `fromSerial`/`toSerial` small range.
2. `POST /admin/etl/run-async` with `batchSize` = 20.
3. Poll `GET /admin/etl/run-status/{jobId}` or check DB:

   ```sql
   SELECT * FROM etl_runs ORDER BY started_at DESC LIMIT 1;
   SELECT count(*) FROM outbox WHERE status='pending';
   SELECT * FROM outbox_processing WHERE etl_run_id = :runId;
   ```

### B. Replay single submission

1. `POST /admin/etl/enqueue-backfill` with `submissionIds: ["01K9..."]`.
2. Trigger run (sync or async). The BACKFILL entry will be appended and processed.

### C. Force retry of failed rows (manual SQL or add endpoint later)

* SQL to requeue:

```sql
UPDATE outbox SET status='pending', next_attempt_at = now()
WHERE outbox_id = ANY(:ids);
```

(We can add an endpoint for this later — recommended.)

---

## Endpoints we talked about but haven’t implemented (quick list)

* `/admin/etl/requeue` — requeue specific outbox rows (we gave SQL; not implemented).
* `/admin/etl/deadletter` — mark row dead-lettered (not implemented).
* `/admin/etl/outbox` listing pages and `/admin/etl/tall` queries (not implemented; useful for UI).
  We can add these quickly if you want.

---

## Safety / operational tips (short)

* Use small `batchSize` for initial backfills (e.g., 20–100).
* Scheduler uses advisory lock — only one instance runs scheduled jobs.
* Use the partial unique index + `ON CONFLICT (...) WHERE (event_type='BACKFILL') DO NOTHING` to prevent duplicate BACKFILL enqueue from repeated migration runs.
* Failure audit is durable (REQUIRES_NEW) — fix and requeue failed rows as needed.

---

If you want, I can:

* (A) add `requeue` and `deadletter` endpoints (thin controllers + service methods) now, or
* (B) produce an exact sample run walkthrough (with SQL queries you can paste) that validates one backfill end-to-end, or
* (C) give the persistent-job-table version of async runs (durable across app restarts).

Which would you like next?
