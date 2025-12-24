## Samples:

**`analytics.dim_org_unit` table sample:**

| id                         | org_unit_uid | code   | name_default | level_id | parent_id                  | path_text                                        | path_array                                        | ancestors                                                                                                                                                                                                                                   | level_0_name | name_en      | name_ar      | level_1_name | level_2_name | level_3_name | level_4_name | last_modified_by | source_last_modified_at    |
|----------------------------|--------------|--------|--------------|----------|----------------------------|--------------------------------------------------|---------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|--------------|--------------|--------------|--------------|--------------|--------------|--------------|------------------|----------------------------|
| 01JYF3VBMB5QPYFJ43YVJQZ1MZ | gBsSQR1DZSU  | 1      | Yemen        | 1        |                            | ,gBsSQR1DZSU                                     | {gBsSQR1DZSU}                                     | [{"uid": "gBsSQR1DZSU",   "name": "Yemen", "level": 1}]                                                                                                                                                                                     | Yemen        | Yemen        | Yemen        |              |              |              |              | system           | 2023-01-01 03:00:02.589+00 |
| 01JYF3VBMBCM7RBMR5SET9ZGAD | HP7pr1yGRlf  | 18     | Al Hudaydah  | 2        | 01JYF3VBMB5QPYFJ43YVJQZ1MZ | ,gBsSQR1DZSU,HP7pr1yGRlf                         | {gBsSQR1DZSU,HP7pr1yGRlf}                         | [{"uid": "gBsSQR1DZSU",   "name": "Yemen", "level": 1}, {"uid":   "HP7pr1yGRlf", "name": "Al Hudaydah",   "level": 2}]                                                                                                                      | Yemen        | Al Hudaydah  | Al Hudaydah  | Al Hudaydah  |              |              |              | system           | 2023-01-01 03:00:12.003+00 |
| 01JYF3VBMBYX9H9FBGNXZZCF0E | P8ZTfpK5hUq  | 17     | Hajjah       | 2        | 01JYF3VBMB5QPYFJ43YVJQZ1MZ | ,gBsSQR1DZSU,P8ZTfpK5hUq                         | {gBsSQR1DZSU,P8ZTfpK5hUq}                         | [{"uid": "gBsSQR1DZSU",   "name": "Yemen", "level": 1}, {"uid":   "P8ZTfpK5hUq", "name": "Hajjah",   "level": 2}]                                                                                                                           | Yemen        | Hajjah       | Hajjah       | Hajjah       |              |              |              | system           | 2023-01-01 03:00:05.128+00 |
| 01JYF3VBMB1TDGJ6BVERWM4DZ4 | nbnUA0v7d3F  | 1727   | Bani Al Awam | 3        | 01JYF3VBMBYX9H9FBGNXZZCF0E | ,gBsSQR1DZSU,P8ZTfpK5hUq,nbnUA0v7d3F             | {gBsSQR1DZSU,P8ZTfpK5hUq,nbnUA0v7d3F}             | [{"uid": "gBsSQR1DZSU",   "name": "Yemen", "level": 1}, {"uid":   "P8ZTfpK5hUq", "name": "Hajjah",   "level": 2}, {"uid": "nbnUA0v7d3F", "name":   "Bani Al Awam", "level": 3}]                                                             | Yemen        | Bani Al Awam | Bani Al Awam | Hajjah       | Bani Al Awam |              |              | system           | 2023-01-01 03:00:12.005+00 |
| 01JYF3VBMBH63D0QE6HHTYKP68 | rHeaMC0cIUU  | 172724 | جبل نمر      | 4        | 01JYF3VBMB1TDGJ6BVERWM4DZ4 | ,gBsSQR1DZSU,P8ZTfpK5hUq,nbnUA0v7d3F,rHeaMC0cIUU | {gBsSQR1DZSU,P8ZTfpK5hUq,nbnUA0v7d3F,rHeaMC0cIUU} | [{"uid": "gBsSQR1DZSU",   "name": "Yemen", "level": 1}, {"uid":   "P8ZTfpK5hUq", "name": "Hajjah",   "level": 2}, {"uid": "nbnUA0v7d3F", "name":   "Bani Al Awam", "level": 3}, {"uid":   "rHeaMC0cIUU", "name": "جبل نمر",   "level": 4}]  | Yemen        | جبل نمر      | جبل نمر      | Hajjah       | Bani Al Awam | جبل نمر      |              | system           | 2023-01-01 06:00:02.048+00 |
| 01JYF3VBMBEZDH8Z35H5834CHD | PM1wztDqmIy  | 172727 | بني غشيم     | 4        | 01JYF3VBMB1TDGJ6BVERWM4DZ4 | ,gBsSQR1DZSU,P8ZTfpK5hUq,nbnUA0v7d3F,PM1wztDqmIy | {gBsSQR1DZSU,P8ZTfpK5hUq,nbnUA0v7d3F,PM1wztDqmIy} | [{"uid": "gBsSQR1DZSU",   "name": "Yemen", "level": 1}, {"uid":   "P8ZTfpK5hUq", "name": "Hajjah",   "level": 2}, {"uid": "nbnUA0v7d3F", "name":   "Bani Al Awam", "level": 3}, {"uid":   "PM1wztDqmIy", "name": "بني غشيم",   "level": 4}] | Yemen        | بني غشيم     | بني غشيم     | Hajjah       | Bani Al Awam | بني غشيم     |              | system           | 2023-01-01 06:00:02.048+00 |

