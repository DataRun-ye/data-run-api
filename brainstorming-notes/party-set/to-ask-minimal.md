<!-- TOC -->
  * [Introduction](#introduction)
    * [the mental model for client-facing data collection](#the-mental-model-for-client-facing-data-collection)
    * [The clean, flowing model (client-facing only)](#the-clean-flowing-model-client-facing-only)
      * [1. Everything the user does is **“making a statement”**](#1-everything-the-user-does-is-making-a-statement)
      * [2. Assignment = **Context window**](#2-assignment--context-window)
      * [3. dataTemplate = **Vocabulary**](#3-datatemplate--vocabulary)
      * [4. Party is just “something you can point at”](#4-party-is-just-something-you-can-point-at)
      * [5. “Source” and “Destination” are **roles, not concepts**](#5-source-and-destination-are-roles-not-concepts)
      * [6. Line vs header is an **expression convenience**](#6-line-vs-header-is-an-expression-convenience)
      * [7. Warehouse flows emerge without naming them](#7-warehouse-flows-emerge-without-naming-them)
      * [8. No “movement type” required](#8-no-movement-type-required)
      * [9. The UI becomes extremely simple](#9-the-ui-becomes-extremely-simple)
      * [10. Why this stays universal](#10-why-this-stays-universal)
  * [map our existing schema 1-to-1 onto this phrasing](#map-our-existing-schema-1-to-1-onto-this-phrasing)
    * [1) One-line mapping (use this in the UI and docs)](#1-one-line-mapping-use-this-in-the-ui-and-docs)
    * [2) UI labels & microcopy (for configurator — avoids exposing schema words)](#2-ui-labels--microcopy-for-configurator--avoids-exposing-schema-words)
    * [3) Concrete mapping table (for devs & product writers)](#3-concrete-mapping-table-for-devs--product-writers)
    * [4) Polished configurator flow (wizard that never confuses)](#4-polished-configurator-flow-wizard-that-never-confuses)
    * [5) How to present partyRefs so configurators don’t get confused](#5-how-to-present-partyrefs-so-configurators-dont-get-confused)
    * [6) Field editor defaults & sane presets (reduces cognitive load)](#6-field-editor-defaults--sane-presets-reduces-cognitive-load)
    * [7) Examples (expressed as vocabulary, showing exactly where your schema plugs in)](#7-examples-expressed-as-vocabulary-showing-exactly-where-your-schema-plugs-in)
  * [Final set of Specifications](#final-set-of-specifications)
    * [1. Core Data Entities (The "Statics")](#1-core-data-entities-the-statics)
      * [A. The Registry (`party`)](#a-the-registry-party)
      * [B. The Policy (`party_set`)](#b-the-policy-party_set)
      * [C. The Template (`vocabulary`)](#c-the-template-vocabulary)
      * [D. The Context (`assignment`)](#d-the-context-assignment)
    * [2. The Linkage Model (The "Connectors")](#2-the-linkage-model-the-connectors)
      * [Assignment-Vocabulary Link](#assignment-vocabulary-link)
      * [Assignment-Party Binding](#assignment-party-binding)
    * [3. The Resolution Engine (The Core Logic)](#3-the-resolution-engine-the-core-logic)
      * [Precedence Algorithm (Order of Operations)](#precedence-algorithm-order-of-operations)
      * [Spec Execution (The "Kinds")](#spec-execution-the-kinds)
    * [4. API Specifications](#4-api-specifications)
      * [I. The Manifest Fetch](#i-the-manifest-fetch)
      * [II. The Party Resolver](#ii-the-party-resolver)
    * [5. System Flow & Sequence](#5-system-flow--sequence)
    * [6. Edge Case Responsibilities](#6-edge-case-responsibilities)
  * [Notes on where and how to start](#notes-on-where-and-how-to-start)
    * [1. The Core Backend Components & Responsibilities](#1-the-core-backend-components--responsibilities)
      * [A. Registry Service (The "Who & Where")](#a-registry-service-the-who--where)
      * [B. Template Service (The "What Facts")](#b-template-service-the-what-facts)
      * [C. Context & Policy Service (The "How & Which")](#c-context--policy-service-the-how--which)
      * [D. Submission Service (The "Execution")](#d-submission-service-the-execution)
    * [2. The Runtime Execution Flow](#2-the-runtime-execution-flow)
    * [3. Edge Logic: Where it should reside](#3-edge-logic-where-it-should-reside)
    * [4. Implementation Priorities (The "Start Tiny" Roadmap)](#4-implementation-priorities-the-start-tiny-roadmap)
    * [5. Conflict Resolution: The "Team" vs "User" Membership](#5-conflict-resolution-the-team-vs-user-membership)
  * [Dev Stack and Platform / Build dependencies](#dev-stack-and-platform--build-dependencies)
    * [1. IDs, UIDs and business keys](#1-ids-uids-and-business-keys)
    * [recommended **Implementation Path** to go from zero to a working "Walking Skeleton](#recommended-implementation-path-to-go-from-zero-to-a-working-walking-skeleton)
      * [Step 1: The Liquibase Schema (Foundation)](#step-1-the-liquibase-schema-foundation)
      * [Step 2: The Domain Model & ID Generation](#step-2-the-domain-model--id-generation)
      * [Step 3: The Registry & Org-Tree Service (jOOQ)](#step-3-the-registry--org-tree-service-jooq)
      * [Step 4: The Resolution Engine (The "Brain")](#step-4-the-resolution-engine-the-brain)
      * [Step 5: The "Manifest" & "Resolve" API](#step-5-the-manifest--resolve-api)
      * [Recommended Rollout Phases](#recommended-rollout-phases)
  * [My Recommendation: What to generate *first*](#my-recommendation-what-to-generate-first)
<!-- TOC -->

## Introduction

This document serves as the Notes and **Canonical Technical Specification** for the "Party Access & Contextual Governance" system
for client-facing data collection app. It focuses on the logic and infrastructure required to resolve what a user can see and select *before* they submit data.

### the mental model for client-facing data collection

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

--


























































## Final set of Specifications

This document serves as the **Canonical Technical Specification** for the "Party Access & Contextual Governance" system
for client-facing data collection app. It focuses on the logic and infrastructure required to resolve what a user can see and select *before* they submit data.

---

### 1. Core Data Entities (The "Statics")

These tables/objects form the backbone of the system.

#### A. The Registry (`party`)

Stores every selectable entity.

* **Fields:** `id`, `type` (orgUnit, team, user, external), `label`, `tags` (JSON array), `parent_id` (for hierarchies),
  `meta` (JSON).

#### B. The Policy (`party_set`)

Defines the **rule** for filtering parties.

* **Fields:** `id`, `kind` (static, org_tree, tag_filter, query, assignment_scoped), `spec` (JSON configuration for that
  kind).

#### C. The Template (`vocabulary`)

Defines the form structure.

* **Fields:** `id`, `label`, `fields` (Array of objects defining `name`, `type`, and optionally a `role`).

#### D. The Context (`assignment`)

The "Window" through which the user works.

* **Fields:** `id`, `label`, `status` (active/closed), `activity_id` (grouping tag).
* **Membership:** `assignment_members` (links `assignment_id` to `user_id` or `team_id`).

---

### 2. The Linkage Model (The "Connectors")

This is where the "Authorisation Lens" is constructed.

#### Assignment-Vocabulary Link

Determines which forms are available in a context.

* **Table:** `assignment_vocabularies` (`assignment_id`, `vocabulary_id`).

#### Assignment-Party Binding

The specific override that controls the dropdowns.

* **Table:** `assignment_party_bindings`
* **Fields:** `assignment_id`, `vocabulary_id` (optional/nullable), `role_name`, `party_set_id`.
* *Logic:* If `vocabulary_id` is null, the binding applies to that role across all forms in that assignment.

---

### 3. The Resolution Engine (The Core Logic)

The backend must implement a `resolve(assignment, vocabulary, role)` function.

#### Precedence Algorithm (Order of Operations)

When a user clicks a dropdown for a specific "Role" (e.g., "From"):

1. **Level 1 (Specific Binding):** Look for a binding where `assignment_id`, `vocabulary_id`, AND `role_name` match.
2. **Level 2 (Global Binding):** Look for a binding where `assignment_id` and `role_name` match, but `vocabulary_id` is
   NULL.
3. **Level 3 (Template Default):** Look for a `partySetRef` defined inside the Vocabulary JSON for that field.
4. **Level 4 (System Fallback):** Use the `assignment_scoped` rule (return parties explicitly linked to the assignment).

#### Spec Execution (The "Kinds")

Once a `party_set_id` is identified, the engine executes the `spec`:

* **`static`:** `SELECT * FROM party WHERE id IN (spec.members)`
* **`org_tree`:** Recursive CTE starting at `spec.root` down to `spec.depth`.
* **`tag_filter`:** `SELECT * FROM party WHERE tags ?| spec.tags AND type IN (spec.types)`
* **`query`:** Execute a named server-side function/prepared statement identified by `spec.sql_key`.

---

### 4. API Specifications

#### I. The Manifest Fetch

**Endpoint:** `GET /context/manifest`
**Purpose:** Called when the app starts. Returns everything the user is "assigned" to.
**Response:**

```json-l
{
  "assignments": [
    {
      "id": "assign-123",
      "label": "District A - Vaccine Drive",
      "vocabularies": ["stock-count", "transfer-form"],
      "bindings": [
        { "role": "from", "party_set_id": "ps-warehouses" },
        { "role": "to", "vocabulary": "transfer-form", "party_set_id": "ps-clinics" }
      ]
    }
  ]
}
```

#### II. The Party Resolver

**Endpoint:** `GET /parties/resolve`
**Parameters:** `assignment_id`, `vocabulary_id`, `role_name`, `q` (search string).
**Purpose:** Populates the autocomplete/dropdown in the UI.
**Logic:** Runs the Precedence Algorithm (see below for pseudo) -> Executes Spec -> Filters by `q`.

---

### 5. System Flow & Sequence

1. **Initial Load:** The client fetches the **Manifest**. It now knows which Assignments are active and which
   Vocabularies they contain.
2. **Form Entry:** User selects an Assignment and a Vocabulary.
3. **Field Interaction:** User taps a `partyRef` field.

* The Client sends the Context (`assignment_id`, `role`, etc.) to the **Party Resolver**.

4. **Backend Filtering:** The **Resolution Engine** identifies the `party_set`. It queries the **Registry** and applies
   the search filter.
5. **Selection:** The UI displays the labels. The user selects a party.

* *Note:* The UI stores the `id`, `label`, and `type` locally to handle the **Snapshotting** requirement during the
  eventual submission.

---

### 6. Edge Case Responsibilities

* **Offline Support:** The Manifest should include the `party_set_id`. The client can pre-fetch and cache small `static`
  or `tag_filter` sets. For large sets (queries), the client must remain online or use a "Last 50 Used" local cache.
* **Governance:** The `assignment_party_bindings` table must have an audit log. Any change to a binding effectively
  changes the "Access Policy" for all users in that assignment.
* **Validation:** Even though the UI filters the list, the backend **must** re-run the `resolve` logic upon data
  submission to ensure the user didn't manually inject an unauthorized `party_id`.

---

## Notes on where and how to start

To get an initial version working that is both maintainable and scalable, you should focus on a **Modular Monolith**
approach. This keeps the logic in one place for now but separates concerns clearly so they can be extracted later if
needed.

The core philosophy is: **The Assignment is the Router.** Everything flows through the Assignment to determine what the
user can see (Parties) and do (Vocabularies).

---

### 1. The Core Backend Components & Responsibilities

You need four primary services (or modules) to handle the lifecycle of a "Statement" (the data submitted by the user).

#### A. Registry Service (The "Who & Where")

* **Responsibility:** Manages the "Parties."
* **Data Entities:** `orgUnit`, `team`, `user`, `external`.
* **Core Logic:** Handles the hierarchy for `orgUnits` (parent/child relationships) and tags/attributes.

#### B. Template Service (The "What Facts")

* **Responsibility:** Manages **Vocabularies** (dataTemplates) and their structure.
* **Core Logic:** Schema validation. It ensures that if a Vocabulary requires a `partyRef` with a specific role, that
  field exists.

#### C. Context & Policy Service (The "How & Which")

* **Responsibility:** The most critical module. It manages **Assignments**, **PartySets**, and the **Bindings** between
  them.
* **Core Logic:** The **Resolution Engine**. This is the logic that interprets the "Kinds" (static, org_tree, etc.) and
  calculates the list of allowed IDs.

#### D. Submission Service (The "Execution")

* **Responsibility:** Records the actual data filled out by users.
* **Core Logic:** The **Human-Readable Sentence Generator**. It captures the "Fact" and snapshots the Party selected so
  the history remains clear even if the Party name changes later.

---

### 2. The Runtime Execution Flow

This is the sequence of events when a user opens the app to perform a task.

1. **Context Fetch:** User requests their active **Assignments** (based on their `user_id` or `team_id` membership).
2. **Manifest Fetch:** User selects an Assignment. The server returns a "Manifest" containing:

* Allowed **Vocabularies** (Templates) for this Assignment.
* Available **Roles** for this Assignment.


3. **Party Resolution:** When a user interacts with a `partyRef` field (e.g., the "From" field):

* The Client sends `assignment_id`, `vocabulary_id`, and `role_name` to the server.
* The **Policy Service** runs the **Precedence Logic**: (Binding > Template > Default).
* It executes the **PartySet Rule** (e.g., runs a SQL query to find all "Clinics" in that "District").
* Returns a paginated list of Parties.

The Resolution Logic (Pseudocode)

```python
# Function: resolve_party_list
# Inputs: 
#   - assignment_id (The task context)
#   - role_name (e.g., "from" or "to")
#   - vocabulary_id (The specific form template, optional)
#   - search_query (The text user typed into the autocomplete, optional)

function resolve_party_list(assignment_id, role_name, vocabulary_id, search_query):
    
    # STEP 1: Determine which PartySet (Rule) to use based on PRECEDENCE
    # Order: Assignment Binding -> Template Reference -> System Default
    
    # Check if this specific assignment has a custom binding for this role
    rule_id = DB.query("""
        SELECT party_set_id FROM assignment_party_binding 
        WHERE assignment_id = ? AND role_name = ? 
        AND (vocabulary_id = ? OR vocabulary_id IS NULL)
        ORDER BY vocabulary_id DESC LIMIT 1
    """, assignment_id, role_name, vocabulary_id)

    # Fallback: If no binding, check the Template itself
    if not rule_id:
        template = DB.get_template(vocabulary_id)
        rule_id = template.fields[role_name].ui.partySetRef
        
    # Final Fallback: Use the standard "Assignment Scoped" rule
    if not rule_id:
        rule_id = "ps:default_assignment_scoped"

    # STEP 2: Fetch the Rule Definition (The "Spec")
    party_set = DB.table('party_set').get(rule_id)
    kind = party_set.kind
    spec = party_set.spec

    # STEP 3: Resolve the rule into a list of Party IDs
    # This is the "Engine" that handles different kinds of rules
    allowed_ids = []

    if kind == "static":
        allowed_ids = spec.members  # Just return the hardcoded list

    elif kind == "org_tree":
        # Get everything under a specific branch of the hierarchy
        allowed_ids = DB.query_tree(root=spec.root_id, depth=spec.depth)

    elif kind == "tag_filter":
        # Find all parties matching specific tags (e.g., "Warehouse" + "Active")
        allowed_ids = DB.table('party').find_by_tags(spec.tags)

    elif kind == "query":
        # Execute a pre-approved, safe server-side query with parameters
        allowed_ids = DB.execute_saved_query(spec.sql_key, spec.params)

    elif kind == "assignment_scoped":
        # Simply return the parties already linked to this assignment record
        allowed_ids = DB.table('assignment').get(assignment_id).allowed_parties

    elif kind == "external":
        # Call an external partner API to get a list
        allowed_ids = ExternalAPI.fetch(spec.endpoint, spec.provider)

    # STEP 4: Apply Search Filter and Pagination
    # Filter the IDs down to match what the user is typing in the UI
    final_results = DB.table('party').where_in(id=allowed_ids)
    
    if search_query:
        final_results = final_results.filter(label.contains(search_query))

    return final_results.limit(20).to_json()
```

---

### 3. Edge Logic: Where it should reside

To keep the system "easy to polish and maintain," you must be strict about where specific logic lives:

| Logic Type                       | Location                    | Why?                                                                                                                          |
|----------------------------------|-----------------------------|-------------------------------------------------------------------------------------------------------------------------------|
| **Precedence Resolution**        | **Server (Policy Service)** | If you decide to change the priority (e.g., Template over Assignment), you only change it in one place, not every mobile app. |
| **Search/Filter Logic**          | **Server (Registry API)**   | Large datasets (10k+ clinics) cannot be filtered on the phone. The server handles the `q=` parameter.                         |
| **Offline Cache Key Generation** | **Client & Server**         | Both need to agree that a list of parties for `Assign-1 + Role-From` is a unique cacheable "bucket."                          |
| **UI Rendering Hints**           | **Template (Vocabulary)**   | Things like `repeatable: true` or `label: "Warehouse"` belong in the Template JSON so the UI stays generic.                   |
| **Validation (Types)**           | **Client**                  | Immediate feedback for the user (e.g., "This must be a number").                                                              |
| **Validation (Permissions)**     | **Server**                  | Final check to ensure the user actually has access to the Party they just submitted.                                          |

---

### 4. Implementation Priorities (The "Start Tiny" Roadmap)

To get a "Walking Skeleton" (an initially working version) running, implement these specific artifacts first:

1. **The Registry API:** Just `GET /parties` with a basic `type` filter.
2. **The Static PartySet Kind:** It’s the easiest to code. It just returns a hardcoded list of IDs.
3. **The Simple Resolver:** A function that takes `assignment_id` and returns the `vocabulary_id` and the `party_set_id`
   for each role.
4. **The Mock UI:** A single screen that fetches the list from the Resolver and renders a dropdown.

---

### 5. Conflict Resolution: The "Team" vs "User" Membership

there was a slight ambiguity regarding whether a User or a Team is linked to an Assignment.

**The Resolved Path:** Use a **Polymorphic Membership**. The `assignment_member` table should have a `member_type`
column (`'user'` or `'team'`).

* If a **Team** is added to an Assignment, all **Users** in that team inherit the access.
* This is the most flexible path because it allows for both individual "Supervisors" (User) and "Field Units" (Team) to
  work within the same Context Window.

---

## Dev Stack and Platform / Build dependencies

The current system is built upon:

* **Java 17+ (Spring Boot 3.4.2)**: A Maven-based project.
* **PostgresSQL (tested with v16.x)**: Utilizes a compatible PostgresSQL JDBC driver.
* **Liquibase (XML)**: Used for managing schema migrations.
* **`jOOQ` & `NamedParameterJdbcTemplate`/`JdbcTemplate`**: Available for analytical queries.
* **Caching**: Employs Ehcache and Hibernate 2nd-level cache annotations where appropriate.
* **Mapping and Codegen Tools**: Lombok (preferred for compactness and brevity) and MapStruct are used.

### 1. IDs, UIDs and business keys

* **id**: internal primary key (VARCHAR(26)) ULID format. Immutable, never recycled. Used for all foreign-key
  relationships.
* **uid**: short system generated business key (VARCHAR(11)), globally unique, stable across environments, used
  extensively in api client's requests and analytics for human-friendly references.

---

### recommended **Implementation Path** to go from zero to a working "Walking Skeleton

#### Step 1: The Liquibase Schema (Foundation)

Before writing Java code, generate your Liquibase XML migrations. This ensures your `id` (ULID) and `uid` (Business Key)
constraints are enforced at the database level.

**What to build first:**

* **The Registry:** `party` table.
* **The Context:** `assignment`, `assignment_member`, and `assignment_data_template`.
* **The Policy:** `party_set` and `assignment_party_binding`.

> **Tip:** Use PostgreSQL's `jsonb` type for the `party_set.spec` and `party.meta` columns to allow for the flexible "
> Kinds" (static, tag_filter, etc.) we discussed.

---

#### Step 2: The Domain Model & ID Generation

Create your JPA Entities (or POJOs if using pure jOOQ) using **Lombok**.

* **ULID Integration:** Use a library like `ulid-creator` to handle the `id` generation.
* **UID Generation:** Implement a small utility to generate the 11-character `uid`.
* **MapStruct:** Create mappers to convert between your DB Records and your "Manifest" DTOs.

---

#### Step 3: The Registry & Org-Tree Service (jOOQ)

This is where you implement the "Who & Where."

**Why jOOQ here?**
The `org_tree` PartySet kind requires a **Recursive Common Table Expression (CTE)** to find all children of a parent
unit. Writing this in standard JPA is painful; jOOQ makes it typesafe and readable.

**Goal:** Create a `PartyRegistryService` that can:

1. Fetch parties by a list of IDs.
2. Execute a recursive search for the `org_tree` logic.
3. Filter by tags using PostgreSQL's `@>` or `?&` JSONB operators.

---

#### Step 4: The Resolution Engine (The "Brain")

Implement the logic we finalized in the Canonical Spec. This should be a `@Service` that takes
`(assignmentUid, vocabularyUid, roleName)` and returns a `PartySetSpec`.

**The Precedence Logic Implementation:**

1. Query `assignment_party_binding` using jOOQ.
2. Apply the "Specific > Global > Template > Fallback" logic using a simple `Optional` chain in Java.
3. Pass the resulting `spec` to a `PartySetExecutor` that returns the actual `Party` objects.

---

#### Step 5: The "Manifest" & "Resolve" API

Build the Spring Boot `@RestController` to expose the endpoints we defined.

* **`GET /api/v1/context/manifest`**: Use Hibernate 2nd-level cache or Ehcache here. Since assignments and vocabularies
  don't change every second, caching the manifest will significantly speed up app boot times.
* **`GET /api/v1/parties/resolve`**: This endpoint will be called frequently as users type. Ensure you use jOOQ's
  `.limit()` and `.offset()` for the pagination.

---

#### Recommended Rollout Phases

| Phase       | Milestone          | Outcome                                                                              |
|-------------|--------------------|--------------------------------------------------------------------------------------|
| **Phase 1** | **Infrastructure** | Liquibase schema applied; ULID/UID utilities working; jOOQ code-gen completed.       |
| **Phase 2** | **The Registry**   | Able to CRUD Parties and perform "Org-Tree" recursive lookups.                       |
| **Phase 3** | **The Engine**     | The Resolver can correctly identify which `party_set` to use based on an Assignment. |
| **Phase 4** | **The API**        | A mobile client can fetch its Manifest and search for allowed Parties.               |

---

## My Recommendation: What to generate *first*

Start with the **Liquibase XML for the `party` and `party_set` tables**.

Once those tables exist, you can use jOOQ's code generation to create the Java classes. This prevents "Double-Entry"
where you have to update both a Java Entity and a SQL table.

**Would you like me to provide the Liquibase XML for the core tables, or should we look at the jOOQ implementation for
the Recursive Org-Tree query?**
