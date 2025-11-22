# Examples

## 🔚 Note for model consumers / downstream teams

The examples here are **illustrative only**.
The system is a *general-purpose, template-driven data collection ETL pipeline*.
Templates may contain **any combination** of:

* repeats (multi-level)
* scalar arrays
* nested groups
* mixed-type structures
* arbitrary forms with arbitrary fields

**Nothing in this document implies a fixed domain model.**
Actual structures depend entirely on each deployed data template.

## 1. **Definitions**

| Concept                | Meaning                                                                    |
|------------------------|----------------------------------------------------------------------------|
| **json_data_path**     | The template-defined structural path (e.g., `group.subfield.items`).       |
| **element_path**       | The actual fully-resolved path in a *submission*, including array indices. |
| **repeat_instance_id** | The `_id` provided by collection app for a repeat row.                     |
| **submission_uid**     | Submission-level unique ID used for top-level array elements.              |
| **instance_key**       | The uniqueness key used for UPSERT into `tall_canonical`.                  |

---

## Example A — Nested repeats + SelectMulti inside inner repeat

### Template (meta)

```json-
{
    "template_uid": "T1",
    "name": "Household survey 103",
    "template_version_uid": "v1"
}
```

### template_elements (only relevant rows, multi elements per canonical element)

| uid              | element_kind        | json_data_path            | canonical_element_uid                | cardinality |
|------------------|---------------------|---------------------------|--------------------------------------|-------------|
| e-rep-household  | REPEAT              | households                | 11111111-1111-1111-1111-111111111111 | N           |
| e-household-name | FIELD               | households.household_name | 11111111-1111-1111-1111-222222222222 | 1           |
| e-rep-member     | REPEAT              | households.members        | 33333333-3333-3333-3333-333333333333 | N           |
| e-member-name    | FIELD               | households.members.name   | 33333333-3333-3333-3333-444444444444 | 1           |
| e-member-age     | FIELD               | households.members.age    | 33333333-3333-3333-3333-555555555555 | 1           |
| e-member-roles   | FIELD (SelectMulti) | households.members.roles  | 33333333-3333-3333-3333-666666666666 | N           |

### canonical_elements table (minimal, json format, across data collections paths)

```json-
[
    {
        "canonical_element_uid": "11111111-1111-1111-1111-111111111111",
        "name_preferred": "households",
        "display_label": {"en": "Households data", "ar": "بيانات المنازل" },
        "paths": ["households"],
        "type": "REPEAT"
    },
    {
        "canonical_element_uid": "22222222-2222-2222-2222-222222222222",
        "name_preferred": "household_name",
        "display_label": {"en": "Household Name", "ar": "اسم رب المنزل" },
        "paths": ["households.household_name"],
        "type": "TEXT"
    },
    {
        "canonical_element_uid": "33333333-3333-3333-3333-333333333333",
        "name_preferred": "members",
        "display_label": {"en": "Household Members", "ar": "أفراد المنزل" },
        "paths": ["households.members"],
        "type": "REPEAT"
    },
    {
        "canonical_element_uid": "44444444-4444-4444-4444-444444444444",
        "name_preferred": "name",
        "display_label": {"en": "Member name", "ar": "اسم الفرد" },
        "paths": ["households.members.name"],
        "type": "TEXT"
    },
    {
        "canonical_element_uid": "55555555-5555-5555-5555-555555555555",
        "name_preferred": "age",
        "display_label": {"en": "Age", "ar": "العمر" },
        "paths": ["households.members.age"],
        "type": "INTEGER"
    },
    {
        "canonical_element_uid": "66666666-6666-6666-6666-666666666666",
        "name_preferred": "roles",
        "display_label": {"en": "Member Roles", "ar": ".." },
        "paths": ["households.members.roles"],
        "type": "SelectMulti"
    }
]
```

### Submission payload (trimmed)

```json-
{
    "formVersion": "v1",
    "started_data_entry_at": "2025-06-24T00:46:49.445+00:00",
    "finished_data_entry_at": "2025-06-24T00:49:49.445+00:00",
    "created_date": "2025-06-25T00:11:49.445+00:00",
    "updated_date": "2025-06-25T00:11:49.445+00:00",
    "activity": "act-uidx",
    "orgUnit": "ou-uidxx",
    "team": "team-uidxxx",
    "households": [
        {
            "_id": "h1",
            "_index": 0,
            "_submissionUid": "S1",
            "household_name": "Alpha",
            "members": [
                {
                    "_id": "m1",
                    "_index": 0,
                    "_parentId": "h1",
                    "name": "Alice",
                    "age": 30,
                    "roles": [
                        "leader",
                        "cook"
                    ],
                    "_submissionUid": "S1"
                },
                {
                    "_id": "m2",
                    "_index": 1,
                    "_parentId": "h1",
                    "name": "Bob",
                    "age": 12,
                    "roles": [
                        "child"
                    ],
                    "_submissionUid": "S1"
                }
            ]
        },
        {
            "_id": "h2",
            "_index": 1,
            "_submissionUid": "S1",
            "household_name": "Beta",
            "members": [
                {
                    "_id": "m3",
                    "_index": 0,
                    "_parentId": "h2",
                    "name": "Carol",
                    "age": 40,
                    "roles": [],
                    "_submissionUid": "S1"
                }
            ]
        }
    ]
}
```