**`public.ou_level` table sample:**

| id | uid         | code | name       | created_by | created_date               | last_modified_by | last_modified_date         |
|----|-------------|------|------------|------------|----------------------------|------------------|----------------------------|
| 1  | AZb0k8s7cf8 |      | Country    | admin      | 2024-05-23 00:06:20.395612 | admin            | 2024-05-23 00:06:20.395612 |
| 2  | SFkMTSqslem |      | Gov        | admin      | 2024-05-23 00:06:20.395612 | admin            | 2024-05-23 00:06:20.395612 |
| 3  | a8UntlZBHNH |      | District   | admin      | 2024-05-23 00:06:20.395612 | admin            | 2024-05-23 00:06:20.395612 |
| 4  | la6MffE1KrN |      | Hf/Village | admin      | 2024-05-23 00:06:20.395612 | admin            | 2024-05-23 00:06:20.395612 |

**`analytics.dim_team` table sample:**

| team_id                    | team_uid    | code | name_default | activity_id                | activity_uid | last_modified_by | source_last_modified_at       |
|----------------------------|-------------|------|--------------|----------------------------|--------------|------------------|-------------------------------|
| 01JYF3WWPF1A736DZ158X9DRKE | Z8VNSl7Rk1f | 4    | 4            | 01JYF39TAENS43R437CVEAENKN | CrFwE3e5w5S  | admin            | 2025-05-22 15:27:37.162759+00 |
| 01JYF3WWPF1N952VNE8BQ507WF | EWxMQUICQ8K | 11   | 11           | 01JYF39TAENS43R437CVEAENKN | CrFwE3e5w5S  | admin            | 2025-05-22 15:36:44.589713+00 |

**`public.team_managed_teams` table sample:**

which team is managing which teams.

* `team_id`: the manager team.
* `managed_team_id`: the managed team

| managed_team_id            | team_id                    |
|----------------------------|----------------------------|
| 01JYF3WWPF1A736DZ158X9DRKE | 01JYF3WWPF81GJSZNEB285V0NJ |
| 01JYF3WWPF1N952VNE8BQ507WF | 01JYF3WWPF7DYP63V0GX0YTG5D |

**`**analytics.dim_team_form_permission`** table samples:

`form_uid`=`template_uid
what forms is a team has access to, to submit.

| id  | team_uid    | team_id                    | template_uid | permissions_jsonb   | last_modified_by | source_last_modified_at       |
|-----|-------------|----------------------------|--------------|---------------------|------------------|-------------------------------|
| 310 | Z8VNSl7Rk1f | 01JYF3WWPF1A736DZ158X9DRKE | Eelt7ZePvz0  | ["ADD_SUBMISSIONS"] | admin            | 2025-05-22 15:27:37.162759+00 |
| 389 | EWxMQUICQ8K | 01JYF3WWPF1N952VNE8BQ507WF | Eelt7ZePvz0  | ["ADD_SUBMISSIONS"] | admin            | 2025-05-22 15:36:44.589713+00 |

**`**analytics.dim_assignment`** table samples:

| org_unit_uid | org_unit_code | team_id                    | team_uid    | team_code | forms_jsonb     | form_uid    | allocated_resources | status | last_submitted_by | last_modified_by | source_last_modified_at       |
|--------------|---------------|----------------------------|-------------|-----------|-----------------|-------------|---------------------|--------|-------------------|------------------|-------------------------------|
| rHeaMC0cIUU  | 18102314003   | 01JYF3WWPFB330039NX5Z53KZD | BsuI11ZMg0b | 9         | ["Eelt7ZePvz0"] | Eelt7ZePvz0 | {}                  |        |                   | system           | 2025-04-26 03:00:02.296794+00 |
| PM1wztDqmIy  | 17112510310   | 01JYF3WWPFZKQVTCNPEHX9M6TZ | eEwxL0QivVE | 9         | ["Eelt7ZePvz0"] | Eelt7ZePvz0 | {}                  |        |                   | system           | 2025-06-03 03:00:00.278363+00 |

**Note:** the activity is 6 days long for the teams to target all assigned assignments
