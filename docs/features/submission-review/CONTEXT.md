# Submission review

The words for searching, checking and changing the submissions of any form after phones send them, and for who may
do it. Status: LIVING. Adopted for the first build (engineering, 2026-11-25), with the owner's answers of
2026-11-25.

- **Arabic** in brackets is proposed; to confirm with operations before any Arabic document or screen uses it.
- **English** labels, states and codes appear verbatim in operations' Arabic documents (owner, 2026-11-19).
- **API** gives the v2 spelling where a term has one.
- **Engineering words** (their own section) appear in the API, the code and these documents, never in operations'
  documents or screens. Operations see history and "deleted".
- Words of the lists feature (entry, listed twice, served twice, link) keep their meaning there.

## Language

### The capability

**Submission review** (مراجعة الاستمارات المرسلة):
The work of searching, checking and changing the submissions of any form after phones send them, with every change
kept in history. Progress views, dashboards and exports are not part of it.
_Avoid_: corrections, data cleaning (what downstream analytics does to its own copy), review alone (in engineering
documents it means an engineering review)

### What is reviewed

**Form**:
A questionnaire that phones fill, published in versions. API: `form`; `forms`.
_Avoid_: template (the code's word for a form)

**Activity**:
An organized piece of field work, such as a yearly campaign, with its forms, teams and assignments. API: `activity`.
_Avoid_: campaign as a model word (it is the organization's label for its yearly activities), project

**Team** and **org unit**:
A team is a group of users who work together; its team form grants drive its phones. An org unit is a place in
DataRun's hierarchy of areas, such as a district or a village. API: `team`, `orgUnit`.

**Submission** (استمارة مرسلة):
One filled form sent from a phone: its values, its rows and its placement, known by a permanent id.
API: `submissions`.
_Avoid_: record, instance, event, form (the definition, not a filled one)

**Form version**:
One published version of a form. A submission is pinned to the version it was filled with, which types its values.
API: `formVersion`; `forms/{id}/versions/{number}`.
_Avoid_: version alone (a submission's version), template

**Field** (حقل):
One question of a form version, known by its field id: the id of the data element it carries, the same in every
version of the form. API: `field`.
_Avoid_: data element (the authoring object whose id the field carries), element, path

**Row** (صف):
One repetition of a repeating section in a submission, such as one household of a village visit, known by its row
id. A nested row has a parent row. A row without a unique stored id is shown with a derived one, which the next
edit, split or accept stores; a server-side change never changes a row id. API: `row`.
_Avoid_: repeat alone (it names the section as often as the row), item, event, entry, household

**Top-level value**:
A value outside every repeating section of a submission. API: `topLevel` (in a split).

**Placement** (القرية والفريق):
Where a submission belongs: its assignment, and so its team, org unit and activity. Access, counts and the feed
follow it. API: `assignment`; `destination` in a move or split.
_Avoid_: location, owner, village (one activity's org-unit level, which names no team)

**Assignment**:
A team's work in one org unit for one activity. An org unit can have assignments of two teams.

**Area** (منطقة):
An org unit with everything under it: a search filter now, a scope later. API: `area`; `orgUnit` filters one org
unit only.

**Entry**:
One line of an activity's list, such as a household, belonging to one org unit (a Reference entry today). A row's
field may pick one. The lists feature owns entries. API: `entry`.

**Entries outside placement**:
Rows whose picked entry belongs to another org unit than the submission's, after a move, split or accept. They are
reported and tracked until a `setEntry`, a removal or a move back resolves them. `setEntry` refuses such an entry.
API: `entriesOutsidePlacement`; problem `entry-outside-placement`.

### Searching and reading

**Search** (بحث):
Listing one form's submissions by activity, team, assignment, org unit, area, created and entered times, open
conflict, or a field's value; deleted ones only on request. A field or entered-time filter also needs a team, an
assignment, an org unit, a small area or a short created range. API: `GET /api/v2/submissions`;
`POST /api/v2/submissions:search`; `showDeleted`.
_Avoid_: find, query, filter (as the name of the action)

**Created**:
When the server stored the submission, `data_submission.created_date`: an upload's receipt, a go-live release's
time, or a split-off's split time. API: `createdFrom`, `createdTo`; in the feed `created_at`.
_Avoid_: received (it is not the first receipt for releases and split-offs)

**Entered**:
When the form was filled on the phone: the start and end times the phone recorded. API: `enteredFrom`,
`enteredTo`; in the feed `entered_start_at`, `entered_end_at`.

**View** (عرض):
Reading one submission: its current state, typed by its form version, its version as the `ETag`, whether it has an
open conflict, and its note count. API: `GET /api/v2/submissions/{id}`.
_Avoid_: open, head

**Form dictionary**:
A form version's fields and sections by id, with names, labels, value types and option codes.
API: `GET /api/v2/forms`; `forms/{id}/versions/{number}`. The feed publishes the same as streams.

**Note** (ملاحظة):
A written remark on a submission or one of its rows, with its author and time. It changes no data, makes no
revision and never causes a conflict. API: `submissions/{id}/notes`.
_Avoid_: comment, query, feedback (a part of the work, not an object)

**History** (سجل التعديلات):
A submission's changes in order, each shown as who did what, when and why, and what changed.
API: `submissions/{id}/revisions`.
_Avoid_: audit, audit log, change log, journal (engineering's storage)

### Changing

**Edit** (تعديل):
One request that changes a submission's values or rows through an ordered list of edit operations, applied all or
none against one version, with one reason. One edit makes one revision. API: `:edit`.
_Avoid_: correction, correct, fix, update, amend, change, as the request's name. "An edit that fixes an error" is
fine in prose.

**Edit operation**:
One step of an edit: `setValue`, `clearValue`, `setEntry`, `removeRow`, `restoreRow`, and later `addRow`.
API: `operations[]`, each with `op`.
_Avoid_: operation alone where it could mean the operations team; long-running work is a job

**Reason** (السبب):
Why a person made an edit, move, split, delete, restore or conflict decision, in their own words. It is required,
kept with the change, and never logged or repeated in an error. API: `reason`.
_Avoid_: note, comment, justification, cause (a code the server assigns). A coded reason, if ever added, is
`reasonCode`.

**Remove a row** (إزالة صف) / **restore a row** (استرجاع صف):
Removing takes a row out of the submission's current state; the row stays in history, and the feed shows it as
deleted. Restoring puts it back, with the same id, from an earlier revision. API: `removeRow`, `restoreRow`.
_Avoid_: delete a row (delete is for whole submissions), exclude (a lists state), void

**Delete** (حذف) / **deleted** (محذوفة) / **restore** (استرجاع):
Deleting marks a whole submission as not counting: it stays with its history, searches leave it out unless asked,
and the feed publishes it as deleted. Restoring makes it count again. Both are revisions.
API: `:delete`, `:restore`, `deleted`; `showDeleted` in a search.
_Avoid_: void, archive, discard, reject, cancel, trash, purge, undelete

**Move** (نقل):
Changing a whole submission's placement to another assignment of the same activity: in operations' words, to
another village or team. It keeps its id and its rows. API: `:move`.
_Avoid_: reassign (a progress status), transfer, handover (the event that may lead to a move)

**Split** (فصل):
Taking some rows, with their nested rows, out of a submission into a new submission, the split-off, at the same or
another placement in the same activity. Rows reach another submission only this way. API: `:split`.
_Avoid_: move rows (not built), re-enter (what split replaces), merge, extract

**Validation** (التحقق):
The server's tests that a change fits the submission's form version: value types, option codes, mandatory fields,
row shape and list entries. The form's own rules run only on phones.
_Avoid_: check (the lists word for a test that raises a question for people), rule (the form's rules)

**Dry run** (تجربة دون حفظ):
A changing request run through every validation, answered with the would-be result and not saved. API: `dryRun`.
_Avoid_: preview, validate only

**Recorded twice** (مسجَّل مرتين):
One real visit or service recorded in two rows or two submissions: a data error, settled by removing the extra row
or deleting the extra submission with that reason. Not **served twice** (a real second service, which stays in
the data) nor **listed twice** (two list entries for one household), both lists words.
_Avoid_: duplicate alone

**Adjustment**:
A new submission recording a quantity difference that happened or was found later, such as a loss, damage or a
recount, with a signed quantity and a reason, in a form operations configure. A typo is an edit instead.
_Avoid_: reconciliation (an activity's form), correction

**Mark** (not built):
A label operations would define as data for an activity, such as "reviewed" or "has issues", put on a submission
with a reason. It would change no version, cause no conflict and stay out of the feed. API, if built: `:mark`.
_Avoid_: review status, approved, rejected, validated, verified, on hold

### Phones and conflicts

**Upload item**:
One submission in a phone's upload, answered on its own: `updated`, or a refusal of that item alone.
_Avoid_: phone row (a row is a repetition of a repeating section)

**Uploader** (المُرسِل):
The user whose phone sent a submission or a phone copy. A submission's creator is its stored `created_by`: the
user who first sent it, or for a split-off the source's creator.

**Phone copy** (نسخة الهاتف):
The whole document a phone sends again for a submission the server already holds. A conflict keeps it unapplied, as
its upload route parsed it.
_Avoid_: held copy, stale re-send

**Unchanged re-send** (إعادة إرسال دون تغيير):
A phone copy equal to the state it was based on, or to that uploader's last decided conflict copy, usually a retry
after a lost answer. It changes nothing, opens no conflict, and is answered `updated`; while that uploader's conflict
is open, it replaces the conflict's copy instead.
_Avoid_: echo, absorbed, duplicate, retry

**Server-side change** (تغيير على الخادم):
A revision the uploader's phone did not make, exactly: an edit, move, split, delete or restore, another uploader's
accepted copy, an admin delete, or a deleted baseline, which counts for every uploader, also as a submission's first
revision.
_Avoid_: office change, room change

**Conflict** (تعارض):
A changed phone copy that arrived after a server-side change the phone had not seen. It is kept aside, the phone is
answered `updated`, and the submission shows `hasOpenConflict`; there is one open conflict per submission and
uploader, and it always holds that uploader's newest copy. API: `conflicts`; states `OPEN`, `KEPT`, `ACCEPTED`; `cause` `SERVER_SIDE_CHANGE` or `DETECTION_ERROR`.
_Avoid_: held, hold, held re-send, and `conflict` as the bare code of an HTTP 409

**Keep** (إبقاء الحالي) / **accept** (قبول نسخة الهاتف) /
**accept and re-apply** (قبول وإعادة تطبيق التعديلات):
Keep closes a conflict and leaves the submission as it is, the phone copy kept as evidence. Accept makes the phone
copy the new state, by the resolver on behalf of the uploader. Accept and re-apply also applies again, in the same
revision, the server-side edits made since the base version. API: `:keep`, `:accept`, `reapplyEdits`.
_Avoid_: reject, approve, dismiss, ours, theirs

### Who may do what

**Access grant** (منح صلاحية):
One permission on one scope, given to one user by an admin. A revoked grant is kept. Access grants never drive
phones, and admins hold no permission they have not granted themselves. API: `access-grants`.
_Avoid_: grant alone, authority (Spring's roles), role, team form grant (phone-facing, such as `EDIT_SUBMISSIONS`)

**Permission** (صلاحية):
What a grant allows; one for each part of the work: `submissions.view` (monitoring), `submissions.viewHistory`
(auditing and reviewing), `submissions.change` (making changes), `submissions.addNotes` (feedback).
_Avoid_: correction permission, `CORRECT_SUBMISSIONS`

**Scope** (نطاق):
What a grant covers: an activity, always; later also an area, a form or a team.

### Engineering words

Operations never see these: they see history and "deleted".

| Word | Meaning |
| --- | --- |
| **Journal** | The revision journal, `journal.submission_revision`: every change of every submission, append-only |
| **Revision** | One recorded change of a submission: version, kind, command, actor, on-behalf-of user, reason, time, and the full state after it. Operations see it as one change in the history. API: `revisions`. _Avoid_: audit record |
| **Version** | A submission's revision number: 0 at creation, plus one per revision; the `ETag`. Write "revision 3", never "v3", and never a bare "version" beside a form version. API: `version` |
| **Head** | A submission's newest revision; its version is the submission's version |
| **Baseline** | The revision that recorded a submission's state when the journal started, or after a gap; history begins there |
| **Base version** | The version a phone copy was based on, taken from the journal: the newest revision equal to what that uploader last sent. Phones send no version. _Avoid_: sync point |
| **Kind** | What a revision did: `CREATE`, `UPDATE`, `DELETE`, `BASELINE`; this feature adds `RESTORE`. _Avoid_: type |
| **Command** | Which action made a revision: `UPLOAD`, `LEGACY_UPLOAD`, `HELD_RELEASE`, `ADMIN_DELETE`; this feature adds `EDIT`, `MOVE`, `SPLIT`, `DELETE`, `RESTORE`, `CONFLICT_ACCEPT`, one per changing method. In v2 code a command is also any changing request |
| **Lineage** | The submissions linked by splits, splits of splits included, recorded both ways. API: `splitFrom`, `splitTo` |
| **Live** | Not deleted, for a submission; in a live submission's current state, for a row. A row id is live in at most one submission of a lineage |
| **Unmapped value** | A stored value no field keys, kept whole at its path: mostly one at a path no version of its form defines, also a second value for a field that already has one. A split cannot name a top-level one, so it stays in the source; one inside a split row travels with the row |
| **Submission lock** | The database lock on the submission (`SELECT … FOR NO KEY UPDATE` on `data_submission`, the lock every update of it takes), taken first in every changing transaction, the upload routes' included, and waited for at most `datarun.submissions.lock-timeout` (5 s). _Avoid_: row lock |
| **Outbox** | The table where each change's transaction leaves one row naming its revision, for the ETL and the feed's publisher |
| **ETL** | The outbox consumer that projects each change into tall (`analytics.tall_canonical`) and events (`analytics.events`), the analytics interface until the feed replaces them. Its **split re-attribution** moves split rows to their new submission in tall |
| **Data path** | Where a value sits in a stored document: the stored keys from the root, joined by dots. Never a target in the API |
| **Submission normalizer** | The one module that turns a stored submission into values keyed by row id and field id, typed by its form version (`SubmissionNormalizer.normalize`). It never fails on data: it flags what it meets. Edits also need the write direction, back to stored places. _Avoid_: translation |
| **Feed** | The published change feed of submissions' current state, in schema `feed` ([its glossary](../submission-feed/CONTEXT.md)) |
| **Publisher** | The feed's single writer |
| **Position** | The feed's order: one number per published record, in commit order. A row's place among its siblings is its row index |
| **Tombstone** | In prose only: a feed record with `deleted = true`, which keeps its last values |
| **Activation switch** | A setting, off by default, that turns on one method or edit operation ([api-v2.md](../../agent-context/api-v2.md)) |
| **Gate** | Switches turned on together, server-wide, once their condition holds: gate A with the capability; gate B once downstream confirms in writing that no model reads tall and events |

## Words to avoid

| Avoid | Why | Say instead |
| --- | --- | --- |
| correction, correct (a request, permission or command) | Presumes an error; also an adjective | edit; "an edit that fixes an error" |
| void, archive, trash, purge | Final where they are terms of art, or taken (the archive of past activities); the stored state is `deleted` | delete |
| held, hold, held copy, held re-send, discard | Go-live and practice holds | conflict, phone copy; keep |
| echo, absorbed, stale re-send | Metaphors, or a verdict on a copy that may hold real changes | unchanged re-send; "changes nothing"; phone copy |
| duplicate (alone) | Four meanings | recorded twice, listed twice, served twice, unchanged re-send |
| merge | A progress status (`MERGED`) | split; delete |
| move rows | Not built: rows reach another submission only by a split | split |
| reassign, transfer | A progress status (`REASSIGNED`) | move |
| reconcile, reconciliation (for a change to submissions) | An activity's form; in the feed, reconcile is the publisher's catch-up | edit (a typo); adjustment (a later difference) |
| exclude | A lists state | delete (submissions), remove (rows) |
| review status, approve, reject, validate (as a verdict) | No approval workflow; `APPROVE_SUBMISSIONS` is a team form grant | a note; delete; a mark (not built) |
| authority, grant (alone) | Spring's roles; team form grants drive phones | permission, access grant, team form grant |
| office, office user, the room (as an actor) | Names no role and no permission | "a user with `submissions.change`" |
| status (alone) | Several meanings | progress status, deleted, a conflict's state |
| received | Not the first receipt for releases and split-offs | created |
| v1, v2 for revisions; version (alone, in prose) | API versions and form versions | revision 3; form version 32 |
| record, entry (for submissions and rows) | Lists words; record is also the feed's | submission, row |
| event (for rows) | Analytics' events table | row |
| repeat (alone) | The section or the row | row, repeating section |
| audit (new tables or endpoints) | Dead code here | history, revisions |
| update, change (as the request's name) | Both mean any revision | edit |
| revise | مراجعة means both review and revision | edit |
| find, query, open | A database word; a clinical data workflow | search, view |
| sync point | An earlier draft's word for the same thing | base version |
| translation (for reading stored submissions) | Means label translations in DataRun | submission normalizer; normalize, normalized |
| bulk | The legacy upload route | batch |
| operation (long-running work) | An edit's list | job |
| data cleaning | Downstream cleans its own copy | submission review |
