# walkthrough an Example samples metadata generation

## Example Samples of a Template Structure and one of its Submissions

below are Example Samples of a Template Structure, say it was pushed by admin, and Submission that was submitted by a user.

**Notes**:
in addition to the context in first attached doc, here are a few detailed points.
- field, and section snapshots are stored in `DataTemplateVersion.fields`, and `DataTemplateVersion.sections`, as JSONB columns, each store the list of snapshots that describe each element in a data template,
- a field, and a section, both are abstracted as typed dtos in code  `FormDataElementConf`, and `FormSectionConf`.
- field.id points to the `DataElement.uid` it configures.
- sections don't point to any canonical entity like fields, and their id is their names.
- enforced unique `name` and `id` of elements in same level. e.g in root submission (no parent repeat in ancestry) and repeat parent are two levels.

sample of random form template version:
```json-
{
	"uid": "MI8KQFsxGFc", // template uid
	"versionUid": "ys2a06ekFXJ", // template version uid
	"versionNumber": 41,
	"name": "Malaria and Mosquito-Borne Disease Emergency Investigation and Response Form",
	"deleted": false,	
	"fields": [
		{
			"parent": "main",
			"name": "visitdate",  // copied from data element immutable in de doesn't change
			"path": "main.LyIGccZ5mna", // per form template
			"label": {      // can be overridden per form template version
				"en": "Visit Date",
				"ar": "تاريخ الزيارة"
			},
			"id": "LyIGccZ5mna", // DataElement.uid it's mapped to
			"type": "Date",     // copied from data element immutable in de
			"aggregationType": "DEFAULT" // copied from data element
		},
		{
			"parent": "case",
			"name": "serialNumber",
			"path": "case.w8VQRZmmz82",
			"label": {
				"en": "serialNumber",
				"ar": "الرقم التسلسلي"
			},
			"id": "w8VQRZmmz82",
			"type": "IntegerPositive",
			"aggregationType": "DEFAULT"
		},
		{
			"parent": "case",
			"name": "PatientName",
			"path": "case.KBmP3fPjrLG",
			"label": {
				"en": "PatientName",
				"ar": "اسم المريض رباعياً"
			},
			"id": "KBmP3fPjrLG",
			"type": "FullName",
			"aggregationType": "DEFAULT"
		},
		{
			"parent": "case",
			"name": "gender",
			"path": "case.eCw9HcbcnW7",
			"label": {
				"en": "Gender",
				"ar": "الجنس"
			},
			"id": "eCw9HcbcnW7",
			"type": "SelectOne",
			"optionSet": "Bu2LhXFDicp", // // copied from data element
			"aggregationType": "DEFAULT"
		},
		{
			"parent": "case",
			"name": "ispregnant",
			"path": "case.FjfKSsEWAYf",
			"rules": [
				{
					"expression": "(#{gender} == 'FEMALE') && (#{age_in_years} > 13 && #{age_in_years} < 55)",
					"action": "Show"
				}
			],
			"id": "FjfKSsEWAYf",
			"type": "YesNo",
			"aggregationType": "DEFAULT"
		},
		{
			"parent": "case",
			"name": "pregnancy_month",
			"path": "case.aEjEOwZ9Mk0",
			"label": {
				"en": "pregnancy_month",
				"ar": "شهر الحمل"
			},
			"id": "aEjEOwZ9Mk0",
			"type": "IntegerPositive",
			"aggregationType": "DEFAULT"
		},
		{
			"parent": "case",
			"name": "hemoglobin",
			"path": "case.UCqRDm5SJ3k",
			"label": {
				"en": "hemoglobin(g/dl)",
				"ar": "hemoglobin(g/dl)"
			},
			
			"id": "UCqRDm5SJ3k",
			"type": "Number",
			"aggregationType": "DEFAULT"
		},
		{
			"parent": "disease",
			"name": "diagnosed_disease_type",
			"path": "disease.UiM4N3o8OiG",
			"label": {
				"en": "diagnosed_disease_type",
				"ar": "نوع المرض المُشخَّص"
			},
			"id": "UiM4N3o8OiG",
			"type": "SelectOne",
			"optionSet": "AjpOXeUAqnQ",
			"aggregationType": "DEFAULT"
		},
		{
			"parent": "measures",
			"name": "cm_measures",
			"path": "measures.W5QDhN3qanw",
			"label": {
				"en": "cm_measures",
				"ar": "التدابير المتخذه"
			},
			"id": "W5QDhN3qanw",
			"type": "SelectOne",
			"optionSet": "vcYOprXCzar",
			"aggregationType": "DEFAULT"
		},
		{
			"parent": "refeeral",
			"name": "referral_type",
			"path": "refeeral.BOGECC3MSLb",
			"label": {
				"en": "referral_type",
				"ar": "نوع الإحالة"
			},
			"id": "BOGECC3MSLb",
			"type": "SelectOne",
			"optionSet": "PnCIWkhM47L",
			"aggregationType": "DEFAULT"
		},
		{
			"parent": "notes",
			"name": "comment",
			"path": "notes.cjY0oSUx6YI",
			"label": {
				"en": "Comments",
				"ar": "ملاحظات وتعليقات"
			},
			"id": "cjY0oSUx6YI",
			"type": "LongText",
			"aggregationType": "DEFAULT"
		},
		{
			"parent": "labtest",
			"name": "lab_test_type",
			"path": "labtest.BqErQ3xrUex",
			"label": {
				"en": "lab_test_type",
				"ar": "نوع الفحص"
			},
			"id": "BqErQ3xrUex",
			"type": "SelectOne",
			"optionSet": "PiGAyROTsAB",
			"aggregationType": "DEFAULT"
		},
		{
			"parent": "labtest",
			"name": "test_result",
			"path": "labtest.jLX69mxv8JJ",
			"label": {
				"en": "test_result",
				"ar": "نتيجة الفحص"
			},
			"id": "jLX69mxv8JJ",
			"type": "SelectOne",
			
			"optionSet": "tQjriOFVC2T",
			"aggregationType": "DEFAULT"
		},
		{
			"parent": "labtest",
			"name": "positive_result_type",
			"path": "labtest.ywxDNy8F6BY",
			"label": {
				"en": "positive_result_type",
				"ar": "نوع النتيجة الإيجابية"
			},
			
			"id": "ywxDNy8F6BY",
			"type": "SelectMulti",
			
			"optionSet": "FNki0zFPleV",
			"aggregationType": "DEFAULT"
		},
		{
			"parent": "disease",
			"name": "other_diagnosed_disease_name",
			"path": "disease.MYZOyP37ilc",
			"label": {
				"en": "other_diagnosed_disease_name",
				"ar": "اسم المرض المُشخَّص الآخر"
			},
			
			"id": "MYZOyP37ilc",
			"type": "Text",
			"aggregationType": "DEFAULT"
		},
		{
			"parent": "medicines",
			"name": "prescribeddrug",
			"path": "medicines.jnm46N3THhf",
			"label": {
				"en": "prescribeddrug",
				"ar": "الصنف"
			},
			
			"id": "jnm46N3THhf",
			"type": "SelectOne",
		
			"optionSet": "PVg1vCjoxfV",
			"aggregationType": "DEFAULT"
		},
		{
			"parent": "medicines",
			"name": "druguom",
			"path": "medicines.gjkfAEM4bwc",
			"label": {
				"en": "druguom",
				"ar": "وحدة القياس"
			},
			
			"id": "gjkfAEM4bwc",
			"type": "SelectOne",
			"optionSet": "k4ZN0nvhrxq",
			"aggregationType": "DEFAULT"
		},
		{
			"parent": "medicines",
			"name": "quantity",
			"path": "medicines.Jck63XyG19y",
			"label": {
				"en": "Quantity",
				"ar": "الكمية"
			},
			
			"id": "Jck63XyG19y",
			"type": "IntegerZeroOrPositive",
			"aggregationType": "DEFAULT"
		}
	],
	"sections": [
		{
			"name": "case",
			"path": "case",
			"label": {
				"en": "case",
				"ar": "بيانات الحالة"
			},
			"rules": [],
			"order": 2,
			"repeatable": false,
			"id": "case"
		},
		{
			"name": "refeeral",
			"path": "refeeral",
			"label": {
				"en": "refeeral",
				"ar": "الاحالة"
			},
			"rules": [],
			"order": 7,
			"repeatable": false,
			"id": "refeeral"
		},
		{
			"name": "medicines",
			"path": "medicines",
			"label": {
				"en": "medicines",
				"ar": "الادوية"
			},
			"rules": [],
			"order": 6,
			"repeatable": true,
			"id": "medicines"
		},
		{
			"name": "notes",
			"path": "notes",
			"label": {
				"en": "notes",
				"ar": "ملاحظات"
			},
			"rules": [],
			"order": 8,
			"repeatable": false,
			"id": "notes"
		},
		{
			"name": "measures",
			"path": "measures",
			"label": {
				"en": "measures",
				"ar": "التدابير"
			},
			"rules": [],
			"order": 5,
			"repeatable": false,
			"id": "measures"
		},
		{
			"name": "disease",
			"path": "disease",
			"label": {
				"en": "disease",
				"ar": "المرض المشخص"
			},
			"rules": [],
			"order": 4,
			"repeatable": false,
			"id": "disease"
		},
		{
			"name": "main",
			"path": "main",
			"label": {
				"en": "main",
				"ar": "بيانات عامه"
			},
			"rules": [],
			"order": 1,
			"repeatable": false,
			"id": "main"
		},
		{
			"name": "labtest",
			"path": "labtest",
			"label": {
				"en": "labtest",
				"ar": "الفحوصات"
			},
			"rules": [],
			"order": 3,
			"repeatable": true,
			"id": "labtest"
		}
	]
}
```

