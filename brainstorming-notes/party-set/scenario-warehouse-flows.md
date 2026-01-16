## Context:

> For context, the current client application is an Android app built with Dart/Flutter, using Drift for local
> persistence. It already supports the initial model we started this discussion with.
>
> The system itself is intended to be a general-purpose data collection platform that can support many scenarios.
> Warehouse-style flows should emerge naturally, just as any other scenario would, without being explicitly named or
> hard-coded. Only when absolutely necessary should we introduce small utilities or helpers for truly essential,
> domain-specific needs. Otherwise, we prefer to keep the client-facing model generic: collect data first, sync to the
> server, interpret later. There are no pipelines, engines, recipes, or predefined lifecycles, and the system must fully
> support offline operation.

## Task:

> Let’s test our model and see how well it accommodates a real scenario, identify what’s still missing, and refine it as
> much as possible. The goal is to improve the model without over-engineering or accumulating unnecessary architectural
> debt. Solutions designed with a narrow perspective often become rigid and non-scalable, creating friction for future
> development.

## Overview

We operate two overlapping supply processes:

* **Routine (continuous) distribution** — regional → malaria unit (MU) → health facility (HF). Replenishment whenever HF
  needs stock.
* **Annual mass campaign** — temporary **campaign warehouses** are stocked from MU/region; **campaign teams** issue
  stock to assigned HFs during the campaign period.

For simplicity initially we prefer Keeping the model minimal: standard transaction types (Receipt, Issue/Distribution,
Transfer, Stock Count, Adjustment) plus **Consumption Report** for HF usage data.

---

## Actors & locations

* **Health Facility (HF)**
* **Malaria Unit (MU) warehouse**
* **Regional / Main warehouse**
* **Campaign warehouse (temporary)**
* **Campaign team** (operational distributor)
* **Logistics / Stock officer** (performs counts, reconciliations)

---

## Core transactions (names and where they apply)

we can stick by a one responsible side of the transaction, the core transaction from higher to down the higher staff is
responsible region->MU->Hfs or Team (teams split by MU and it's higher than and issue to hfs, returns follow reverse
order of responsibility but as and done by higher up one level e.g hfs returns are issued by the team or the MU received
them (the side that his count went up), teams returns to MU are issued by the MU's team/staff receiving it and would add
up to the MU stock, damage or expired are handled similarly but clearly stated as such)

* **Receipt (Goods Receipt / GRN)** — when a location records incoming stock (region, MU, campaign warehouse, HF).
* **Issue / Distribution** — when a warehouse issues stock out (to HF, campaign team).
* **Transfer** — internal move between warehouses (region ↔ MU ↔ campaign store) — modeled as Transfer Out +
  Receipt (by the MU/region team/staff).
* **Return** — HF or team returns stock up the chain (modeled as a Transfer/Receipt back by the MU staff).
* **Stock Count / Stocktake** — physical inventory checks (routine or campaign).
* **Adjustment / Write-off** — corrections for losses, damage, expiry.
* **Consumption Report** — HF’s monthly report of what was dispensed to patients (used for reconciliation and
  forecasting).

---

## Routine flow (short)

1. HF requests refill (or triggers reorder).
2. MU creates an **Issue/Distribution** to HF (system records Transfer Out).
3. HF dispenses to patients; at month-end sends **Consumption Report**.
4. MU performs periodic **Stock Count** and reconciles system vs receipts vs consumption.
5. Discrepancies are handled as **Adjustment** or **Return** (if returning to MU/region).

---

## More context to know how things are done:

## Campaign flow (short)

1. Plan campaign: define HFs per team and set up campaign warehouses.
2. Region/MU performs **Transfer → Receipt** to stock campaign warehouses.
3. Campaign teams perform **Issue/Distribution** from campaign warehouse to HFs (teams may also carry stock and issue
   on-site).
4. Immediate **Returns** recorded for damaged/unused items back to campaign warehouse.
5. Mid- and end-campaign **Stock Counts** reconcile disbursements, receipts, and HF consumption.
6. Closeout: remaining usable stock **Transferred** back to MU/region or formally handed into routine stores; final *
   *Adjustments** and reports completed.

---
























# Clean, organized problem statement — ready to send to the team

## Short summary

We need a **stable, minimal mapping** from the current submission/event/tall_canonical schema into three canonical supply-chain artifacts (`inventory_ledger`, `stock_balance`, `movement_fact`) so analytics and dashboards work **now** and keep working when we later replace `team/org_unit/assignment` with a polymorphic `party` table. The mapping must be idempotent, support offline-first submissions, and preserve the existing reporting semantics for routine and campaign flows.

