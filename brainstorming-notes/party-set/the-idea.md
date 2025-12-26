**"Party"** is any entity involved in a process (a warehouse, a clinic, a staff member, or a vendor).

The goal of this is to stop hardcoding lists into forms and instead use a flexible "Rule Engine" to decide who a user
can see and select.

---

## 1. Key Terminology

To understand the proposal, you first need to define these core concepts:

* **Party:** The "Who" or "Where." It is a generic object (e.g., Warehouse A, Clinic B).
* **PartySet:** A **Rule** or **Policy**. Instead of saying "Show Warehouse A," a PartySet says "Show all warehouses in
  the North District."
* **Assignment:** The **Context**. This represents a specific task given to a user (e.g., "Inventory Audit for December
  in District 5").
* **Role:** The **Function**. In a transfer, you have a "From" role and a "To" role. Different rules might apply to
  each.
* **Vocabulary:** This refers to the **Form Template**. It’s the set of questions or fields being filled out.
* **Binding:** The **Link**. This is the data record that connects a specific Assignment to a specific PartySet.

---

## 2. The Relationship Model

The core idea is that the **Template** stays simple, while the **Assignment** does the heavy lifting of filtering.

### How the Data Tables Work:

1. **`party`**: Your master list of all locations/people. it might be entities in the system (team, orgUnit, user, etc) or external
2. **`party_set`**: The "Folder" or "Filter" definition.
3. **`assignment_party_binding`**: The configuration table. It says: *"For Assignment X, when using the 'Transfer' form,
   the 'From' field should only show parties in the 'Warehouse' PartySet."*

---

## 3. The Six "Kinds" of PartySets (Rules)

The text lists different ways to define a group of parties. Here is what they mean in plain English:

| Kind                  | How it works              | Example                                                      |
|-----------------------|---------------------------|--------------------------------------------------------------|
| **Static**            | A hand-picked list.       | "Only these 3 specific VIP clinics."                         |
| **Org Tree**          | Uses a hierarchy.         | "The District Office and everything below it."               |
| **Tag Filter**        | Uses "labels" on parties. | "Show everything tagged as #ColdChain."                      |
| **Query**             | A dynamic search.         | "All warehouses that have more than 100 vaccines in stock."  |
| **Assignment Scoped** | Automatic filtering.      | "Only show parties that were already assigned to this user." |
| **External**          | Asks another system.      | "Check the HR database for a list of active drivers."        |

---

## 4. Understanding the Logic (Precedence)

The text mentions a "Precedence" or "Resolution" logic. This is the order of operations the app uses to decide which
list to show the user:

1. **Check Assignment Bindings:** Does this specific task have a custom rule? (Most Specific)
2. **Check Template:** Does the form itself have a hardcoded rule?
3. **Default:** If nothing else is found, use the "Assignment Scoped" rule (show what the user usually has access to).

---

## 5. Practical Example: A Vaccine Transfer

Imagine a user is using a **"Vaccine Transfer"** form (the Vocabulary).

* **The Problem:** You don't want the user to accidentally send vaccines from a "Clinic" to a "Warehouse" (it should be
  Warehouse → Clinic).
* **The Solution (The "Binding"):**
* For the **"From" Role**, bind a PartySet that only includes "Warehouses."
* For the **"To" Role**, bind a PartySet that only includes "Clinics" with "Cold Storage" tags.


* **The Result:** The user opens the "From" dropdown and only sees their warehouse. They open the "To" dropdown and only
  see valid clinics.

---

## 6. Technical Implementation Details

* **Decoupling:** By not "baking" the list into the template, you can change the list of clinics without having to
  redesign the form.
* **Offline Mode:** Since the server tells the app which "PartySet" to use, the app can download that specific list and
  save it (cache it) so the user can work without internet.
* **Configurator UX:** This refers to the screen where an admin (not a coder) sets these rules up using dropdowns and
  checkboxes instead of writing SQL code.
