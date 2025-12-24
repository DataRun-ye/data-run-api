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

## Sample 3: Data Template Version sample (ck2pHW93sk2)
```json

```

**Sample 4: canonical elements samples of the elements of DataTemplate=`ck2pHW93sk2` (kept it short by omitting many elements, this just to provide an idea on what it is):**

```json
[
    {
        "canonical_element_uid": "0e7bdab9-6213-3437-9a90-16c071b0b169",
        "template_uid": "ck2pHW93sk2",
        "preferred_name": "patients",
        "data_type": "ARRAY",
        "semantic_type": "Repeat",
        "canonical_path": null,
        "cardinality": "N",
        "option_set_uid": null,
        "option_set_id": null,
        "display_label": "{\"ar\": \"الحالة\", \"en\": \"patients\"}",
        "json_data_paths": "[\"patients\", \"patients\", \"patients\", \"patients\", \"patients\", \"patients\", \"patients\", \"patients\", \"patients\", \"patients\", \"patients\", \"patients\"]",
        "notes": null,
        "created_date": "2025-11-11 00:51:50.911555",
        "last_modified_date": "2025-11-13 00:43:07.158926"
    },
    {
        "canonical_element_uid": "0aaead1e-ac2e-3206-8728-818b1c6ad714",
        "template_uid": "ck2pHW93sk2",
        "preferred_name": "serialNumber",
        "data_type": "INTEGER",
        "semantic_type": null,
        "canonical_path": null,
        "cardinality": "N",
        "option_set_uid": null,
        "option_set_id": null,
        "display_label": "{\"ar\": \"الرقم التسلسلي\", \"en\": \"serialNumber\"}",
        "json_data_paths": "[\"patients.serialNumber\", \"patients.serialNumber\", \"patients.serialNumber\", \"patients.serialNumber\", \"patients.serialNumber\", \"patients.serialNumber\", \"patients.serialNumber\", \"patients.serialNumber\", \"patients.serialNumber\", \"patients.serialNumber\", \"patients.serialNumber\", \"patients.serialNumber\"]",
        "notes": null,
        "created_date": "2025-11-11 00:51:50.911555",
        "last_modified_date": "2025-11-13 00:43:07.158926"
    },
    {
        "canonical_element_uid": "046ee0b0-ac85-384f-bbc7-9a3ab7f9dc74",
        "template_uid": "ck2pHW93sk2",
        "preferred_name": "PatientName",
        "data_type": "TEXT",
        "semantic_type": null,
        "canonical_path": null,
        "cardinality": "N",
        "option_set_uid": null,
        "option_set_id": null,
        "display_label": "{\"ar\": \"اسم المريض\", \"en\": \"PatientName\"}",
        "json_data_paths": "[\"patients.PatientName\", \"patients.PatientName\", \"patients.PatientName\", \"patients.PatientName\", \"patients.PatientName\", \"patients.PatientName\", \"patients.PatientName\", \"patients.PatientName\", \"patients.PatientName\", \"patients.PatientName\", \"patients.PatientName\", \"patients.PatientName\"]",
        "notes": null,
        "created_date": "2025-11-11 00:51:50.911555",
        "last_modified_date": "2025-11-13 00:43:07.158926"
    },
    {
        "canonical_element_uid": "cf12fa47-ab1d-3c31-a9dd-a5d995b1678f",
        "template_uid": "ck2pHW93sk2",
        "preferred_name": "age",
        "data_type": "TEXT",
        "semantic_type": "Age",
        "canonical_path": null,
        "cardinality": "N",
        "option_set_uid": null,
        "option_set_id": null,
        "display_label": "{\"ar\": \"العمر\", \"en\": \"age\"}",
        "json_data_paths": "[\"patients.age\", \"patients.age\", \"patients.age\", \"patients.age\", \"patients.age\", \"patients.age\", \"patients.age\", \"patients.age\", \"patients.age\", \"patients.age\", \"patients.age\", \"patients.age\"]",
        "notes": null,
        "created_date": "2025-11-11 00:51:50.911555",
        "last_modified_date": "2025-11-13 00:43:07.158926"
    },
    {
        "canonical_element_uid": "9fd10fe9-fc81-35e6-b617-69234e5cb901",
        "template_uid": "ck2pHW93sk2",
        "preferred_name": "gender",
        "data_type": "TEXT",
        "semantic_type": "Option",
        "canonical_path": null,
        "cardinality": "N",
        "option_set_uid": "Bu2LhXFDicp",
        "option_set_id": "01JYF3WW0H5R3NFXGDCMX4K70D",
        "display_label": "{\"ar\": \"الجنس\", \"en\": \"gender\"}",
        "json_data_paths": "[\"patients.gender\", \"patients.gender\", \"patients.gender\", \"patients.gender\", \"patients.gender\", \"patients.gender\", \"patients.gender\", \"patients.gender\", \"patients.gender\", \"patients.gender\", \"patients.gender\", \"patients.gender\"]",
        "notes": null,
        "created_date": "2025-11-11 00:51:50.911555",
        "last_modified_date": "2025-11-13 00:43:07.158926"
    },
    {
        "canonical_element_uid": "f52766cb-abd5-368d-a211-6a49c0759d3c",
        "template_uid": "ck2pHW93sk2",
        "preferred_name": "ispregnant",
        "data_type": "TEXT",
        "semantic_type": null,
        "canonical_path": null,
        "cardinality": "N",
        "option_set_uid": null,
        "option_set_id": null,
        "display_label": "{\"ar\": \"حامل\", \"en\": \"ispregnant\"}",
        "json_data_paths": "[\"patients.ispregnant\", \"patients.ispregnant\", \"patients.ispregnant\", \"patients.ispregnant\", \"patients.ispregnant\", \"patients.ispregnant\", \"patients.ispregnant\", \"patients.ispregnant\", \"patients.ispregnant\", \"patients.ispregnant\", \"patients.ispregnant\", \"patients.ispregnant\"]",
        "notes": null,
        "created_date": "2025-11-11 00:51:50.911555",
        "last_modified_date": "2025-11-13 00:43:07.158926"
    },
    {
        "canonical_element_uid": "d91d6e71-5798-38f7-aa53-3c54ba09a036",
        "template_uid": "ck2pHW93sk2",
        "preferred_name": "pregnancy_month",
        "data_type": "INTEGER",
        "semantic_type": null,
        "canonical_path": null,
        "cardinality": "N",
        "option_set_uid": null,
        "option_set_id": null,
        "display_label": "{\"ar\": \"شهر الحمل\", \"en\": \"pregnancy_month\"}",
        "json_data_paths": "[\"patients.pregnancy_month\", \"patients.pregnancy_month\", \"patients.pregnancy_month\", \"patients.pregnancy_month\", \"patients.pregnancy_month\", \"patients.pregnancy_month\", \"patients.pregnancy_month\", \"patients.pregnancy_month\", \"patients.pregnancy_month\", \"patients.pregnancy_month\", \"patients.pregnancy_month\", \"patients.pregnancy_month\"]",
        "notes": null,
        "created_date": "2025-11-11 00:51:50.911555",
        "last_modified_date": "2025-11-13 00:43:07.158926"
    },
    {
        "canonical_element_uid": "04a75f1a-74e2-3885-bb64-bdb46adaf59c",
        "template_uid": "ck2pHW93sk2",
        "preferred_name": "symptomsv2",
        "data_type": "ARRAY",
        "semantic_type": "Option",
        "canonical_path": null,
        "cardinality": "N",
        "option_set_uid": "HFccPl89PPX",
        "option_set_id": "01K89XVCHE2QT90JTTYNK7R7J9",
        "display_label": "{\"ar\": \"الاعراض والعلامات\", \"en\": \"symptoms\"}",
        "json_data_paths": "[\"patients.symptomsv2\", \"patients.symptomsv2\", \"patients.symptomsv2\", \"patients.symptomsv2\", \"patients.symptomsv2\", \"patients.symptomsv2\", \"patients.symptomsv2\", \"patients.symptomsv2\", \"patients.symptomsv2\", \"patients.symptomsv2\", \"patients.symptomsv2\", \"patients.symptomsv2\"]",
        "notes": null,
        "created_date": "2025-11-11 00:51:50.911555",
        "last_modified_date": "2025-11-13 00:43:07.158926"
    },
    {
        "canonical_element_uid": "f95fc8d5-e54a-314f-8c04-643268b87af5",
        "template_uid": "ck2pHW93sk2",
        "preferred_name": "lab_test",
        "data_type": "ARRAY",
        "semantic_type": "Option",
        "canonical_path": null,
        "cardinality": "N",
        "option_set_uid": "AxpBXSCqnDa",
        "option_set_id": "01JZT74FWP1VHQTBEGCTW61G2J",
        "display_label": "{\"ar\": \"الفحوصات المطلوبة\", \"en\": \"lab_test\"}",
        "json_data_paths": "[\"patients.lab_test\", \"patients.lab_test\", \"patients.lab_test\", \"patients.lab_test\", \"patients.lab_test\", \"patients.lab_test\", \"patients.lab_test\", \"patients.lab_test\", \"patients.lab_test\", \"patients.lab_test\", \"patients.lab_test\", \"patients.lab_test\"]",
        "notes": null,
        "created_date": "2025-11-11 00:51:50.911555",
        "last_modified_date": "2025-11-13 00:43:07.158926"
    },
    {
        "canonical_element_uid": "c27b75e9-aeae-3a7c-9f2f-ef930a8229ce",
        "template_uid": "ck2pHW93sk2",
        "preferred_name": "diagnosed_disease_type",
        "data_type": "TEXT",
        "semantic_type": "Option",
        "canonical_path": null,
        "cardinality": "N",
        "option_set_uid": "AjpOXeUAqnQ",
        "option_set_id": "01JYF3WW0HD3CTRDRZ4DG6E71G",
        "display_label": "{\"ar\": \"التشخيص\", \"en\": \"diagnosed_disease_typ\"}",
        "json_data_paths": "[\"patients.diagnosed_disease_type\", \"patients.diagnosed_disease_type\", \"patients.diagnosed_disease_type\", \"patients.diagnosed_disease_type\", \"patients.diagnosed_disease_type\", \"patients.diagnosed_disease_type\", \"patients.diagnosed_disease_type\", \"patients.diagnosed_disease_type\", \"patients.diagnosed_disease_type\", \"patients.diagnosed_disease_type\", \"patients.diagnosed_disease_type\", \"patients.diagnosed_disease_type\"]",
        "notes": null,
        "created_date": "2025-11-11 00:51:50.911555",
        "last_modified_date": "2025-11-13 00:43:07.158926"
    },
    {
        "canonical_element_uid": "84d5aaaa-ee37-3c12-9adf-b1a5632a1979",
        "template_uid": "ck2pHW93sk2",
        "preferred_name": "other_diagnosed_disease_name",
        "data_type": "TEXT",
        "semantic_type": null,
        "canonical_path": null,
        "cardinality": "N",
        "option_set_uid": null,
        "option_set_id": null,
        "display_label": "{\"ar\": \"مرض اخر\", \"en\": \"other_diagnosed_disease_name\"}",
        "json_data_paths": "[\"patients.other_diagnosed_disease_name\", \"patients.other_diagnosed_disease_name\", \"patients.other_diagnosed_disease_name\", \"patients.other_diagnosed_disease_name\", \"patients.other_diagnosed_disease_name\", \"patients.other_diagnosed_disease_name\", \"patients.other_diagnosed_disease_name\", \"patients.other_diagnosed_disease_name\", \"patients.other_diagnosed_disease_name\", \"patients.other_diagnosed_disease_name\", \"patients.other_diagnosed_disease_name\", \"patients.other_diagnosed_disease_name\"]",
        "notes": null,
        "created_date": "2025-11-11 00:51:50.911555",
        "last_modified_date": "2025-11-13 00:43:07.158926"
    },
    {
        "canonical_element_uid": "82ee0992-0ab0-3eac-8305-e23a5f945e18",
        "template_uid": "ck2pHW93sk2",
        "preferred_name": "malariadrugs",
        "data_type": "ARRAY",
        "semantic_type": "Option",
        "canonical_path": null,
        "cardinality": "N",
        "option_set_uid": "YPubNKrnO1e",
        "option_set_id": "01K89WKVR94ZBJDE0TKCCTZV0P",
        "display_label": "{\"ar\": \"أدوية الملاريا\", \"en\": \"malariadrugs\"}",
        "json_data_paths": "[\"patients.malariadrugs\", \"patients.malariadrugs\", \"patients.malariadrugs\", \"patients.malariadrugs\", \"patients.malariadrugs\", \"patients.malariadrugs\", \"patients.malariadrugs\", \"patients.malariadrugs\", \"patients.malariadrugs\", \"patients.malariadrugs\", \"patients.malariadrugs\", \"patients.malariadrugs\"]",
        "notes": null,
        "created_date": "2025-11-11 00:51:50.911555",
        "last_modified_date": "2025-11-13 00:43:07.158926"
    },
    {
        "canonical_element_uid": "3a58846f-337f-308f-8f58-a4039903102f",
        "template_uid": "ck2pHW93sk2",
        "preferred_name": "other_prescribeddrugs",
        "data_type": "TEXT",
        "semantic_type": null,
        "canonical_path": null,
        "cardinality": "N",
        "option_set_uid": null,
        "option_set_id": null,
        "display_label": "{\"ar\": \"صنف اخر\", \"en\": \"other_prescribeddrugs\"}",
        "json_data_paths": "[\"patients.other_prescribeddrugs\", \"patients.other_prescribeddrugs\", \"patients.other_prescribeddrugs\", \"patients.other_prescribeddrugs\", \"patients.other_prescribeddrugs\", \"patients.other_prescribeddrugs\", \"patients.other_prescribeddrugs\", \"patients.other_prescribeddrugs\", \"patients.other_prescribeddrugs\", \"patients.other_prescribeddrugs\", \"patients.other_prescribeddrugs\", \"patients.other_prescribeddrugs\"]",
        "notes": null,
        "created_date": "2025-11-11 00:51:50.911555",
        "last_modified_date": "2025-11-13 00:43:07.158926"
    },
    {
        "canonical_element_uid": "37f8ec22-3c9e-3ce1-89a1-0b96be441e60",
        "template_uid": "ck2pHW93sk2",
        "preferred_name": "cm_measures",
        "data_type": "TEXT",
        "semantic_type": "Option",
        "canonical_path": null,
        "cardinality": "N",
        "option_set_uid": "vcYOprXCzar",
        "option_set_id": "01JYF3WW0HM5TPPGMG7F22A3MP",
        "display_label": "{\"ar\": \"التدابير\", \"en\": \"cm_measures\"}",
        "json_data_paths": "[\"patients.cm_measures\", \"patients.cm_measures\", \"patients.cm_measures\", \"patients.cm_measures\", \"patients.cm_measures\", \"patients.cm_measures\", \"patients.cm_measures\", \"patients.cm_measures\", \"patients.cm_measures\", \"patients.cm_measures\", \"patients.cm_measures\", \"patients.cm_measures\"]",
        "notes": null,
        "created_date": "2025-11-11 00:51:50.911555",
        "last_modified_date": "2025-11-13 00:43:07.158926"
    },
    {
        "canonical_element_uid": "70878f3e-9808-3ef5-a279-88ec0542553f",
        "template_uid": "ck2pHW93sk2",
        "preferred_name": "referral_type",
        "data_type": "TEXT",
        "semantic_type": "Option",
        "canonical_path": null,
        "cardinality": "N",
        "option_set_uid": "PnCIWkhM47L",
        "option_set_id": "01JYF3WW0HFH9T5C8ZMQZDSQ3R",
        "display_label": "{\"ar\": \"إحالة\", \"en\": \"referral_type\"}",
        "json_data_paths": "[\"patients.referral_type\", \"patients.referral_type\", \"patients.referral_type\", \"patients.referral_type\", \"patients.referral_type\", \"patients.referral_type\", \"patients.referral_type\", \"patients.referral_type\", \"patients.referral_type\", \"patients.referral_type\", \"patients.referral_type\", \"patients.referral_type\"]",
        "notes": null,
        "created_date": "2025-11-11 00:51:50.911555",
        "last_modified_date": "2025-11-13 00:43:07.158926"
    },
    {
        "canonical_element_uid": "08e6aad1-c067-30cd-9858-f1c7fe9cde54",
        "template_uid": "ck2pHW93sk2",
        "preferred_name": "visitdate",
        "data_type": "TIMESTAMP",
        "semantic_type": null,
        "canonical_path": null,
        "cardinality": "1",
        "option_set_uid": null,
        "option_set_id": null,
        "display_label": "{\"ar\": \"تاريخ الاستجابة\", \"en\": \"visitdate\"}",
        "json_data_paths": "[\"main.visitdate\", \"main.visitdate\", \"main.visitdate\", \"main.visitdate\", \"main.visitdate\", \"main.visitdate\", \"main.visitdate\", \"main.visitdate\", \"main.visitdate\", \"main.visitdate\", \"main.visitdate\", \"main.visitdate\"]",
        "notes": null,
        "created_date": "2025-11-11 00:51:50.911555",
        "last_modified_date": "2025-11-13 00:43:07.158926"
    }
]
```

