# Spring boot app, with postgresql

## Some of the System core entities:
- entities that are widely used and have relation to data submission.
- samples were exported from the pg db table as json or tabular. the below samples have the same schema in db.

## Sample 1: OptionSet sample:
```json
[
  {
    "id": "01K1EHPE2H6JQ2T5JCC771BP5W",
    "uid": "LVPJtB1pcr7",
    "code": null,
    "name": "posters_lab",
    "options": null,
    "translations": "[]",
    "created_by": "admin",
    "created_date": "30/7/2025 20:52:01.745324",
    "last_modified_by": "admin",
    "last_modified_date": "30/7/2025 20:52:01.745324",
    "properties_map": null
  }
]
```

### Sample 2: OptionSet's Options sample:

```json
[
  {
    "id": "01K1EHH6BPZ6C0MRVMNEXMQWAC",
    "code": "malaria_case_management",
    "name": "malaria_case_management",
    "translations": "[{\"value\": \"malaria_case_management\", \"locale\": \"en\", \"property\": \"name\"}, {\"value\": \"ملصقات تدبير حالات الملاريا\", \"locale\": \"ar\", \"property\": \"name\"}]",
    "sort_order": 1,
    "option_set_id": "01K1EHPE2H6JQ2T5JCC771BP5W",
    "uid": "yqNt9De42lV"
  },
  {
    "id": "01K1EHJN98G1V9BWZZF5NF3XZ8",
    "code": "mrdt_posters",
    "name": "mrdt_posters",
    "translations": "[{\"value\": \"ملصقات فحص الملاري\", \"locale\": \"ar\", \"property\": \"name\"}, {\"value\": \"mrdt_posters\", \"locale\": \"en\", \"property\": \"name\"}]",
    "sort_order": 2,
    "option_set_id": "01K1EHPE2H6JQ2T5JCC771BP5W",
    "uid": "JAkbOcS6pZB"
  },
  {
    "id": "01K1EHN2HG1ZQPN5DHXYH0ZDNE",
    "code": "standard_case_difination",
    "name": "standard_case_difination",
    "translations": "[{\"value\": \"standard_case_difination\", \"locale\": \"en\", \"property\": \"name\"}, {\"value\": \"ملصقات التعريف القياسي للأمراض\", \"locale\": \"ar\", \"property\": \"name\"}]",
    "sort_order": 3,
    "option_set_id": "01K1EHPE2H6JQ2T5JCC771BP5W",
    "uid": "Yf6TbbmpXxo"
  },
  {
    "id": "01K1EHP25XCYSC6SWW966QDF4R",
    "code": "drdt_poster",
    "name": "drdt_poster",
    "translations": "[{\"value\": \"drdt_poster\", \"locale\": \"en\", \"property\": \"name\"}, {\"value\": \"ملصق فحص الضنك\", \"locale\": \"ar\", \"property\": \"name\"}]",
    "sort_order": 4,
    "option_set_id": "01K1EHPE2H6JQ2T5JCC771BP5W",
    "uid": "rWbfMeioTil"
  }
]
```


**org_unit sample:**

