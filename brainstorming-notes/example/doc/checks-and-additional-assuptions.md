Checkout the last query check result, very important

```sql
-- ensures no referential mismatch between tall_canonical and canonical_element
SELECT COUNT(*) AS missing_ce_count
FROM public.tall_canonical tc
         LEFT JOIN public.canonical_element ce
                   ON tc.canonical_element_id::varchar = ce.id::varchar
WHERE ce.id IS NULL;
```

**result:** 0

---

```sql
-- confirms which CEs are repeats (have semantic_type = 'Repeat' and data_type = 'ARRAY'), and whether parent_repeat_id is actually NULL, empty string, or populated:

SELECT id,
       template_uid,
       preferred_name,
       data_type,
       semantic_type,
       parent_repeat_id,
       json_data_paths
FROM public.canonical_element
WHERE template_uid = 'YLcsWJlB7uy'
ORDER BY preferred_name NULLS LAST;
```

**result table:** (no parent is null, not empty string)

| id                                   | template_uid | preferred_name        | data_type | semantic_type | parent_repeat_id                     | json_data_paths                                   |
|--------------------------------------|--------------|-----------------------|-----------|---------------|--------------------------------------|---------------------------------------------------|
| 83d305b9-452a-3de1-8a6e-1ecfa0b43339 | YLcsWJlB7uy  | drdt_positive_types   | ARRAY     | Option        | 896f8227-2920-3f4b-8c11-ebaa03a7ec70 | ["patients.investigations.drdt_positive_types"]   |
| 2657f0c8-74c9-33aa-8211-7999d0c932fc | YLcsWJlB7uy  | emergency_team_type   | TEXT      | Option        |                                      | ["main.emergency_team_type"]                      |
| 896f8227-2920-3f4b-8c11-ebaa03a7ec70 | YLcsWJlB7uy  | investigations        | ARRAY     | Repeat        | 896f8227-2920-3f4b-8c11-ebaa03a7ec70 | ["patients.investigations"]                       |
| 26172272-d946-36c6-927b-9bb445bf332d | YLcsWJlB7uy  | is_test_preformed     | TEXT      | Option        | e69c29ac-ece3-3f06-8ced-fcf8b2da1d3b | ["patients.is_test_preformed"]                    |
| 69d7d0fb-9480-3457-8fe6-72fa70f6fe4e | YLcsWJlB7uy  | lab_test_type         | TEXT      | Option        | 896f8227-2920-3f4b-8c11-ebaa03a7ec70 | ["patients.investigations.lab_test_type"]         |
| f8bac256-d673-345c-aad7-6092cfa3add6 | YLcsWJlB7uy  | mic_positive_types    | ARRAY     | Option        | 896f8227-2920-3f4b-8c11-ebaa03a7ec70 | ["patients.investigations.mic_positive_types"]    |
| 0220bf1d-3800-3de6-a78a-6c7fe049f5fc | YLcsWJlB7uy  | mrdt_positive_types   | ARRAY     | Option        | 896f8227-2920-3f4b-8c11-ebaa03a7ec70 | ["patients.investigations.mrdt_positive_types"]   |
| 3462b78c-eb4c-3fcb-ba5a-858ddd783440 | YLcsWJlB7uy  | NotificationNumber    | INTEGER   |               |                                      | ["main.NotificationNumber"]                       |
| a431220c-3eec-32c6-a7c0-2524657498f0 | YLcsWJlB7uy  | other                 | TEXT      |               |                                      | ["main.other"]                                    |
| e269925e-01e7-3cff-9e09-6529ee58d4a4 | YLcsWJlB7uy  | other_lab_test_result | TEXT      |               | 896f8227-2920-3f4b-8c11-ebaa03a7ec70 | ["patients.investigations.other_lab_test_result"] |
| 80f698c9-5efe-30ae-b3f1-e9354371b37e | YLcsWJlB7uy  | other_lab_test_type   | TEXT      |               | 896f8227-2920-3f4b-8c11-ebaa03a7ec70 | ["patients.investigations.other_lab_test_type"]   |
| 63bff109-5d9d-39e7-8d7e-cff5cdda66b5 | YLcsWJlB7uy  | PatientId             | INTEGER   |               | e69c29ac-ece3-3f06-8ced-fcf8b2da1d3b | ["patients.PatientId"]                            |
| 7d1cd87b-c7b4-3ed7-b865-d3b47079e3cf | YLcsWJlB7uy  | PatientName           | TEXT      |               | e69c29ac-ece3-3f06-8ced-fcf8b2da1d3b | ["patients.PatientName"]                          |
| e69c29ac-ece3-3f06-8ced-fcf8b2da1d3b | YLcsWJlB7uy  | patients              | ARRAY     | Repeat        | e69c29ac-ece3-3f06-8ced-fcf8b2da1d3b | ["patients"]                                      |
| fef67f83-bbbf-3aa1-bb6c-4af8901f7b1c | YLcsWJlB7uy  | serialNumber          | INTEGER   |               | e69c29ac-ece3-3f06-8ced-fcf8b2da1d3b | ["patients.serialNumber"]                         |
| 41bde44c-de7c-35fe-8369-62cbd5f3e47e | YLcsWJlB7uy  | test_result           | TEXT      | Option        | 896f8227-2920-3f4b-8c11-ebaa03a7ec70 | ["patients.investigations.test_result"]           |
| ed46f43d-c8e3-303b-abdb-cb19f491ad01 | YLcsWJlB7uy  | visitdate             | TIMESTAMP |               |                                      | ["main.visitdate"]                                |