**Sample 5: ElementTemplate samples of DataTemplate=`ck2pHW93sk2`, also kept it short by omitting many elements of the data template:**

```json
[
    {
        "id": 2262,
        "uid": "nKpaGR36b7a",
        "element_kind": "REPEAT",
        "template_uid": "ck2pHW93sk2",
        "template_version_uid": "Nvsp2XDPaTq",
        "template_version_no": 15,
        "name": "patients",
        "option_set_uid": null,
        "option_set_id": null,
        "sort_order": 13,
        "json_data_path": "patients",
        "canonical_path": "patients",
        "parent_repeat_canonical_path": "patients",
        "parent_repeat_json_data_path": "patients",
        "display_label": "{\"ar\": \"الحالة\", \"en\": \"patients\"}",
        "data_type": "ARRAY",
        "semantic_type": "Repeat",
        "cardinality": "N",
        "canonical_element_uid": "0e7bdab9-6213-3437-9a90-16c071b0b169",
        "natural_key_candidates": "[]",
        "created_date": "2025-11-11 00:51:50.911555"
    },
    {
        "id": 2264,
        "uid": "wKZoP8NvduT",
        "element_kind": "FIELD",
        "template_uid": "ck2pHW93sk2",
        "template_version_uid": "Nvsp2XDPaTq",
        "template_version_no": 15,
        "name": "serialNumber",
        "option_set_uid": null,
        "option_set_id": null,
        "sort_order": 15,
        "json_data_path": "patients.serialNumber",
        "canonical_path": "patients.serialNumber",
        "parent_repeat_canonical_path": "patients",
        "parent_repeat_json_data_path": "patients",
        "display_label": "{\"ar\": \"الرقم التسلسلي\", \"en\": \"serialNumber\"}",
        "data_type": "INTEGER",
        "semantic_type": null,
        "cardinality": "N",
        "canonical_element_uid": "0aaead1e-ac2e-3206-8728-818b1c6ad714",
        "natural_key_candidates": "[]",
        "created_date": "2025-11-11 00:51:50.911555"
    },
    {
        "id": 2265,
        "uid": "xFoPgA8ScDK",
        "element_kind": "FIELD",
        "template_uid": "ck2pHW93sk2",
        "template_version_uid": "Nvsp2XDPaTq",
        "template_version_no": 15,
        "name": "PatientName",
        "option_set_uid": null,
        "option_set_id": null,
        "sort_order": 16,
        "json_data_path": "patients.PatientName",
        "canonical_path": "patients.PatientName",
        "parent_repeat_canonical_path": "patients",
        "parent_repeat_json_data_path": "patients",
        "display_label": "{\"ar\": \"اسم المريض\", \"en\": \"PatientName\"}",
        "data_type": "TEXT",
        "semantic_type": null,
        "cardinality": "N",
        "canonical_element_uid": "046ee0b0-ac85-384f-bbc7-9a3ab7f9dc74",
        "natural_key_candidates": "[]",
        "created_date": "2025-11-11 00:51:50.911555"
    },
    {
        "id": 2266,
        "uid": "XiHffC6EImH",
        "element_kind": "FIELD",
        "template_uid": "ck2pHW93sk2",
        "template_version_uid": "Nvsp2XDPaTq",
        "template_version_no": 15,
        "name": "age",
        "option_set_uid": null,
        "option_set_id": null,
        "sort_order": 17,
        "json_data_path": "patients.age",
        "canonical_path": "patients.age",
        "parent_repeat_canonical_path": "patients",
        "parent_repeat_json_data_path": "patients",
        "display_label": "{\"ar\": \"العمر\", \"en\": \"age\"}",
        "data_type": "TEXT",
        "semantic_type": "Age",
        "cardinality": "N",
        "canonical_element_uid": "cf12fa47-ab1d-3c31-a9dd-a5d995b1678f",
        "natural_key_candidates": "[]",
        "created_date": "2025-11-11 00:51:50.911555"
    },
    {
        "id": 2267,
        "uid": "zh8M6KrHVWT",
        "element_kind": "FIELD",
        "template_uid": "ck2pHW93sk2",
        "template_version_uid": "Nvsp2XDPaTq",
        "template_version_no": 15,
        "name": "gender",
        "option_set_uid": "Bu2LhXFDicp",
        "option_set_id": "01JYF3WW0H5R3NFXGDCMX4K70D",
        "sort_order": 18,
        "json_data_path": "patients.gender",
        "canonical_path": "patients.gender",
        "parent_repeat_canonical_path": "patients",
        "parent_repeat_json_data_path": "patients",
        "display_label": "{\"ar\": \"الجنس\", \"en\": \"gender\"}",
        "data_type": "TEXT",
        "semantic_type": "Option",
        "cardinality": "N",
        "canonical_element_uid": "9fd10fe9-fc81-35e6-b617-69234e5cb901",
        "natural_key_candidates": "[]",
        "created_date": "2025-11-11 00:51:50.911555"
    },
    {
        "id": 2268,
        "uid": "mmRl6z8TAQn",
        "element_kind": "FIELD",
        "template_uid": "ck2pHW93sk2",
        "template_version_uid": "Nvsp2XDPaTq",
        "template_version_no": 15,
        "name": "ispregnant",
        "option_set_uid": null,
        "option_set_id": null,
        "sort_order": 19,
        "json_data_path": "patients.ispregnant",
        "canonical_path": "patients.ispregnant",
        "parent_repeat_canonical_path": "patients",
        "parent_repeat_json_data_path": "patients",
        "display_label": "{\"ar\": \"حامل\", \"en\": \"ispregnant\"}",
        "data_type": "TEXT",
        "semantic_type": null,
        "cardinality": "N",
        "canonical_element_uid": "f52766cb-abd5-368d-a211-6a49c0759d3c",
        "natural_key_candidates": "[]",
        "created_date": "2025-11-11 00:51:50.911555"
    },
    {
        "id": 2269,
        "uid": "xfs8KYG7F13",
        "element_kind": "FIELD",
        "template_uid": "ck2pHW93sk2",
        "template_version_uid": "Nvsp2XDPaTq",
        "template_version_no": 15,
        "name": "pregnancy_month",
        "option_set_uid": null,
        "option_set_id": null,
        "sort_order": 20,
        "json_data_path": "patients.pregnancy_month",
        "canonical_path": "patients.pregnancy_month",
        "parent_repeat_canonical_path": "patients",
        "parent_repeat_json_data_path": "patients",
        "display_label": "{\"ar\": \"شهر الحمل\", \"en\": \"pregnancy_month\"}",
        "data_type": "INTEGER",
        "semantic_type": null,
        "cardinality": "N",
        "canonical_element_uid": "d91d6e71-5798-38f7-aa53-3c54ba09a036",
        "natural_key_candidates": "[]",
        "created_date": "2025-11-11 00:51:50.911555"
    },
    {
        "id": 2277,
        "uid": "fkQAZl8fpLR",
        "element_kind": "FIELD",
        "template_uid": "ck2pHW93sk2",
        "template_version_uid": "Nvsp2XDPaTq",
        "template_version_no": 15,
        "name": "symptomsv2",
        "option_set_uid": "HFccPl89PPX",
        "option_set_id": "01K89XVCHE2QT90JTTYNK7R7J9",
        "sort_order": 21,
        "json_data_path": "patients.symptomsv2",
        "canonical_path": "patients.symptomsv2",
        "parent_repeat_canonical_path": "patients",
        "parent_repeat_json_data_path": "patients",
        "display_label": "{\"ar\": \"الاعراض والعلامات\", \"en\": \"symptoms\"}",
        "data_type": "ARRAY",
        "semantic_type": "Option",
        "cardinality": "N",
        "canonical_element_uid": "04a75f1a-74e2-3885-bb64-bdb46adaf59c",
        "natural_key_candidates": "[]",
        "created_date": "2025-11-11 00:51:50.911555"
    },
    {
        "id": 2270,
        "uid": "PT1Ne3qT4KE",
        "element_kind": "FIELD",
        "template_uid": "ck2pHW93sk2",
        "template_version_uid": "Nvsp2XDPaTq",
        "template_version_no": 15,
        "name": "lab_test",
        "option_set_uid": "AxpBXSCqnDa",
        "option_set_id": "01JZT74FWP1VHQTBEGCTW61G2J",
        "sort_order": 22,
        "json_data_path": "patients.lab_test",
        "canonical_path": "patients.lab_test",
        "parent_repeat_canonical_path": "patients",
        "parent_repeat_json_data_path": "patients",
        "display_label": "{\"ar\": \"الفحوصات المطلوبة\", \"en\": \"lab_test\"}",
        "data_type": "ARRAY",
        "semantic_type": "Option",
        "cardinality": "N",
        "canonical_element_uid": "f95fc8d5-e54a-314f-8c04-643268b87af5",
        "natural_key_candidates": "[]",
        "created_date": "2025-11-11 00:51:50.911555"
    },
    {
        "id": 2271,
        "uid": "bO85besGd0G",
        "element_kind": "FIELD",
        "template_uid": "ck2pHW93sk2",
        "template_version_uid": "Nvsp2XDPaTq",
        "template_version_no": 15,
        "name": "diagnosed_disease_type",
        "option_set_uid": "AjpOXeUAqnQ",
        "option_set_id": "01JYF3WW0HD3CTRDRZ4DG6E71G",
        "sort_order": 23,
        "json_data_path": "patients.diagnosed_disease_type",
        "canonical_path": "patients.diagnosed_disease_type",
        "parent_repeat_canonical_path": "patients",
        "parent_repeat_json_data_path": "patients",
        "display_label": "{\"ar\": \"التشخيص\", \"en\": \"diagnosed_disease_typ\"}",
        "data_type": "TEXT",
        "semantic_type": "Option",
        "cardinality": "N",
        "canonical_element_uid": "c27b75e9-aeae-3a7c-9f2f-ef930a8229ce",
        "natural_key_candidates": "[]",
        "created_date": "2025-11-11 00:51:50.911555"
    },
    {
        "id": 2272,
        "uid": "KAEaDc7YlhF",
        "element_kind": "FIELD",
        "template_uid": "ck2pHW93sk2",
        "template_version_uid": "Nvsp2XDPaTq",
        "template_version_no": 15,
        "name": "other_diagnosed_disease_name",
        "option_set_uid": null,
        "option_set_id": null,
        "sort_order": 24,
        "json_data_path": "patients.other_diagnosed_disease_name",
        "canonical_path": "patients.other_diagnosed_disease_name",
        "parent_repeat_canonical_path": "patients",
        "parent_repeat_json_data_path": "patients",
        "display_label": "{\"ar\": \"مرض اخر\", \"en\": \"other_diagnosed_disease_name\"}",
        "data_type": "TEXT",
        "semantic_type": null,
        "cardinality": "N",
        "canonical_element_uid": "84d5aaaa-ee37-3c12-9adf-b1a5632a1979",
        "natural_key_candidates": "[]",
        "created_date": "2025-11-11 00:51:50.911555"
    },
    {
        "id": 2276,
        "uid": "afbdWRuqFlR",
        "element_kind": "FIELD",
        "template_uid": "ck2pHW93sk2",
        "template_version_uid": "Nvsp2XDPaTq",
        "template_version_no": 15,
        "name": "malariadrugs",
        "option_set_uid": "YPubNKrnO1e",
        "option_set_id": "01K89WKVR94ZBJDE0TKCCTZV0P",
        "sort_order": 31,
        "json_data_path": "patients.malariadrugs",
        "canonical_path": "patients.malariadrugs",
        "parent_repeat_canonical_path": "patients",
        "parent_repeat_json_data_path": "patients",
        "display_label": "{\"ar\": \"أدوية الملاريا\", \"en\": \"malariadrugs\"}",
        "data_type": "ARRAY",
        "semantic_type": "Option",
        "cardinality": "N",
        "canonical_element_uid": "82ee0992-0ab0-3eac-8305-e23a5f945e18",
        "natural_key_candidates": "[]",
        "created_date": "2025-11-11 00:51:50.911555"
    },
    {
        "id": 2275,
        "uid": "LFH09VEKdWY",
        "element_kind": "FIELD",
        "template_uid": "ck2pHW93sk2",
        "template_version_uid": "Nvsp2XDPaTq",
        "template_version_no": 15,
        "name": "other_prescribeddrugs",
        "option_set_uid": null,
        "option_set_id": null,
        "sort_order": 32,
        "json_data_path": "patients.other_prescribeddrugs",
        "canonical_path": "patients.other_prescribeddrugs",
        "parent_repeat_canonical_path": "patients",
        "parent_repeat_json_data_path": "patients",
        "display_label": "{\"ar\": \"صنف اخر\", \"en\": \"other_prescribeddrugs\"}",
        "data_type": "TEXT",
        "semantic_type": null,
        "cardinality": "N",
        "canonical_element_uid": "3a58846f-337f-308f-8f58-a4039903102f",
        "natural_key_candidates": "[]",
        "created_date": "2025-11-11 00:51:50.911555"
    },
    {
        "id": 2273,
        "uid": "i8WFxzB4dTs",
        "element_kind": "FIELD",
        "template_uid": "ck2pHW93sk2",
        "template_version_uid": "Nvsp2XDPaTq",
        "template_version_no": 15,
        "name": "cm_measures",
        "option_set_uid": "vcYOprXCzar",
        "option_set_id": "01JYF3WW0HM5TPPGMG7F22A3MP",
        "sort_order": 40,
        "json_data_path": "patients.cm_measures",
        "canonical_path": "patients.cm_measures",
        "parent_repeat_canonical_path": "patients",
        "parent_repeat_json_data_path": "patients",
        "display_label": "{\"ar\": \"التدابير\", \"en\": \"cm_measures\"}",
        "data_type": "TEXT",
        "semantic_type": "Option",
        "cardinality": "N",
        "canonical_element_uid": "37f8ec22-3c9e-3ce1-89a1-0b96be441e60",
        "natural_key_candidates": "[]",
        "created_date": "2025-11-11 00:51:50.911555"
    },
    {
        "id": 2274,
        "uid": "PMFrhunX7qB",
        "element_kind": "FIELD",
        "template_uid": "ck2pHW93sk2",
        "template_version_uid": "Nvsp2XDPaTq",
        "template_version_no": 15,
        "name": "referral_type",
        "option_set_uid": "PnCIWkhM47L",
        "option_set_id": "01JYF3WW0HFH9T5C8ZMQZDSQ3R",
        "sort_order": 41,
        "json_data_path": "patients.referral_type",
        "canonical_path": "patients.referral_type",
        "parent_repeat_canonical_path": "patients",
        "parent_repeat_json_data_path": "patients",
        "display_label": "{\"ar\": \"إحالة\", \"en\": \"referral_type\"}",
        "data_type": "TEXT",
        "semantic_type": "Option",
        "cardinality": "N",
        "canonical_element_uid": "70878f3e-9808-3ef5-a279-88ec0542553f",
        "natural_key_candidates": "[]",
        "created_date": "2025-11-11 00:51:50.911555"
    },
    {
        "id": 2263,
        "uid": "OfeCbQdMMVe",
        "element_kind": "FIELD",
        "template_uid": "ck2pHW93sk2",
        "template_version_uid": "Nvsp2XDPaTq",
        "template_version_no": 15,
        "name": "visitdate",
        "option_set_uid": null,
        "option_set_id": null,
        "sort_order": 2,
        "json_data_path": "main.visitdate",
        "canonical_path": "visitdate",
        "parent_repeat_canonical_path": null,
        "parent_repeat_json_data_path": null,
        "display_label": "{\"ar\": \"تاريخ الاستجابة\", \"en\": \"visitdate\"}",
        "data_type": "TIMESTAMP",
        "semantic_type": null,
        "cardinality": "1",
        "canonical_element_uid": "08e6aad1-c067-30cd-9858-f1c7fe9cde54",
        "natural_key_candidates": "[]",
        "created_date": "2025-11-11 00:51:50.911555"
    }
]
```