| id                         | uid         | code    | name                   | path                                             | level | parent_id                  | translations | created_by | created_date    | last_modified_by | last_modified_date |
|----------------------------|-------------|---------|------------------------|--------------------------------------------------|-------|----------------------------|--------------|------------|-----------------|------------------|--------------------|
| 01JYF3VBMB5QPYFJ43YVJQZ1MZ | gBsSQR1DZSU | 1       | Yemen                  | ,gBsSQR1DZSU                                     | 1     |                            |              | admin      | 1/1/01 12:06 AM | system           | 1/1/23 12:00 AM    |
| 01JYF3VBMBCM7RBMR5SET9ZGAD | HP7pr1yGRlf | 18      | Al Hudaydah            | ,gBsSQR1DZSU,HP7pr1yGRlf                         | 2     | 01JYF3VBMB5QPYFJ43YVJQZ1MZ |              | admin      | 1/1/01 12:06 AM | system           | 1/1/23 12:00 AM    |
| 01JYF3VBMBXD1J29AR083KDJ0C | BVIGsLlGR1a | 1807    | Az Zaydiyah            | ,gBsSQR1DZSU,HP7pr1yGRlf,BVIGsLlGR1a             | 3     | 01JYF3VBMBCM7RBMR5SET9ZGAD |              | admin      | 1/1/01 12:06 AM | system           | 1/1/23 12:00 AM    |
| 01JYF3VBMBA1DV4NQQS8WC9PDE | B8RiTGy6Emb | 1807034 | وحدة  الصحية   دير علي | ,gBsSQR1DZSU,HP7pr1yGRlf,BVIGsLlGR1a,B8RiTGy6Emb | 4     | 01JYF3VBMBXD1J29AR083KDJ0C |              | admin      | 1/1/01 12:06 AM | system           | 1/1/23 3:05 AM     |

**org unit levels sample (`ou_level` table):**
| id | uid         | code | name     | level | translations | created_by | created_date | last_modified_by | last_modified_date |
|----|-------------|------|----------|-------|--------------|------------|--------------|------------------|--------------------|
| 1  | AZb0k8s7cf8 |      | Country  | 0     |              | admin      | 5/23/2024    | admin            | 5/23/2024          |
| 2  | SFkMTSqslem |      | Gov      | 1     |              | admin      | 5/23/2024    | admin            | 5/23/2024          |
| 3  | a8UntlZBHNH |      | District | 2     |              | admin      | 5/23/2024    | admin            | 5/23/2024          |
| 4  | la6MffE1KrN |      | Hf       | 3     |              | admin      | 5/23/2024    | admin            | 5/23/2024          |

**org_unit_group sample:**
| id                         | uid         | code                       | name                       | symbol | color | inactive | translations | created_date | last_modified_by | last_modified_date | properties_map |
|----------------------------|-------------|----------------------------|----------------------------|--------|-------|----------|--------------|--------------|------------------|--------------------|----------------|
| 01K1HTJQY2816CYK7E649BA4D0 | VTfNVNyFmvH | health_facilities_targeted | health_facilities_targeted |        |       | f        | []           | 25:01.3      | admin            | 11:00.8            | {}             |
| 01K1M3E72G7V9T5370P40M2HNY | SCwVQlLnHyE | all_health_facilities      | all_health_facilities      |        |       | f        | []           | 38:19.0      | admin            | 49:16.8            | {}             |
| 01K2TF7GMG2J162KM1E54JS1D4 | hanDriCGzuH | districts                  | districts                  |        |       | f        | []           | 15:30.7      | admin            | 37:19.0            | {}             |
| 01K31EE4YK4J10YKA8VM3WB49Z | bMibxEBlYFh | malaria_units              | malaria_units              |        |       | f        | []           | 16:20.6      | admin            | 52:10.2            | {}             |
| 01K8RRH96AW1V953JBXCW0WZTX | doXtIO3wsQt | chvs                       | chvs                       |        |       | f        | []           | 53:50.0      | admin            | 53:50.0            | {}             |

**org_unit_group_members sample:**

| group_id                   | org_unit_id                |
|----------------------------|----------------------------|
| 01K2TF7GMG2J162KM1E54JS1D4 | 01JYF3VBMBXD1J29AR083KDJ0C |

**assignment sample**:

