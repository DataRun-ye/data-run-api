## canonical_element:

```sql
SELECT
    ce."id",  -- canonical element id, type `uuid`
    ce.preferred_name, 
    ce.data_type, -- available `TEXT`, `BOOLEAN`, `INTEGER`, `DECIMAL`, `TIMESTAMP`, `ARRAY` (for multi select, and multi object repeat canonical elements)
    ce.semantic_type, -- e.g. Team, Option, OrgUnit, Activity.
    ce.option_set_uid, -- when semantic_type = `Option`, or `MultiSelectOption`, or null
    ce.parent_repeat_id, -- parent repeat canonical element id, when this element belongs to a repeat or null otherwise.
    ce.safe_name,  -- per templat safe pivoting column name, validated up stream.
    ce.template_uid,
    ce.anchor_allowed,
    ce.anchor_priority
FROM
	canonical_element AS ce
```

a template's canonical elements rows samples, minimal columns imported from db for one template:

```text
id	preferred_name	data_type	semantic_type	option_set_uid	parent_repeat_id	safe_name	template_uid	anchor_allowed	anchor_priority
2657f0c8-74c9-33aa-8211-7999d0c932fc	emergency_team_type	TEXT	Option	pb6PTAOtcZ5		emergency_team_type	YLcsWJlB7uy		
a431220c-3eec-32c6-a7c0-2524657498f0	other	TEXT				other	YLcsWJlB7uy		
3462b78c-eb4c-3fcb-ba5a-858ddd783440	NotificationNumber	INTEGER				notificationnumber	YLcsWJlB7uy		
ed46f43d-c8e3-303b-abdb-cb19f491ad01	visitdate	TIMESTAMP				visitdate	YLcsWJlB7uy		
e69c29ac-ece3-3f06-8ced-fcf8b2da1d3b	patients	ARRAY	Repeat		e69c29ac-ece3-3f06-8ced-fcf8b2da1d3b	patients	YLcsWJlB7uy		
fef67f83-bbbf-3aa1-bb6c-4af8901f7b1c	serialNumber	INTEGER			e69c29ac-ece3-3f06-8ced-fcf8b2da1d3b	serialnumber	YLcsWJlB7uy		
63bff109-5d9d-39e7-8d7e-cff5cdda66b5	PatientId	INTEGER			e69c29ac-ece3-3f06-8ced-fcf8b2da1d3b	patientid	YLcsWJlB7uy		
7d1cd87b-c7b4-3ed7-b865-d3b47079e3cf	PatientName	TEXT			e69c29ac-ece3-3f06-8ced-fcf8b2da1d3b	patientname	YLcsWJlB7uy		
26172272-d946-36c6-927b-9bb445bf332d	is_test_preformed	TEXT	Option	zK7EdFCwpx9	e69c29ac-ece3-3f06-8ced-fcf8b2da1d3b	is_test_preformed	YLcsWJlB7uy	t	30
896f8227-2920-3f4b-8c11-ebaa03a7ec70	investigations	ARRAY	Repeat		896f8227-2920-3f4b-8c11-ebaa03a7ec70	investigations	YLcsWJlB7uy		
69d7d0fb-9480-3457-8fe6-72fa70f6fe4e	lab_test_type	TEXT	Option	PiGAyROTsAB	896f8227-2920-3f4b-8c11-ebaa03a7ec70	lab_test_type	YLcsWJlB7uy		
41bde44c-de7c-35fe-8369-62cbd5f3e47e	test_result	TEXT	Option	tQjriOFVC2T	896f8227-2920-3f4b-8c11-ebaa03a7ec70	test_result	YLcsWJlB7uy	t	20
0220bf1d-3800-3de6-a78a-6c7fe049f5fc	mrdt_positive_types	ARRAY	Option	ISh7s9y1md4	896f8227-2920-3f4b-8c11-ebaa03a7ec70	mrdt_positive_types	YLcsWJlB7uy		
f8bac256-d673-345c-aad7-6092cfa3add6	mic_positive_types	ARRAY	Option	YINY2NUHdPo	896f8227-2920-3f4b-8c11-ebaa03a7ec70	mic_positive_types	YLcsWJlB7uy		
83d305b9-452a-3de1-8a6e-1ecfa0b43339	drdt_positive_types	ARRAY	Option	ySRdItzNQH4	896f8227-2920-3f4b-8c11-ebaa03a7ec70	drdt_positive_types	YLcsWJlB7uy		
80f698c9-5efe-30ae-b3f1-e9354371b37e	other_lab_test_type	TEXT			896f8227-2920-3f4b-8c11-ebaa03a7ec70	other_lab_test_type	YLcsWJlB7uy		
e269925e-01e7-3cff-9e09-6529ee58d4a4	other_lab_test_result	TEXT			896f8227-2920-3f4b-8c11-ebaa03a7ec70	other_lab_test_result	YLcsWJlB7uy		
```

## events
events can be of event_type = root i.e submission which same row as submissions_enriched, or repeat which is inside a
submission or nested in another repeat in a submission.

```sql
DROP TABLE IF EXISTS analytics.events_enriched;
CREATE TABLE analytics.events_enriched AS
SELECT
    ev.event_id, -- nonnull for root level submission id, or repeat instance_id for nested repeat events
    ev.parent_event_id, -- nearist parent event = the submission event id if this event is a repeat at root submission.
    ev.event_ce_id, -- canonincal_element_id of the repeat that describ this event, null for root level
    ev.template_uid,  -- nonnull 
    ev.submission_uid,  -- nonnull 
    ev.submission_creation_time, -- nonnull 
    ev.event_type,  -- root , repeat -- nonnull 
    -- name of the repeat e.g. patiants, orders, etc
    COALESCE(rce.preferred_name, dt.template_code) as event_name, -- if this is a repeat it would take the repeat's ce name otherwise (i.e root) it's the template that define the name of the event 

    ev.assignment_uid, -- nonnull
    ev.activity_uid,-- nonnull
    ev.team_uid,  -- nonnull
    ev.org_unit_uid,  -- nonnull

    ------------------------------------  
    -- 	event's anchor details, nullable
    ------------------------------------
    ev.anchor_ce_id AS anchor_ce_id,
    ance.semantic_type AS anchor_semantic_type,  -- Only Option, Team, OrgUnit, etc, can be anchors
    ance.data_type AS anchor_data_type,
    ance.preferred_name AS anchor_name,
    ance.option_set_uid AS anchor_option_set_uid, -- anchor's  option_set_uid

    ev.anchor_value_text, -- anchor's raw value
    ev.anchor_ref_uid, -- anchor's resolved value uid
    refe.ref_label AS anchor_resolved_lable -- anchor's resolved value label

FROM analytics.events ev
         LEFT JOIN public.canonical_element rce ON rce.id::uuid = ev.event_ce_id::uuid
         LEFT JOIN public.canonical_element ance ON ance.id::uuid = ev.anchor_ce_id::uuid
         LEFT JOIN analytics.dim_option_set ops ON ance.option_set_uid = ops.option_set_uid
         LEFT JOIN analytics.dim_data_template dt ON dt.template_uid = ev.template_uid
         LEFT JOIN analytics.ref_value_enriched AS refe ON refe.resolved_ref_uid = ev.anchor_ref_uid
```

events samples:

