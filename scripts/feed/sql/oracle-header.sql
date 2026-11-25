-- The feed's oracle, for a batch of journal heads: the ids of the form and of the pinned form version each head names,
-- looked up by uid with no form definition (data_submission.template_uid; the head's metadata template_version_uid).
-- Parameters, in order: the batch's submission ids (text[]) and head versions (bigint[]), aligned. Read-only.
SELECT b.submission_id, t.id AS form_id, v.id AS form_version_id
FROM unnest(?::text[], ?::bigint[]) AS b(submission_id, version)
JOIN journal.submission_revision r ON r.submission_id = b.submission_id AND r.version = b.version
JOIN data_submission s ON s.id = b.submission_id
LEFT JOIN data_template t ON t.uid = s.template_uid
LEFT JOIN data_template_version v ON v.uid = r.metadata ->> 'template_version_uid'
