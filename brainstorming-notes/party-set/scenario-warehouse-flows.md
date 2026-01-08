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

## Minimal starter set (what to implement first)

1. **Receipt**
2. **Issue / Distribution**
3. **Transfer** (internal movements)
4. **Stock Count / Stocktake**
5. **Adjustment / Write-off**
6. **Consumption Report** (reporting, not a stock-moving event)

Everything else (returns, campaign-specific movements) can be implemented as combinations/special cases of the above.

---

## Essential fields to capture (per transaction)

Transaction ID, date/time, origin, destination, SKU, batch/lot, expiry, quantity, unit, transaction type (routine vs
campaign), reference (delivery note), responsible person, condition on receipt, remarks/signature/photo. (logged in user
context and his config migh be utilized to infere parts of these to make the ux best)