## Sample 6: A data submission sample for data template=`ck2pHW93sk2`:
```json
{
    "createdBy": "772759361",
    "lastModifiedBy": "772759361",
    "createdDate": "2025-11-11T16:40:08.119047Z",
    "lastModifiedDate": "2025-11-11T16:40:08.119047Z",
    "serialNumber": 72597,
    "uid": "xiipWGCVkl7",
    "deleted": false,
    "formData": {
        "main": {
            "visitdate": "2025-11-11"
        },
        "patients": [
            {
                "_id": "01K9SWKYMRDH43EZBKMHA7RZ7T",
                "age": "2017-11-10T21:00:00.000Z",
                "_index": 1,
                "gender": "MALE",
                "lab_test": [
                    "الفحص السريع للملاريا"
                ],
                "_parentId": "xiipWGCVkl7",
                "symptomsv2": [
                    "حمى"
                ],
                "PatientName": "علاء عبده ربه علي ربيش",
                "cm_measures": "none",
                "serialNumber": 1,
                "_submissionUid": "xiipWGCVkl7",
                "other_prescribeddrugs": null,
                "diagnosed_disease_type": "none"
            },
            {
                "_id": "01K9SWKYMSVRY7XSPPWYHM06HT",
                "age": "2013-11-10T21:00:00.000Z",
                "_index": 2,
                "gender": "MALE",
                "lab_test": [
                    "الفحص السريع للملاريا"
                ],
                "_parentId": "xiipWGCVkl7",
                "symptomsv2": [
                    "حمى"
                ],
                "PatientName": "حبيب عبده ربه علي ربيش",
                "cm_measures": "none",
                "serialNumber": 2,
                "_submissionUid": "xiipWGCVkl7",
                "other_prescribeddrugs": null,
                "diagnosed_disease_type": "none"
            },
            {
                "_id": "01K9SWKYMSQTGTX5Q2S48X044X",
                "age": "2015-11-10T21:00:00.000Z",
                "_index": 3,
                "gender": "MALE",
                "lab_test": [
                    "الفحص السريع للملاريا"
                ],
                "_parentId": "xiipWGCVkl7",
                "symptomsv2": [
                    "حمى"
                ],
                "PatientName": "ضياء عبده ربه علي ربيش",
                "cm_measures": "none",
                "serialNumber": 3,
                "_submissionUid": "xiipWGCVkl7",
                "other_prescribeddrugs": null,
                "diagnosed_disease_type": "none"
            },
            {
                "_id": "01K9SWKYMS4WHQJ6N3YBX1TWGX",
                "age": "2018-11-10T21:00:00.000Z",
                "_index": 4,
                "gender": "MALE",
                "lab_test": [
                    "الفحص السريع للملاريا"
                ],
                "_parentId": "xiipWGCVkl7",
                "symptomsv2": [
                    "حمى"
                ],
                "PatientName": "محمد جمال محمد ربيش",
                "cm_measures": "treatment",
                "malariadrugs": [
                    "شريط ACT 40",
                    "قرص بريماكوين 15 ملجم"
                ],
                "serialNumber": 4,
                "_submissionUid": "xiipWGCVkl7",
                "other_prescribeddrugs": null,
                "diagnosed_disease_type": "malaria"
            },
            {
                "_id": "01K9SWKYMSEC146ZBF4PQ16X8X",
                "age": "2018-11-10T21:00:00.000Z",
                "_index": 5,
                "gender": "FEMALE",
                "lab_test": [
                    "الفحص السريع للملاريا"
                ],
                "_parentId": "xiipWGCVkl7",
                "ispregnant": false,
                "symptomsv2": [
                    "حمى"
                ],
                "PatientName": "ابرار عبده ربه علي ربيش",
                "cm_measures": "treatment",
                "malariadrugs": [
                    "شريط ACT 40",
                    "قرص بريماكوين 15 ملجم"
                ],
                "serialNumber": 5,
                "_submissionUid": "xiipWGCVkl7",
                "other_prescribeddrugs": null,
                "diagnosed_disease_type": "malaria"
            },
            {
                "_id": "01K9SWKYMSY4YPMCHW1ZQJDHCG",
                "age": "2006-11-10T21:00:00.000Z",
                "_index": 6,
                "gender": "MALE",
                "lab_test": [
                    "الفحص السريع للملاريا"
                ],
                "_parentId": "xiipWGCVkl7",
                "symptomsv2": [
                    "حمى"
                ],
                "PatientName": "صقر سعيد احمد علي",
                "cm_measures": "treatment",
                "malariadrugs": [
                    "شريط ACT 80",
                    "قرص بريماكوين 15 ملجم"
                ],
                "serialNumber": 6,
                "_submissionUid": "xiipWGCVkl7",
                "other_prescribeddrugs": null,
                "diagnosed_disease_type": "malaria"
            },
            {
                "_id": "01K9SWKYMSZFCMDNHKV0NXYXF2",
                "age": "1990-11-10T21:00:00.000Z",
                "_index": 7,
                "gender": "FEMALE",
                "lab_test": [
                    "الفحص السريع للملاريا"
                ],
                "_parentId": "xiipWGCVkl7",
                "ispregnant": false,
                "symptomsv2": [
                    "حمى"
                ],
                "PatientName": "افراح عبدالله محمد ربيش",
                "cm_measures": "treatment",
                "malariadrugs": [
                    "شريط ACT 80",
                    "قرص بريماكوين 15 ملجم"
                ],
                "serialNumber": 7,
                "_submissionUid": "xiipWGCVkl7",
                "other_prescribeddrugs": null,
                "diagnosed_disease_type": "malaria"
            },
            {
                "_id": "01K9SWKYMSRVHMEJT0VHNG6QKS",
                "age": "2023-05-10T21:00:00.000Z",
                "_index": 8,
                "gender": "FEMALE",
                "lab_test": [
                    "الفحص السريع للملاريا"
                ],
                "_parentId": "xiipWGCVkl7",
                "ispregnant": false,
                "symptomsv2": [
                    "حمى"
                ],
                "PatientName": "براءة صالح علي ربيش",
                "cm_measures": "none",
                "serialNumber": 8,
                "_submissionUid": "xiipWGCVkl7",
                "other_prescribeddrugs": null,
                "diagnosed_disease_type": "none"
            },
            {
                "_id": "01K9SWKYMS72VKBJWKMCVGT5MV",
                "age": "2018-11-10T21:00:00.000Z",
                "_index": 9,
                "gender": "MALE",
                "lab_test": [
                    "الفحص السريع للملاريا"
                ],
                "_parentId": "xiipWGCVkl7",
                "symptomsv2": [
                    "حمى"
                ],
                "PatientName": "شامخ صالح علي ربيش",
                "cm_measures": "none",
                "serialNumber": 9,
                "_submissionUid": "xiipWGCVkl7",
                "other_prescribeddrugs": null,
                "diagnosed_disease_type": "none"
            },
            {
                "_id": "01K9SWKYMSQ8HZC3R5FVH2DVJB",
                "age": "1985-11-10T21:00:00.000Z",
                "_index": 10,
                "gender": "MALE",
                "lab_test": [
                    "الفحص السريع للملاريا"
                ],
                "_parentId": "xiipWGCVkl7",
                "symptomsv2": [
                    "حمى"
                ],
                "PatientName": "عبده علي محمد ربيش",
                "cm_measures": "none",
                "serialNumber": 10,
                "_submissionUid": "xiipWGCVkl7",
                "other_prescribeddrugs": null,
                "diagnosed_disease_type": "none"
            },
            {
                "_id": "01K9SWKYMSE2VP048FZ9A54BK4",
                "age": "2016-11-10T21:00:00.000Z",
                "_index": 11,
                "gender": "MALE",
                "lab_test": [
                    "الفحص السريع للملاريا"
                ],
                "_parentId": "xiipWGCVkl7",
                "symptomsv2": [
                    "حمى"
                ],
                "PatientName": "محمد عبده علي ربيش",
                "cm_measures": "none",
                "serialNumber": 11,
                "_submissionUid": "xiipWGCVkl7",
                "other_prescribeddrugs": null,
                "diagnosed_disease_type": "none"
            },
            {
                "_id": "01K9SWKYMSH6B8X6QRT56YXTQP",
                "age": "2021-11-10T21:00:00.000Z",
                "_index": 12,
                "gender": "MALE",
                "lab_test": [
                    "الفحص السريع للملاريا"
                ],
                "_parentId": "xiipWGCVkl7",
                "symptomsv2": [
                    "حمى"
                ],
                "PatientName": "فارس عبده علي ربيش",
                "cm_measures": "none",
                "serialNumber": 12,
                "_submissionUid": "xiipWGCVkl7",
                "other_prescribeddrugs": null,
                "diagnosed_disease_type": "none"
            },
            {
                "_id": "01K9SWKYMSATT3WCS7NG8SGM6K",
                "age": "1993-11-10T21:00:00.000Z",
                "_index": 13,
                "gender": "FEMALE",
                "lab_test": [
                    "الفحص السريع للملاريا"
                ],
                "_parentId": "xiipWGCVkl7",
                "ispregnant": false,
                "symptomsv2": [
                    "حمى"
                ],
                "PatientName": "بخيته علي محمد ربيش",
                "cm_measures": "none",
                "serialNumber": 13,
                "_submissionUid": "xiipWGCVkl7",
                "other_prescribeddrugs": null,
                "diagnosed_disease_type": "none"
            },
            {
                "_id": "01K9SWKYMS81WJQV374X4H7JQP",
                "age": "2013-11-10T21:00:00.000Z",
                "_index": 14,
                "gender": "FEMALE",
                "lab_test": [
                    "الفحص السريع للملاريا"
                ],
                "_parentId": "xiipWGCVkl7",
                "ispregnant": false,
                "symptomsv2": [
                    "حمى"
                ],
                "PatientName": "هنادي صالح علي ربيش",
                "cm_measures": "none",
                "serialNumber": 14,
                "_submissionUid": "xiipWGCVkl7",
                "other_prescribeddrugs": null,
                "diagnosed_disease_type": "none"
            },
            {
                "_id": "01K9SWKYMSK54WK62X1Z2GEN70",
                "age": "2020-11-10T21:00:00.000Z",
                "_index": 15,
                "gender": "MALE",
                "lab_test": [
                    "الفحص السريع للملاريا"
                ],
                "_parentId": "xiipWGCVkl7",
                "symptomsv2": [
                    "حمى"
                ],
                "PatientName": "محمد صالح علي ربيش",
                "cm_measures": "treatment",
                "malariadrugs": [
                    "شريط ACT 40",
                    "قرص بريماكوين 15 ملجم"
                ],
                "serialNumber": 15,
                "_submissionUid": "xiipWGCVkl7",
                "other_prescribeddrugs": null,
                "diagnosed_disease_type": "malaria"
            },
            {
                "_id": "01K9SWKYMSK0WWADWP6R6HTTTQ",
                "age": "2010-11-10T21:00:00.000Z",
                "_index": 16,
                "gender": "FEMALE",
                "lab_test": [
                    "الفحص السريع للملاريا"
                ],
                "_parentId": "xiipWGCVkl7",
                "ispregnant": false,
                "symptomsv2": [
                    "حمى"
                ],
                "PatientName": "افراح مرشد احمد القصلي",
                "cm_measures": "none",
                "serialNumber": 16,
                "_submissionUid": "xiipWGCVkl7",
                "other_prescribeddrugs": null,
                "diagnosed_disease_type": "none"
            },
            {
                "_id": "01K9SWKYMSPV5K8779MVHDBXC8",
                "age": "2021-11-10T21:00:00.000Z",
                "_index": 17,
                "gender": "MALE",
                "lab_test": [
                    "الفحص السريع للملاريا"
                ],
                "_parentId": "xiipWGCVkl7",
                "symptomsv2": [
                    "حمى"
                ],
                "PatientName": "جبير مرشد احمد القصلي",
                "cm_measures": "none",
                "serialNumber": 17,
                "_submissionUid": "xiipWGCVkl7",
                "other_prescribeddrugs": null,
                "diagnosed_disease_type": "none"
            },
            {
                "_id": "01K9SWKYMSG11XEX52BVYASY4W",
                "age": "2024-07-10T21:00:00.000Z",
                "_index": 18,
                "gender": "FEMALE",
                "lab_test": [
                    "الفحص السريع للملاريا"
                ],
                "_parentId": "xiipWGCVkl7",
                "ispregnant": false,
                "symptomsv2": [
                    "حمى"
                ],
                "PatientName": "رفح عبده علي ربيش",
                "cm_measures": "none",
                "serialNumber": 18,
                "_submissionUid": "xiipWGCVkl7",
                "other_prescribeddrugs": null,
                "diagnosed_disease_type": "none"
            },
            {
                "_id": "01K9SWKYMSQSBTZE6D0Z3TV7XW",
                "age": "2012-11-10T21:00:00.000Z",
                "_index": 19,
                "gender": "FEMALE",
                "lab_test": [
                    "الفحص السريع للملاريا"
                ],
                "_parentId": "xiipWGCVkl7",
                "ispregnant": false,
                "symptomsv2": [
                    "حمى"
                ],
                "PatientName": "عبير عبده علي ربيش",
                "cm_measures": "none",
                "serialNumber": 19,
                "_submissionUid": "xiipWGCVkl7",
                "other_prescribeddrugs": null,
                "diagnosed_disease_type": "none"
            },
            {
                "_id": "01K9SWKYMSSAJFCZWW5Q04FNQ3",
                "age": "2018-11-10T21:00:00.000Z",
                "_index": 20,
                "gender": "FEMALE",
                "lab_test": [
                    "الفحص السريع للملاريا"
                ],
                "_parentId": "xiipWGCVkl7",
                "ispregnant": false,
                "symptomsv2": [
                    "حمى"
                ],
                "PatientName": "مسيرة سعيد احمد المعزبي",
                "cm_measures": "none",
                "serialNumber": 20,
                "_submissionUid": "xiipWGCVkl7",
                "other_prescribeddrugs": null,
                "diagnosed_disease_type": "none"
            },
            {
                "_id": "01K9SWKYMSY9CXMR00ZMNCYG4G",
                "age": "2022-11-10T21:00:00.000Z",
                "_index": 21,
                "gender": "MALE",
                "lab_test": [
                    "الفحص السريع للملاريا"
                ],
                "_parentId": "xiipWGCVkl7",
                "symptomsv2": [
                    "حمى"
                ],
                "PatientName": "مختار صالح علي ربيش",
                "cm_measures": "none",
                "serialNumber": 21,
                "_submissionUid": "xiipWGCVkl7",
                "other_prescribeddrugs": null,
                "diagnosed_disease_type": "none"
            },
            {
                "_id": "01K9SWKYMS64GCVCVTJ5VTK2KW",
                "age": "1975-11-10T21:00:00.000Z",
                "_index": 22,
                "gender": "FEMALE",
                "lab_test": [
                    "الفحص السريع للملاريا"
                ],
                "_parentId": "xiipWGCVkl7",
                "ispregnant": false,
                "symptomsv2": [
                    "حمى"
                ],
                "PatientName": "جمعه علي احمد الغماري",
                "cm_measures": "none",
                "serialNumber": 22,
                "_submissionUid": "xiipWGCVkl7",
                "other_prescribeddrugs": null,
                "diagnosed_disease_type": "none"
            },
            {
                "_id": "01K9SWKYMSEXPF5TN31WTHPR59",
                "age": "1965-11-10T21:00:00.000Z",
                "_index": 23,
                "gender": "MALE",
                "lab_test": [
                    "الفحص السريع للملاريا"
                ],
                "_parentId": "xiipWGCVkl7",
                "symptomsv2": [
                    "حمى"
                ],
                "PatientName": "علي محمد ربيش المعزبي",
                "cm_measures": "none",
                "serialNumber": 23,
                "_submissionUid": "xiipWGCVkl7",
                "other_prescribeddrugs": null,
                "diagnosed_disease_type": "none"
            },
            {
                "_id": "01K9SWKYMS10V0A74QE7Z0V90E",
                "age": "1993-11-10T21:00:00.000Z",
                "_index": 24,
                "gender": "FEMALE",
                "lab_test": [
                    "الفحص السريع للملاريا"
                ],
                "_parentId": "xiipWGCVkl7",
                "ispregnant": false,
                "symptomsv2": [
                    "حمى"
                ],
                "PatientName": "هديه عبدالله ربيش المعزبي",
                "cm_measures": "none",
                "serialNumber": 24,
                "_submissionUid": "xiipWGCVkl7",
                "other_prescribeddrugs": null,
                "diagnosed_disease_type": "none"
            },
            {
                "_id": "01K9SWKYMSBK4X7PWETGBTJZV9",
                "age": "2013-11-10T21:00:00.000Z",
                "_index": 25,
                "gender": "MALE",
                "lab_test": [
                    "الفحص السريع للملاريا"
                ],
                "_parentId": "xiipWGCVkl7",
                "symptomsv2": [
                    "حمى"
                ],
                "PatientName": "بندر ناصر صالح المعزبي",
                "cm_measures": "none",
                "serialNumber": 25,
                "_submissionUid": "xiipWGCVkl7",
                "other_prescribeddrugs": null,
                "diagnosed_disease_type": "none"
            }
        ]
    },
    "form": "ck2pHW93sk2",
    "formVersion": "Nvsp2XDPaTq",
    "version": 15,
    "team": "nmn2OCrBNgt",
    "teamCode": "1102",
    "orgUnit": "Mi1X6B3zIFJ",
    "orgUnitCode": "11012810114",
    "orgUnitName": "المعازبه-الحماري",
    "activity": "TYduVflHzfZ",
    "assignment": "YN5ZpINUUFU",
    "startEntryTime": "2025-11-11T16:26:12.253498Z",
    "finishedEntryTime": "2025-11-11T16:40:02.998226Z",
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
