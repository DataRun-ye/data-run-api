
C) **Project update — vocabulary view**

* Vocabulary: “Progress update”.
* Facts:

    * `update_date`, `project` (partyRef → team/project), `status` (option), `notes` (string), `attachments` (file).
* Assignment: `assignment_id = program-management` with allowed parties = project list.

# 8) Backwards compatibility — how to reuse your current tables without confusing configurators

* Don’t change table names / keys. Build a *thin metadata layer* called `vocabulary_mapping` where a `dataTemplate` references existing IDs:

    * `vocabulary.template -> dataTemplate.id` (existing)
    * `vocabulary.allowed_parties -> list of orgUnit IDs or team IDs` (existing orgUnit/team table ids)
      This keeps DB stable while configurator UI shows friendly labels and hides ids.

# 9) Small UX copy bundle (copy you can paste into the UI)

* For Vocabulary create screen hint: “A vocabulary defines the *words* someone can use to state facts (e.g., items, quantities, locations). It does not perform actions — it only collects facts.”
* For Party role help: “A role is how the vocabulary refers to a party — e.g. ‘from’, ‘to’, or ‘reported_by’ — it’s just a label.”
* For Assignment help: “An assignment is the *place* (context) where this vocabulary will be available. It limits which parties and templates appear to users.”

# 10) Implementation checklist (short, practical)

* [ ] Expose composer UI with the labels above.
* [ ] Preload a few opinionated starter vocabularies (warehouse, case report, count).
* [ ] Store only IDs in DB; show friendly preview sentences in the composer.
* [ ] Save `vocabulary -> dataTemplate` mapping and `assignment -> allowed_party_ids` mapping.
* [ ] Provide “Preview” and “Test fill” buttons in composer so configurator sees what users will experience.

---
