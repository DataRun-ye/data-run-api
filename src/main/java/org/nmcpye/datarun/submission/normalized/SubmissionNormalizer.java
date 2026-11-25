package org.nmcpye.datarun.submission.normalized;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Resolves a stored submission against its pinned form version into a {@link NormalizedSubmission}.
 *
 * <p>A pure function: it reads its arguments, changes neither, keeps no state and touches no database, so the feed
 * publisher and submission review's read model call it alike, in a transaction or outside one. The same arguments
 * always give an equal result.</p>
 *
 * <p><b>The walk</b> (the D feed spike's translation rules, measured on all 67,176 heads of the staging clone):</p>
 * <ul>
 *   <li>Each key of an object, from the root down, is resolved at its data path by the form's definitions
 *       ({@link FormDefinition}): the pinned version first, then the form's other versions, by the stored value's
 *       shape.</li>
 *   <li>An object at a plain section, or at a path no version defines, is a container: its keys are walked in turn.
 *       A non-empty array of objects at a repeating section is that section's rows; an empty one is zero rows.</li>
 *   <li>Anything resolving to a field is that field's value, kept whole. Anything else is unmapped, kept whole with
 *       its data path and the reason; nothing is dropped. A stored JSON null is no value.</li>
 *   <li>A row's {@code _id}, {@code _index}, {@code _parentId} and {@code _submissionUid}, and the root's
 *       {@code _status}, are structure, not values (the spike found every one consistent with the structure and the
 *       {@code status} column).</li>
 *   <li>A row is keyed by its stored {@code _id} exactly as stored; a row without one gets a derived id
 *       ({@link DerivedIds}). Its parent is the row it is nested in, through any plain sections between them.</li>
 * </ul>
 */
public final class SubmissionNormalizer {

    private static final Set<String> ROW_STRUCTURE_KEYS = Set.of("_id", "_index", "_parentId", "_submissionUid");
    private static final String ROOT_STRUCTURE_KEY = "_status";

    private SubmissionNormalizer() {
    }

    /**
     * Normalizes one stored submission.
     *
     * @param submission the stored submission
     * @param form       the definition of the submission's form, holding its pinned version
     * @throws IllegalArgumentException when the form has no version with the submission's pinned uid
     */
    public static NormalizedSubmission normalize(StoredSubmission submission, FormDefinition form) {
        FormVersionDefinition pinned = form.versionByUid(submission.formVersionUid())
            .orElseThrow(() -> new IllegalArgumentException("submission " + submission.id()
                + " is pinned to form version uid " + submission.formVersionUid() + ", not a version of form "
                + form.id()));
        Walk walk = new Walk(form, pinned, DerivedIds.submissionKey(submission.id()));
        Container top = new Container(null);
        JsonNode data = submission.formData();
        if (data != null && !data.isNull() && !data.isMissingNode()) {
            if (data.isObject()) {
                walk.members(data, null, top, null, false);
            } else {
                // the spike drops a non-object document; it is kept here, unmapped at the empty path (none on staging)
                top.unmapped.add(new UnmappedValue("", copy(data), UnmappedReason.PATH_NOT_IN_FORM));
            }
        }
        List<NormalizedSubmission.ValueKey> duplicateValueKeys = new ArrayList<>(top.duplicateKeys);
        for (Container container : walk.rowContainers) {
            duplicateValueKeys.addAll(container.duplicateKeys);
        }
        return new NormalizedSubmission(
            submission.id(),
            form.id(),
            pinned.id(),
            top.values,
            top.unmapped,
            walk.rows,
            List.copyOf(walk.duplicateRowIds),
            duplicateValueKeys);
    }

    private static JsonNode copy(JsonNode value) {
        return value.isContainerNode() ? value.deepCopy() : value;
    }

    /** One submission's walk. */
    private static final class Walk {

        private final FormDefinition form;
        private final FormVersionDefinition pinned;
        private final UUID submissionKey;
        private final List<NormalizedRow> rows = new ArrayList<>();
        private final List<Container> rowContainers = new ArrayList<>();
        private final Set<String> rowIds = new HashSet<>();
        private final Set<String> duplicateRowIds = new LinkedHashSet<>();

        Walk(FormDefinition form, FormVersionDefinition pinned, UUID submissionKey) {
            this.form = form;
            this.pinned = pinned;
            this.submissionKey = submissionKey;
        }

        /**
         * Walks an object's keys in a fixed order.
         *
         * @param object    the object
         * @param path      its data path, or {@code null} for the document's root
         * @param container where its values go: the enclosing row's, or the submission's
         * @param row       the enclosing row, or {@code null}
         * @param isRow     whether the object is a row (its structure keys are skipped)
         */
        void members(JsonNode object, String path, Container container, RowKey row, boolean isRow) {
            List<String> keys = new ArrayList<>(object.size());
            for (Iterator<String> names = object.fieldNames(); names.hasNext(); ) {
                keys.add(names.next());
            }
            keys.sort(null);
            for (String key : keys) {
                if (isRow ? ROW_STRUCTURE_KEYS.contains(key) : path == null && ROOT_STRUCTURE_KEY.equals(key)) {
                    continue;
                }
                node(object.get(key), path == null ? key : path + "." + key, container, row);
            }
        }

        private void node(JsonNode value, String path, Container container, RowKey row) {
            if (value == null || value.isNull()) {
                return;
            }
            StoredShape shape = StoredShape.of(value);
            FormDefinition.Resolution resolution = form.resolve(pinned, path, shape);
            if (shape == StoredShape.OBJECT && (resolution == null || resolution.isPlainSection())) {
                members(value, path, container, row, false);
            } else if (resolution != null && resolution.isRepeatingSection()
                && (shape == StoredShape.ROWS || shape == StoredShape.ANY)) {
                rows(value, path, resolution.section(), row);
            } else if (resolution != null && resolution.isField()) {
                FieldDefinition field = resolution.field();
                container.add(new FieldValue(
                    field.id(),
                    field.sectionId(),
                    path,
                    copy(value),
                    field.valueType(),
                    resolution.version().id(),
                    resolution.fromPinnedVersion()));
            } else {
                UnmappedReason reason = resolution == null ? UnmappedReason.PATH_NOT_IN_FORM
                    : resolution.isRepeatingSection() ? UnmappedReason.REPEATING_SECTION_HOLDS_NON_ROWS
                    : UnmappedReason.SECTION_HOLDS_VALUE;
                container.unmapped.add(new UnmappedValue(path, copy(value), reason));
            }
        }

        private void rows(JsonNode array, String sectionPath, SectionDefinition section, RowKey parent) {
            int index = 0;
            for (JsonNode element : array) {
                index++;
                RowKey key = rowKey(element.get("_id"), sectionPath, parent, index);
                if (!rowIds.add(key.id())) {
                    duplicateRowIds.add(key.id());
                }
                // the row goes before its nested rows: reserve its place, walk it, then fill the place
                int place = rows.size();
                rows.add(null);
                Container container = new Container(key.id());
                rowContainers.add(container);
                members(element, sectionPath, container, key, true);
                rows.set(place, new NormalizedRow(
                    key.id(),
                    key.kind(),
                    parent == null ? null : parent.id(),
                    section.id(),
                    sectionPath,
                    index,
                    container.values,
                    container.unmapped));
            }
        }

        private RowKey rowKey(JsonNode storedIdNode, String sectionPath, RowKey parent, int index) {
            String storedId = storedIdNode == null || storedIdNode.isNull() ? null
                : storedIdNode.isTextual() ? storedIdNode.textValue() : storedIdNode.toString();
            if (storedId == null || storedId.isEmpty()) {
                UUID derived = DerivedIds.derivedRowKey(
                    submissionKey, sectionPath, parent == null ? null : parent.key(), index);
                return new RowKey(derived.toString(), derived, RowKeyKind.DERIVED);
            }
            UUID ulid = DerivedIds.ulidToUuid(storedId);
            return ulid != null
                ? new RowKey(storedId, ulid, RowKeyKind.CLIENT)
                : new RowKey(storedId, DerivedIds.legacyRowKey(submissionKey, storedId), RowKeyKind.LEGACY);
        }
    }

    /** A row's id, and the key a derived id of a row nested in it is computed from. */
    private record RowKey(String id, UUID key, RowKeyKind kind) {
    }

    /** The values of one row, or of the submission outside every row. */
    private static final class Container {

        private final String rowId;
        private final List<FieldValue> values = new ArrayList<>();
        private final List<UnmappedValue> unmapped = new ArrayList<>();
        private final Set<NormalizedSubmission.ValueKey> keys = new HashSet<>();
        private final Set<NormalizedSubmission.ValueKey> duplicateKeys = new LinkedHashSet<>();

        Container(String rowId) {
            this.rowId = rowId;
        }

        void add(FieldValue value) {
            NormalizedSubmission.ValueKey key =
                new NormalizedSubmission.ValueKey(rowId, value.sectionId(), value.fieldId());
            if (!keys.add(key)) {
                duplicateKeys.add(key);
            }
            values.add(value);
        }
    }
}