```text
event_id	parent_event_id	event_ce_id	template_uid	submission_uid	submission_creation_time	event_type	event_name	assignment_uid	activity_uid	team_uid	org_unit_uid	anchor_ce_id	anchor_semantic_type	anchor_data_type	anchor_name	anchor_option_set_uid	anchor_value_text	anchor_ref_uid	anchor_resolved_lable
01K67S98Y5CJEY5CB9N4MW4S43	jiH04Qbxr55	e69c29ac-ece3-3f06-8ced-fcf8b2da1d3b	YLcsWJlB7uy	jiH04Qbxr55	2025-09-28 12:08:27.268809	repeat	patients		TYduVflHzfZ	kLnZwSbdouA	OqcUGUYZYez	26172272-d946-36c6-927b-9bb445bf332d	Option	TEXT	is_test_preformed	zK7EdFCwpx9	yes	zjE0FT2oNAV	yes
01K67S98Y22WQYB6B2Q4W3YDEH	01K67S98Y20W4317V4ZTK1M9BH	896f8227-2920-3f4b-8c11-ebaa03a7ec70	YLcsWJlB7uy	jiH04Qbxr55	2025-09-28 12:08:27.268809	repeat	investigations		TYduVflHzfZ	kLnZwSbdouA	OqcUGUYZYez	41bde44c-de7c-35fe-8369-62cbd5f3e47e	Option	TEXT	test_result	tQjriOFVC2T	negative	gMCfcPlgAOs	negative
01K67S98Y2HVF9K1ND7VT5XXS8	jiH04Qbxr55	e69c29ac-ece3-3f06-8ced-fcf8b2da1d3b	YLcsWJlB7uy	jiH04Qbxr55	2025-09-28 12:08:27.268809	repeat	patients		TYduVflHzfZ	kLnZwSbdouA	OqcUGUYZYez	26172272-d946-36c6-927b-9bb445bf332d	Option	TEXT	is_test_preformed	zK7EdFCwpx9	yes	zjE0FT2oNAV	yes
01K67S98Y62DQ8XASSKRVC7BWD	01K67S98Y60GQMA8JAGMCSPREF	896f8227-2920-3f4b-8c11-ebaa03a7ec70	YLcsWJlB7uy	jiH04Qbxr55	2025-09-28 12:08:27.268809	repeat	investigations		TYduVflHzfZ	kLnZwSbdouA	OqcUGUYZYez	41bde44c-de7c-35fe-8369-62cbd5f3e47e	Option	TEXT	test_result	tQjriOFVC2T	negative	gMCfcPlgAOs	negative
01K67S98Y62SK2EBQH8NDBEZ8D	01K67S98Y6VGMT1AEQBWDPA7JQ	896f8227-2920-3f4b-8c11-ebaa03a7ec70	YLcsWJlB7uy	jiH04Qbxr55	2025-09-28 12:08:27.268809	repeat	investigations		TYduVflHzfZ	kLnZwSbdouA	OqcUGUYZYez	41bde44c-de7c-35fe-8369-62cbd5f3e47e	Option	TEXT	test_result	tQjriOFVC2T	negative	gMCfcPlgAOs	negative
01K67S98Y54P4P48ZWCCKAHZ88	jiH04Qbxr55	e69c29ac-ece3-3f06-8ced-fcf8b2da1d3b	YLcsWJlB7uy	jiH04Qbxr55	2025-09-28 12:08:27.268809	repeat	patients		TYduVflHzfZ	kLnZwSbdouA	OqcUGUYZYez	26172272-d946-36c6-927b-9bb445bf332d	Option	TEXT	is_test_preformed	zK7EdFCwpx9	yes	zjE0FT2oNAV	yes
01K67S98Y3A0SYEH7AD080WDQS	01K67S98Y3GV8PQA00FRW4Q8Y7	896f8227-2920-3f4b-8c11-ebaa03a7ec70	YLcsWJlB7uy	jiH04Qbxr55	2025-09-28 12:08:27.268809	repeat	investigations		TYduVflHzfZ	kLnZwSbdouA	OqcUGUYZYez	41bde44c-de7c-35fe-8369-62cbd5f3e47e	Option	TEXT	test_result	tQjriOFVC2T	negative	gMCfcPlgAOs	negative
01K67S98Y2AQPN8AYQW5FQMWZX	jiH04Qbxr55	e69c29ac-ece3-3f06-8ced-fcf8b2da1d3b	YLcsWJlB7uy	jiH04Qbxr55	2025-09-28 12:08:27.268809	repeat	patients		TYduVflHzfZ	kLnZwSbdouA	OqcUGUYZYez	26172272-d946-36c6-927b-9bb445bf332d	Option	TEXT	is_test_preformed	zK7EdFCwpx9	yes	zjE0FT2oNAV	yes
01K67S98Y7R8AX47D92KKSST6P	01K67S98Y7N4P5QX4Z7DZ8SQY8	896f8227-2920-3f4b-8c11-ebaa03a7ec70	YLcsWJlB7uy	jiH04Qbxr55	2025-09-28 12:08:27.268809	repeat	investigations		TYduVflHzfZ	kLnZwSbdouA	OqcUGUYZYez	41bde44c-de7c-35fe-8369-62cbd5f3e47e	Option	TEXT	test_result	tQjriOFVC2T	negative	gMCfcPlgAOs	negative
01K67S98Y85S8DFB015WJM9ABP	01K67S98Y8W0ER166HDB2DCH7X	896f8227-2920-3f4b-8c11-ebaa03a7ec70	YLcsWJlB7uy	jiH04Qbxr55	2025-09-28 12:08:27.268809	repeat	investigations		TYduVflHzfZ	kLnZwSbdouA	OqcUGUYZYez	41bde44c-de7c-35fe-8369-62cbd5f3e47e	Option	TEXT	test_result	tQjriOFVC2T	negative	gMCfcPlgAOs	negative
01K67S98Y5TE1CS4VE557GQFJH	01K67S98Y5CFZES29C6AHC5SWQ	896f8227-2920-3f4b-8c11-ebaa03a7ec70	YLcsWJlB7uy	jiH04Qbxr55	2025-09-28 12:08:27.268809	repeat	investigations		TYduVflHzfZ	kLnZwSbdouA	OqcUGUYZYez	41bde44c-de7c-35fe-8369-62cbd5f3e47e	Option	TEXT	test_result	tQjriOFVC2T	negative	gMCfcPlgAOs	negative
01K67S98Y2E346WTCRAEK861RZ	01K67S98Y2JXQFTG9K6NFN79C4	896f8227-2920-3f4b-8c11-ebaa03a7ec70	YLcsWJlB7uy	jiH04Qbxr55	2025-09-28 12:08:27.268809	repeat	investigations		TYduVflHzfZ	kLnZwSbdouA	OqcUGUYZYez	41bde44c-de7c-35fe-8369-62cbd5f3e47e	Option	TEXT	test_result	tQjriOFVC2T	negative	gMCfcPlgAOs	negative
01K67S98Y5PCSTY712HZNHAPX9	01K67S98Y55T63WP6BAGG3B9HC	896f8227-2920-3f4b-8c11-ebaa03a7ec70	YLcsWJlB7uy	jiH04Qbxr55	2025-09-28 12:08:27.268809	repeat	investigations		TYduVflHzfZ	kLnZwSbdouA	OqcUGUYZYez	41bde44c-de7c-35fe-8369-62cbd5f3e47e	Option	TEXT	test_result	tQjriOFVC2T	negative	gMCfcPlgAOs	negative
01K67S98Y6SPMEEY8TDJPV3YZZ	jiH04Qbxr55	e69c29ac-ece3-3f06-8ced-fcf8b2da1d3b	YLcsWJlB7uy	jiH04Qbxr55	2025-09-28 12:08:27.268809	repeat	patients		TYduVflHzfZ	kLnZwSbdouA	OqcUGUYZYez	26172272-d946-36c6-927b-9bb445bf332d	Option	TEXT	is_test_preformed	zK7EdFCwpx9	yes	zjE0FT2oNAV	yes
01K67S98Y50JY1K098ZSPRZPK7	01K67S98Y56C8G415SV6XEGX4A	896f8227-2920-3f4b-8c11-ebaa03a7ec70	YLcsWJlB7uy	jiH04Qbxr55	2025-09-28 12:08:27.268809	repeat	investigations		TYduVflHzfZ	kLnZwSbdouA	OqcUGUYZYez	41bde44c-de7c-35fe-8369-62cbd5f3e47e	Option	TEXT	test_result	tQjriOFVC2T	negative	gMCfcPlgAOs	negative
01K67S98Y6HEGA2FZ4WSG3VV12	01K67S98Y60D651Y4QSEMEVW61	896f8227-2920-3f4b-8c11-ebaa03a7ec70	YLcsWJlB7uy	jiH04Qbxr55	2025-09-28 12:08:27.268809	repeat	investigations		TYduVflHzfZ	kLnZwSbdouA	OqcUGUYZYez	41bde44c-de7c-35fe-8369-62cbd5f3e47e	Option	TEXT	test_result	tQjriOFVC2T	negative	gMCfcPlgAOs	negative
01K67S98Y3NEV6W1K4693R3ZFA	01K67S98Y3RYSVEV1NV31BFD28	896f8227-2920-3f4b-8c11-ebaa03a7ec70	YLcsWJlB7uy	jiH04Qbxr55	2025-09-28 12:08:27.268809	repeat	investigations		TYduVflHzfZ	kLnZwSbdouA	OqcUGUYZYez	41bde44c-de7c-35fe-8369-62cbd5f3e47e	Option	TEXT	test_result	tQjriOFVC2T	negative	gMCfcPlgAOs	negative
01K67S98Y6MV5F1H8TT234MT2N	01K67S98Y67D1P4FGWX3XVF6PF	896f8227-2920-3f4b-8c11-ebaa03a7ec70	YLcsWJlB7uy	jiH04Qbxr55	2025-09-28 12:08:27.268809	repeat	investigations		TYduVflHzfZ	kLnZwSbdouA	OqcUGUYZYez	41bde44c-de7c-35fe-8369-62cbd5f3e47e	Option	TEXT	test_result	tQjriOFVC2T	negative	gMCfcPlgAOs	negative
01K67S98Y67D1P4FGWX3XVF6PF	jiH04Qbxr55	e69c29ac-ece3-3f06-8ced-fcf8b2da1d3b	YLcsWJlB7uy	jiH04Qbxr55	2025-09-28 12:08:27.268809	repeat	patients		TYduVflHzfZ	kLnZwSbdouA	OqcUGUYZYez	26172272-d946-36c6-927b-9bb445bf332d	Option	TEXT	is_test_preformed	zK7EdFCwpx9	yes	zjE0FT2oNAV	yes
01K67S98Y6EQDQT06WWWM6Y7EB	01K67S98Y6BPSEEAJ8KN25Q4P7	896f8227-2920-3f4b-8c11-ebaa03a7ec70	YLcsWJlB7uy	jiH04Qbxr55	2025-09-28 12:08:27.268809	repeat	investigations		TYduVflHzfZ	kLnZwSbdouA	OqcUGUYZYez	41bde44c-de7c-35fe-8369-62cbd5f3e47e	Option	TEXT	test_result	tQjriOFVC2T	negative	gMCfcPlgAOs	negative
01K67S98Y4F13CBD8CNSBRGFDY	jiH04Qbxr55	e69c29ac-ece3-3f06-8ced-fcf8b2da1d3b	YLcsWJlB7uy	jiH04Qbxr55	2025-09-28 12:08:27.268809	repeat	patients		TYduVflHzfZ	kLnZwSbdouA	OqcUGUYZYez	26172272-d946-36c6-927b-9bb445bf332d	Option	TEXT	is_test_preformed	zK7EdFCwpx9	yes	zjE0FT2oNAV	yes
01K67S98Y2ZNWBVRS6VY5F1QH8	jiH04Qbxr55	e69c29ac-ece3-3f06-8ced-fcf8b2da1d3b	YLcsWJlB7uy	jiH04Qbxr55	2025-09-28 12:08:27.268809	repeat	patients		TYduVflHzfZ	kLnZwSbdouA	OqcUGUYZYez	26172272-d946-36c6-927b-9bb445bf332d	Option	TEXT	is_test_preformed	zK7EdFCwpx9	yes	zjE0FT2oNAV	yes
01K67S98Y5G07WTRN7HERGSVSZ	01K67S98Y5F3N3QH6FSJ1T8TST	896f8227-2920-3f4b-8c11-ebaa03a7ec70	YLcsWJlB7uy	jiH04Qbxr55	2025-09-28 12:08:27.268809	repeat	investigations		TYduVflHzfZ	kLnZwSbdouA	OqcUGUYZYez	41bde44c-de7c-35fe-8369-62cbd5f3e47e	Option	TEXT	test_result	tQjriOFVC2T	negative	gMCfcPlgAOs	negative
01K67S98Y5CTWN2JVDM6HFEBXS	01K67S98Y4QZX7CQ6AQCJKCY9M	896f8227-2920-3f4b-8c11-ebaa03a7ec70	YLcsWJlB7uy	jiH04Qbxr55	2025-09-28 12:08:27.268809	repeat	investigations		TYduVflHzfZ	kLnZwSbdouA	OqcUGUYZYez	41bde44c-de7c-35fe-8369-62cbd5f3e47e	Option	TEXT	test_result	tQjriOFVC2T	negative	gMCfcPlgAOs	negative
01K67S98Y4V1WX67CDGTY1NK7V	jiH04Qbxr55	e69c29ac-ece3-3f06-8ced-fcf8b2da1d3b	YLcsWJlB7uy	jiH04Qbxr55	2025-09-28 12:08:27.268809	repeat	patients		TYduVflHzfZ	kLnZwSbdouA	OqcUGUYZYez	26172272-d946-36c6-927b-9bb445bf332d	Option	TEXT	is_test_preformed	zK7EdFCwpx9	yes	zjE0FT2oNAV	yes
01K67S98Y56C8G415SV6XEGX4A	jiH04Qbxr55	e69c29ac-ece3-3f06-8ced-fcf8b2da1d3b	YLcsWJlB7uy	jiH04Qbxr55	2025-09-28 12:08:27.268809	repeat	patients		TYduVflHzfZ	kLnZwSbdouA	OqcUGUYZYez	26172272-d946-36c6-927b-9bb445bf332d	Option	TEXT	is_test_preformed	zK7EdFCwpx9	yes	zjE0FT2oNAV	yes
01K67S98Y8T2W70SGQQM1MMSA5	jiH04Qbxr55	e69c29ac-ece3-3f06-8ced-fcf8b2da1d3b	YLcsWJlB7uy	jiH04Qbxr55	2025-09-28 12:08:27.268809	repeat	patients		TYduVflHzfZ	kLnZwSbdouA	OqcUGUYZYez	26172272-d946-36c6-927b-9bb445bf332d	Option	TEXT	is_test_preformed	zK7EdFCwpx9	yes	zjE0FT2oNAV	yes
01K67S98Y464AAYT7P6NF8AKCM	01K67S98Y4TTJFCJWS0HS2DV0E	896f8227-2920-3f4b-8c11-ebaa03a7ec70	YLcsWJlB7uy	jiH04Qbxr55	2025-09-28 12:08:27.268809	repeat	investigations		TYduVflHzfZ	kLnZwSbdouA	OqcUGUYZYez	41bde44c-de7c-35fe-8369-62cbd5f3e47e	Option	TEXT	test_result	tQjriOFVC2T	negative	gMCfcPlgAOs	negative
01K67S98Y4RE1JW35NAN8TPD6P	01K67S98Y44RR4G4N8RP6JP57V	896f8227-2920-3f4b-8c11-ebaa03a7ec70	YLcsWJlB7uy	jiH04Qbxr55	2025-09-28 12:08:27.268809	repeat	investigations		TYduVflHzfZ	kLnZwSbdouA	OqcUGUYZYez	41bde44c-de7c-35fe-8369-62cbd5f3e47e	Option	TEXT	test_result	tQjriOFVC2T	negative	gMCfcPlgAOs	negative
01K67S98Y4QZX7CQ6AQCJKCY9M	jiH04Qbxr55	e69c29ac-ece3-3f06-8ced-fcf8b2da1d3b	YLcsWJlB7uy	jiH04Qbxr55	2025-09-28 12:08:27.268809	repeat	patients		TYduVflHzfZ	kLnZwSbdouA	OqcUGUYZYez	26172272-d946-36c6-927b-9bb445bf332d	Option	TEXT	is_test_preformed	zK7EdFCwpx9	yes	zjE0FT2oNAV	yes
01K67S98Y6VGMT1AEQBWDPA7JQ	jiH04Qbxr55	e69c29ac-ece3-3f06-8ced-fcf8b2da1d3b	YLcsWJlB7uy	jiH04Qbxr55	2025-09-28 12:08:27.268809	repeat	patients		TYduVflHzfZ	kLnZwSbdouA	OqcUGUYZYez	26172272-d946-36c6-927b-9bb445bf332d	Option	TEXT	is_test_preformed	zK7EdFCwpx9	yes	zjE0FT2oNAV	yes
01K67S98Y4TTJFCJWS0HS2DV0E	jiH04Qbxr55	e69c29ac-ece3-3f06-8ced-fcf8b2da1d3b	YLcsWJlB7uy	jiH04Qbxr55	2025-09-28 12:08:27.268809	repeat	patients		TYduVflHzfZ	kLnZwSbdouA	OqcUGUYZYez	26172272-d946-36c6-927b-9bb445bf332d	Option	TEXT	is_test_preformed	zK7EdFCwpx9	yes	zjE0FT2oNAV	yes
01K67S98Y4BF7EPQ3FKYV6B6EM	01K67S98Y41BEZDDVNJE0HVQXS	896f8227-2920-3f4b-8c11-ebaa03a7ec70	YLcsWJlB7uy	jiH04Qbxr55	2025-09-28 12:08:27.268809	repeat	investigations		TYduVflHzfZ	kLnZwSbdouA	OqcUGUYZYez	41bde44c-de7c-35fe-8369-62cbd5f3e47e	Option	TEXT	test_result	tQjriOFVC2T	negative	gMCfcPlgAOs	negative
01K67S98Y4ERKNRR5NYNAVR80A	01K67S98Y4S6M5K2WG7B26BTYN	896f8227-2920-3f4b-8c11-ebaa03a7ec70	YLcsWJlB7uy	jiH04Qbxr55	2025-09-28 12:08:27.268809	repeat	investigations		TYduVflHzfZ	kLnZwSbdouA	OqcUGUYZYez	41bde44c-de7c-35fe-8369-62cbd5f3e47e	Option	TEXT	test_result	tQjriOFVC2T	negative	gMCfcPlgAOs	negative
01K67S98Y4WJ9Y83FY1D6A4A3M	01K67S98Y4V1WX67CDGTY1NK7V	896f8227-2920-3f4b-8c11-ebaa03a7ec70	YLcsWJlB7uy	jiH04Qbxr55	2025-09-28 12:08:27.268809	repeat	investigations		TYduVflHzfZ	kLnZwSbdouA	OqcUGUYZYez	41bde44c-de7c-35fe-8369-62cbd5f3e47e	Option	TEXT	test_result	tQjriOFVC2T	negative	gMCfcPlgAOs	negative
01K67S98Y2JXQFTG9K6NFN79C4	jiH04Qbxr55	e69c29ac-ece3-3f06-8ced-fcf8b2da1d3b	YLcsWJlB7uy	jiH04Qbxr55	2025-09-28 12:08:27.268809	repeat	patients		TYduVflHzfZ	kLnZwSbdouA	OqcUGUYZYez	26172272-d946-36c6-927b-9bb445bf332d	Option	TEXT	is_test_preformed	zK7EdFCwpx9	yes	zjE0FT2oNAV	yes
jiH04Qbxr55			YLcsWJlB7uy	jiH04Qbxr55	2025-09-28 12:08:27.268809	root			TYduVflHzfZ	kLnZwSbdouA	OqcUGUYZYez								
01K67S98Y28NN8MBCKSTEBNWFY	01K67S98Y2AQPN8AYQW5FQMWZX	896f8227-2920-3f4b-8c11-ebaa03a7ec70	YLcsWJlB7uy	jiH04Qbxr55	2025-09-28 12:08:27.268809	repeat	investigations		TYduVflHzfZ	kLnZwSbdouA	OqcUGUYZYez	41bde44c-de7c-35fe-8369-62cbd5f3e47e	Option	TEXT	test_result	tQjriOFVC2T	positive	zyk9n6HF0OP	positive
01K67S98Y60D651Y4QSEMEVW61	jiH04Qbxr55	e69c29ac-ece3-3f06-8ced-fcf8b2da1d3b	YLcsWJlB7uy	jiH04Qbxr55	2025-09-28 12:08:27.268809	repeat	patients		TYduVflHzfZ	kLnZwSbdouA	OqcUGUYZYez	26172272-d946-36c6-927b-9bb445bf332d	Option	TEXT	is_test_preformed	zK7EdFCwpx9	yes	zjE0FT2oNAV	yes
01K67S98Y8M8ANNSG2EQ52FMVQ	01K67S98Y8PH24Q3CYGZRQ7S5H	896f8227-2920-3f4b-8c11-ebaa03a7ec70	YLcsWJlB7uy	jiH04Qbxr55	2025-09-28 12:08:27.268809	repeat	investigations		TYduVflHzfZ	kLnZwSbdouA	OqcUGUYZYez	41bde44c-de7c-35fe-8369-62cbd5f3e47e	Option	TEXT	test_result	tQjriOFVC2T	negative	gMCfcPlgAOs	negative
01K67S98Y69RFX3PSECTW7TW9A	01K67S98Y6YRWRCJSND6YCFMYD	896f8227-2920-3f4b-8c11-ebaa03a7ec70	YLcsWJlB7uy	jiH04Qbxr55	2025-09-28 12:08:27.268809	repeat	investigations		TYduVflHzfZ	kLnZwSbdouA	OqcUGUYZYez	41bde44c-de7c-35fe-8369-62cbd5f3e47e	Option	TEXT	test_result	tQjriOFVC2T	negative	gMCfcPlgAOs	negative
01K67S98Y2H0AJ4HC0JEMAPB8E	jiH04Qbxr55	e69c29ac-ece3-3f06-8ced-fcf8b2da1d3b	YLcsWJlB7uy	jiH04Qbxr55	2025-09-28 12:08:27.268809	repeat	patients		TYduVflHzfZ	kLnZwSbdouA	OqcUGUYZYez	26172272-d946-36c6-927b-9bb445bf332d	Option	TEXT	is_test_preformed	zK7EdFCwpx9	yes	zjE0FT2oNAV	yes
01K67S98Y44CYWNAC36SN70S7Z	jiH04Qbxr55	e69c29ac-ece3-3f06-8ced-fcf8b2da1d3b	YLcsWJlB7uy	jiH04Qbxr55	2025-09-28 12:08:27.268809	repeat	patients		TYduVflHzfZ	kLnZwSbdouA	OqcUGUYZYez	26172272-d946-36c6-927b-9bb445bf332d	Option	TEXT	is_test_preformed	zK7EdFCwpx9	yes	zjE0FT2oNAV	yes
01K67S98Y6BPSEEAJ8KN25Q4P7	jiH04Qbxr55	e69c29ac-ece3-3f06-8ced-fcf8b2da1d3b	YLcsWJlB7uy	jiH04Qbxr55	2025-09-28 12:08:27.268809	repeat	patients		TYduVflHzfZ	kLnZwSbdouA	OqcUGUYZYez	26172272-d946-36c6-927b-9bb445bf332d	Option	TEXT	is_test_preformed	zK7EdFCwpx9	yes	zjE0FT2oNAV	yes
01K67S98Y7N4P5QX4Z7DZ8SQY8	jiH04Qbxr55	e69c29ac-ece3-3f06-8ced-fcf8b2da1d3b	YLcsWJlB7uy	jiH04Qbxr55	2025-09-28 12:08:27.268809	repeat	patients		TYduVflHzfZ	kLnZwSbdouA	OqcUGUYZYez	26172272-d946-36c6-927b-9bb445bf332d	Option	TEXT	is_test_preformed	zK7EdFCwpx9	yes	zjE0FT2oNAV	yes
01K67S98Y8MGMDZNQEJTVRHWK4	01K67S98Y8GJM80RBRM6KB38R6	896f8227-2920-3f4b-8c11-ebaa03a7ec70	YLcsWJlB7uy	jiH04Qbxr55	2025-09-28 12:08:27.268809	repeat	investigations		TYduVflHzfZ	kLnZwSbdouA	OqcUGUYZYez	41bde44c-de7c-35fe-8369-62cbd5f3e47e	Option	TEXT	test_result	tQjriOFVC2T	negative	gMCfcPlgAOs	negative
01K67S98Y4AN4K5XSYKSPWNYKF	01K67S98Y44MF4SQ7DPFR9JJ6Z	896f8227-2920-3f4b-8c11-ebaa03a7ec70	YLcsWJlB7uy	jiH04Qbxr55	2025-09-28 12:08:27.268809	repeat	investigations		TYduVflHzfZ	kLnZwSbdouA	OqcUGUYZYez	41bde44c-de7c-35fe-8369-62cbd5f3e47e	Option	TEXT	test_result	tQjriOFVC2T	negative	gMCfcPlgAOs	negative
01K67S98Y60GQMA8JAGMCSPREF	jiH04Qbxr55	e69c29ac-ece3-3f06-8ced-fcf8b2da1d3b	YLcsWJlB7uy	jiH04Qbxr55	2025-09-28 12:08:27.268809	repeat	patients		TYduVflHzfZ	kLnZwSbdouA	OqcUGUYZYez	26172272-d946-36c6-927b-9bb445bf332d	Option	TEXT	is_test_preformed	zK7EdFCwpx9	yes	zjE0FT2oNAV	yes
01K67S98Y3RYSVEV1NV31BFD28	jiH04Qbxr55	e69c29ac-ece3-3f06-8ced-fcf8b2da1d3b	YLcsWJlB7uy	jiH04Qbxr55	2025-09-28 12:08:27.268809	repeat	patients		TYduVflHzfZ	kLnZwSbdouA	OqcUGUYZYez	26172272-d946-36c6-927b-9bb445bf332d	Option	TEXT	is_test_preformed	zK7EdFCwpx9	yes	zjE0FT2oNAV	yes
01K67S98Y75Y3T1DH16YT9AWFJ	01K67S98Y7NCTM0SZ8PB9RY4XM	896f8227-2920-3f4b-8c11-ebaa03a7ec70	YLcsWJlB7uy	jiH04Qbxr55	2025-09-28 12:08:27.268809	repeat	investigations		TYduVflHzfZ	kLnZwSbdouA	OqcUGUYZYez	41bde44c-de7c-35fe-8369-62cbd5f3e47e	Option	TEXT	test_result	tQjriOFVC2T	negative	gMCfcPlgAOs	negative
01K67S98Y5562Q8EV898E9HVRY	01K67S98Y5ECD936T9YNZNQCW5	896f8227-2920-3f4b-8c11-ebaa03a7ec70	YLcsWJlB7uy	jiH04Qbxr55	2025-09-28 12:08:27.268809	repeat	investigations		TYduVflHzfZ	kLnZwSbdouA	OqcUGUYZYez	41bde44c-de7c-35fe-8369-62cbd5f3e47e	Option	TEXT	test_result	tQjriOFVC2T	negative	gMCfcPlgAOs	negative
01K67S98Y34RGJ3XE23EP904N5	01K67S98Y2BH1DX1B4ZYYQ2HHJ	896f8227-2920-3f4b-8c11-ebaa03a7ec70	YLcsWJlB7uy	jiH04Qbxr55	2025-09-28 12:08:27.268809	repeat	investigations		TYduVflHzfZ	kLnZwSbdouA	OqcUGUYZYez	41bde44c-de7c-35fe-8369-62cbd5f3e47e	Option	TEXT	test_result	tQjriOFVC2T	negative	gMCfcPlgAOs	negative
01K67S98Y8W0ER166HDB2DCH7X	jiH04Qbxr55	e69c29ac-ece3-3f06-8ced-fcf8b2da1d3b	YLcsWJlB7uy	jiH04Qbxr55	2025-09-28 12:08:27.268809	repeat	patients		TYduVflHzfZ	kLnZwSbdouA	OqcUGUYZYez	26172272-d946-36c6-927b-9bb445bf332d	Option	TEXT	is_test_preformed	zK7EdFCwpx9	yes	zjE0FT2oNAV	yes
01K67S98Y5NP4NGP1HGBJRQ3QP	01K67S98Y55BPMERZ9G6RDFXGQ	896f8227-2920-3f4b-8c11-ebaa03a7ec70	YLcsWJlB7uy	jiH04Qbxr55	2025-09-28 12:08:27.268809	repeat	investigations		TYduVflHzfZ	kLnZwSbdouA	OqcUGUYZYez	41bde44c-de7c-35fe-8369-62cbd5f3e47e	Option	TEXT	test_result	tQjriOFVC2T	negative	gMCfcPlgAOs	negative
01K67S98Y7NCTM0SZ8PB9RY4XM	jiH04Qbxr55	e69c29ac-ece3-3f06-8ced-fcf8b2da1d3b	YLcsWJlB7uy	jiH04Qbxr55	2025-09-28 12:08:27.268809	repeat	patients		TYduVflHzfZ	kLnZwSbdouA	OqcUGUYZYez	26172272-d946-36c6-927b-9bb445bf332d	Option	TEXT	is_test_preformed	zK7EdFCwpx9	yes	zjE0FT2oNAV	yes
01K67S98Y80P9ESK551WGBXZDJ	jiH04Qbxr55	e69c29ac-ece3-3f06-8ced-fcf8b2da1d3b	YLcsWJlB7uy	jiH04Qbxr55	2025-09-28 12:08:27.268809	repeat	patients		TYduVflHzfZ	kLnZwSbdouA	OqcUGUYZYez	26172272-d946-36c6-927b-9bb445bf332d	Option	TEXT	is_test_preformed	zK7EdFCwpx9	yes	zjE0FT2oNAV	yes
01K67S98Y2F4XKH5T5ME0VMZ81	01K67S98Y2YD34NFRA3ZA6NSFW	896f8227-2920-3f4b-8c11-ebaa03a7ec70	YLcsWJlB7uy	jiH04Qbxr55	2025-09-28 12:08:27.268809	repeat	investigations		TYduVflHzfZ	kLnZwSbdouA	OqcUGUYZYez	41bde44c-de7c-35fe-8369-62cbd5f3e47e	Option	TEXT	test_result	tQjriOFVC2T	negative	gMCfcPlgAOs	negative
01K67S98Y6W1537M3WN1H2PJD5	01K67S98Y6DQ7W2CZ75KAG45CG	896f8227-2920-3f4b-8c11-ebaa03a7ec70	YLcsWJlB7uy	jiH04Qbxr55	2025-09-28 12:08:27.268809	repeat	investigations		TYduVflHzfZ	kLnZwSbdouA	OqcUGUYZYez	41bde44c-de7c-35fe-8369-62cbd5f3e47e	Option	TEXT	test_result	tQjriOFVC2T	negative	gMCfcPlgAOs	negative
01K67S98Y5E3NG9WRTGT93YXJ2	01K67S98Y555MYMY1X7N483917	896f8227-2920-3f4b-8c11-ebaa03a7ec70	YLcsWJlB7uy	jiH04Qbxr55	2025-09-28 12:08:27.268809	repeat	investigations		TYduVflHzfZ	kLnZwSbdouA	OqcUGUYZYez	41bde44c-de7c-35fe-8369-62cbd5f3e47e	Option	TEXT	test_result	tQjriOFVC2T	negative	gMCfcPlgAOs	negative
01K67S98Y5F3N3QH6FSJ1T8TST	jiH04Qbxr55	e69c29ac-ece3-3f06-8ced-fcf8b2da1d3b	YLcsWJlB7uy	jiH04Qbxr55	2025-09-28 12:08:27.268809	repeat	patients		TYduVflHzfZ	kLnZwSbdouA	OqcUGUYZYez	26172272-d946-36c6-927b-9bb445bf332d	Option	TEXT	is_test_preformed	zK7EdFCwpx9	yes	zjE0FT2oNAV	yes
01K67S98Y8MRW9ZXJQVSZ55XVH	01K67S98Y8WCT7HK3TVPNXTCBN	896f8227-2920-3f4b-8c11-ebaa03a7ec70	YLcsWJlB7uy	jiH04Qbxr55	2025-09-28 12:08:27.268809	repeat	investigations		TYduVflHzfZ	kLnZwSbdouA	OqcUGUYZYez	41bde44c-de7c-35fe-8369-62cbd5f3e47e	Option	TEXT	test_result	tQjriOFVC2T	negative	gMCfcPlgAOs	negative
01K67S98Y555MYMY1X7N483917	jiH04Qbxr55	e69c29ac-ece3-3f06-8ced-fcf8b2da1d3b	YLcsWJlB7uy	jiH04Qbxr55	2025-09-28 12:08:27.268809	repeat	patients		TYduVflHzfZ	kLnZwSbdouA	OqcUGUYZYez	26172272-d946-36c6-927b-9bb445bf332d	Option	TEXT	is_test_preformed	zK7EdFCwpx9	yes	zjE0FT2oNAV	yes
01K67S98Y5X4MWQZZ6FZZ4FKV1	01K67S98Y54P4P48ZWCCKAHZ88	896f8227-2920-3f4b-8c11-ebaa03a7ec70	YLcsWJlB7uy	jiH04Qbxr55	2025-09-28 12:08:27.268809	repeat	investigations		TYduVflHzfZ	kLnZwSbdouA	OqcUGUYZYez	41bde44c-de7c-35fe-8369-62cbd5f3e47e	Option	TEXT	test_result	tQjriOFVC2T	negative	gMCfcPlgAOs	negative
01K67S98Y57SJZDPGMHCR8RAJY	01K67S98Y5SAYQ1986M9K3JANX	896f8227-2920-3f4b-8c11-ebaa03a7ec70	YLcsWJlB7uy	jiH04Qbxr55	2025-09-28 12:08:27.268809	repeat	investigations		TYduVflHzfZ	kLnZwSbdouA	OqcUGUYZYez	41bde44c-de7c-35fe-8369-62cbd5f3e47e	Option	TEXT	test_result	tQjriOFVC2T	negative	gMCfcPlgAOs	negative
01K67S98Y61FX4X35CEV4EZFAV	jiH04Qbxr55	e69c29ac-ece3-3f06-8ced-fcf8b2da1d3b	YLcsWJlB7uy	jiH04Qbxr55	2025-09-28 12:08:27.268809	repeat	patients		TYduVflHzfZ	kLnZwSbdouA	OqcUGUYZYez	26172272-d946-36c6-927b-9bb445bf332d	Option	TEXT	is_test_preformed	zK7EdFCwpx9	yes	zjE0FT2oNAV	yes
01K67S98Y5BKZWBQWNNFCQ4M7W	01K67S98Y5P4XQAV660YFHQDJR	896f8227-2920-3f4b-8c11-ebaa03a7ec70	YLcsWJlB7uy	jiH04Qbxr55	2025-09-28 12:08:27.268809	repeat	investigations		TYduVflHzfZ	kLnZwSbdouA	OqcUGUYZYez	41bde44c-de7c-35fe-8369-62cbd5f3e47e	Option	TEXT	test_result	tQjriOFVC2T	negative	gMCfcPlgAOs	negative
01K67S98Y2YD34NFRA3ZA6NSFW	jiH04Qbxr55	e69c29ac-ece3-3f06-8ced-fcf8b2da1d3b	YLcsWJlB7uy	jiH04Qbxr55	2025-09-28 12:08:27.268809	repeat	patients		TYduVflHzfZ	kLnZwSbdouA	OqcUGUYZYez	26172272-d946-36c6-927b-9bb445bf332d	Option	TEXT	is_test_preformed	zK7EdFCwpx9	yes	zjE0FT2oNAV	yes
01K67S98Y28CV3P727EQWB9VNY	01K67S98Y2H0AJ4HC0JEMAPB8E	896f8227-2920-3f4b-8c11-ebaa03a7ec70	YLcsWJlB7uy	jiH04Qbxr55	2025-09-28 12:08:27.268809	repeat	investigations		TYduVflHzfZ	kLnZwSbdouA	OqcUGUYZYez	41bde44c-de7c-35fe-8369-62cbd5f3e47e	Option	TEXT	test_result	tQjriOFVC2T	negative	gMCfcPlgAOs	negative
01K67S98Y55BPMERZ9G6RDFXGQ	jiH04Qbxr55	e69c29ac-ece3-3f06-8ced-fcf8b2da1d3b	YLcsWJlB7uy	jiH04Qbxr55	2025-09-28 12:08:27.268809	repeat	patients		TYduVflHzfZ	kLnZwSbdouA	OqcUGUYZYez	26172272-d946-36c6-927b-9bb445bf332d	Option	TEXT	is_test_preformed	zK7EdFCwpx9	yes	zjE0FT2oNAV	yes
01K67S98Y4NCARPKNYFG823WJP	01K67S98Y44CYWNAC36SN70S7Z	896f8227-2920-3f4b-8c11-ebaa03a7ec70	YLcsWJlB7uy	jiH04Qbxr55	2025-09-28 12:08:27.268809	repeat	investigations		TYduVflHzfZ	kLnZwSbdouA	OqcUGUYZYez	41bde44c-de7c-35fe-8369-62cbd5f3e47e	Option	TEXT	test_result	tQjriOFVC2T	negative	gMCfcPlgAOs	negative
01K67S98Y62TNJVDXZMNPD8FN7	01K67S98Y6SPMEEY8TDJPV3YZZ	896f8227-2920-3f4b-8c11-ebaa03a7ec70	YLcsWJlB7uy	jiH04Qbxr55	2025-09-28 12:08:27.268809	repeat	investigations		TYduVflHzfZ	kLnZwSbdouA	OqcUGUYZYez	41bde44c-de7c-35fe-8369-62cbd5f3e47e	Option	TEXT	test_result	tQjriOFVC2T	negative	gMCfcPlgAOs	negative
01K67S98Y2272WSBZV8NQZ07Z3	01K67S98Y2ZNWBVRS6VY5F1QH8	896f8227-2920-3f4b-8c11-ebaa03a7ec70	YLcsWJlB7uy	jiH04Qbxr55	2025-09-28 12:08:27.268809	repeat	investigations		TYduVflHzfZ	kLnZwSbdouA	OqcUGUYZYez	41bde44c-de7c-35fe-8369-62cbd5f3e47e	Option	TEXT	test_result	tQjriOFVC2T	negative	gMCfcPlgAOs	negative
01K67S98Y47SK7HJZQD7DRDK9B	01K67S98Y4F13CBD8CNSBRGFDY	896f8227-2920-3f4b-8c11-ebaa03a7ec70	YLcsWJlB7uy	jiH04Qbxr55	2025-09-28 12:08:27.268809	repeat	investigations		TYduVflHzfZ	kLnZwSbdouA	OqcUGUYZYez	41bde44c-de7c-35fe-8369-62cbd5f3e47e	Option	TEXT	test_result	tQjriOFVC2T	negative	gMCfcPlgAOs	negative
01K67S98Y8WCT7HK3TVPNXTCBN	jiH04Qbxr55	e69c29ac-ece3-3f06-8ced-fcf8b2da1d3b	YLcsWJlB7uy	jiH04Qbxr55	2025-09-28 12:08:27.268809	repeat	patients		TYduVflHzfZ	kLnZwSbdouA	OqcUGUYZYez	26172272-d946-36c6-927b-9bb445bf332d	Option	TEXT	is_test_preformed	zK7EdFCwpx9	yes	zjE0FT2oNAV	yes
01K67S98Y5P4XQAV660YFHQDJR	jiH04Qbxr55	e69c29ac-ece3-3f06-8ced-fcf8b2da1d3b	YLcsWJlB7uy	jiH04Qbxr55	2025-09-28 12:08:27.268809	repeat	patients		TYduVflHzfZ	kLnZwSbdouA	OqcUGUYZYez	26172272-d946-36c6-927b-9bb445bf332d	Option	TEXT	is_test_preformed	zK7EdFCwpx9	yes	zjE0FT2oNAV	yes
01K67S98Y5A2JDZB4JSJJPF13Y	01K67S98Y5CJEY5CB9N4MW4S43	896f8227-2920-3f4b-8c11-ebaa03a7ec70	YLcsWJlB7uy	jiH04Qbxr55	2025-09-28 12:08:27.268809	repeat	investigations		TYduVflHzfZ	kLnZwSbdouA	OqcUGUYZYez	41bde44c-de7c-35fe-8369-62cbd5f3e47e	Option	TEXT	test_result	tQjriOFVC2T	negative	gMCfcPlgAOs	negative
01K67S98Y5SAYQ1986M9K3JANX	jiH04Qbxr55	e69c29ac-ece3-3f06-8ced-fcf8b2da1d3b	YLcsWJlB7uy	jiH04Qbxr55	2025-09-28 12:08:27.268809	repeat	patients		TYduVflHzfZ	kLnZwSbdouA	OqcUGUYZYez	26172272-d946-36c6-927b-9bb445bf332d	Option	TEXT	is_test_preformed	zK7EdFCwpx9	yes	zjE0FT2oNAV	yes
01K67S98Y44RR4G4N8RP6JP57V	jiH04Qbxr55	e69c29ac-ece3-3f06-8ced-fcf8b2da1d3b	YLcsWJlB7uy	jiH04Qbxr55	2025-09-28 12:08:27.268809	repeat	patients		TYduVflHzfZ	kLnZwSbdouA	OqcUGUYZYez	26172272-d946-36c6-927b-9bb445bf332d	Option	TEXT	is_test_preformed	zK7EdFCwpx9	yes	zjE0FT2oNAV	yes
01K67S98Y2BH1DX1B4ZYYQ2HHJ	jiH04Qbxr55	e69c29ac-ece3-3f06-8ced-fcf8b2da1d3b	YLcsWJlB7uy	jiH04Qbxr55	2025-09-28 12:08:27.268809	repeat	patients		TYduVflHzfZ	kLnZwSbdouA	OqcUGUYZYez	26172272-d946-36c6-927b-9bb445bf332d	Option	TEXT	is_test_preformed	zK7EdFCwpx9	yes	zjE0FT2oNAV	yes
01K67S98Y44MF4SQ7DPFR9JJ6Z	jiH04Qbxr55	e69c29ac-ece3-3f06-8ced-fcf8b2da1d3b	YLcsWJlB7uy	jiH04Qbxr55	2025-09-28 12:08:27.268809	repeat	patients		TYduVflHzfZ	kLnZwSbdouA	OqcUGUYZYez	26172272-d946-36c6-927b-9bb445bf332d	Option	TEXT	is_test_preformed	zK7EdFCwpx9	yes	zjE0FT2oNAV	yes
01K67S98Y6DQ7W2CZ75KAG45CG	jiH04Qbxr55	e69c29ac-ece3-3f06-8ced-fcf8b2da1d3b	YLcsWJlB7uy	jiH04Qbxr55	2025-09-28 12:08:27.268809	repeat	patients		TYduVflHzfZ	kLnZwSbdouA	OqcUGUYZYez	26172272-d946-36c6-927b-9bb445bf332d	Option	TEXT	is_test_preformed	zK7EdFCwpx9	yes	zjE0FT2oNAV	yes
01K67S98Y20W4317V4ZTK1M9BH	jiH04Qbxr55	e69c29ac-ece3-3f06-8ced-fcf8b2da1d3b	YLcsWJlB7uy	jiH04Qbxr55	2025-09-28 12:08:27.268809	repeat	patients		TYduVflHzfZ	kLnZwSbdouA	OqcUGUYZYez	26172272-d946-36c6-927b-9bb445bf332d	Option	TEXT	is_test_preformed	zK7EdFCwpx9	yes	zjE0FT2oNAV	yes
01K67S98Y66B4HJ4HVGXYM2F2P	01K67S98Y61FX4X35CEV4EZFAV	896f8227-2920-3f4b-8c11-ebaa03a7ec70	YLcsWJlB7uy	jiH04Qbxr55	2025-09-28 12:08:27.268809	repeat	investigations		TYduVflHzfZ	kLnZwSbdouA	OqcUGUYZYez	41bde44c-de7c-35fe-8369-62cbd5f3e47e	Option	TEXT	test_result	tQjriOFVC2T	negative	gMCfcPlgAOs	negative
01K67S98Y8N2AW5V0KYKS7D3Y9	01K67S98Y8T2W70SGQQM1MMSA5	896f8227-2920-3f4b-8c11-ebaa03a7ec70	YLcsWJlB7uy	jiH04Qbxr55	2025-09-28 12:08:27.268809	repeat	investigations		TYduVflHzfZ	kLnZwSbdouA	OqcUGUYZYez	41bde44c-de7c-35fe-8369-62cbd5f3e47e	Option	TEXT	test_result	tQjriOFVC2T	negative	gMCfcPlgAOs	negative
01K67S98Y4S6M5K2WG7B26BTYN	jiH04Qbxr55	e69c29ac-ece3-3f06-8ced-fcf8b2da1d3b	YLcsWJlB7uy	jiH04Qbxr55	2025-09-28 12:08:27.268809	repeat	patients		TYduVflHzfZ	kLnZwSbdouA	OqcUGUYZYez	26172272-d946-36c6-927b-9bb445bf332d	Option	TEXT	is_test_preformed	zK7EdFCwpx9	yes	zjE0FT2oNAV	yes
01K67S98Y3GV8PQA00FRW4Q8Y7	jiH04Qbxr55	e69c29ac-ece3-3f06-8ced-fcf8b2da1d3b	YLcsWJlB7uy	jiH04Qbxr55	2025-09-28 12:08:27.268809	repeat	patients		TYduVflHzfZ	kLnZwSbdouA	OqcUGUYZYez	26172272-d946-36c6-927b-9bb445bf332d	Option	TEXT	is_test_preformed	zK7EdFCwpx9	yes	zjE0FT2oNAV	yes
01K67S98Y2WPQR6D2EHPXTAMYZ	jiH04Qbxr55	e69c29ac-ece3-3f06-8ced-fcf8b2da1d3b	YLcsWJlB7uy	jiH04Qbxr55	2025-09-28 12:08:27.268809	repeat	patients		TYduVflHzfZ	kLnZwSbdouA	OqcUGUYZYez	26172272-d946-36c6-927b-9bb445bf332d	Option	TEXT	is_test_preformed	zK7EdFCwpx9	yes	zjE0FT2oNAV	yes
01K67S98Y6YRWRCJSND6YCFMYD	jiH04Qbxr55	e69c29ac-ece3-3f06-8ced-fcf8b2da1d3b	YLcsWJlB7uy	jiH04Qbxr55	2025-09-28 12:08:27.268809	repeat	patients		TYduVflHzfZ	kLnZwSbdouA	OqcUGUYZYez	26172272-d946-36c6-927b-9bb445bf332d	Option	TEXT	is_test_preformed	zK7EdFCwpx9	yes	zjE0FT2oNAV	yes
01K67S98Y29EN57G4533EKGHPE	01K67S98Y2WPQR6D2EHPXTAMYZ	896f8227-2920-3f4b-8c11-ebaa03a7ec70	YLcsWJlB7uy	jiH04Qbxr55	2025-09-28 12:08:27.268809	repeat	investigations		TYduVflHzfZ	kLnZwSbdouA	OqcUGUYZYez	41bde44c-de7c-35fe-8369-62cbd5f3e47e	Option	TEXT	test_result	tQjriOFVC2T	negative	gMCfcPlgAOs	negative
01K67S98Y41BEZDDVNJE0HVQXS	jiH04Qbxr55	e69c29ac-ece3-3f06-8ced-fcf8b2da1d3b	YLcsWJlB7uy	jiH04Qbxr55	2025-09-28 12:08:27.268809	repeat	patients		TYduVflHzfZ	kLnZwSbdouA	OqcUGUYZYez	26172272-d946-36c6-927b-9bb445bf332d	Option	TEXT	is_test_preformed	zK7EdFCwpx9	yes	zjE0FT2oNAV	yes
01K67S98Y293H5ZX0RETKSTM6D	01K67S98Y2HVF9K1ND7VT5XXS8	896f8227-2920-3f4b-8c11-ebaa03a7ec70	YLcsWJlB7uy	jiH04Qbxr55	2025-09-28 12:08:27.268809	repeat	investigations		TYduVflHzfZ	kLnZwSbdouA	OqcUGUYZYez	41bde44c-de7c-35fe-8369-62cbd5f3e47e	Option	TEXT	test_result	tQjriOFVC2T	negative	gMCfcPlgAOs	negative
01K67S98Y8PH24Q3CYGZRQ7S5H	jiH04Qbxr55	e69c29ac-ece3-3f06-8ced-fcf8b2da1d3b	YLcsWJlB7uy	jiH04Qbxr55	2025-09-28 12:08:27.268809	repeat	patients		TYduVflHzfZ	kLnZwSbdouA	OqcUGUYZYez	26172272-d946-36c6-927b-9bb445bf332d	Option	TEXT	is_test_preformed	zK7EdFCwpx9	yes	zjE0FT2oNAV	yes
01K67S98Y8GJM80RBRM6KB38R6	jiH04Qbxr55	e69c29ac-ece3-3f06-8ced-fcf8b2da1d3b	YLcsWJlB7uy	jiH04Qbxr55	2025-09-28 12:08:27.268809	repeat	patients		TYduVflHzfZ	kLnZwSbdouA	OqcUGUYZYez	26172272-d946-36c6-927b-9bb445bf332d	Option	TEXT	is_test_preformed	zK7EdFCwpx9	yes	zjE0FT2oNAV	yes
01K67S98Y8CDVMHT75BYHRWPVX	01K67S98Y80P9ESK551WGBXZDJ	896f8227-2920-3f4b-8c11-ebaa03a7ec70	YLcsWJlB7uy	jiH04Qbxr55	2025-09-28 12:08:27.268809	repeat	investigations		TYduVflHzfZ	kLnZwSbdouA	OqcUGUYZYez	41bde44c-de7c-35fe-8369-62cbd5f3e47e	Option	TEXT	test_result	tQjriOFVC2T	negative	gMCfcPlgAOs	negative
01K67S98Y5ECD936T9YNZNQCW5	jiH04Qbxr55	e69c29ac-ece3-3f06-8ced-fcf8b2da1d3b	YLcsWJlB7uy	jiH04Qbxr55	2025-09-28 12:08:27.268809	repeat	patients		TYduVflHzfZ	kLnZwSbdouA	OqcUGUYZYez	26172272-d946-36c6-927b-9bb445bf332d	Option	TEXT	is_test_preformed	zK7EdFCwpx9	yes	zjE0FT2oNAV	yes
01K67S98Y5CFZES29C6AHC5SWQ	jiH04Qbxr55	e69c29ac-ece3-3f06-8ced-fcf8b2da1d3b	YLcsWJlB7uy	jiH04Qbxr55	2025-09-28 12:08:27.268809	repeat	patients		TYduVflHzfZ	kLnZwSbdouA	OqcUGUYZYez	26172272-d946-36c6-927b-9bb445bf332d	Option	TEXT	is_test_preformed	zK7EdFCwpx9	yes	zjE0FT2oNAV	yes
01K67S98Y55T63WP6BAGG3B9HC	jiH04Qbxr55	e69c29ac-ece3-3f06-8ced-fcf8b2da1d3b	YLcsWJlB7uy	jiH04Qbxr55	2025-09-28 12:08:27.268809	repeat	patients		TYduVflHzfZ	kLnZwSbdouA	OqcUGUYZYez	26172272-d946-36c6-927b-9bb445bf332d	Option	TEXT	is_test_preformed	zK7EdFCwpx9	yes	zjE0FT2oNAV	yes
```
per template ce EAV-like tall values table:

