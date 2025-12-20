### 1. `mv_surveillance_totals` (The Foundation)

* **What it does:** Aggregates raw facility-level data into weekly totals.
* **Key Variables:**
* `tpr`: Test Positivity Rate .
* `rdt_share` / `mic_share`: The proportion of cases diagnosed via Rapid Diagnostic Test vs. Microscopy.

* **Assessment:** Correct. It uses `CASE` statements to prevent "division by zero" errors and filters for
  `class_id < 7` (likely excluding non-malaria or aggregate rows).

```sql
-- 1. create schema for analytics if not exists
-- 2. create materialized view of totals (replace public.surveillance_by_class with your table)
CREATE MATERIALIZED VIEW analytics.mv_surveillance_totals AS
SELECT year,
       week,
       period_start_date,
       gov_id,
       gov_name,
       district_id_nmcp                                                                    AS district_id,
       dis_name,
       hf_code,
       hf_name,
       hf_owner,
       hf_type,
       -- raw sums (only rows with class_id < 7)
       SUM(rdt_examined)                                                                   AS rdt_examined,
       SUM(rdt_positive)                                                                   AS rdt_positive,
       SUM(rdt_pf)                                                                         AS rdt_pf,
       SUM(rdt_pv)                                                                         AS rdt_pv,
       SUM(rdt_other)                                                                      AS rdt_other,
       SUM(mic_examined)                                                                   AS mic_examined,
       SUM(mic_positive)                                                                   AS mic_positive,
       SUM(mic_pf)                                                                         AS mic_pf,
       SUM(mic_pv)                                                                         AS mic_pv,
       SUM(mic_mix)                                                                        AS mic_mix,
       SUM(mic_pother)                                                                     AS mic_pother,
       SUM(probable_cases)                                                                 AS probable_cases,
       SUM(inpatienttotal)                                                                 AS inpatienttotal,
       SUM(deaths)                                                                         AS deaths,
       -- derived fundamentals
       (SUM(rdt_positive) + SUM(mic_positive))::bigint                                     AS confirmed_cases,
       (SUM(rdt_examined) + SUM(mic_examined))::bigint                                     AS tests_total,
       CASE
           WHEN (SUM(rdt_examined) + SUM(mic_examined)) = 0
               THEN NULL
           ELSE ((SUM(rdt_positive) + SUM(mic_positive))::numeric
               / (SUM(rdt_examined) + SUM(mic_examined))) END                              AS tpr,
       CASE
           WHEN (SUM(rdt_positive) + SUM(mic_positive)) = 0
               THEN NULL
           ELSE (SUM(rdt_positive)::numeric / (SUM(rdt_positive) + SUM(mic_positive))) END AS rdt_share,
       CASE
           WHEN (SUM(rdt_positive) + SUM(mic_positive)) = 0
               THEN NULL
           ELSE (SUM(mic_positive)::numeric / (SUM(rdt_positive) + SUM(mic_positive))) END AS mic_share
FROM public.surveillance_by_class
WHERE class_id < 7
GROUP BY year, week, period_start_date,
         gov_id, gov_name,
         district_id_nmcp, dis_name,
         hf_code, hf_name, hf_owner, hf_type;

```

### 2. MOVING AVG & week-on-week growth, `mv_surveillance_ma` (The Trend)

* **What it does:** Smooths out "noise" in the data using window functions.
* **Key Variables:**
* `confirmed_cases_ma3`: A 3-week average of cases.
* `wow_growth`: The percentage change from the previous week.


* **Assessment:** Makes sense. Using a moving average is standard practice to avoid overreacting to a single-day
  reporting spike.

