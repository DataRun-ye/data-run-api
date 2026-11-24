package org.nmcpye.datarun.characterization.a2;

import com.fasterxml.jackson.databind.JsonNode;
import org.nmcpye.datarun.characterization.a2.A2Payloads.Group;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * What installed phones need from the server's answers, checked on every golden response of the matching
 * generation, so that a re-recorded golden cannot quietly break a phone. Mobile code is cited as
 * {@code m@<tag> path:lines}: the data-run-mobile repository at that release tag.
 * <p>
 * Upload responses (2xx):
 * <ul>
 *   <li>M1, G1 to G3 (m@v6.1.2 lib/core/sync/sync_summary_model.dart:20-63; data_submissions_dao.dart:46-82): a
 *       JSON object; created and updated absent or lists of strings; failed absent or an object with string keys;
 *       every attempted uid in one of the three, or the row is failed as a bad response.</li>
 *   <li>M0, G0 (m@v5.3.0+20 packages/drun_sdk/lib/core/sync/sync_summary_model.dart:15-35;
 *       packages/drun_sdk/lib/database/dao/data_submissions_dao.dart:66-153,743-757): created, updated and failed
 *       all present and non-null, because the model's fields are non-nullable and a missing one throws inside the
 *       upload's try, whose catch fails every row of the batch, rows the server saved included; created and updated
 *       lists of strings; failed an object whose values are non-null strings (a null value leaves the row in
 *       "uploading", another type throws as above); every attempted uid answered, since a missing one also stays in
 *       "uploading", and G0 never offers such a row again. The model would also accept each of the three as a
 *       string holding JSON; the server has never sent that, so it is not accepted here.</li>
 * </ul>
 * Phone reads are in {@link #g0SubmissionPull()}, {@link #referenceCatalogPage(int, String)} and
 * {@link #assignmentForms(boolean, Set)}.
 */
final class A2PhoneContract {

    /** The Reference uid G2 and G3 accept from the catalog (m@v6.1.2 lib/data/reference_uid.dart:4-7). */
    private static final Pattern REFERENCE_UID = Pattern.compile("^[a-zA-Z][a-zA-Z0-9]{10}$");

    /** The largest catalog page G2 and G3 accept (m@v6.1.2 lib/data/reference_entry_repository.dart:10). */
    private static final int CATALOG_PAGE_SIZE = 500;

    /**
     * The keys G0's {@code DataInstance.fromJson} needs non-null and its pull mapper does not supply
     * (m@v5.3.0+20 packages/drun_sdk/lib/database/app_database.g.dart:6783-6807;
     * packages/drun_sdk/lib/datasource/remote_data_sources/data_submission_datasource.dart:20-38).
     */
    private static final List<String> G0_ROW_KEYS =
        List.of("formTemplate", "templateVersion", "syncState", "deleted", "startEntryTime");

    private static final List<String> ASSIGNMENT_FORM_FLAGS =
        List.of("canAddSubmissions", "canViewSubmissions", "canEditSubmissions", "canDeleteSubmissions");

    private A2PhoneContract() {
    }

    /** A phone read: the generations that make it, and what they need from its answer. */
    record Read(String phones, ReadCheck check) {
    }

    @FunctionalInterface
    interface ReadCheck {
        List<String> violations(int status, JsonNode body);
    }

    /** M1 or M0 for an upload answer. */
    static List<String> violations(Group group, JsonNode requestBody, int status, JsonNode body) {
        List<String> problems = new ArrayList<>();
        if (status >= 400) {
            return problems; // the phone fails the whole batch; nothing more to check
        }
        if (body == null || !body.isObject()) {
            problems.add("the body is not a JSON object");
            return problems;
        }
        if (group == Group.G0) {
            for (String key : List.of("created", "updated", "failed")) {
                if (!body.hasNonNull(key)) {
                    problems.add("G0 needs " + key + " present and non-null, or the whole batch fails");
                }
            }
        }
        Set<String> answered = new HashSet<>();
        for (String list : List.of("created", "updated")) {
            JsonNode ids = body.get(list);
            if (ids == null || ids.isNull()) {
                continue;
            }
            if (!ids.isArray()) {
                problems.add(list + " is not a list");
                continue;
            }
            ids.forEach(id -> {
                if (!id.isTextual()) {
                    problems.add(list + " holds a non-string " + id);
                } else {
                    answered.add(id.asText());
                }
            });
        }
        JsonNode failed = body.get("failed");
        if (failed != null && !failed.isNull()) {
            if (!failed.isObject()) {
                problems.add("failed is not an object");
            } else {
                Iterator<String> keys = failed.fieldNames();
                while (keys.hasNext()) {
                    String key = keys.next();
                    answered.add(key);
                    JsonNode value = failed.get(key);
                    if (group == Group.G0 && (value == null || !value.isTextual())) {
                        problems.add("G0 needs a string failure for " + key + ", got " + value);
                    }
                }
            }
        }
        if (requestBody != null && requestBody.isArray()) {
            for (JsonNode item : requestBody) {
                JsonNode uid = item.get("uid");
                if (uid != null && uid.isTextual() && !answered.contains(uid.asText())) {
                    problems.add("attempted uid " + uid.asText() + " is missing from the response");
                }
            }
        }
        return problems;
    }

    /**
     * G0's submission pull ({@code GET dataSubmission/objects?paged=false}, on every metadata sync) must stay inert:
     * any item G0 could build a row from would overwrite its local copy as synced
     * (m@v5.3.0+20 packages/drun_sdk/lib/datasource/base_datasource.dart:40-62,87-116,171-208). A failed request
     * is inert: G0 catches it and stores nothing. So is an item that lacks any of {@link #G0_ROW_KEYS}: G0 logs it
     * as a mapping error and skips it.
     */
    static Read g0SubmissionPull() {
        return new Read("G0", (status, body) -> {
            List<String> problems = new ArrayList<>();
            if (status < 200 || status >= 300 || body == null || !body.isObject()) {
                return problems;
            }
            JsonNode items = body.get("dataSubmission");
            if (items == null || !items.isArray()) {
                return problems;
            }
            for (JsonNode item : items) {
                if (item.isObject() && G0_ROW_KEYS.stream().allMatch(item::hasNonNull)) {
                    problems.add("G0 would store item " + item.path("uid").asText() + " over its local copy");
                }
            }
            return problems;
        });
    }

    /**
     * One page of an assignment's Reference catalog as G2 and G3 walk it
     * (m@v6.1.2 lib/datasource/remote_data_sources/reference_entry_datasource.dart:61-91,171-202,246-270;
     * lib/data/reference_entry_repository.dart:82-104; the same rules in v6.0.2). Anything else fails the
     * catalog sync. An empty catalog is {@code totalPages} 0 with no entries.
     */
    static Read referenceCatalogPage(int requestedPage, String orgUnitUid) {
        return new Read("G2, G3", (status, body) -> {
            List<String> problems = new ArrayList<>();
            if (status < 200 || status >= 300) {
                problems.add("status " + status + " fails the catalog sync");
                return problems;
            }
            if (body == null || !body.isObject()) {
                problems.add("the body is not a JSON object");
                return problems;
            }
            JsonNode paged = body.get("paged");
            JsonNode page = body.get("page");
            JsonNode totalPages = body.get("totalPages");
            JsonNode entries = body.get("referenceEntries");
            if (paged == null || !paged.isBoolean() || page == null || !page.isIntegralNumber()
                || totalPages == null || !totalPages.isIntegralNumber() || entries == null || !entries.isArray()) {
                problems.add("needed: paged (a boolean), page and totalPages (integers), referenceEntries (a list)");
                return problems;
            }
            long total = totalPages.asLong();
            if (!paged.asBoolean() || page.asLong() != requestedPage || total < 0
                || (total == 0 && !entries.isEmpty()) || (total > 0 && page.asLong() >= total)
                || entries.size() > CATALOG_PAGE_SIZE) {
                problems.add("page metadata the phone refuses: paged " + paged + ", page " + page + " (asked "
                    + requestedPage + "), totalPages " + totalPages + ", " + entries.size() + " entries");
            }
            for (JsonNode entry : entries) {
                JsonNode uid = entry.get("uid");
                JsonNode orgUnit = entry.get("orgUnitUid");
                JsonNode name = entry.get("name");
                if (!entry.isObject() || uid == null || !uid.isTextual() || orgUnit == null || !orgUnit.isTextual()
                    || name == null || !name.isTextual() || name.asText().strip().isEmpty()) {
                    problems.add("an incomplete entry: " + entry);
                } else if (!REFERENCE_UID.matcher(uid.asText()).matches()) {
                    problems.add("an entry uid the phone refuses: " + uid.asText());
                } else if (!orgUnit.asText().equals(orgUnitUid)) {
                    problems.add("entry " + uid.asText() + " is in " + orgUnit.asText() + ", not " + orgUnitUid);
                }
            }
            return problems;
        });
    }

    /**
     * The assignment-form flags every generation reads on each sync, G0 and G1 without {@code referenceVersion},
     * G2 and G3 with {@code referenceVersion=1} (m@v5.3.0+20
     * packages/drun_sdk/lib/datasource/remote_data_sources/assignment_datasource.dart:62-126; m@v6.1.2
     * lib/datasource/remote_data_sources/assignment_datasource.dart:63-131; the generated
     * {@code AssignmentForm.fromJson}, app_database.g.dart:5232-5246 in both). A violation fails the assignment
     * sync. Without {@code referenceVersion=1} the server also keeps out forms with a Reference field, which the
     * apps before 6.0.2 cannot open.
     */
    static Read assignmentForms(boolean referenceVersion, Set<String> referenceForms) {
        return new Read(referenceVersion ? "G2, G3" : "G0, G1", (status, body) -> {
            List<String> problems = new ArrayList<>();
            if (status < 200 || status >= 300) {
                problems.add("status " + status + " fails the assignment sync");
                return problems;
            }
            if (body == null || !body.isObject()) {
                problems.add("the body is not a JSON object");
                return problems;
            }
            JsonNode assignments = body.get("assignments");
            if (assignments == null || assignments.isNull()) {
                return problems;
            }
            if (!assignments.isArray()) {
                problems.add("assignments is not a list");
                return problems;
            }
            for (JsonNode assignment : assignments) {
                if (!assignment.isObject()) {
                    problems.add("an assignment that is not an object: " + assignment);
                    continue;
                }
                for (String key : List.of("id", "activity", "team", "orgUnit")) {
                    if (!assignment.path(key).isTextual()) {
                        problems.add("assignment " + assignment.path("id") + " needs a string " + key);
                    }
                }
                JsonNode progress = assignment.get("progressStatus");
                if (progress != null && !progress.isNull() && !progress.isTextual()) {
                    problems.add("assignment " + assignment.path("id") + " has a non-string progressStatus");
                }
                JsonNode forms = assignment.get("accessibleForms");
                if (forms == null || forms.isNull()) {
                    continue;
                }
                if (!forms.isArray()) {
                    problems.add("accessibleForms of " + assignment.path("id") + " is not a list");
                    continue;
                }
                for (JsonNode form : forms) {
                    if (!form.isObject() || !form.path("form").isTextual() || !form.path("assignment").isTextual()) {
                        problems.add("an accessible form needs string form and assignment: " + form);
                        continue;
                    }
                    for (String flag : ASSIGNMENT_FORM_FLAGS) {
                        JsonNode value = form.get(flag);
                        if (value != null && !value.isNull() && !value.isBoolean()) {
                            problems.add(flag + " of " + form.path("form").asText() + " is not a boolean");
                        }
                    }
                    if (!referenceVersion && referenceForms.contains(form.path("form").asText())) {
                        problems.add("Reference form " + form.path("form").asText() + " offered to G0 or G1");
                    }
                }
            }
            return problems;
        });
    }
}
