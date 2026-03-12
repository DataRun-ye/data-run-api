# Gateway Integration Requirements & Gap Analysis 

> **Context:** `datarunapi` (Data Capture)
> **Target:** Integrating with the standalone Python *Delivery Engine Gateway*

## 1. The Integration Contract

The Delivery Gateway is an independent product (documented in `../gateway/`). It acts as a universal router and transformer. To plug `datarunapi` into this ecosystem cleanly—without violating Domain-Driven Design boundaries—`datarunapi` must provide a specific "surface area" for the Gateway to attach to.

`datarunapi` **must not** make direct HTTP calls to the Gateway or to external Ledgers. It must exclusively utilize the **Transactional Outbox Pattern**.

### What Data Capture Must Provide (The Fat Event)

Whenever a submission is created or updated, `datarunapi` must insert a row into a local `outbox_event` table in the exact same database transaction as the submission save.

The schema of this event must contain:

```json
{
  "event_id": "uuid-v4",
  "event_type": "SubmissionCreated",
  "aggregate_id": "z3Ye07TDj7a", // The Submission UID
  "correlation_id": "uuid-v4-from-client", // MUST be traced from the mobile app
  "occurred_at": "2026-03-08T10:00:00.000Z", // Mobile device timestamp
  "recorded_at": "2026-03-08T10:05:00.000Z", // Server DB lock timestamp
  "payload": { 
     // The ENTIRE V2 Normalized JSON Shape.
     // The Gateway will NOT query DataRun for missing data.
  }
}
```

---

## 2. Gap Analysis (What is currently missing in `datarunapi`)

At present, `datarunapi` is the only product in production. To support integrating the Gateway in the future, the following architectural gaps exist within the `datarunapi` codebase and must be addressed:

### Gap 1: Missing Outbox Table and Publisher Interface
**Current State:** Submissions are saved directly to `data_submission`. There is no Outbox table.
**Required Action:** 
- Create a Liquibase/migration to add the `outbox_event` table.
- Implement an `OutboxPublisher` interface in Java (e.g., Spring Data or JDBC) that is called transactionally during the `SubmissionService.save()` method.

### Gap 2: Incomplete Provenance Tracking (`correlation_id` & `occurred_at`)
**Current State:** The V1 `DataSubmission` entity tracks server-side auditing (created_by, created_date), but lacks strict offline-first bi-temporal timestamps and global tracing IDs.
**Required Action:**
- Add `correlation_id` to the `data_submission` schema.
- Update the V1 and V2 APIs to accept `correlation_id` and `occurred_at` (device time) from the Mobile Client.
- Ensure these variables explicitly pass through the layers and into the Outbox event.

### Gap 3: Missing V2 "Fat Payload" Materialization at Save Time
**Current State:** `datarunapi` stores data as an opaque V1 `formData` JSONB blob. The Gateway expects the *Normalized V2 Shape* in the Outbox payload.
**Required Action:**
- When saving a submission, `datarunapi` must dynamically translate the V1 `formData` into the V2 Normalized Shape (the "Fat Payload") *during* the transaction, so it can be inserted into the Outbox. 
- *Note:* This translation logic is part of the Anti-Corruption Layer (ACL) discussed in the core V2 Strategy docs.

---

## 3. Summary of Development Path

To make `datarunapi` "Gateway-Ready", the Java backend team must execute these steps:
1. DB Migration: Add `correlation_id` to submissions, add `outbox_event` table.
2. Code: Intercept the Submission Save flow.
3. Code: Translate the saved data into the V2 Normalized JSON.
4. Code: Insert the Event + Translation into the Outbox.
5. Provide the Gateway team with a mock SQL dump of the Outbox table to build their polling engine against.