```sql
-- B — Materialize moving averages & week-on-week growth
CREATE MATERIALIZED VIEW analytics.mv_surveillance_ma AS
SELECT YEAR,
       week,
       period_start_date,
       gov_id,
       gov_name,
       district_id,
       dis_name,
       hf_code,
       hf_name,
       confirmed_cases,
       tests_total,
       tpr,
       deaths,
       inpatienttotal,
-- 3-week moving average (current + 2 previous weeks)
       AVG(confirmed_cases)
       OVER (PARTITION BY gov_id, district_id, hf_code ORDER BY period_start_date ROWS BETWEEN 2 PRECEDING AND CURRENT ROW) AS confirmed_cases_ma3,
       AVG(tpr)
       OVER (PARTITION BY gov_id, district_id, hf_code ORDER BY period_start_date ROWS BETWEEN 2 PRECEDING AND CURRENT ROW) AS tpr_ma3,
-- week-on-week growth (relative)
       LAG(confirmed_cases)
       OVER (PARTITION BY gov_id, district_id, hf_code ORDER BY period_start_date)                                          AS confirmed_prev_week,
       CASE
           WHEN LAG(confirmed_cases) OVER (PARTITION BY gov_id, district_id, hf_code ORDER BY period_start_date) IS NULL
               OR LAG(confirmed_cases) OVER (PARTITION BY gov_id, district_id, hf_code ORDER BY period_start_date) = 0
               THEN NULL
           ELSE (confirmed_cases -
                 LAG(confirmed_cases) OVER (PARTITION BY gov_id, district_id, hf_code ORDER BY period_start_date)) /
                NULLIF(LAG(confirmed_cases) OVER (PARTITION BY gov_id, district_id, hf_code ORDER BY period_start_date),
                       0)
           END                                                                                                              AS wow_growth

FROM analytics.mv_surveillance_totals;
```

### 3. `mv_surveillance_baseline` (The "Normal")

* **What it does:** Calculates what a "typical" week looks like based on historical data.
* **Key Variables:**
* `epidemic_threshold`: Calculated as .


* **Assessment:** Statistically sound. It excludes the current year (`maxy`) to ensure the baseline isn't biased by the
  current outbreak you are trying to detect.

```sql
-- C — Materialize historical weekly baseline & epidemic threshold
CREATE MATERIALIZED VIEW analytics.mv_surveillance_baseline AS

-- WITH maxy AS (SELECT MAX(YEAR) AS max_year FROM analytics.mv_surveillance_totals)

SELECT gov_id,
       gov_name,
       district_id,
       dis_name,
       hf_code,
       hf_name,
       week                                                                             AS week_of_year,
       COUNT(DISTINCT YEAR)                                                             AS years_count,
       AVG(confirmed_cases) :: NUMERIC                                                  AS weekly_mean_cases,
       COALESCE(STDDEV_POP(confirmed_cases), 0) :: NUMERIC                              AS weekly_std_cases,
-- epidemic threshold = mean + 2*std (tunable)
       (AVG(confirmed_cases) + 2 * COALESCE(STDDEV_POP(confirmed_cases), 0)) :: NUMERIC AS epidemic_threshold
FROM analytics.mv_surveillance_totals
-- WHERE YEAR < (SELECT max_year FROM maxy) -- exclude latest year
WHERE YEAR < 2025 -- exclude latest year

GROUP BY gov_id,
         gov_name,
         district_id,
         dis_name,
         hf_code,
         hf_name,
         week;
```

### 4 & 5. `mv_surveillance_alerts` (The Trigger)

* **What it does:** Compares current cases to the historical baseline.
* **Logic:** * `epidemic_flag`: True if current cases exceed the threshold.
* `epidemic_sustained`: True if the flag has been active for **two consecutive weeks**.


* **Assessment:** Excellent. In epidemiology, a single-week spike might be a reporting error; two weeks of sustained
  high cases is a confirmed outbreak.

#### 5.1 Alerts Sustained

