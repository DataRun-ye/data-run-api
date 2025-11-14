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

```json-
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

## Sample 3: Data Template Version sample (rkwH5QNofRn)
```json
{
  "uid": "rkwH5QNofRn",
  "versionUid": "Y0UVcIutUWv",
  "versionNumber": 8,
  "name": "health_facilities_supervision_laboratory_402",
  "deleted": false,
  "fields": [
    {
      "parent": "main",
      "name": "DateAndTime",
      "id": "E0mkcxV1MAU",
      "type": "DateTime"
    },
    {
      "parent": "main",
      "name": "contact_name",
      "id": "ztV4iHHaNzF",
      "type": "FullName"
    },
    {
      "parent": "main",
      "name": "mobile_phone",
      "id": "dzs71rsLPrt",
      "type": "IntegerPositive"
    },
    {
      "parent": "main",
      "name": "lab_registers",
      "id": "eCWUsNAc8Ma",
      "type": "SelectMulti",
      "optionSet": "bh7ZbTM33rX"
    },
    {
      "parent": "main",
      "name": "lab_posters",
      "id": "WPcJigF5d3c",
      "type": "SelectMulti",
      "optionSet": "LVPJtB1pcr7"
    },
    {
      "parent": "cases",
      "name": "mrdt_tested",
      "id": "UCkj09fyP6k",
      "type": "IntegerZeroOrPositive"
    },
    {
      "parent": "cases",
      "name": "mrdt_pf",
      "id": "wmPvpmUiLhM",
      "type": "IntegerZeroOrPositive"
    },
    {
      "parent": "cases",
      "name": "mrdt_pv_pan_mixed",
      "id": "xoAeJZiHpS3",
      "type": "IntegerZeroOrPositive"
    },
    {
      "parent": "cases",
      "name": "mic_tested",
      "id": "rMVnjcEOBdR",
      "type": "IntegerZeroOrPositive"
    },
    {
      "parent": "cases",
      "name": "mic_pf",
      "id": "nrV6is8GHit",
      "type": "IntegerZeroOrPositive"
    },
    {
      "parent": "cases",
      "name": "mic_pv_mixed_other",
      "id": "oQbto93KT9E",
      "type": "IntegerZeroOrPositive"
    },
    {
      "parent": "cases",
      "name": "drdt_tested",
      "id": "rOmzE8QozC7",
      "type": "IntegerZeroOrPositive"
    },
    {
      "parent": "cases",
      "name": "drdt_ns1",
      "id": "rYqB5hxtYKH",
      "type": "IntegerZeroOrPositive"
    },
    {
      "parent": "cases",
      "name": "drdt_igm",
      "id": "sN5iwFPUjO8",
      "type": "IntegerZeroOrPositive"
    },
    {
      "parent": "cases",
      "name": "drdt_igg",
      "id": "zBGJVg0yqSv",
      "type": "IntegerZeroOrPositive"
    },
    {
      "parent": "quality",
      "name": "microscopy_applied",
      "id": "yMDltXGuqx4",
      "type": "YesNo"
    },
    {
      "parent": "quality",
      "name": "mic_slides_evaluated",
      "id": "YNgmEGrn23U",
      "type": "IntegerZeroOrPositive"
    },
    {
      "parent": "quality",
      "name": "negative_mic_slides_lab",
      "id": "eGONKKTXYiu",
      "type": "IntegerZeroOrPositive"
    },
    {
      "parent": "quality",
      "name": "negative_mic_slides_evaluated",
      "id": "dARoBiyAR4j",
      "type": "IntegerZeroOrPositive"
    },
    {
      "parent": "quality",
      "name": "positive_mic_slides_lab",
      "id": "k4nXyMzy2EA",
      "type": "IntegerZeroOrPositive"
    },
    {
      "parent": "quality",
      "name": "positive_mic_slides_evaluated",
      "id": "mTOLMjDP8I2",
      "type": "IntegerZeroOrPositive"
    },
    {
      "parent": "quality",
      "name": "parasite_density_mic_slides_lab",
      "id": "hufRMlBeind",
      "type": "IntegerZeroOrPositive"
    },
    {
      "parent": "quality",
      "name": "parasite_density_mic_slides_evaluated",
      "id": "xFkMvYaw28D",
      "type": "IntegerZeroOrPositive"
    },
    {
      "parent": "quality",
      "name": "pf_mic_slides_lab",
      "id": "KMsPOC6YfLQ",
      "type": "IntegerZeroOrPositive"
    },
    {
      "parent": "quality",
      "name": "pf_mic_slides_evaluated",
      "id": "hyM3XFU2jGn",
      "type": "IntegerZeroOrPositive"
    },
    {
      "parent": "quality",
      "name": "pv_mic_slides_lab",
      "id": "mB6DBxK6fQ2",
      "type": "IntegerZeroOrPositive"
    },
    {
      "parent": "quality",
      "name": "pv_mic_slides_evaluated",
      "id": "lAsYUoYGfn1",
      "type": "IntegerZeroOrPositive"
    },
    {
      "parent": "quality",
      "name": "mixed_mic_slides_lab",
      "id": "A8D9pk73zbG",
      "type": "IntegerZeroOrPositive"
    },
    {
      "parent": "quality",
      "name": "mixed_mic_slides_evaluated",
      "id": "QUlPYRzKa2M",
      "type": "IntegerZeroOrPositive"
    },
    {
      "parent": "quality",
      "name": "pm_mic_slides_lab",
      "id": "hYMNELAzTGH",
      "type": "IntegerZeroOrPositive"
    },
    {
      "parent": "quality",
      "name": "pm_mic_slides_evaluated",
      "id": "Iyy8aTjKF9E",
      "type": "IntegerZeroOrPositive"
    },
    {
      "parent": "trainee",
      "name": "mrdt_testing",
      "id": "vmJTcIl8cFL",
      "type": "YesNo"
    },
    {
      "parent": "trainee",
      "name": "drdt_testing",
      "id": "Pxcqc0tpGdL",
      "type": "YesNo"
    },
    {
      "parent": "trainee",
      "name": "pf_mic__identification",
      "id": "TICY5Y3Az2F",
      "type": "YesNo"
    },
    {
      "parent": "trainee",
      "name": "pv_pm_mic_identification_trained",
      "id": "DM5Sc5sxCH9",
      "type": "YesNo"
    },
    {
      "parent": "trainee",
      "name": "malaria_smear_staining",
      "id": "xfOBmpykXLD",
      "type": "YesNo"
    },
    {
      "parent": "trainee",
      "name": "mrdt_testing_trained",
      "id": "xYH8EJpjq5D",
      "type": "YesNo"
    },
    {
      "parent": "trainee",
      "name": "drdt_testing_trained",
      "id": "PvlSjS8r3rB",
      "type": "YesNo"
    },
    {
      "parent": "trainee",
      "name": "pf_mic_identification_trained",
      "id": "KnKQmfhRjCQ",
      "type": "YesNo"
    },
    {
      "parent": "trainee",
      "name": "malaria_smear_staining_trained",
      "id": "IrTvgEEkCM4",
      "type": "YesNo"
    },
    {
      "parent": "trainee",
      "name": "microscope_handling_trained",
      "id": "HHUziixvBzD",
      "type": "YesNo"
    },
    {
      "parent": "trainee",
      "name": "lab_recording_trained",
      "id": "HYZefhuTsyX",
      "type": "YesNo"
    },
    {
      "parent": "trainee",
      "name": "other_lab_skills_trained",
      "id": "uHGNMg0yxaA",
      "type": "LongText"
    },
    {
      "parent": "cases",
      "name": "month_name",
      "id": "T7QqiTeFdHF",
      "type": "SelectOne",
      "optionSet": "NN8fDdIJmCn"
    },
    {
      "parent": "training",
      "name": "lab_staff_total",
      "id": "WmJGlvdSW1c",
      "type": "IntegerZeroOrPositive"
    },
    {
      "parent": "training",
      "name": "lab_staff_present",
      "id": "aro2zqKnqgg",
      "type": "IntegerZeroOrPositive"
    },
    {
      "parent": "trainee",
      "name": "name",
      "id": "YtBEZsXvD5A",
      "type": "FullName"
    },
    {
      "parent": "trainee",
      "name": "phone",
      "id": "EHIXCT3t3te",
      "type": "IntegerPositive"
    }
  ],
  "sections": [
    {
      "name": "cases",
      "repeatable": true,
      "id": "cases"
    },
    {
      "parent": "training",
      "name": "trainee",
      "repeatable": true,
      "id": "trainee"
    },
    {
      "name": "training",
      "repeatable": false,
      "id": "training"
    },
    {
      "name": "main",
      "repeatable": false,
      "id": "main"
    },
    {
      "name": "quality",
      "repeatable": false,
      "id": "quality"
    }
  ]
}
```

**Sample 4: some of the data template's `rkwH5QNofRn` canonical elements (kept it short by omitting many elements, this just to provide an idea on what it is):**

```json
[
  {
    "canonical_element_uid": "af0fc741-8274-3db2-9f3d-e6f24024392d",
    "template_uid": "rkwH5QNofRn",
    "preferred_name": "trainee",
    "data_type": "ARRAY",
    "semantic_type": "Repeat",
    "canonical_path": null,
    "cardinality": "N",
    "option_set_uid": null,
    "option_set_id": null,
    "display_label": {
      "ar": "المدربين",
      "en": "trainee"
    },
    "json_data_paths": [
      "training.trainee"
    ],
    "created_date": "2025-11-11 00:51:50.360925",
    "last_modified_date": "2025-11-11 00:51:50.360925"
  },
  {
    "canonical_element_uid": "88334666-28a7-37f9-a9fb-97050ec8d98e",
    "template_uid": "rkwH5QNofRn",
    "preferred_name": "cases",
    "data_type": "ARRAY",
    "semantic_type": "Repeat",
    "canonical_path": null,
    "cardinality": "N",
    "option_set_uid": null,
    "option_set_id": null,
    "display_label": {
      "ar": "الحالات",
      "en": "cases"
    },
    "json_data_paths": [
      "cases"
    ],
    "created_date": "2025-11-11 00:51:50.360925",
    "last_modified_date": "2025-11-11 00:51:50.360925"
  },
  {
    "canonical_element_uid": "14c709ea-5942-3ad6-9f2e-2dc297e27bce",
    "template_uid": "rkwH5QNofRn",
    "preferred_name": "DateAndTime",
    "data_type": "TIMESTAMP",
    "semantic_type": null,
    "canonical_path": null,
    "cardinality": "1",
    "option_set_uid": null,
    "option_set_id": null,
    "display_label": {
      "ar": "الوقت والتاريخ",
      "en": "DateAndTime"
    },
    "json_data_paths": [
      "main.DateAndTime"
    ],
    "created_date": "2025-11-11 00:51:50.360925",
    "last_modified_date": "2025-11-11 00:51:50.360925"
  },
  {
    "canonical_element_uid": "c9e5e122-772d-31da-a8f6-a60dd3036f63",
    "template_uid": "rkwH5QNofRn",
    "preferred_name": "contact_name",
    "data_type": "TEXT",
    "semantic_type": null,
    "canonical_path": null,
    "cardinality": "1",
    "option_set_uid": null,
    "option_set_id": null,
    "display_label": {
      "ar": "اسم مسؤول المختبر",
      "en": "contact_name"
    },
    "json_data_paths": [
      "main.contact_name"
    ],
    "created_date": "2025-11-11 00:51:50.360925",
    "last_modified_date": "2025-11-11 00:51:50.360925"
  },
  {
    "canonical_element_uid": "610e4150-3db0-3b88-82c9-ba94676bd844",
    "template_uid": "rkwH5QNofRn",
    "preferred_name": "mobile_phone",
    "data_type": "INTEGER",
    "semantic_type": null,
    "canonical_path": null,
    "cardinality": "1",
    "option_set_uid": null,
    "option_set_id": null,
    "display_label": {
      "ar": "رقم تلفون مسؤول المختبر",
      "en": "mobile_phone"
    },
    "json_data_paths": [
      "main.mobile_phone"
    ],
    "created_date": "2025-11-11 00:51:50.360925",
    "last_modified_date": "2025-11-11 00:51:50.360925"
  },
  {
    "canonical_element_uid": "c9499bb9-40e3-307e-a80c-974ce80daa26",
    "template_uid": "rkwH5QNofRn",
    "preferred_name": "lab_registers",
    "data_type": "ARRAY",
    "semantic_type": "Option",
    "canonical_path": null,
    "cardinality": "N",
    "option_set_uid": "bh7ZbTM33rX",
    "option_set_id": "01K1EH91PBXSPVF8PNS73PY69F",
    "display_label": {
      "ar": "السجلات",
      "en": "lab_registers"
    },
    "json_data_paths": [
      "main.lab_registers"
    ],
    "created_date": "2025-11-11 00:51:50.360925",
    "last_modified_date": "2025-11-11 00:51:50.360925"
  },
  {
    "canonical_element_uid": "595fcd7e-9f63-3f8f-9a05-2caca4aecbd3",
    "template_uid": "rkwH5QNofRn",
    "preferred_name": "lab_posters",
    "data_type": "ARRAY",
    "semantic_type": "Option",
    "canonical_path": null,
    "cardinality": "N",
    "option_set_uid": "LVPJtB1pcr7",
    "option_set_id": "01K1EHPE2H6JQ2T5JCC771BP5W",
    "display_label": {
      "ar": "الملصقات",
      "en": "lab_posters"
    },
    "json_data_paths": [
      "main.lab_posters"
    ],
    "created_date": "2025-11-11 00:51:50.360925",
    "last_modified_date": "2025-11-11 00:51:50.360925"
  },
  {
    "canonical_element_uid": "d898835c-8d6d-3f73-b6ab-e03ef79ffd53",
    "template_uid": "rkwH5QNofRn",
    "preferred_name": "mrdt_tested",
    "data_type": "INTEGER",
    "semantic_type": null,
    "canonical_path": null,
    "cardinality": "N",
    "option_set_uid": null,
    "option_set_id": null,
    "display_label": {
      "ar": "عدد المفحوصين للملاريا (فحص سريع)",
      "en": "mrdt_tested"
    },
    "json_data_paths": [
      "cases.mrdt_tested"
    ],
    "created_date": "2025-11-11 00:51:50.360925",
    "last_modified_date": "2025-11-11 00:51:50.360925"
  },
  {
    "canonical_element_uid": "8986b825-a0a9-3331-bf0d-abf75f8f51f4",
    "template_uid": "rkwH5QNofRn",
    "preferred_name": "mrdt_pf",
    "data_type": "INTEGER",
    "semantic_type": null,
    "canonical_path": null,
    "cardinality": "N",
    "option_set_uid": null,
    "option_set_id": null,
    "display_label": {
      "ar": "PF",
      "en": "mrdt_pf"
    },
    "json_data_paths": [
      "cases.mrdt_pf"
    ],
    "created_date": "2025-11-11 00:51:50.360925",
    "last_modified_date": "2025-11-11 00:51:50.360925"
  }
]
```

**Sample 5: some of data template `rkwH5QNofRn` element_template samples, also kept it short by omitting many elements of the data template:**

```json
[
  {
    "uid": "GPlCIVia0hw",
    "element_kind": "FIELD",
    "template_uid": "rkwH5QNofRn",
    "template_version_uid": "Y0UVcIutUWv",
    "template_version_no": 8,
    "name": "mrdt_pf",
    "option_set_uid": null,
    "json_data_path": "cases.mrdt_pf",
    "canonical_path": "cases.mrdt_pf",
    "parent_repeat_canonical_path": "cases",
    "parent_repeat_json_data_path": "cases",
    "data_type": "INTEGER",
    "semantic_type": null,
    "cardinality": "N",
    "canonical_element_uid": "8986b825-a0a9-3331-bf0d-abf75f8f51f4",
    "natural_key_candidates": "[]"
  },
  {
    "uid": "KxuNvywKaka",
    "element_kind": "FIELD",
    "template_uid": "rkwH5QNofRn",
    "template_version_uid": "Y0UVcIutUWv",
    "template_version_no": 8,
    "name": "mrdt_pv_pan_mixed",
    "option_set_uid": null,
    "json_data_path": "cases.mrdt_pv_pan_mixed",
    "canonical_path": "cases.mrdt_pv_pan_mixed",
    "parent_repeat_canonical_path": "cases",
    "parent_repeat_json_data_path": "cases",
    "data_type": "INTEGER",
    "semantic_type": null,
    "cardinality": "N",
    "canonical_element_uid": "c77d52ea-770f-3fb9-9c8c-392cf7ca4a19",
    "natural_key_candidates": "[]"
  },
  {
    "uid": "o0FmNlDl5am",
    "element_kind": "REPEAT",
    "template_uid": "rkwH5QNofRn",
    "template_version_uid": "Y0UVcIutUWv",
    "template_version_no": 8,
    "name": "cases",
    "option_set_uid": null,
    "json_data_path": "cases",
    "canonical_path": "cases",
    "parent_repeat_canonical_path": "cases",
    "parent_repeat_json_data_path": "cases",
    "data_type": "ARRAY",
    "semantic_type": "Repeat",
    "cardinality": "N",
    "canonical_element_uid": "88334666-28a7-37f9-a9fb-97050ec8d98e",
    "natural_key_candidates": "[]"
  },
  {
    "uid": "idZnqWYn8Ec",
    "element_kind": "REPEAT",
    "template_uid": "rkwH5QNofRn",
    "template_version_uid": "Y0UVcIutUWv",
    "template_version_no": 8,
    "name": "trainee",
    "option_set_uid": null,
    "json_data_path": "training.trainee",
    "canonical_path": "trainee",
    "parent_repeat_canonical_path": "trainee",
    "parent_repeat_json_data_path": "training.trainee",
    "data_type": "ARRAY",
    "semantic_type": "Repeat",
    "cardinality": "N",
    "canonical_element_uid": "af0fc741-8274-3db2-9f3d-e6f24024392d",
    "natural_key_candidates": "[]"
  },
  {
    "uid": "wggbsTIwJWC",
    "element_kind": "FIELD",
    "template_uid": "rkwH5QNofRn",
    "template_version_uid": "Y0UVcIutUWv",
    "template_version_no": 8,
    "name": "DateAndTime",
    "option_set_uid": null,
    "json_data_path": "main.DateAndTime",
    "canonical_path": "DateAndTime",
    "parent_repeat_canonical_path": null,
    "parent_repeat_json_data_path": null,
    "data_type": "TIMESTAMP",
    "semantic_type": null,
    "cardinality": "1",
    "canonical_element_uid": "14c709ea-5942-3ad6-9f2e-2dc297e27bce",
    "natural_key_candidates": "[]"
  },
  {
    "uid": "kzwYhDwTOcv",
    "element_kind": "FIELD",
    "template_uid": "rkwH5QNofRn",
    "template_version_uid": "Y0UVcIutUWv",
    "template_version_no": 8,
    "name": "contact_name",
    "option_set_uid": null,
    "json_data_path": "main.contact_name",
    "canonical_path": "contact_name",
    "parent_repeat_canonical_path": null,
    "parent_repeat_json_data_path": null,
    "data_type": "TEXT",
    "semantic_type": null,
    "cardinality": "1",
    "canonical_element_uid": "c9e5e122-772d-31da-a8f6-a60dd3036f63",
    "natural_key_candidates": "[]"
  },
  {
    "uid": "saH33YSXoGJ",
    "element_kind": "FIELD",
    "template_uid": "rkwH5QNofRn",
    "template_version_uid": "Y0UVcIutUWv",
    "template_version_no": 8,
    "name": "mobile_phone",
    "option_set_uid": null,
    "json_data_path": "main.mobile_phone",
    "canonical_path": "mobile_phone",
    "parent_repeat_canonical_path": null,
    "parent_repeat_json_data_path": null,
    "data_type": "INTEGER",
    "semantic_type": null,
    "cardinality": "1",
    "canonical_element_uid": "610e4150-3db0-3b88-82c9-ba94676bd844",
    "natural_key_candidates": "[]"
  },
  {
    "uid": "NVaiCOajan5",
    "element_kind": "FIELD",
    "template_uid": "rkwH5QNofRn",
    "template_version_uid": "Y0UVcIutUWv",
    "template_version_no": 8,
    "name": "lab_registers",
    "option_set_uid": "bh7ZbTM33rX",
    "json_data_path": "main.lab_registers",
    "canonical_path": "lab_registers",
    "parent_repeat_canonical_path": null,
    "parent_repeat_json_data_path": null,
    "data_type": "ARRAY",
    "semantic_type": "Option",
    "cardinality": "N",
    "canonical_element_uid": "c9499bb9-40e3-307e-a80c-974ce80daa26",
    "natural_key_candidates": "[]"
  },
  {
    "uid": "jMRic7B9UWU",
    "element_kind": "FIELD",
    "template_uid": "rkwH5QNofRn",
    "template_version_uid": "Y0UVcIutUWv",
    "template_version_no": 8,
    "name": "lab_posters",
    "option_set_uid": "LVPJtB1pcr7",
    "json_data_path": "main.lab_posters",
    "canonical_path": "lab_posters",
    "parent_repeat_canonical_path": null,
    "parent_repeat_json_data_path": null,
    "data_type": "ARRAY",
    "semantic_type": "Option",
    "cardinality": "N",
    "canonical_element_uid": "595fcd7e-9f63-3f8f-9a05-2caca4aecbd3",
    "natural_key_candidates": "[]"
  },
  {
    "uid": "KoFl3ZWrLhk",
    "element_kind": "FIELD",
    "template_uid": "rkwH5QNofRn",
    "template_version_uid": "Y0UVcIutUWv",
    "template_version_no": 8,
    "name": "mrdt_tested",
    "option_set_uid": null,
    "json_data_path": "cases.mrdt_tested",
    "canonical_path": "cases.mrdt_tested",
    "parent_repeat_canonical_path": "cases",
    "parent_repeat_json_data_path": "cases",
    "data_type": "INTEGER",
    "semantic_type": null,
    "cardinality": "N",
    "canonical_element_uid": "d898835c-8d6d-3f73-b6ab-e03ef79ffd53",
    "natural_key_candidates": "[]"
  },
  {
    "uid": "N8rNj4E163G",
    "element_kind": "FIELD",
    "template_uid": "rkwH5QNofRn",
    "template_version_uid": "Y0UVcIutUWv",
    "template_version_no": 8,
    "name": "mic_tested",
    "option_set_uid": null,
    "json_data_path": "cases.mic_tested",
    "canonical_path": "cases.mic_tested",
    "parent_repeat_canonical_path": "cases",
    "parent_repeat_json_data_path": "cases",
    "data_type": "INTEGER",
    "semantic_type": null,
    "cardinality": "N",
    "canonical_element_uid": "116ada1e-6bc8-325b-9a4f-1f89ff019916",
    "natural_key_candidates": "[]"
  },
  {
    "uid": "nqd5UR58dVh",
    "element_kind": "FIELD",
    "template_uid": "rkwH5QNofRn",
    "template_version_uid": "Y0UVcIutUWv",
    "template_version_no": 8,
    "name": "mic_pf",
    "option_set_uid": null,
    "json_data_path": "cases.mic_pf",
    "canonical_path": "cases.mic_pf",
    "parent_repeat_canonical_path": "cases",
    "parent_repeat_json_data_path": "cases",
    "data_type": "INTEGER",
    "semantic_type": null,
    "cardinality": "N",
    "canonical_element_uid": "e021b7b7-d930-3e03-bf2f-6ad06d91f8b1",
    "natural_key_candidates": "[]"
  },
  {
    "uid": "ioW76CyCIPI",
    "element_kind": "FIELD",
    "template_uid": "rkwH5QNofRn",
    "template_version_uid": "Y0UVcIutUWv",
    "template_version_no": 8,
    "name": "mic_pv_mixed_other",
    "option_set_uid": null,
    "json_data_path": "cases.mic_pv_mixed_other",
    "canonical_path": "cases.mic_pv_mixed_other",
    "parent_repeat_canonical_path": "cases",
    "parent_repeat_json_data_path": "cases",
    "data_type": "INTEGER",
    "semantic_type": null,
    "cardinality": "N",
    "canonical_element_uid": "603e9c8b-c7a8-36f6-abe1-21333daf6382",
    "natural_key_candidates": "[]"
  },
  {
    "uid": "M2nHxm5cxlu",
    "element_kind": "FIELD",
    "template_uid": "rkwH5QNofRn",
    "template_version_uid": "Y0UVcIutUWv",
    "template_version_no": 8,
    "name": "drdt_tested",
    "option_set_uid": null,
    "json_data_path": "cases.drdt_tested",
    "canonical_path": "cases.drdt_tested",
    "parent_repeat_canonical_path": "cases",
    "parent_repeat_json_data_path": "cases",
    "data_type": "INTEGER",
    "semantic_type": null,
    "cardinality": "N",
    "canonical_element_uid": "570d5c3e-6f5e-3abb-b3ff-aa651218aab4",
    "natural_key_candidates": "[]"
  },
  {
    "uid": "wq9VdmHU0f3",
    "element_kind": "FIELD",
    "template_uid": "rkwH5QNofRn",
    "template_version_uid": "Y0UVcIutUWv",
    "template_version_no": 8,
    "name": "drdt_ns1",
    "option_set_uid": null,
    "json_data_path": "cases.drdt_ns1",
    "canonical_path": "cases.drdt_ns1",
    "parent_repeat_canonical_path": "cases",
    "parent_repeat_json_data_path": "cases",
    "data_type": "INTEGER",
    "semantic_type": null,
    "cardinality": "N",
    "canonical_element_uid": "46dc9adc-57ff-3401-b605-b1dc959afd5c",
    "natural_key_candidates": "[]"
  },
  {
    "uid": "rKbF3b1Bt6p",
    "element_kind": "FIELD",
    "template_uid": "rkwH5QNofRn",
    "template_version_uid": "Y0UVcIutUWv",
    "template_version_no": 8,
    "name": "drdt_igm",
    "option_set_uid": null,
    "json_data_path": "cases.drdt_igm",
    "canonical_path": "cases.drdt_igm",
    "parent_repeat_canonical_path": "cases",
    "parent_repeat_json_data_path": "cases",
    "data_type": "INTEGER",
    "semantic_type": null,
    "cardinality": "N",
    "canonical_element_uid": "d4e571ae-cbbf-3e56-9e4f-84bb2770fa60",
    "natural_key_candidates": "[]"
  },
  {
    "uid": "aUTXuZe26uS",
    "element_kind": "FIELD",
    "template_uid": "rkwH5QNofRn",
    "template_version_uid": "Y0UVcIutUWv",
    "template_version_no": 8,
    "name": "drdt_igg",
    "option_set_uid": null,
    "json_data_path": "cases.drdt_igg",
    "canonical_path": "cases.drdt_igg",
    "parent_repeat_canonical_path": "cases",
    "parent_repeat_json_data_path": "cases",
    "data_type": "INTEGER",
    "semantic_type": null,
    "cardinality": "N",
    "canonical_element_uid": "7f01025d-600c-311d-8ea1-a29bae96c37a",
    "natural_key_candidates": "[]"
  },
  {
    "uid": "V80ZP1rahKU",
    "element_kind": "FIELD",
    "template_uid": "rkwH5QNofRn",
    "template_version_uid": "Y0UVcIutUWv",
    "template_version_no": 8,
    "name": "microscopy_applied",
    "option_set_uid": null,
    "json_data_path": "quality.microscopy_applied",
    "canonical_path": "microscopy_applied",
    "parent_repeat_canonical_path": null,
    "parent_repeat_json_data_path": null,
    "data_type": "TEXT",
    "semantic_type": null,
    "cardinality": "1",
    "canonical_element_uid": "906c2ad0-256d-3ecd-8d30-7d860c376d7f",
    "natural_key_candidates": "[]"
  },
  {
    "uid": "vgl6pRg8brf",
    "element_kind": "FIELD",
    "template_uid": "rkwH5QNofRn",
    "template_version_uid": "Y0UVcIutUWv",
    "template_version_no": 8,
    "name": "mic_slides_evaluated",
    "option_set_uid": null,
    "json_data_path": "quality.mic_slides_evaluated",
    "canonical_path": "mic_slides_evaluated",
    "parent_repeat_canonical_path": null,
    "parent_repeat_json_data_path": null,
    "data_type": "INTEGER",
    "semantic_type": null,
    "cardinality": "1",
    "canonical_element_uid": "c82d9b52-011e-301f-8e95-a8f11f562c94",
    "natural_key_candidates": "[]"
  },
  {
    "uid": "utXKZpueHfk",
    "element_kind": "FIELD",
    "template_uid": "rkwH5QNofRn",
    "template_version_uid": "Y0UVcIutUWv",
    "template_version_no": 8,
    "name": "negative_mic_slides_lab",
    "option_set_uid": null,
    "json_data_path": "quality.negative_mic_slides_lab",
    "canonical_path": "negative_mic_slides_lab",
    "parent_repeat_canonical_path": null,
    "parent_repeat_json_data_path": null,
    "data_type": "INTEGER",
    "semantic_type": null,
    "cardinality": "1",
    "canonical_element_uid": "c3c17077-24af-33f2-a06c-c182ea8784e0",
    "natural_key_candidates": "[]"
  },
  {
    "uid": "RuWJs5PV7ty",
    "element_kind": "FIELD",
    "template_uid": "rkwH5QNofRn",
    "template_version_uid": "Y0UVcIutUWv",
    "template_version_no": 8,
    "name": "negative_mic_slides_evaluated",
    "option_set_uid": null,
    "json_data_path": "quality.negative_mic_slides_evaluated",
    "canonical_path": "negative_mic_slides_evaluated",
    "parent_repeat_canonical_path": null,
    "parent_repeat_json_data_path": null,
    "data_type": "INTEGER",
    "semantic_type": null,
    "cardinality": "1",
    "canonical_element_uid": "51d2b684-f508-33f3-930d-f8a2cf293039",
    "natural_key_candidates": "[]"
  }
]
```

## Sample 6: A data submission sample:
```json
{
    "createdBy": "733920955",
    "lastModifiedBy": "733920955",
    "createdDate": "2025-10-27T07:49:24.892649Z",
    "lastModifiedDate": "2025-10-27T07:49:24.892649Z",
    "serialNumber": 71707,
    "uid": "Zdu0qS87Wlp",
    "deleted": false,
    "formData": {
        "main": {
            "DateAndTime": "2025-10-27 09:00",
            "lab_posters": [
                "ملصقات تدبير حالات الملاريا",
                "ملصقات فحص الملاري",
                "ملصقات التعريف القياسي للأمراض",
                "ملصق فحص الضنك"
            ],
            "contact_name": "قاسم علي محمد البوني",
            "mobile_phone": 776026359,
            "lab_registers": [
                "سجل المختبر",
                "سجل الإمداد",
                "سجل الطبيب",
                "سجل الحالات",
                "استمارات الترصد"
            ]
        },
        "cases": [
            {
                "_id": "01K8JA9CRDV3N68KH9BZPB4WNM",
                "_index": 1,
                "mrdt_pf": 0,
                "_parentId": "Zdu0qS87Wlp",
                "mic_tested": 0,
                "month_name": "September",
                "drdt_tested": 0,
                "mrdt_tested": 34,
                "_submissionUid": "Zdu0qS87Wlp",
                "mrdt_pv_pan_mixed": 0
            }
        ],
        "training": {
            "trainee": [
                {
                    "_id": "01K8JA9CRDR1ZQR5Q0J9BXX6R9",
                    "name": "قاسم علي محمد البوني",
                    "phone": 776026359,
                    "_index": 1,
                    "_parentId": "Zdu0qS87Wlp",
                    "drdt_testing": true,
                    "mrdt_testing": true,
                    "_submissionUid": "Zdu0qS87Wlp",
                    "drdt_testing_trained": true,
                    "mrdt_testing_trained": true,
                    "lab_recording_trained": false,
                    "malaria_smear_staining": false,
                    "pf_mic__identification": false,
                    "other_lab_skills_trained": "مسؤول المرفق ",
                    "microscope_handling_trained": false,
                    "pf_mic_identification_trained": false,
                    "malaria_smear_staining_trained": false,
                    "pv_pm_mic_identification_trained": false
                }
            ],
            "lab_staff_total": 5,
            "lab_staff_present": 1
        }
    },
    "form": "rkwH5QNofRn",
    "formVersion": "Y0UVcIutUWv",
    "version": 8,
    "team": "xj2gqlJHCet",
    "teamCode": "1303",
    "orgUnit": "n7cQS76qlgj",
    "orgUnitCode": "1724013",
    "orgUnitName": "الوحدة الصحية الظهر",
    "activity": "OKP7MWeEEZe",
    "assignment": "a6czaWtAssA",
    "startEntryTime": "2025-10-27T07:42:21.753838Z",
    "finishedEntryTime": "2025-10-27T07:48:21.751066Z",
    "lockVersion": 0
}
```

**OrgUnit sample:**

| id                         | uid         | code    | name                   | path                                             | level | parent_id                  | translations | created_by | created_date    | last_modified_by | last_modified_date |
|----------------------------|-------------|---------|------------------------|--------------------------------------------------|-------|----------------------------|--------------|------------|-----------------|------------------|--------------------|
| 01JYF3VBMB5QPYFJ43YVJQZ1MZ | gBsSQR1DZSU | 1       | Yemen                  | ,gBsSQR1DZSU                                     | 1     |                            |              | admin      | 1/1/01 12:06 AM | system           | 1/1/23 12:00 AM    |
| 01JYF3VBMBCM7RBMR5SET9ZGAD | HP7pr1yGRlf | 18      | Al Hudaydah            | ,gBsSQR1DZSU,HP7pr1yGRlf                         | 2     | 01JYF3VBMB5QPYFJ43YVJQZ1MZ |              | admin      | 1/1/01 12:06 AM | system           | 1/1/23 12:00 AM    |
| 01JYF3VBMBXD1J29AR083KDJ0C | BVIGsLlGR1a | 1807    | Az Zaydiyah            | ,gBsSQR1DZSU,HP7pr1yGRlf,BVIGsLlGR1a             | 3     | 01JYF3VBMBCM7RBMR5SET9ZGAD |              | admin      | 1/1/01 12:06 AM | system           | 1/1/23 12:00 AM    |
| 01JYF3VBMBA1DV4NQQS8WC9PDE | B8RiTGy6Emb | 1807034 | وحدة  الصحية   دير علي | ,gBsSQR1DZSU,HP7pr1yGRlf,BVIGsLlGR1a,B8RiTGy6Emb | 4     | 01JYF3VBMBXD1J29AR083KDJ0C |              | admin      | 1/1/01 12:06 AM | system           | 1/1/23 3:05 AM     |

**OrgUnit level sample:**
| id | uid         | code | name     | level | translations | created_by | created_date | last_modified_by | last_modified_date |
|----|-------------|------|----------|-------|--------------|------------|--------------|------------------|--------------------|
| 1  | AZb0k8s7cf8 |      | Country  | 0     |              | admin      | 5/23/2024    | admin            | 5/23/2024          |
| 2  | SFkMTSqslem |      | Gov      | 1     |              | admin      | 5/23/2024    | admin            | 5/23/2024          |
| 3  | a8UntlZBHNH |      | District | 2     |              | admin      | 5/23/2024    | admin            | 5/23/2024          |
| 4  | la6MffE1KrN |      | Hf       | 3     |              | admin      | 5/23/2024    | admin            | 5/23/2024          |


**team, assignment, activity, and user samples where omitted for brevity**