---

## Context (one-paragraph)

Client is an Android app (Dart/Flutter + Drift). Submissions produce `events` and `tall_canonical` rows (examples below). We will ETL these into tall canonical artifacts and outbox events (Kafka outbox ignored for simplicity). For now parties are derived from `team`, `org_unit`, `assignment` tables; later a polymorphic `party` table will replace them. The goal is to design the canonical output tables and mapping rules so switching to `party` affects only the party-id lookup, not downstream reports.

---

## Goals / success criteria

1. Produce **inventory_ledger** rows per submission line (idempotent; UNIQUE on `submission_id, submission_line_id`).
2. Maintain **stock_balance** by aggregating ledger rows (batch or realtime).
3. Produce **movement_fact** rows for reporting (one row per movement line, direction IN/OUT/ADJUST).
4. Keep mappings stable so switching to a polymorphic `party` table requires little/no change to artifacts or dashboards.
5. Handle both **routine** and **campaign** flows (receipt/issue/transfer/return/adjust/stockcount/consumption).
6. Preserve provenance (submission, assignment, user, template binding, timestamps).

---

## Actors / locations (canonical)

* Health Facility (HF) — `org_unit`
* Malaria Unit (MU) warehouse
* Regional / Main warehouse
* Campaign warehouse (temporary)
* Campaign team (operational distributor)
* Logistics / Stock officer (performs counts)

`party_type` currently: `wh`, `hf`, `chv` (and `team` implied from `team` table / assignment). Later becomes a row in `party`.

---

## Core transaction types (to detect / derive)

* `Receipt` / GRN
* `Issue` / Distribution
* `Transfer` (modeled as Transfer Out + Receipt)
* `Return` (reverse flow)
* `StockCount` / Stocktake
* `Adjustment` / Write-off
* `Consumption` (HF usage report)

---

## Source data available

* `events` header rows (assignment_uid, team_uid, org_unit_uid, template_uid, start_time, submission_creation_time, etc.)
* `tall_canonical` rows (one row per element: `element_name`, `canonical_element_id`, `value_text/value_number/value_ref_uid`, `repeat_instance_id`, `repeat_index`, etc.)
* `data_template` which links `template_uid` → template type (e.g., `hf_receipt_902`, `wh_team_return_904`, `inventory_901`), and canonical element ids for `qty`, `category`, `tx_date`, `team`, etc.
* `assignment` dimension with assignment properties (planned counts, wh_name, party_type).
* `canonical_element` (canonical_element_id, template_uid, name (qty, category, tx_date, team, batch, expiry), data_type, etc).
* `template_uid/name -> transaction_type` mapping table.
* Sample pivoted submission (shown in original message) that shows how repeats map to category + qty lines.

---

## Desired canonical tables (already defined)

1. `inventory_ledger` — idempotent per submission-line, affects party balances; fields: `id, submission_id, submission_line_id, transaction_type, tx_date, assignment_id, from_party_id, to_party_id, party_id, sku_id, batch_id, qty_delta, unit, balance_after, provenance, created_at`.
2. `stock_balance` — aggregated OH per `(party_id, sku_id, coalesce(batch_id, '-'))`.
3. `movement_fact` — reporting-optimized rows: `id, submission_id, tx_date, tx_type, assignment_id, from_party_id, to_party_id, party_id, sku_id, batch_id, qty, direction, user_id, team_id, campaign_id, created_at`.

---

## Concrete mapping rules (proposal — apply to ETL)

**A. Identify transaction type**

* Use `template_uid` → `template_name` (or a mapping table) to determine `transaction_type` (Receipt|Issue|Transfer|Return|StockCount|Adjustment|Consumption).
* If ambiguous, prefer conservative mapping (e.g., treat unknown as `Adjustment` and flag).

**B. Derive party roles**

* `assignment.team_id` (or `events.team_uid`) = submitting team.
* `org_unit_uid` = the receiving/target org unit (HF/WH) present in template header.
* Decide `from_party_id` and `to_party_id` by `transaction_type`:

    * Receipt: `from_party_id` = supplier (if known) or NULL; `to_party_id` = org_unit (HF/campaign store/MU).
    * Issue: `from_party_id` = issuing warehouse (org_unit or team-owned wh); `to_party_id` = HF or team.
    * Transfer: create two logical sides — create ledger rows consistent with the side-of-responsibility approach (the "higher" level counts it as outgoing; the receiver creates a Receipt).
    * Return: reverse of Issue.