### Expected `tall_canonical` rows (illustrative subset)

> Notes on `instance_key`: using the InstanceKey rule you adopted — if a `repeat_instance_id` exists and `element_path`
> ends with `]` we use `repeatId|elementPath`, otherwise `repeatId`. For non-repeat fields we use
`submissionUid|elementPath`. `element_path` normalized like `households[0].members[0].name`.
> In markdown tables format below the `|` in instance_key was replaced with `#` so the markdown doesn't confuse it for
> an end of a cell, but originally in db they are `|`.

| element (row)                                           |                         instance_key | canonical_element_uid                | element_path                      | repeat_instance_id | parent_instance_id | repeat_index | value_text / value_number               |  
|---------------------------------------------------------|-------------------------------------:|--------------------------------------|-----------------------------------|-------------------:|-------------------:|-------------:|-----------------------------------------|
| household repeat object (h1)                            |                                   h1 | 11111111-1111-1111-1111-111111111111 | households[0]                     |                 h1 |             (null) |            0 | value_json = `{...household object...}` |
| household_name (h1)                                     |                                   h1 | 22222222-2222-2222-2222-222222222222 | households[0].household_name      |                 h1 |             (null) |            0 | "Alpha"                                 |
| member repeat object (Alice)                            |                                   m1 | 33333333-3333-3333-3333-333333333333 | households[0].members[0]          |                 m1 |                 h1 |            0 | value_json = `{...member object...}`    |
| member name (Alice)                                     |                                   m1 | 44444444-4444-4444-4444-444444444444 | households[0].members[0].name     |                 m1 |                 h1 |            0 | "Alice"                                 |
| member age (Alice)                                      |                                   m1 | 55555555-5555-5555-5555-555555555555 | households[0].members[0].age      |                 m1 |                 h1 |            0 | 30                                      |
| member role scalar 0 (Alice->roles[0])                  | m1#households[0].members[0].roles[0] | 66666666-6666-6666-6666-666666666666 | households[0].members[0].roles[0] |                 m1 |                 h1 |            0 | "leader"                                |
| member role scalar 1 (Alice->roles[1])                  | m1#households[0].members[0].roles[1] | 66666666-6666-6666-6666-666666666666 | households[0].members[0].roles[1] |                 m1 |                 h1 |            1 | "cook"                                  |
| member repeat object (Bob)                              |                                   m2 | 33333333-...                         | households[0].members[1]          |                 m2 |                 h1 |            1 | value_json `{...}`                      |
| Bob name                                                |                                   m2 | 4444...                              | households[0].members[1].name     |                 m2 |                 h1 |            1 | "Bob"                                   |
| Bob role scalar 0                                       | m2#households[0].members[1].roles[0] | 6666...                              | households[0].members[1].roles[0] |                 m2 |                 h1 |            0 | "child"                                 |
| household (h2) and its member (Carol) rows similarly... |                                      |                                      |                                   |                    |                    |              |                                         |

This shows:

* nested repeat rows (household repeat rows, member repeat rows),
* parent/child relation via `parent_instance_id` (member.parent = household._id),
* scalar arrays inside inner repeats (`roles[...]`) produce individual rows with element_path ending with `]` and
  instance key `repeatId|elementPath` so each selection is tracked individually.
* numeric fields (`age`) go to `value_number`.

---

## Example B — Top-level SelectMulti (scalar array not in a repeat)

### template_elements

| uid        | element_kind | json_data_path | canonical_element_uid                | cardinality |
|------------|--------------|----------------|--------------------------------------|-------------|
| e-symptoms | FIELD        | symptoms       | aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa | N           |

### canonical_element

```json
{
    "canonical_element_uid": "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
    "name_preferred": "symptoms",
    "display_label": {"en": "Symptoms", "ar": "الأعراض" },
    "paths": ["symptoms"],
    "type": "SelectMulti"
}
```

### submission payload

```json-
{
    "formVersion": "v1",
    "symptoms": [
        "fever",
        "cough"
    ]
}
```

### tall_canonical rows (expected)

| element     |   instance_key | canonical_element_uid | element_path | repeat_instance_id | repeat_index | value_text |
|-------------|---------------:|-----------------------|--------------|-------------------:|-------------:|------------|
| symptoms[0] | S1#symptoms[0] | aaaa...               | symptoms[0]  |             (null) |            0 | "fever"    |
| symptoms[1] | S1#symptoms[1] | aaaa...               | symptoms[1]  |             (null) |            1 | "cough"    |
