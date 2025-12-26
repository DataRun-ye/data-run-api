
# the mental model for client-facing data collection

Let me re-center and **strip this down to the essence**, only the **mental model** for *client-facing data collection*,
no pipelines, no engines, no recipes, no lifecycle, no validation DSL, no upstream logic.

What I want is a **conceptual model that flows**, stays *general*, and lets many scenarios emerge **without naming the
scenario** (warehouse, HR, health, logistics, etc.).

---

### The clean, flowing model (client-facing only)

#### 1. Everything the user does is **“making a statement”**

Not a transaction, not a movement, not a receipt.

> A user **states something about the world** at a point in time.

That statement:

* is contextual (activity)
* is scoped (assignment)
* references parties
* carries facts (fields)

This matches our existing success.

---

#### 2. Assignment = **Context window**

An assignment is not “permission” and not “workflow”.

It is:

> *A lens that tells the client:
> “When you are here, these are the things you are allowed to talk about, and these are the parties you may mention.”*

Assignment defines:

* Which **activities** are relevant
* Which **orgUnits / teams / parties** appear in selectors
* Which **dataTemplates** are available

Nothing procedural. No meaning beyond *context*.

---

#### 3. dataTemplate = **Vocabulary**

A dataTemplate is not a form, not a transaction type.

It is:

> *A vocabulary for making a certain kind of statement.*

A template defines:

* What facts can be stated
* What references may be mentioned
* Which references are optional or repeatable

Example vocabularies:

* “I observed something”
* “I moved something”
* “I counted something”
* “I linked these things”
* “I described a situation”

The template does **not** know what a receipt or transfer is.

---

#### 4. Party is just “something you can point at”

This is the key unification.

> A **party** is anything the user can refer to in a statement.

Party may be:

* orgUnit
* team
* user
* external entity
* abstract location (“store room”, “truck”)
* logical bucket (“damaged items”, “consumed”)

Client-side, it is just:

```
{ type, id, label }
```

No behavior. No rules.

---

#### 5. “Source” and “Destination” are **roles, not concepts**

This is the *water* part.
Source” and “Destination are **roles, not concepts, and roles can be made of anything.
You **never model** source/destination globally.

Instead:

* A template may define **roles** (labels)
* A partyRef field may carry a `role`

Examples of roles:

* `from`
* `to`
* `handled_by`
* `responsible`
* `reported_by`
* `affects`
* `belongs_to`

The client only knows:

> “This field expects a party playing this role.”

The system never hardcodes what a role *means*.

---

#### 6. Line vs header is an **expression convenience**

Not a semantic difference.

> A header is a shared default.
> A line is a localized override.

Client rule:

* If a line omits a party, it inherits from header.
* No deeper meaning.

This lets:

* Single-item statements
* Multi-item statements
* Mixed-party statements
  all emerge naturally.

---

#### 7. Warehouse flows emerge without naming them

If we took the Warehouse flows scenario as an example.
Now the important part: **why warehouse works without special modeling**

A user submits a statement like:

> “Item X, quantity 10, from Party A, to Party B”

That’s it.

Whether that is:

* receipt
* issue
* transfer
* adjustment
* return

…is **interpretation**, not collection.

Client doesn’t care.

---

#### 8. No “movement type” required

If you want maximum fluidity:

Do **not** force `movement_type`.

Let meaning be inferred later from:

* which roles are present
* which parties are involved
* sign of quantity (optional)
* presence/absence of fields

Example:

* only `to` → looks like receipt
* only `from` → looks like issue
* both → looks like transfer
* neither but with count → looks like inventory count

Client just collects facts.

---

#### 9. The UI becomes extremely simple

The client only needs 5 primitives:

1. **Context picker**

    * assignment → activity → template

2. **Party picker**

    * filtered by assignment
    * labeled by role, not type

3. **Fact inputs**

    * number, text, date, option

4. **Repeat group**

    * add many “facts about things”

5. **Preview**

    * “You are stating: X about Y with Z”

No workflow, no branching logic.

---

#### 10. Why this stays universal