sample of a submission:

```json
{
    "createdBy": "777768534",
    "createdDate": "2025-04-22T18:05:06.245Z",
    "lastModifiedBy": "777768534",
    "lastModifiedDate": "2025-04-22T18:05:06.245Z",
    "uid": "AcO606OpOcB",
    "deleted": false,
    "dataTemplateUid": "MI8KQFsxGFc",
    "dataTemplateVersionUid": "ys2a06ekFXJ", 
    "dataTemplateVersionNo": 41,
    "teamUid": "PIFi00ALU8L",
    "orgUnitUid": "xbz5SoJ8lFn",
    "activityUid": "lHWAgDRdMHr",
    "assignmentUid": "xCrk3Bx7FmN",
    "startEntryTime": "2025-04-22T14:59:35.291Z",
    "finishedEntryTime": "2025-04-22T15:01:44.036Z",
    "formData": {
        "measures": {
            "cm_measures": "treatment"
        },
        "refeeral": {
        },
        "disease": {
            "diagnosed_disease_type": "malaria"
        },
        "medicines": [
            {
                "prescribeddrug": "ACT40",
                "druguom": "tape",
                "quantity": 1,
                "_id": "zYhMkxSZc6GRauWD", // this repeat id
                "_parentId": null, // parent repeat `_id` if nested or null
                "_submissionUid": "AcO606OpOcB", // root submission uid
                "_index": 1 // just for order
            }
        ],
        "notes": {
            "comment": "العلاج تركيز 20 لانه لا يوجد خيار قوة 20"
        },
        "main": {
            "visitdate": "2025-04-22T00:00:00.000"
        },
        "_status": "IN_PROGRESS",
        "labtest": [
            {
                "lab_test_type": "mrdt",
                "test_result": "positive",
                "positive_result_type": [
                    "pf"
                ],
                "_id": "D5vi4cFz2sYZTzeC",
                "_parentId": "AcO606OpOcB",
                "_submissionUid": "AcO606OpOcB",
                "_index": 1
            }
        ],
        "case": {
            "serialNumber": 48,
            "gender": "FEMALE",
            "PatientName": "رباب مجاهد علي عيطان"
        }
    }
}
```

---