---

```sql
-- per-CE row counts:
SELECT canonical_element_id, COUNT(*) AS cnt
FROM public.tall_canonical
WHERE template_uid = 'YLcsWJlB7uy'
GROUP BY canonical_element_id
ORDER BY cnt DESC;
```

**result:**

| canonical_element_id                 | cnt  |
|--------------------------------------|------|
| 896f8227-2920-3f4b-8c11-ebaa03a7ec70 | 1692 |
| 26172272-d946-36c6-927b-9bb445bf332d | 1683 |
| fef67f83-bbbf-3aa1-bb6c-4af8901f7b1c | 1683 |
| 63bff109-5d9d-39e7-8d7e-cff5cdda66b5 | 1683 |
| 7d1cd87b-c7b4-3ed7-b865-d3b47079e3cf | 1683 |
| e69c29ac-ece3-3f06-8ced-fcf8b2da1d3b | 1683 |
| 41bde44c-de7c-35fe-8369-62cbd5f3e47e | 1676 |
| 69d7d0fb-9480-3457-8fe6-72fa70f6fe4e | 1676 |
| 0220bf1d-3800-3de6-a78a-6c7fe049f5fc | 94   |
| 2657f0c8-74c9-33aa-8211-7999d0c932fc | 63   |
| 3462b78c-eb4c-3fcb-ba5a-858ddd783440 | 63   |
| ed46f43d-c8e3-303b-abdb-cb19f491ad01 | 63   |
| a431220c-3eec-32c6-a7c0-2524657498f0 | 13   |
| 83d305b9-452a-3de1-8a6e-1ecfa0b43339 | 2    |

---

```sql
-- ensures no referential mismatch between tall_canonical and canonical_element
SELECT canonical_element_id, COUNT(*) AS cnt
FROM public.tall_canonical
WHERE template_uid = 'YLcsWJlB7uy'
GROUP BY canonical_element_id
ORDER BY cnt DESC;
```

**result:**
This is really important. I misunderstood the meaning of `parent_instance_id`. It is actually **null** for root-level
elements (we need to check), and it should be understood as the parent-level event: either the submission itself or the
nearest upward ancestor `repeat_instance_id` of the current repeat instance and its children.

So a child repeat row—and its children—should point to the submission if that repeat grouping is first-level, or to an
ancestor `repeat_instance_id` if it is nested inside a higher-level repeat. This point is crucial and we need to lock it
in. Only root individual elements (that are not repeat or a child of repeat) have their `parent_instance_id`, and `repeat_instance_id` = null.