This same surface supports:

* warehouse inventory
* health reporting
* surveys
* incident reporting
* asset tracking
* logistics
* HR records
* monitoring & evaluation

---

## map our existing schema 1-to-1 onto this phrasing

### 1) One-line mapping (use this in the UI and docs)

* **Activity** → **Context tag** (What kind of statement this is usually about)
* **Assignment** → **Context window** (Where you configure *which* vocabularies and parties are usable)
* **dataTemplate** → **Vocabulary** (What facts can be stated — the reusable form the user will fill)
* **orgUnit / team / user / user_group / external** → **Party** (Things you can point at)
* **Fields inside dataTemplate** → **Facts** (the bite-sized things the user states)
* **Header / line groups** → **Shared defaults / Fact lines** (convenience: header is default; lines override)
* **Team membership / assignment binding** → **Authorisation lens** (who sees which context window and party lists)

### 2) UI labels & microcopy (for configurator — avoids exposing schema words)

Use these exact labels in the config screens so non-technical users understand easily:

* “Create a new **Vocabulary**” — *A vocabulary is the set of facts people will state.*
* “Add a **Fact**” — *A field the user will fill (number, text, date, picklist, or point at a party).*
* “Add a **Party role**” — *A label for things people can point at inside this vocabulary (e.g. ‘from’, ‘to’,
  ‘reported_by’).*
* “Create an **Assignment (Context Window)**” — *Decides which vocabularies and parties are available when working in
  this context.*
* “Allowed Parties for this Context” — *Which org units, teams or external parties should appear in pickers here.*
* “Header or Line?” toggle with helper: *Header = default for all facts; Line = one row per item (can override header).*
* “Preview statement” button — *Show human-readable sentence the user will submit based on this vocabulary.*

### 3) Concrete mapping table (for devs & product writers)