```sql
CREATE MATERIALIZED VIEW analytics.mv_surveillance_alerts AS
SELECT 
    M.YEAR,
    M.week,
    M.period_start_date,
    M.gov_id,
    M.district_id,
    M.hf_code,
    M.confirmed_cases,
    M.confirmed_cases_ma3,
    b.weekly_mean_cases,
    b.weekly_std_cases,
    b.epidemic_threshold,
    CASE
       WHEN b.weekly_mean_cases IS NULL THEN
           NULL
       WHEN M.confirmed_cases > b.epidemic_threshold THEN
           TRUE
       ELSE FALSE
       END AS epidemic_flag
FROM analytics.mv_surveillance_ma M
         LEFT JOIN analytics.mv_surveillance_baseline b ON M.gov_id = b.gov_id
    AND M.district_id = b.district_id
    AND M.hf_code = b.hf_code
    AND M.week = b.week_of_year;
```

#### 5.2 Alerts Sustained

```sql
CREATE MATERIALIZED VIEW analytics.mv_surveillance_alerts2 AS
SELECT b.*,
       CASE
           WHEN b.epidemic_flag = TRUE
               AND LAG(b.epidemic_flag)
                   OVER (PARTITION BY b.gov_id, b.district_id, b.hf_code ORDER BY b.period_start_date) = TRUE THEN
               TRUE
           ELSE FALSE
           END AS epidemic_sustained
FROM analytics.mv_surveillance_alerts b;
```

### 6 & 7. District & Governorate Baselines

* **What it does:** Aggregates facility data up to higher geographic levels.
* **Formula Note:** It uses `SQRT(AVG(POWER(weekly_std_cases,2)))`. This is the correct way to calculate the "Pooled
  Standard Deviation" across different facilities.

#### 6. Baseline governorate

```sql
CREATE MATERIALIZED VIEW IF NOT EXISTS analytics.mv_baseline_district AS
SELECT gov_id,
       gov_name,
       district_id,
       dis_name,
       week_of_year,
       AVG(weekly_mean_cases)                AS weekly_mean_cases,
       SQRT(AVG(POWER(weekly_std_cases, 2))) AS weekly_std_cases,
       AVG(epidemic_threshold)               AS epidemic_threshold,
       SUM(years_count)                      AS years_count
FROM analytics.mv_surveillance_baseline
GROUP BY gov_id, gov_name, district_id, dis_name, week_of_year;
```

#### 7. Baseline governorate

```sql
CREATE MATERIALIZED VIEW IF NOT EXISTS analytics.mv_baseline_governorate AS
SELECT gov_id,
       gov_name,
       week_of_year,
       AVG(weekly_mean_cases)                AS weekly_mean_cases,
       SQRT(AVG(POWER(weekly_std_cases, 2))) AS weekly_std_cases,
       AVG(epidemic_threshold)               AS epidemic_threshold,
       SUM(years_count)                      AS years_count
FROM analytics.mv_surveillance_baseline
GROUP BY gov_id, gov_name, week_of_year;
```

### 8. `mv_surveillance_baseline_fallback` (The Safety Net)

* **What it does:** Solves the "New Facility" problem. If a health facility is new and has no history, the system "falls
  back" to use the District average as its baseline.
* **Hierarchy:** Facility District Governorate.
* **Assessment:** Very clever. This prevents "None" results for newer clinics.