```sql

DROP TABLE IF EXISTS analytics.data_values_enriched;
CREATE TABLE analytics.data_values_enriched AS

SELECT
    tc.instance_key AS event_id,
    tc.template_uid,
    tc.value_bool,
    tc.value_json,
    tc.updated_at,
    tc.value_text,
    tc.value_number,
    tc.value_ref_uid, -- resolved ref uid
    tc.value_ref_type,
    refe.ref_label As resolved_ref_label, -- resolved ref value's label
    refe.ref_option_set_uid AS ref_option_set_uid, -- resolved value ops if option, or null if Team, OrgUnit, etc
    ----------------
    -- canonical attribute metadata, enriched from canonical_element
    ----------------
    tc.canonical_element_id,
    ce.safe_name, -- attribute safe name for a flat table attribute's column name for this element, scoped by template
    ce.semantic_type AS ce_semantic_type, -- attribute semantic type e.g. OrgUnit, Team, Option, MultiSelectOption
    ce.data_type AS ce_data_type,
    ce.preferred_name AS ce_name


FROM
    analytics.tall_canonical AS tc
        LEFT JOIN public.canonical_element AS ce ON ce."id" = tc.canonical_element_id
        LEFT JOIN analytics.dim_option_set AS ops ON ce.option_set_uid = ops.option_set_uid

        LEFT JOIN analytics.ref_value_enriched AS refe ON refe.resolved_ref_uid = tc.value_ref_uid

WHERE tc.submission_uid = 'jiH04Qbxr55' AND ce.semantic_type != 'Repeat'
;
```