| Your table / concept                 |  Mental-model name | What to store / show in UI                                                    | Example UI text                                               |
|--------------------------------------|-------------------:|-------------------------------------------------------------------------------|---------------------------------------------------------------|
| `activity`                           |        Context tag | `id, label, description` — shown as a tag when choosing vocabulary            | “Context: Vaccine Campaign”                                   |
| `assignment`                         |     Context window | `id, label, allowed_parties[], vocabularies[]` — selected when user opens app | “You are working in: District Store → Allowed locations: X,Y” |
| `dataTemplate`                       |         Vocabulary | `id, version, label, fields[], headerDefinition, lineDefinition`              | “Vocabulary: Count stock / Vocabulary: Case report”           |
| `orgUnit`, `team`, `user`, external` |              Party | store as `{type, id, label}`; UI shows type badge + name                      | PartyPicker shows “Warehouse — Al Hodeidah (orgUnit)”         |
| `template fields`                    |              Facts | `name,label,type,role(optional),repeatable,helpText`                          | Fact editor shows “Batch number (optional)”                   |
| `assignment_user/team`               | Authorisation lens | `assignment -> allowed_party_ids` used to filter PartyPicker options          | “Only show orgUnits: Hodeidah HC, Taiz Store”                 |

### 4) Polished configurator flow (wizard that never confuses)

Make a 4-step wizard for non-technical config users —each step minimal, with live preview sentence:

1. **Name & Purpose** — “Name this vocabulary” + one-sentence purpose (“Used to record inventory movements between
   sites”).
2. **Facts (fields)** — Add facts one-by-one. For each fact ask: label, type (text/number/date/partyRef/option), short
   help text, optional/required, repeatable? Show preview.
3. **Roles & Defaults** — For any `partyRef` fact, ask: “Should users pick a role for this party?” (default roles:
   from / to / handled_by / reported_by). Optionally link a header default.
4. **Scope: Which Context Window?** — Pick existing assignment(s) or create a new context window that says “where this
   vocabulary appears” and which parties should be visible there.

After step 4 show a **Preview Statement** that converts a filled example into a human sentence. Always show the “What we
call this in the app” label (e.g., Vocabulary: “Stock Move”).

### 5) How to present partyRefs so configurators don’t get confused

* Never show `orgUnit/team` raw names in the composer. Instead:

    * When adding a `partyRef` fact, show three simple choices:

        * “Pick from existing locations (orgUnits), teams / users (system entities that can be parties)”
        * “Allow external / free-entry parties (suppliers/customers)”
    * Show counts: “Allowed: 12 locations, 3 teams” and a link “Edit allowed parties for this context”.
    * Default to “locations” if the configurator doesn’t choose (keeps most UIs simple).

### 6) Field editor defaults & sane presets (reduces cognitive load)

* When adding a numeric `Fact` named `quantity`, default its label to “Quantity” and make it required.
* When adding a `partyRef` named `from` or `to`, auto-suggest role labels `from` / `to`.
* When adding a repeatable line, pre-create common line facts: `item`, `qty`, `uom`, `batch`, `expiry` to speed up the
  common cases. The configurator can remove what they don’t want.

### 7) Examples (expressed as vocabulary, showing exactly where your schema plugs in)

Short examples showing how existing pieces map directly — copy these into docs for configurators.

A) **Warehouse (Transfer) — vocabulary view**

* Vocabulary label: “Move items” (dataTemplate).
* Facts:

    * `movement_date` (date)
    * `from` (partyRef; allowed: orgUnit)
    * `to` (partyRef; allowed: orgUnit)
    * `lines` (repeatable) → inside line: `item`, `qty`, `uom`, `batch`, `expiry`
* Assignment: `assignment_id = warehouse-transfers` with allowed parties set to the organization’s warehouses (orgUnit
  IDs).

B) **Health (Case report) — vocabulary view**

* Vocabulary label: “Case report”.
* Facts:

    * `report_date` (date)
    * `facility` (partyRef → orgUnit)
    * `reported_by` (partyRef → user/team)
    * `patient_age`, `sex`, `symptoms[]` (facts)
* Assignment: `assignment_id = district-surveillance` showing only facilities in that district.

C) **Project update — vocabulary view**

* Vocabulary: “Progress update”.
* Facts:

    * `update_date`, `project` (partyRef → team/project), `status` (option), `notes` (string), `attachments` (file).
* Assignment: `assignment_id = program-management` with allowed parties = project list.

### 8) Backwards compatibility — how to reuse your current tables without confusing configurators

* Don’t change table names / keys. Build a *thin metadata layer* called `vocabulary_mapping` where a `dataTemplate`
  references existing IDs:

    * `vocabulary.template -> dataTemplate.id` (existing)
    * `vocabulary.allowed_parties -> list of orgUnit IDs or team IDs` (existing orgUnit/team table ids)
      This keeps DB stable while configurator UI shows friendly labels and hides ids.

### 9) Small UX copy bundle (copy you can paste into the UI)

* For Vocabulary create screen hint: “A vocabulary defines the *words* someone can use to state facts (e.g., items,
  quantities, locations). It does not perform actions — it only collects facts.”
* For Party role help: “A role is how the vocabulary refers to a party — e.g. ‘from’, ‘to’, or ‘reported_by’ — it’s just
  a label.”
* For Assignment help: “An assignment is the *place* (context) where this vocabulary will be available. It limits which
  parties and templates appear to users.”

### 10) Implementation checklist (short, practical)

* [ ] Expose composer UI with the labels above.
* [ ] Preload a few opinionated starter vocabularies (warehouse, case report, count).
* [ ] Store only IDs in DB; show friendly preview sentences in the composer.
* [ ] Save `vocabulary -> dataTemplate` mapping and `assignment -> allowed_party_ids` mapping.
* [ ] Provide “Preview” and “Test fill” buttons in composer so configurator sees what users will experience.

---

### If you like, I’ll now generate:

* a single **one-page cheat-sheet** (3-column table) you can hand to product/configuration team that maps each screen
  control to exact DB fields and JSON keys; **or**
* a ready-to-drop **example `vocabulary` JSON** (using your current dataTemplate fields & orgUnit IDs) for the Transfer
  vocabulary so you can paste it into your system.

Which one do you want?

---