| submission_uid | instance_key                                                                    | canonical_element_id                 | element_path                                         | parent_instance_id | repeat_instance_id         | value_text              |
|----------------|---------------------------------------------------------------------------------|--------------------------------------|------------------------------------------------------|--------------------|----------------------------|-------------------------|
| AEV5B3wJzYM    | AEV5B3wJzYM#main.emergency_team_type                                            | 2657f0c8-74c9-33aa-8211-7999d0c932fc | main.emergency_team_type                             |                    |                            | malaria_unit            |
| AEV5B3wJzYM    | AEV5B3wJzYM#main.NotificationNumber                                             | 3462b78c-eb4c-3fcb-ba5a-858ddd783440 | main.NotificationNumber                              |                    |                            | 22                      |
| AEV5B3wJzYM    | AEV5B3wJzYM#main.visitdate                                                      | ed46f43d-c8e3-303b-abdb-cb19f491ad01 | main.visitdate                                       |                    |                            | 2025-10-13              |
| AEV5B3wJzYM    | 01K9TJ770X55MDD930PQEG7TQJ#patients[0]                                          | e69c29ac-ece3-3f06-8ced-fcf8b2da1d3b | patients[0]                                          | AEV5B3wJzYM        | 01K9TJ770X55MDD930PQEG7TQJ |                         |
| AEV5B3wJzYM    | 01K9TJ770X55MDD930PQEG7TQJ#patients[0].investigations[0]                        | 896f8227-2920-3f4b-8c11-ebaa03a7ec70 | patients[0].investigations[0]                        | AEV5B3wJzYM        | 01K9TJ770X55MDD930PQEG7TQJ |                         |
| AEV5B3wJzYM    | 01K9TJ770X55MDD930PQEG7TQJ#patients[0].investigations[0].drdt_positive_types[0] | 83d305b9-452a-3de1-8a6e-1ecfa0b43339 | patients[0].investigations[0].drdt_positive_types[0] | AEV5B3wJzYM        | 01K9TJ770X55MDD930PQEG7TQJ | IgM                     |
| AEV5B3wJzYM    | 01K9TJ770X55MDD930PQEG7TQJ                                                      | 69d7d0fb-9480-3457-8fe6-72fa70f6fe4e | patients[0].investigations[0].lab_test_type          | AEV5B3wJzYM        | 01K9TJ770X55MDD930PQEG7TQJ | drdt                    |
| AEV5B3wJzYM    | 01K9TJ770X55MDD930PQEG7TQJ                                                      | 41bde44c-de7c-35fe-8369-62cbd5f3e47e | patients[0].investigations[0].test_result            | AEV5B3wJzYM        | 01K9TJ770X55MDD930PQEG7TQJ | positive                |
| AEV5B3wJzYM    | 01K9TJ770X55MDD930PQEG7TQJ                                                      | 26172272-d946-36c6-927b-9bb445bf332d | patients[0].is_test_preformed                        | AEV5B3wJzYM        | 01K9TJ770X55MDD930PQEG7TQJ | yes                     |
| AEV5B3wJzYM    | 01K9TJ770X55MDD930PQEG7TQJ                                                      | 63bff109-5d9d-39e7-8d7e-cff5cdda66b5 | patients[0].PatientId                                | AEV5B3wJzYM        | 01K9TJ770X55MDD930PQEG7TQJ | 1                       |
| AEV5B3wJzYM    | 01K9TJ770X55MDD930PQEG7TQJ                                                      | 7d1cd87b-c7b4-3ed7-b865-d3b47079e3cf | patients[0].PatientName                              | AEV5B3wJzYM        | 01K9TJ770X55MDD930PQEG7TQJ | ميمونه احمد محمد سليملن |
| AEV5B3wJzYM    | 01K9TJ770X55MDD930PQEG7TQJ                                                      | fef67f83-bbbf-3aa1-bb6c-4af8901f7b1c | patients[0].serialNumber                             | AEV5B3wJzYM        | 01K9TJ770X55MDD930PQEG7TQJ | 1                       |
| AEV5B3wJzYM    | 01K9TJ770XV06P0E40TSGKSHVW#patients[1]                                          | e69c29ac-ece3-3f06-8ced-fcf8b2da1d3b | patients[1]                                          | AEV5B3wJzYM        | 01K9TJ770XV06P0E40TSGKSHVW |                         |
| AEV5B3wJzYM    | 01K9TJ770XV06P0E40TSGKSHVW#patients[1].investigations[0]                        | 896f8227-2920-3f4b-8c11-ebaa03a7ec70 | patients[1].investigations[0]                        | AEV5B3wJzYM        | 01K9TJ770XV06P0E40TSGKSHVW |                         |
| AEV5B3wJzYM    | 01K9TJ770XV06P0E40TSGKSHVW                                                      | 69d7d0fb-9480-3457-8fe6-72fa70f6fe4e | patients[1].investigations[0].lab_test_type          | AEV5B3wJzYM        | 01K9TJ770XV06P0E40TSGKSHVW | drdt                    |
| AEV5B3wJzYM    | 01K9TJ770XV06P0E40TSGKSHVW                                                      | 41bde44c-de7c-35fe-8369-62cbd5f3e47e | patients[1].investigations[0].test_result            | AEV5B3wJzYM        | 01K9TJ770XV06P0E40TSGKSHVW | negative                |
| AEV5B3wJzYM    | 01K9TJ770XV06P0E40TSGKSHVW                                                      | 26172272-d946-36c6-927b-9bb445bf332d | patients[1].is_test_preformed                        | AEV5B3wJzYM        | 01K9TJ770XV06P0E40TSGKSHVW | yes                     |
| AEV5B3wJzYM    | 01K9TJ770XV06P0E40TSGKSHVW                                                      | 63bff109-5d9d-39e7-8d7e-cff5cdda66b5 | patients[1].PatientId                                | AEV5B3wJzYM        | 01K9TJ770XV06P0E40TSGKSHVW | 2                       |
| AEV5B3wJzYM    | 01K9TJ770XV06P0E40TSGKSHVW                                                      | 7d1cd87b-c7b4-3ed7-b865-d3b47079e3cf | patients[1].PatientName                              | AEV5B3wJzYM        | 01K9TJ770XV06P0E40TSGKSHVW | سالمه احمد محمد سليمان  |
| AEV5B3wJzYM    | 01K9TJ770XV06P0E40TSGKSHVW                                                      | fef67f83-bbbf-3aa1-bb6c-4af8901f7b1c | patients[1].serialNumber                             | AEV5B3wJzYM        | 01K9TJ770XV06P0E40TSGKSHVW | 2                       |
| APwd5sViApH    | APwd5sViApH#main.emergency_team_type                                            | 2657f0c8-74c9-33aa-8211-7999d0c932fc | main.emergency_team_type                             |                    |                            | central                 |
| APwd5sViApH    | APwd5sViApH#main.NotificationNumber                                             | 3462b78c-eb4c-3fcb-ba5a-858ddd783440 | main.NotificationNumber                              |                    |                            | 1                       |
| APwd5sViApH    | APwd5sViApH#main.visitdate                                                      | ed46f43d-c8e3-303b-abdb-cb19f491ad01 | main.visitdate                                       |                    |                            | 2025-11-14              |
| APwd5sViApH    | 01KAGZF1X0N5HC5XD9ZAEKM9YT#patients[0]                                          | e69c29ac-ece3-3f06-8ced-fcf8b2da1d3b | patients[0]                                          | APwd5sViApH        | 01KAGZF1X0N5HC5XD9ZAEKM9YT |                         |
| APwd5sViApH    | 01KAGZF1X0N5HC5XD9ZAEKM9YT#patients[0].investigations[0]                        | 896f8227-2920-3f4b-8c11-ebaa03a7ec70 | patients[0].investigations[0]                        | APwd5sViApH        | 01KAGZF1X0N5HC5XD9ZAEKM9YT |                         |
| APwd5sViApH    | 01KAGZF1X0N5HC5XD9ZAEKM9YT                                                      | 69d7d0fb-9480-3457-8fe6-72fa70f6fe4e | patients[0].investigations[0].lab_test_type          | APwd5sViApH        | 01KAGZF1X0N5HC5XD9ZAEKM9YT | mrdt                    |
| APwd5sViApH    | 01KAGZF1X0N5HC5XD9ZAEKM9YT                                                      | 41bde44c-de7c-35fe-8369-62cbd5f3e47e | patients[0].investigations[0].test_result            | APwd5sViApH        | 01KAGZF1X0N5HC5XD9ZAEKM9YT | negative                |
| APwd5sViApH    | 01KAGZF1X0N5HC5XD9ZAEKM9YT                                                      | 26172272-d946-36c6-927b-9bb445bf332d | patients[0].is_test_preformed                        | APwd5sViApH        | 01KAGZF1X0N5HC5XD9ZAEKM9YT | yes                     |
| APwd5sViApH    | 01KAGZF1X0N5HC5XD9ZAEKM9YT                                                      | 63bff109-5d9d-39e7-8d7e-cff5cdda66b5 | patients[0].PatientId                                | APwd5sViApH        | 01KAGZF1X0N5HC5XD9ZAEKM9YT | 1                       |
| APwd5sViApH    | 01KAGZF1X0N5HC5XD9ZAEKM9YT                                                      | 7d1cd87b-c7b4-3ed7-b865-d3b47079e3cf | patients[0].PatientName                              | APwd5sViApH        | 01KAGZF1X0N5HC5XD9ZAEKM9YT | علي قايد سعيد احمد      |
| APwd5sViApH    | 01KAGZF1X0N5HC5XD9ZAEKM9YT                                                      | fef67f83-bbbf-3aa1-bb6c-4af8901f7b1c | patients[0].serialNumber                             | APwd5sViApH        | 01KAGZF1X0N5HC5XD9ZAEKM9YT | 1                       |