a submission and event tall attribute values samples, minimal columns imported from db for one submission and its repeats

```text
event_id	template_uid	value_bool	value_json	updated_at	value_text	value_number	value_ref_uid	value_ref_type	resolved_ref_label	ref_option_set_uid	canonical_element_id	safe_name	ce_semantic_type	ce_data_type	ce_name
01K67S98Y293H5ZX0RETKSTM6D	YLcsWJlB7uy			2025-12-09 03:57:00.515529	negative						41bde44c-de7c-35fe-8369-62cbd5f3e47e	test_result	Option	TEXT	test_result
01K67S98Y2272WSBZV8NQZ07Z3	YLcsWJlB7uy			2025-12-09 03:57:00.515529	negative						41bde44c-de7c-35fe-8369-62cbd5f3e47e	test_result	Option	TEXT	test_result
01K67S98Y28CV3P727EQWB9VNY	YLcsWJlB7uy			2025-12-09 03:57:00.515529	negative						41bde44c-de7c-35fe-8369-62cbd5f3e47e	test_result	Option	TEXT	test_result
01K67S98Y28NN8MBCKSTEBNWFY	YLcsWJlB7uy			2025-12-09 03:57:00.515529	positive						41bde44c-de7c-35fe-8369-62cbd5f3e47e	test_result	Option	TEXT	test_result
01K67S98Y2E346WTCRAEK861RZ	YLcsWJlB7uy			2025-12-09 03:57:00.515529	negative						41bde44c-de7c-35fe-8369-62cbd5f3e47e	test_result	Option	TEXT	test_result
01K67S98Y22WQYB6B2Q4W3YDEH	YLcsWJlB7uy			2025-12-09 03:57:00.515529	negative						41bde44c-de7c-35fe-8369-62cbd5f3e47e	test_result	Option	TEXT	test_result
01K67S98Y29EN57G4533EKGHPE	YLcsWJlB7uy			2025-12-09 03:57:00.515529	negative						41bde44c-de7c-35fe-8369-62cbd5f3e47e	test_result	Option	TEXT	test_result
01K67S98Y2F4XKH5T5ME0VMZ81	YLcsWJlB7uy			2025-12-09 03:57:00.515529	negative						41bde44c-de7c-35fe-8369-62cbd5f3e47e	test_result	Option	TEXT	test_result
01K67S98Y34RGJ3XE23EP904N5	YLcsWJlB7uy			2025-12-09 03:57:00.515529	negative						41bde44c-de7c-35fe-8369-62cbd5f3e47e	test_result	Option	TEXT	test_result
01K67S98Y3A0SYEH7AD080WDQS	YLcsWJlB7uy			2025-12-09 03:57:00.515529	negative						41bde44c-de7c-35fe-8369-62cbd5f3e47e	test_result	Option	TEXT	test_result
01K67S98Y3NEV6W1K4693R3ZFA	YLcsWJlB7uy			2025-12-09 03:57:00.515529	negative						41bde44c-de7c-35fe-8369-62cbd5f3e47e	test_result	Option	TEXT	test_result
01K67S98Y47SK7HJZQD7DRDK9B	YLcsWJlB7uy			2025-12-09 03:57:00.515529	negative						41bde44c-de7c-35fe-8369-62cbd5f3e47e	test_result	Option	TEXT	test_result
01K67S98Y464AAYT7P6NF8AKCM	YLcsWJlB7uy			2025-12-09 03:57:00.515529	negative						41bde44c-de7c-35fe-8369-62cbd5f3e47e	test_result	Option	TEXT	test_result
01K67S98Y4RE1JW35NAN8TPD6P	YLcsWJlB7uy			2025-12-09 03:57:00.515529	negative						41bde44c-de7c-35fe-8369-62cbd5f3e47e	test_result	Option	TEXT	test_result
01K67S98Y4ERKNRR5NYNAVR80A	YLcsWJlB7uy			2025-12-09 03:57:00.515529	negative						41bde44c-de7c-35fe-8369-62cbd5f3e47e	test_result	Option	TEXT	test_result
01K67S98Y4BF7EPQ3FKYV6B6EM	YLcsWJlB7uy			2025-12-09 03:57:00.515529	negative						41bde44c-de7c-35fe-8369-62cbd5f3e47e	test_result	Option	TEXT	test_result
01K67S98Y4AN4K5XSYKSPWNYKF	YLcsWJlB7uy			2025-12-09 03:57:00.515529	negative						41bde44c-de7c-35fe-8369-62cbd5f3e47e	test_result	Option	TEXT	test_result
01K67S98Y4WJ9Y83FY1D6A4A3M	YLcsWJlB7uy			2025-12-09 03:57:00.515529	negative						41bde44c-de7c-35fe-8369-62cbd5f3e47e	test_result	Option	TEXT	test_result
01K67S98Y4NCARPKNYFG823WJP	YLcsWJlB7uy			2025-12-09 03:57:00.515529	negative						41bde44c-de7c-35fe-8369-62cbd5f3e47e	test_result	Option	TEXT	test_result
01K67S98Y5CTWN2JVDM6HFEBXS	YLcsWJlB7uy			2025-12-09 03:57:00.515529	negative						41bde44c-de7c-35fe-8369-62cbd5f3e47e	test_result	Option	TEXT	test_result
01K67S98Y5NP4NGP1HGBJRQ3QP	YLcsWJlB7uy			2025-12-09 03:57:00.515529	negative						41bde44c-de7c-35fe-8369-62cbd5f3e47e	test_result	Option	TEXT	test_result
01K67S98Y5E3NG9WRTGT93YXJ2	YLcsWJlB7uy			2025-12-09 03:57:00.515529	negative						41bde44c-de7c-35fe-8369-62cbd5f3e47e	test_result	Option	TEXT	test_result
01K67S98Y5PCSTY712HZNHAPX9	YLcsWJlB7uy			2025-12-09 03:57:00.515529	negative						41bde44c-de7c-35fe-8369-62cbd5f3e47e	test_result	Option	TEXT	test_result
01K67S98Y5G07WTRN7HERGSVSZ	YLcsWJlB7uy			2025-12-09 03:57:00.515529	negative						41bde44c-de7c-35fe-8369-62cbd5f3e47e	test_result	Option	TEXT	test_result
01K67S98Y57SJZDPGMHCR8RAJY	YLcsWJlB7uy			2025-12-09 03:57:00.515529	negative						41bde44c-de7c-35fe-8369-62cbd5f3e47e	test_result	Option	TEXT	test_result
01K67S98Y5BKZWBQWNNFCQ4M7W	YLcsWJlB7uy			2025-12-09 03:57:00.515529	negative						41bde44c-de7c-35fe-8369-62cbd5f3e47e	test_result	Option	TEXT	test_result
01K67S98Y5X4MWQZZ6FZZ4FKV1	YLcsWJlB7uy			2025-12-09 03:57:00.515529	negative						41bde44c-de7c-35fe-8369-62cbd5f3e47e	test_result	Option	TEXT	test_result
01K67S98Y5562Q8EV898E9HVRY	YLcsWJlB7uy			2025-12-09 03:57:00.515529	negative						41bde44c-de7c-35fe-8369-62cbd5f3e47e	test_result	Option	TEXT	test_result
01K67S98Y5A2JDZB4JSJJPF13Y	YLcsWJlB7uy			2025-12-09 03:57:00.515529	negative						41bde44c-de7c-35fe-8369-62cbd5f3e47e	test_result	Option	TEXT	test_result
01K67S98Y5TE1CS4VE557GQFJH	YLcsWJlB7uy			2025-12-09 03:57:00.515529	negative						41bde44c-de7c-35fe-8369-62cbd5f3e47e	test_result	Option	TEXT	test_result
01K67S98Y50JY1K098ZSPRZPK7	YLcsWJlB7uy			2025-12-09 03:57:00.515529	negative						41bde44c-de7c-35fe-8369-62cbd5f3e47e	test_result	Option	TEXT	test_result
01K67S98Y62SK2EBQH8NDBEZ8D	YLcsWJlB7uy			2025-12-09 03:57:00.515529	negative						41bde44c-de7c-35fe-8369-62cbd5f3e47e	test_result	Option	TEXT	test_result
01K67S98Y66B4HJ4HVGXYM2F2P	YLcsWJlB7uy			2025-12-09 03:57:00.515529	negative						41bde44c-de7c-35fe-8369-62cbd5f3e47e	test_result	Option	TEXT	test_result
01K67S98Y6W1537M3WN1H2PJD5	YLcsWJlB7uy			2025-12-09 03:57:00.515529	negative						41bde44c-de7c-35fe-8369-62cbd5f3e47e	test_result	Option	TEXT	test_result
01K67S98Y6HEGA2FZ4WSG3VV12	YLcsWJlB7uy			2025-12-09 03:57:00.515529	negative						41bde44c-de7c-35fe-8369-62cbd5f3e47e	test_result	Option	TEXT	test_result
01K67S98Y6EQDQT06WWWM6Y7EB	YLcsWJlB7uy			2025-12-09 03:57:00.515529	negative						41bde44c-de7c-35fe-8369-62cbd5f3e47e	test_result	Option	TEXT	test_result
01K67S98Y62TNJVDXZMNPD8FN7	YLcsWJlB7uy			2025-12-09 03:57:00.515529	negative						41bde44c-de7c-35fe-8369-62cbd5f3e47e	test_result	Option	TEXT	test_result
01K67S98Y62DQ8XASSKRVC7BWD	YLcsWJlB7uy			2025-12-09 03:57:00.515529	negative						41bde44c-de7c-35fe-8369-62cbd5f3e47e	test_result	Option	TEXT	test_result
01K67S98Y69RFX3PSECTW7TW9A	YLcsWJlB7uy			2025-12-09 03:57:00.515529	negative						41bde44c-de7c-35fe-8369-62cbd5f3e47e	test_result	Option	TEXT	test_result
01K67S98Y6MV5F1H8TT234MT2N	YLcsWJlB7uy			2025-12-09 03:57:00.515529	negative						41bde44c-de7c-35fe-8369-62cbd5f3e47e	test_result	Option	TEXT	test_result
01K67S98Y7R8AX47D92KKSST6P	YLcsWJlB7uy			2025-12-09 03:57:00.515529	negative						41bde44c-de7c-35fe-8369-62cbd5f3e47e	test_result	Option	TEXT	test_result
01K67S98Y75Y3T1DH16YT9AWFJ	YLcsWJlB7uy			2025-12-09 03:57:00.515529	negative						41bde44c-de7c-35fe-8369-62cbd5f3e47e	test_result	Option	TEXT	test_result
01K67S98Y8M8ANNSG2EQ52FMVQ	YLcsWJlB7uy			2025-12-09 03:57:00.515529	negative						41bde44c-de7c-35fe-8369-62cbd5f3e47e	test_result	Option	TEXT	test_result
01K67S98Y8MGMDZNQEJTVRHWK4	YLcsWJlB7uy			2025-12-09 03:57:00.515529	negative						41bde44c-de7c-35fe-8369-62cbd5f3e47e	test_result	Option	TEXT	test_result
01K67S98Y85S8DFB015WJM9ABP	YLcsWJlB7uy			2025-12-09 03:57:00.515529	negative						41bde44c-de7c-35fe-8369-62cbd5f3e47e	test_result	Option	TEXT	test_result
01K67S98Y8CDVMHT75BYHRWPVX	YLcsWJlB7uy			2025-12-09 03:57:00.515529	negative						41bde44c-de7c-35fe-8369-62cbd5f3e47e	test_result	Option	TEXT	test_result
01K67S98Y8MRW9ZXJQVSZ55XVH	YLcsWJlB7uy			2025-12-09 03:57:00.515529	negative						41bde44c-de7c-35fe-8369-62cbd5f3e47e	test_result	Option	TEXT	test_result
01K67S98Y8N2AW5V0KYKS7D3Y9	YLcsWJlB7uy			2025-12-09 03:57:00.515529	negative						41bde44c-de7c-35fe-8369-62cbd5f3e47e	test_result	Option	TEXT	test_result
01K67S98Y2HVF9K1ND7VT5XXS8	YLcsWJlB7uy			2025-12-09 03:57:00.515529	yes						26172272-d946-36c6-927b-9bb445bf332d	is_test_preformed	Option	TEXT	is_test_preformed
01K67S98Y2ZNWBVRS6VY5F1QH8	YLcsWJlB7uy			2025-12-09 03:57:00.515529	yes						26172272-d946-36c6-927b-9bb445bf332d	is_test_preformed	Option	TEXT	is_test_preformed
01K67S98Y2H0AJ4HC0JEMAPB8E	YLcsWJlB7uy			2025-12-09 03:57:00.515529	yes						26172272-d946-36c6-927b-9bb445bf332d	is_test_preformed	Option	TEXT	is_test_preformed
01K67S98Y2AQPN8AYQW5FQMWZX	YLcsWJlB7uy			2025-12-09 03:57:00.515529	yes						26172272-d946-36c6-927b-9bb445bf332d	is_test_preformed	Option	TEXT	is_test_preformed
01K67S98Y2JXQFTG9K6NFN79C4	YLcsWJlB7uy			2025-12-09 03:57:00.515529	yes						26172272-d946-36c6-927b-9bb445bf332d	is_test_preformed	Option	TEXT	is_test_preformed
01K67S98Y20W4317V4ZTK1M9BH	YLcsWJlB7uy			2025-12-09 03:57:00.515529	yes						26172272-d946-36c6-927b-9bb445bf332d	is_test_preformed	Option	TEXT	is_test_preformed
01K67S98Y2WPQR6D2EHPXTAMYZ	YLcsWJlB7uy			2025-12-09 03:57:00.515529	yes						26172272-d946-36c6-927b-9bb445bf332d	is_test_preformed	Option	TEXT	is_test_preformed
01K67S98Y2YD34NFRA3ZA6NSFW	YLcsWJlB7uy			2025-12-09 03:57:00.515529	yes						26172272-d946-36c6-927b-9bb445bf332d	is_test_preformed	Option	TEXT	is_test_preformed
01K67S98Y2BH1DX1B4ZYYQ2HHJ	YLcsWJlB7uy			2025-12-09 03:57:00.515529	yes						26172272-d946-36c6-927b-9bb445bf332d	is_test_preformed	Option	TEXT	is_test_preformed
01K67S98Y3GV8PQA00FRW4Q8Y7	YLcsWJlB7uy			2025-12-09 03:57:00.515529	yes						26172272-d946-36c6-927b-9bb445bf332d	is_test_preformed	Option	TEXT	is_test_preformed
01K67S98Y3RYSVEV1NV31BFD28	YLcsWJlB7uy			2025-12-09 03:57:00.515529	yes						26172272-d946-36c6-927b-9bb445bf332d	is_test_preformed	Option	TEXT	is_test_preformed
01K67S98Y4F13CBD8CNSBRGFDY	YLcsWJlB7uy			2025-12-09 03:57:00.515529	yes						26172272-d946-36c6-927b-9bb445bf332d	is_test_preformed	Option	TEXT	is_test_preformed
01K67S98Y4TTJFCJWS0HS2DV0E	YLcsWJlB7uy			2025-12-09 03:57:00.515529	yes						26172272-d946-36c6-927b-9bb445bf332d	is_test_preformed	Option	TEXT	is_test_preformed
01K67S98Y44RR4G4N8RP6JP57V	YLcsWJlB7uy			2025-12-09 03:57:00.515529	yes						26172272-d946-36c6-927b-9bb445bf332d	is_test_preformed	Option	TEXT	is_test_preformed
01K67S98Y4S6M5K2WG7B26BTYN	YLcsWJlB7uy			2025-12-09 03:57:00.515529	yes						26172272-d946-36c6-927b-9bb445bf332d	is_test_preformed	Option	TEXT	is_test_preformed
01K67S98Y41BEZDDVNJE0HVQXS	YLcsWJlB7uy			2025-12-09 03:57:00.515529	yes						26172272-d946-36c6-927b-9bb445bf332d	is_test_preformed	Option	TEXT	is_test_preformed
01K67S98Y44MF4SQ7DPFR9JJ6Z	YLcsWJlB7uy			2025-12-09 03:57:00.515529	yes						26172272-d946-36c6-927b-9bb445bf332d	is_test_preformed	Option	TEXT	is_test_preformed
01K67S98Y4V1WX67CDGTY1NK7V	YLcsWJlB7uy			2025-12-09 03:57:00.515529	yes						26172272-d946-36c6-927b-9bb445bf332d	is_test_preformed	Option	TEXT	is_test_preformed
01K67S98Y44CYWNAC36SN70S7Z	YLcsWJlB7uy			2025-12-09 03:57:00.515529	yes						26172272-d946-36c6-927b-9bb445bf332d	is_test_preformed	Option	TEXT	is_test_preformed
01K67S98Y4QZX7CQ6AQCJKCY9M	YLcsWJlB7uy			2025-12-09 03:57:00.515529	yes						26172272-d946-36c6-927b-9bb445bf332d	is_test_preformed	Option	TEXT	is_test_preformed
01K67S98Y55BPMERZ9G6RDFXGQ	YLcsWJlB7uy			2025-12-09 03:57:00.515529	yes						26172272-d946-36c6-927b-9bb445bf332d	is_test_preformed	Option	TEXT	is_test_preformed
01K67S98Y555MYMY1X7N483917	YLcsWJlB7uy			2025-12-09 03:57:00.515529	yes						26172272-d946-36c6-927b-9bb445bf332d	is_test_preformed	Option	TEXT	is_test_preformed
01K67S98Y55T63WP6BAGG3B9HC	YLcsWJlB7uy			2025-12-09 03:57:00.515529	yes						26172272-d946-36c6-927b-9bb445bf332d	is_test_preformed	Option	TEXT	is_test_preformed
01K67S98Y5F3N3QH6FSJ1T8TST	YLcsWJlB7uy			2025-12-09 03:57:00.515529	yes						26172272-d946-36c6-927b-9bb445bf332d	is_test_preformed	Option	TEXT	is_test_preformed
01K67S98Y5SAYQ1986M9K3JANX	YLcsWJlB7uy			2025-12-09 03:57:00.515529	yes						26172272-d946-36c6-927b-9bb445bf332d	is_test_preformed	Option	TEXT	is_test_preformed
01K67S98Y5P4XQAV660YFHQDJR	YLcsWJlB7uy			2025-12-09 03:57:00.515529	yes						26172272-d946-36c6-927b-9bb445bf332d	is_test_preformed	Option	TEXT	is_test_preformed
01K67S98Y54P4P48ZWCCKAHZ88	YLcsWJlB7uy			2025-12-09 03:57:00.515529	yes						26172272-d946-36c6-927b-9bb445bf332d	is_test_preformed	Option	TEXT	is_test_preformed
01K67S98Y5ECD936T9YNZNQCW5	YLcsWJlB7uy			2025-12-09 03:57:00.515529	yes						26172272-d946-36c6-927b-9bb445bf332d	is_test_preformed	Option	TEXT	is_test_preformed
01K67S98Y5CJEY5CB9N4MW4S43	YLcsWJlB7uy			2025-12-09 03:57:00.515529	yes						26172272-d946-36c6-927b-9bb445bf332d	is_test_preformed	Option	TEXT	is_test_preformed
01K67S98Y5CFZES29C6AHC5SWQ	YLcsWJlB7uy			2025-12-09 03:57:00.515529	yes						26172272-d946-36c6-927b-9bb445bf332d	is_test_preformed	Option	TEXT	is_test_preformed
01K67S98Y56C8G415SV6XEGX4A	YLcsWJlB7uy			2025-12-09 03:57:00.515529	yes						26172272-d946-36c6-927b-9bb445bf332d	is_test_preformed	Option	TEXT	is_test_preformed
01K67S98Y6VGMT1AEQBWDPA7JQ	YLcsWJlB7uy			2025-12-09 03:57:00.515529	yes						26172272-d946-36c6-927b-9bb445bf332d	is_test_preformed	Option	TEXT	is_test_preformed
01K67S98Y61FX4X35CEV4EZFAV	YLcsWJlB7uy			2025-12-09 03:57:00.515529	yes						26172272-d946-36c6-927b-9bb445bf332d	is_test_preformed	Option	TEXT	is_test_preformed
01K67S98Y6DQ7W2CZ75KAG45CG	YLcsWJlB7uy			2025-12-09 03:57:00.515529	yes						26172272-d946-36c6-927b-9bb445bf332d	is_test_preformed	Option	TEXT	is_test_preformed
01K67S98Y60D651Y4QSEMEVW61	YLcsWJlB7uy			2025-12-09 03:57:00.515529	yes						26172272-d946-36c6-927b-9bb445bf332d	is_test_preformed	Option	TEXT	is_test_preformed
01K67S98Y6BPSEEAJ8KN25Q4P7	YLcsWJlB7uy			2025-12-09 03:57:00.515529	yes						26172272-d946-36c6-927b-9bb445bf332d	is_test_preformed	Option	TEXT	is_test_preformed
01K67S98Y6SPMEEY8TDJPV3YZZ	YLcsWJlB7uy			2025-12-09 03:57:00.515529	yes						26172272-d946-36c6-927b-9bb445bf332d	is_test_preformed	Option	TEXT	is_test_preformed
01K67S98Y60GQMA8JAGMCSPREF	YLcsWJlB7uy			2025-12-09 03:57:00.515529	yes						26172272-d946-36c6-927b-9bb445bf332d	is_test_preformed	Option	TEXT	is_test_preformed
01K67S98Y6YRWRCJSND6YCFMYD	YLcsWJlB7uy			2025-12-09 03:57:00.515529	yes						26172272-d946-36c6-927b-9bb445bf332d	is_test_preformed	Option	TEXT	is_test_preformed
01K67S98Y67D1P4FGWX3XVF6PF	YLcsWJlB7uy			2025-12-09 03:57:00.515529	yes						26172272-d946-36c6-927b-9bb445bf332d	is_test_preformed	Option	TEXT	is_test_preformed
01K67S98Y7N4P5QX4Z7DZ8SQY8	YLcsWJlB7uy			2025-12-09 03:57:00.515529	yes						26172272-d946-36c6-927b-9bb445bf332d	is_test_preformed	Option	TEXT	is_test_preformed
01K67S98Y7NCTM0SZ8PB9RY4XM	YLcsWJlB7uy			2025-12-09 03:57:00.515529	yes						26172272-d946-36c6-927b-9bb445bf332d	is_test_preformed	Option	TEXT	is_test_preformed
01K67S98Y8PH24Q3CYGZRQ7S5H	YLcsWJlB7uy			2025-12-09 03:57:00.515529	yes						26172272-d946-36c6-927b-9bb445bf332d	is_test_preformed	Option	TEXT	is_test_preformed
01K67S98Y8GJM80RBRM6KB38R6	YLcsWJlB7uy			2025-12-09 03:57:00.515529	yes						26172272-d946-36c6-927b-9bb445bf332d	is_test_preformed	Option	TEXT	is_test_preformed
01K67S98Y8W0ER166HDB2DCH7X	YLcsWJlB7uy			2025-12-09 03:57:00.515529	yes						26172272-d946-36c6-927b-9bb445bf332d	is_test_preformed	Option	TEXT	is_test_preformed
01K67S98Y80P9ESK551WGBXZDJ	YLcsWJlB7uy			2025-12-09 03:57:00.515529	yes						26172272-d946-36c6-927b-9bb445bf332d	is_test_preformed	Option	TEXT	is_test_preformed
01K67S98Y8WCT7HK3TVPNXTCBN	YLcsWJlB7uy			2025-12-09 03:57:00.515529	yes						26172272-d946-36c6-927b-9bb445bf332d	is_test_preformed	Option	TEXT	is_test_preformed
01K67S98Y8T2W70SGQQM1MMSA5	YLcsWJlB7uy			2025-12-09 03:57:00.515529	yes						26172272-d946-36c6-927b-9bb445bf332d	is_test_preformed	Option	TEXT	is_test_preformed
01K67S98Y293H5ZX0RETKSTM6D	YLcsWJlB7uy			2025-12-09 03:57:00.515529	mrdt						69d7d0fb-9480-3457-8fe6-72fa70f6fe4e	lab_test_type	Option	TEXT	lab_test_type
01K67S98Y2272WSBZV8NQZ07Z3	YLcsWJlB7uy			2025-12-09 03:57:00.515529	mrdt						69d7d0fb-9480-3457-8fe6-72fa70f6fe4e	lab_test_type	Option	TEXT	lab_test_type
01K67S98Y28CV3P727EQWB9VNY	YLcsWJlB7uy			2025-12-09 03:57:00.515529	mrdt						69d7d0fb-9480-3457-8fe6-72fa70f6fe4e	lab_test_type	Option	TEXT	lab_test_type
01K67S98Y28NN8MBCKSTEBNWFY	YLcsWJlB7uy			2025-12-09 03:57:00.515529	mrdt						69d7d0fb-9480-3457-8fe6-72fa70f6fe4e	lab_test_type	Option	TEXT	lab_test_type
01K67S98Y2E346WTCRAEK861RZ	YLcsWJlB7uy			2025-12-09 03:57:00.515529	mrdt						69d7d0fb-9480-3457-8fe6-72fa70f6fe4e	lab_test_type	Option	TEXT	lab_test_type
01K67S98Y22WQYB6B2Q4W3YDEH	YLcsWJlB7uy			2025-12-09 03:57:00.515529	mrdt						69d7d0fb-9480-3457-8fe6-72fa70f6fe4e	lab_test_type	Option	TEXT	lab_test_type
01K67S98Y29EN57G4533EKGHPE	YLcsWJlB7uy			2025-12-09 03:57:00.515529	mrdt						69d7d0fb-9480-3457-8fe6-72fa70f6fe4e	lab_test_type	Option	TEXT	lab_test_type
01K67S98Y2F4XKH5T5ME0VMZ81	YLcsWJlB7uy			2025-12-09 03:57:00.515529	mrdt						69d7d0fb-9480-3457-8fe6-72fa70f6fe4e	lab_test_type	Option	TEXT	lab_test_type
01K67S98Y34RGJ3XE23EP904N5	YLcsWJlB7uy			2025-12-09 03:57:00.515529	mrdt						69d7d0fb-9480-3457-8fe6-72fa70f6fe4e	lab_test_type	Option	TEXT	lab_test_type
01K67S98Y3A0SYEH7AD080WDQS	YLcsWJlB7uy			2025-12-09 03:57:00.515529	mrdt						69d7d0fb-9480-3457-8fe6-72fa70f6fe4e	lab_test_type	Option	TEXT	lab_test_type
01K67S98Y3NEV6W1K4693R3ZFA	YLcsWJlB7uy			2025-12-09 03:57:00.515529	mrdt						69d7d0fb-9480-3457-8fe6-72fa70f6fe4e	lab_test_type	Option	TEXT	lab_test_type
01K67S98Y47SK7HJZQD7DRDK9B	YLcsWJlB7uy			2025-12-09 03:57:00.515529	mrdt						69d7d0fb-9480-3457-8fe6-72fa70f6fe4e	lab_test_type	Option	TEXT	lab_test_type
01K67S98Y464AAYT7P6NF8AKCM	YLcsWJlB7uy			2025-12-09 03:57:00.515529	mrdt						69d7d0fb-9480-3457-8fe6-72fa70f6fe4e	lab_test_type	Option	TEXT	lab_test_type
01K67S98Y4RE1JW35NAN8TPD6P	YLcsWJlB7uy			2025-12-09 03:57:00.515529	mrdt						69d7d0fb-9480-3457-8fe6-72fa70f6fe4e	lab_test_type	Option	TEXT	lab_test_type
01K67S98Y4ERKNRR5NYNAVR80A	YLcsWJlB7uy			2025-12-09 03:57:00.515529	mrdt						69d7d0fb-9480-3457-8fe6-72fa70f6fe4e	lab_test_type	Option	TEXT	lab_test_type
01K67S98Y4BF7EPQ3FKYV6B6EM	YLcsWJlB7uy			2025-12-09 03:57:00.515529	mrdt						69d7d0fb-9480-3457-8fe6-72fa70f6fe4e	lab_test_type	Option	TEXT	lab_test_type
01K67S98Y4AN4K5XSYKSPWNYKF	YLcsWJlB7uy			2025-12-09 03:57:00.515529	mrdt						69d7d0fb-9480-3457-8fe6-72fa70f6fe4e	lab_test_type	Option	TEXT	lab_test_type
01K67S98Y4WJ9Y83FY1D6A4A3M	YLcsWJlB7uy			2025-12-09 03:57:00.515529	mrdt						69d7d0fb-9480-3457-8fe6-72fa70f6fe4e	lab_test_type	Option	TEXT	lab_test_type
01K67S98Y4NCARPKNYFG823WJP	YLcsWJlB7uy			2025-12-09 03:57:00.515529	mrdt						69d7d0fb-9480-3457-8fe6-72fa70f6fe4e	lab_test_type	Option	TEXT	lab_test_type
01K67S98Y5CTWN2JVDM6HFEBXS	YLcsWJlB7uy			2025-12-09 03:57:00.515529	mrdt						69d7d0fb-9480-3457-8fe6-72fa70f6fe4e	lab_test_type	Option	TEXT	lab_test_type
01K67S98Y5NP4NGP1HGBJRQ3QP	YLcsWJlB7uy			2025-12-09 03:57:00.515529	mrdt						69d7d0fb-9480-3457-8fe6-72fa70f6fe4e	lab_test_type	Option	TEXT	lab_test_type
01K67S98Y5E3NG9WRTGT93YXJ2	YLcsWJlB7uy			2025-12-09 03:57:00.515529	mrdt						69d7d0fb-9480-3457-8fe6-72fa70f6fe4e	lab_test_type	Option	TEXT	lab_test_type
01K67S98Y5PCSTY712HZNHAPX9	YLcsWJlB7uy			2025-12-09 03:57:00.515529	mrdt						69d7d0fb-9480-3457-8fe6-72fa70f6fe4e	lab_test_type	Option	TEXT	lab_test_type
01K67S98Y5G07WTRN7HERGSVSZ	YLcsWJlB7uy			2025-12-09 03:57:00.515529	mrdt						69d7d0fb-9480-3457-8fe6-72fa70f6fe4e	lab_test_type	Option	TEXT	lab_test_type
01K67S98Y57SJZDPGMHCR8RAJY	YLcsWJlB7uy			2025-12-09 03:57:00.515529	mrdt						69d7d0fb-9480-3457-8fe6-72fa70f6fe4e	lab_test_type	Option	TEXT	lab_test_type
01K67S98Y5BKZWBQWNNFCQ4M7W	YLcsWJlB7uy			2025-12-09 03:57:00.515529	mrdt						69d7d0fb-9480-3457-8fe6-72fa70f6fe4e	lab_test_type	Option	TEXT	lab_test_type
01K67S98Y5X4MWQZZ6FZZ4FKV1	YLcsWJlB7uy			2025-12-09 03:57:00.515529	mrdt						69d7d0fb-9480-3457-8fe6-72fa70f6fe4e	lab_test_type	Option	TEXT	lab_test_type
01K67S98Y5562Q8EV898E9HVRY	YLcsWJlB7uy			2025-12-09 03:57:00.515529	mrdt						69d7d0fb-9480-3457-8fe6-72fa70f6fe4e	lab_test_type	Option	TEXT	lab_test_type
01K67S98Y5A2JDZB4JSJJPF13Y	YLcsWJlB7uy			2025-12-09 03:57:00.515529	mrdt						69d7d0fb-9480-3457-8fe6-72fa70f6fe4e	lab_test_type	Option	TEXT	lab_test_type
01K67S98Y5TE1CS4VE557GQFJH	YLcsWJlB7uy			2025-12-09 03:57:00.515529	mrdt						69d7d0fb-9480-3457-8fe6-72fa70f6fe4e	lab_test_type	Option	TEXT	lab_test_type
01K67S98Y50JY1K098ZSPRZPK7	YLcsWJlB7uy			2025-12-09 03:57:00.515529	mrdt						69d7d0fb-9480-3457-8fe6-72fa70f6fe4e	lab_test_type	Option	TEXT	lab_test_type
01K67S98Y62SK2EBQH8NDBEZ8D	YLcsWJlB7uy			2025-12-09 03:57:00.515529	mrdt						69d7d0fb-9480-3457-8fe6-72fa70f6fe4e	lab_test_type	Option	TEXT	lab_test_type
01K67S98Y66B4HJ4HVGXYM2F2P	YLcsWJlB7uy			2025-12-09 03:57:00.515529	mrdt						69d7d0fb-9480-3457-8fe6-72fa70f6fe4e	lab_test_type	Option	TEXT	lab_test_type
01K67S98Y6W1537M3WN1H2PJD5	YLcsWJlB7uy			2025-12-09 03:57:00.515529	mrdt						69d7d0fb-9480-3457-8fe6-72fa70f6fe4e	lab_test_type	Option	TEXT	lab_test_type
01K67S98Y6HEGA2FZ4WSG3VV12	YLcsWJlB7uy			2025-12-09 03:57:00.515529	mrdt						69d7d0fb-9480-3457-8fe6-72fa70f6fe4e	lab_test_type	Option	TEXT	lab_test_type
01K67S98Y6EQDQT06WWWM6Y7EB	YLcsWJlB7uy			2025-12-09 03:57:00.515529	mrdt						69d7d0fb-9480-3457-8fe6-72fa70f6fe4e	lab_test_type	Option	TEXT	lab_test_type
01K67S98Y62TNJVDXZMNPD8FN7	YLcsWJlB7uy			2025-12-09 03:57:00.515529	mrdt						69d7d0fb-9480-3457-8fe6-72fa70f6fe4e	lab_test_type	Option	TEXT	lab_test_type
01K67S98Y62DQ8XASSKRVC7BWD	YLcsWJlB7uy			2025-12-09 03:57:00.515529	mrdt						69d7d0fb-9480-3457-8fe6-72fa70f6fe4e	lab_test_type	Option	TEXT	lab_test_type
01K67S98Y69RFX3PSECTW7TW9A	YLcsWJlB7uy			2025-12-09 03:57:00.515529	mrdt						69d7d0fb-9480-3457-8fe6-72fa70f6fe4e	lab_test_type	Option	TEXT	lab_test_type
01K67S98Y6MV5F1H8TT234MT2N	YLcsWJlB7uy			2025-12-09 03:57:00.515529	mrdt						69d7d0fb-9480-3457-8fe6-72fa70f6fe4e	lab_test_type	Option	TEXT	lab_test_type
01K67S98Y7R8AX47D92KKSST6P	YLcsWJlB7uy			2025-12-09 03:57:00.515529	mrdt						69d7d0fb-9480-3457-8fe6-72fa70f6fe4e	lab_test_type	Option	TEXT	lab_test_type
01K67S98Y75Y3T1DH16YT9AWFJ	YLcsWJlB7uy			2025-12-09 03:57:00.515529	mrdt						69d7d0fb-9480-3457-8fe6-72fa70f6fe4e	lab_test_type	Option	TEXT	lab_test_type
01K67S98Y8M8ANNSG2EQ52FMVQ	YLcsWJlB7uy			2025-12-09 03:57:00.515529	mrdt						69d7d0fb-9480-3457-8fe6-72fa70f6fe4e	lab_test_type	Option	TEXT	lab_test_type
01K67S98Y8MGMDZNQEJTVRHWK4	YLcsWJlB7uy			2025-12-09 03:57:00.515529	mrdt						69d7d0fb-9480-3457-8fe6-72fa70f6fe4e	lab_test_type	Option	TEXT	lab_test_type
01K67S98Y85S8DFB015WJM9ABP	YLcsWJlB7uy			2025-12-09 03:57:00.515529	mrdt						69d7d0fb-9480-3457-8fe6-72fa70f6fe4e	lab_test_type	Option	TEXT	lab_test_type
01K67S98Y8CDVMHT75BYHRWPVX	YLcsWJlB7uy			2025-12-09 03:57:00.515529	mrdt						69d7d0fb-9480-3457-8fe6-72fa70f6fe4e	lab_test_type	Option	TEXT	lab_test_type
01K67S98Y8MRW9ZXJQVSZ55XVH	YLcsWJlB7uy			2025-12-09 03:57:00.515529	mrdt						69d7d0fb-9480-3457-8fe6-72fa70f6fe4e	lab_test_type	Option	TEXT	lab_test_type
01K67S98Y8N2AW5V0KYKS7D3Y9	YLcsWJlB7uy			2025-12-09 03:57:00.515529	mrdt						69d7d0fb-9480-3457-8fe6-72fa70f6fe4e	lab_test_type	Option	TEXT	lab_test_type
jiH04Qbxr55	YLcsWJlB7uy			2025-12-09 03:57:00.515529	malaria_unit						2657f0c8-74c9-33aa-8211-7999d0c932fc	emergency_team_type	Option	TEXT	emergency_team_type
```