| id                         | uid         | deleted | deleted_at | activity_id                | start_day | org_unit_id                | team_id                    | forms                          | allocated_resources | status | last_submitted_by | created_date               | last_modified_date         | properties_map |
|----------------------------|-------------|---------|------------|----------------------------|-----------|----------------------------|----------------------------|--------------------------------|---------------------|--------|-------------------|----------------------------|----------------------------|----------------|
| 01JYF3NXDHSASQM9P2WZSF9GMV | nVQPGrQDsMv | f       |            | 01JYF39TAEZJSYVR98ZDY3RF05 |           | 01JYF3VBMBXD1J29AR083KDJ0C | 01JYF3WWPFEJW6J6KFMW2Z82QP | []                             | {}                  |        |                   | 2025-09-11 21:26:07.730383 | 2025-09-11 21:26:07.730383 | {}             |
| 01K4XAQCQDS0XE5BBGAT3B975N | A43O3LtVXiw | f       |            | 01K3PD3AH5V8NX0ZK3VSJPZ25T | 1         | 01JYF3VBMBA1DV4NQQS8WC9PDE | 01K490152M016ZNV65HTCF2SDJ | ["rkwH5QNofRn", "M3fdtzBSpn8"] | {}                  |        |                   | 2025-09-11 23:26:07.730383 | 2025-09-11 21:26:07.730383 | {}             |

**team**

| id                         | uid         | code   | name       | description | disabled | activity_id                | form_permissions                                                                                                                                                                                | created_date               | last_modified_date         | properties_map |
|----------------------------|-------------|--------|------------|-------------|----------|----------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------------------------|----------------------------|----------------|
| 01JYF3WWPF0035PFSVW4W8Z7KW | kCdSuDDiy7K | 171313 | chv_171313 |             | f        | 01JYF39TAEWBHJQYWYS23BY9Y7 | [{"form": "KcsA3KETRbY",   "permissions": ["ADD_SUBMISSIONS"]}, {"form":   "RQlMiMcukid", "permissions":   ["ADD_SUBMISSIONS"]}, {"form": "zglED4TsbTh",   "permissions": ["ADD_SUBMISSIONS"]}] | 2025-09-11 21:26:07.730383 | 2025-09-11 21:26:07.730383 | {}             |
| 01JYF3WWPF00NG4DESBM2XMH9N | p1Uzd0fnD5l | 110144 | chv_110144 |             | f        | 01JYF39TAEWBHJQYWYS23BY9Y7 | [{"form": "zglED4TsbTh",   "permissions": ["ADD_SUBMISSIONS"]}, {"form":   "KcsA3KETRbY", "permissions":   ["ADD_SUBMISSIONS"]}, {"form": "RQlMiMcukid",   "permissions": ["ADD_SUBMISSIONS"]}] | 2025-09-11 21:26:07.730383 | 2025-09-11 21:26:07.730383 | {}             |

**activity**

| id                         | uid         | code                        | name                                                 | start_date              | end_date                | disabled | project_id                 | translations | created_by | created_date               | last_modified_by | last_modified_date           | properties_map |
|----------------------------|-------------|-----------------------------|------------------------------------------------------|-------------------------|-------------------------|----------|----------------------------|--------------|------------|----------------------------|------------------|------------------------------|----------------|
| 01JYF39TAEYP616TGNCXC4X440 | kSH3mgaplDg | ITNSMovement202412Phase2FTL | حركة الناموسيات-ديسمبر-2024-المرحلة الثانية-قادة فرق | 2024-12-16 00:00:00     | 2024-12-21 00:00:00     | t        | 01JYF3WWHDP96AB526NQ8CQ7ND | []           | admin      | 2024-12-11 14:44:03.229209 | admin            | 2024-12-11   14:44:03.229209 |                |
| 01JYF39TAEZJSYVR98ZDY3RF05 | Xw7SDybz1Un | LSMSeP2024Supers            | المكافحة اليرقية 2024 مشرفين                         | 2024-09-20 23:36:12.335 | 2024-09-29 23:36:12.335 | t        | 01JYF3WWHD0JQ1Y0EQG20P84G9 | []           | admin      | 2024-09-17 23:36:18.079917 | admin            | 2024-09-17   23:36:18.079917 |                |