```sql
CREATE MATERIALIZED VIEW IF NOT EXISTS analytics.mv_surveillance_baseline_fallback AS
SELECT m.year,
       m.week                     AS week_of_year,
       m.period_start_date,
       m.gov_id,
       m.gov_name,
       m.district_id,
       m.dis_name,
       m.hf_code,
       m.hf_name,
       -- prefer facility baseline if exists with >=2 years, else district baseline if >=2, else governorate baseline
       COALESCE(
           CASE WHEN b.years_count >= 2 THEN b.weekly_mean_cases END,
           CASE WHEN d.years_count >= 2 THEN d.weekly_mean_cases END,
           CASE WHEN g.years_count >= 2 THEN g.weekly_mean_cases END
       )                          AS weekly_mean_cases,
       COALESCE(
           CASE WHEN b.years_count >= 2 THEN b.weekly_std_cases END,
           CASE WHEN d.years_count >= 2 THEN d.weekly_std_cases END,
           CASE WHEN g.years_count >= 2 THEN g.weekly_std_cases END
       )                          AS weekly_std_cases,
       COALESCE(
           CASE WHEN b.years_count >= 2 THEN b.epidemic_threshold END,
           CASE WHEN d.years_count >= 2 THEN d.epidemic_threshold END,
           CASE WHEN g.years_count >= 2 THEN g.epidemic_threshold END
       )                          AS epidemic_threshold,
       -- source used for baseline
       CASE
           WHEN b.years_count >= 2 THEN 'hf'
           WHEN d.years_count >= 2 THEN 'district'
           WHEN g.years_count >= 2 THEN 'gov'
           ELSE 'none'
           END                    AS baseline_source,
       COALESCE(b.years_count, 0) AS hf_years_count,
       COALESCE(d.years_count, 0) AS district_years_count,
       COALESCE(g.years_count, 0) AS gov_years_count
FROM analytics.mv_surveillance_ma m
         LEFT JOIN analytics.mv_surveillance_baseline b
                   ON m.hf_code = b.hf_code AND m.week = b.week_of_year
         LEFT JOIN analytics.mv_baseline_district d
                   ON m.district_id = d.district_id AND m.week = d.week_of_year
         LEFT JOIN analytics.mv_baseline_governorate g
                   ON m.gov_id = g.gov_id AND m.week = g.week_of_year;
```

### 9. `mv_surveillance_priority` (The Final Outcome)

* **What it does:** This is your "Action Dashboard." It calculates a **Priority Score** (0–200+) to tell health
  officials where to send resources first.
* **The Scoring Logic:**
* **+100 pts:** Sustained epidemic (highest priority).
* **+60 pts:** New epidemic flag.
* **+Up to 40 pts:** High week-on-week growth.
* **+Up to 40 pts:** High Test Positivity Rate.
* **+20 pts per death:** Heavily weights mortality.