---


lock to those table shapes as the canonical contract for the rest of the chat. Short, exact summary of **key columns,
relations and notes** (one line per important thing) below:

### Global notes

* All upstream parsing/typing/validation is already done; these `analytics.*` tables are silver ready for analysis /
  modeling.

### `analytics.events_enriched`

* Purpose: all events (root submission + repeat) with template + assignment + anchor metadata, etc
* Indexed for filtering by `event_type`, `submission_uid`, `event_id`, `template_uid`,
  `team/org_unit`, etc.

### `analytics.data_values_enriched`

* Purpose: flattened, fully-enriched tall canonical rows (typed values + CE metadata + assignment + template + anchor).
  Main analytic surface for CE-driven queries.
* Indexes: same pattern — event_id, event_type, submission_uid, template_name, team/org fields, etc.

## Common Abbreviations used in system discussions, including some context info about other tables available in the system:

* `dt`: Data templates i.e forms, (**dbtable:** `analytics.dim_data_template`).
* `ds`: Data submission.
* `ou`: OrgUnit. (**dbtable:** `analytics.dim_org_unit`).
* `assc`: assignment, links org unit, team, activity and templates per assignment, plus other attribute. (**dbtable:**
  `analytics.dim_assignment_closure`).
* `ops`: Option Set, (**dbtable:** `analytics.dim_option_set`), (**Schema:**  `option_set_uid`, `option_set_name`,
  `option_set_name_en`, `option_set_name_ar`).