* `party_id` column in `inventory_ledger` = the party whose balance the row affects (for receipts this is `to_party_id`, for issues it is `from_party_id`).

**C. Lines / submission_line_id**

* Use `tall_canonical.repeat_instance_id` or `event_id` + canonical_element_id to form a deterministic `submission_line_id` (must be unique per line). This supports the `UNIQUE(submission_id, submission_line_id)` protection.

**D. Quantity sign**

* Set `qty_delta` positive for IN (Receipt, positive adjustments), negative for OUT (Issue), zero or signed appropriately for Consumption/StockCount/Adjustment according to `tx_type`.
* Also set `movement_fact.direction` = IN|OUT|ADJUST.

**E. SKU / category mapping**

* Map `category` canonical element → `sku_id` using existing canonical element-to-sku dictionary (maintain a lookup).
* If missing mapping, persist `value_text` as `sku_external_code` and flag for reconciliation.

**F. Batch & expiry**

* If `expiry_date` / `batch` fields present in template, fill `batch_id` and `expiry` in provenance or ledger row.

**G. Timestamps**

* `tx_date` should prefer a canonical date element (`tx_date` in tall_canonical) otherwise fallback to `start_time` then `submission_creation_time`. Always record provenance of chosen timestamp.

**H. Provenance**

* Populate `provenance` JSON with `{submission_uid, submission_id, event_id, template_uid, assignment_id, team_uid, org_unit_uid, user_uid, canonical_element_ids, source_raw_values}`.

**I. Idempotency & ordering**

* Insert ledger rows guarded by the UNIQUE key. If reprocessing, skip or update only `balance_after` if needed.
* If `balance_after` is null on insert, compute balance via aggregation (or maintain a background job to compute snapshot).

---

## Edge-cases & decisions to make (list for the team)

1. How to detect `from_party` for receipts when supplier is external / unknown? (NULL vs synthetic party)
2. For transfers: do we write **two** ledger rows in the same transaction (Out on from_party, In on to_party) or write one ledger row per side only when the receiver submits? (Proposed: write both when we can — keep `provenance` so deduping is possible.)
3. Unit normalization: templates may use different unit names — who is responsible for canonical unit mapping?
4. Batch-less aggregation vs batch-tracked: `stock_balance` primary key uses `coalesce(batch_id, '-')`. OK?
5. How to treat `Consumption` lines: create `inventory_ledger` rows that reduce HF balance or treat as separate reconciliation input? (Proposed: create OUT rows for HF with `transaction_type = Consumption`.)
6. How to derive `campaign_id` — currently routine is an activity, and a campaign is another activity, we will later refactor for better modeling

---

## Acceptance tests (what to validate)

* For the sample pivoted submission rows:

    * ETL produces one ledger row per pivoted line with correct `submission_line_id`, `sku_id`, `qty_delta` (34, 12, 2, 33), proper `party_id`, `tx_date`, and `provenance`.
    * `movement_fact` contains matching rows with correct `direction` and `team_id`.
    * Re-running ETL for same submission does not create duplicates (UNIQUE enforced).
    * `stock_balance` aggregated from ledger matches expected totals after applying the sample submission.

---

## Minimal next steps (practical)

1. Create a small mapping table: `template_name -> transaction_type` and `canonical_element_id -> element_role (qty, category, tx_date, team, batch, expiry)`.
2. Implement ETL for one template type (e.g., `hf_receipt_902`) end-to-end and run the acceptance tests above.
3. Iterate to support other templates (hf_return, wh_team_receipt, inventory_901).
4. Add `party` abstraction later: swap lookups `team/org_unit/assignment` → `party` mapping; downstream ETL unchanged.

---

## Short checklist to attach to the ticket

* [ ] `template -> tx_type` mapping table created.
* [ ] `element -> role` mapping table created (qty, category→sku, tx_date, batch, expiry).
* [ ] Deterministic `submission_line_id` strategy implemented.
* [ ] `inventory_ledger` inserts idempotent (UNIQUE enforced).
* [ ] `movement_fact` generation implemented.
* [ ] `stock_balance` aggregation validated.
* [ ] Provenance JSON populated for every ledger row.
* [ ] Migration path documented for future `party` table swap.