```sql
CREATE MATERIALIZED VIEW IF NOT EXISTS analytics.mv_surveillance_priority AS
WITH base AS (
SELECT 
    t.year,
    t.week,
    t.period_start_date,
    t.gov_id,
    t.gov_name,
    t.district_id,
    t.dis_name,
    t.hf_code,
    t.hf_name,
    t.hf_owner,
    t.hf_type,
    t.rdt_examined,
    t.rdt_positive,
    t.mic_examined,
    t.mic_positive,
    t.probable_cases,
    t.inpatienttotal,
    t.rdt_share,
    t.mic_share,
    t.confirmed_cases,
    t.tests_total,
    t.deaths,
    t.tpr,
    -- 3-week moving average of confirmed cases (current + 2 prev)
    AVG(t.confirmed_cases)
    OVER (PARTITION BY t.gov_id, t.district_id, t.hf_code ORDER BY t.period_start_date ROWS BETWEEN 2 PRECEDING AND CURRENT ROW) AS confirmed_cases_ma3,
    -- 3-week moving average of tpr
    AVG(t.tpr)
    OVER (PARTITION BY t.gov_id, t.district_id, t.hf_code ORDER BY t.period_start_date ROWS BETWEEN 2 PRECEDING AND CURRENT ROW) AS tpr_ma3,
    -- previous week cases
    LAG(t.confirmed_cases)
    OVER (PARTITION BY t.gov_id, t.district_id, t.hf_code ORDER BY t.period_start_date)                                          AS prev_week_cases,
    -- recent 2-week sum (current + previous)
    t.confirmed_cases + COALESCE(LAG(t.confirmed_cases)
                              OVER (PARTITION BY t.gov_id, t.district_id, t.hf_code ORDER BY t.period_start_date),
                              0)                                                                                              AS recent_2wk_cases
FROM analytics.mv_surveillance_totals t

)

SELECT b.year,
       b.week,
       b.period_start_date,
       b.gov_id,
       b.gov_name,
       b.district_id,
       b.dis_name,
       b.hf_code,
       b.hf_name,
       b.confirmed_cases,
       b.confirmed_cases_ma3,
       b.tpr_ma3,
       b.prev_week_cases,
       b.recent_2wk_cases,
       b.deaths,
       -- growth
       CASE
           WHEN b.prev_week_cases IS NULL OR b.prev_week_cases = 0 THEN NULL
           ELSE (b.confirmed_cases - b.prev_week_cases)::numeric / b.prev_week_cases END AS wow_growth,
       -- join to fallback baseline
       f.weekly_mean_cases,
       f.weekly_std_cases,
       f.epidemic_threshold,
       f.baseline_source,
       b.tests_total,
       -- flags from alerts2 if present
       a.epidemic_flag,
       a.epidemic_sustained,
       -- relative risk vs baseline
       CASE
           WHEN f.weekly_mean_cases IS NULL OR f.weekly_mean_cases = 0 THEN NULL
           ELSE b.confirmed_cases::numeric / f.weekly_mean_cases END                     AS rr_vs_baseline,
       -- priority score (tunable)
       (
           (CASE WHEN a.epidemic_sustained THEN 100 ELSE 0 END) +
           (CASE WHEN a.epidemic_flag AND NOT a.epidemic_sustained THEN 60 ELSE 0 END) +
           (CASE
                WHEN (b.confirmed_cases - COALESCE(b.prev_week_cases, 0)) > 0
                    THEN LEAST(((b.confirmed_cases - COALESCE(b.prev_week_cases, 0))::numeric /
                                GREATEST(NULLIF(b.prev_week_cases, 0), 1)) * 20, 40)
                ELSE 0 END) +
           (CASE WHEN b.tpr_ma3 IS NOT NULL THEN LEAST(b.tpr_ma3 * 100.0, 40) ELSE 0 END) +
           LEAST(b.deaths * 20, 40) +
           (CASE
                WHEN (CASE
                          WHEN f.weekly_mean_cases IS NULL OR f.weekly_mean_cases = 0 THEN NULL
                          ELSE b.confirmed_cases::numeric / f.weekly_mean_cases END) > 1
                    THEN LEAST(((COALESCE(b.confirmed_cases::numeric, 0) / NULLIF(f.weekly_mean_cases, 0)) - 1) * 20, 40)
                ELSE 0 END)
           )::numeric                                                                    AS priority_score
FROM base b
         LEFT JOIN analytics.mv_surveillance_baseline_fallback f
                   ON b.year = f.year AND b.week = f.week_of_year AND b.hf_code = f.hf_code AND
                      b.district_id = f.district_id AND b.gov_id = f.gov_id
         LEFT JOIN analytics.mv_surveillance_alerts2 a
                   ON b.year = a.year AND b.week = a.week AND b.hf_code = a.hf_code AND
                      b.district_id = a.district_id AND b.gov_id = a.gov_id;
```

### How the Final Outcome looks

The final table (`mv_surveillance_priority`) will look like a ranked list of health facilities.

| Year | Week | HF Name        | Confirmed | Growth | Status    | Priority Score |
|------|------|----------------|-----------|--------|-----------|----------------|
| 2025 | 50   | Al-Amal Clinic | 45        | +120%  | Sustained | **245.5**      |
| 2025 | 50   | City Hospital  | 12        | +10%   | Normal    | **15.2**       |

### Potential Improvements & Corrections

1. **Redundancy:** SQL #9 re-calculates `confirmed_cases_ma3` and `wow_growth`. Since these were already calculated in
   SQL #2, you could simply join to `mv_surveillance_ma` to save processing time.
2. **The "Max Year" Logic:** In SQL #3, if the "latest year" in your database only has 1 week of data, the baseline will
   include the rest of that year. It is usually safer to define a specific cut-off date rather than just
   `YEAR < MAX(YEAR)`.
3. **Null Handling:** In the Priority Score, ensure `LEAST` functions handle `NULL` values; in some SQL dialects,
   `LEAST(NULL, 40)` returns `NULL`, which would break the entire addition. Adding `COALESCE(variable, 0)` inside the
   score calculation is safer.