* `opv`: Option value, (**dbtable:** `analytics.dim_option`), (**schema:**  `option_uid`,
  `option_set_uid`, `option_code`, `option_name`, `option_name_en`, `option_name_ar`, `option_sort_order`).
* `ce`: Canonical element, (**dbtable:** `public.canonical_element`).
* `te`: Tall canonical, (**dbtable:** `analytics.tall_canonical_enriched`).
* `ev`: contains both submission level and nested repeats rows, (**dbtable:** `analytics.events_enriched`).
* `sub`: submissions_enriched, submission's level only rows, (**dbtable:** `analytics.submissions_enriched`), schema is
  pretty much like even.
* `dv`: tall data values, per "element ce" data value, (**dbtable:** `analytics.data_values_enriched`).

---


Goals: Implement **single per-template table** `facts_wide_template_X_new, creating per-template wide tables spring boot app, rely on each CE’s safe_name as column names.

* Events model: one unified per-template pivoted table treat both submissions and repeats as events in a single table:
  one row per event. Add event metas to distinguish repeat events. using ce.safe_name only for columns

1. **Fetch metadata per template**

    * Change `getElementsForTemplate(templateUid)` (query `public.canonical_element WHERE template_uid = ?`).

2. **SqlGenerator**

    * Use template-specific CE list.
    * Column name = `sanitize(safe_name)` (snake_case, lower, remove weird chars). **No prefix**.
    * Add event-level dims:

        * `event_type` (enum: `'submission'|'repeat'`)
        * `submission_uid` (non-nullable)
        * `submission_creation_time, start_time, etc`
        * `event_id` (non-nullable)
        * `event_ce_id` (nullable if root submission)
        * `event_name` (non-nullable)
        * `assignment_uid` (non-nullable)
        * `template_uid` (non-nullable)
        * etc..
    * SELECT pulls from `analytics.events_enriched` or `analytics.data_values_enriched` joining on `event_id`.

4. **Collision guard (optional but cheap)**

    * Because `safe_name` uniqueness is guaranteed in template, we can *not* append ids. Still: sanitize and if
      duplicate appears (unexpected), append `__ce123` — generator can detect duplicates and only then append short
      suffix.
