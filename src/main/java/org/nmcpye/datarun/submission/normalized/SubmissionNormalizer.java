package org.nmcpye.datarun.submission.normalized;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Resolves a stored submission against its pinned form version into a {@link NormalizedSubmission}.
 *
 * <p>A pure function: it reads its arguments, changes neither, keeps no state and touches no database, so the feed
 * publisher and submission review's read model call it alike, in a transaction or outside one. The same arguments
 * always give an equal result.</p>
 *
 * <p><b>It never fails on data.</b> Its callers run it inside transactions, where one exception would retry a
 * change until it dies. Whatever the stored document and the stored definitions hold, it returns a result, and
 * counts what it met in {@link NormalizedSubmission#flags()}: a document that is not JSON, a missing form or pinned
 * version, values no field keys (by reason), values that do not parse as their type, ids and fields used twice,
 * nesting deeper than {@link #MAX_DEPTH}. Only a programming error (a null submission) throws.</p>
 *
 * <p><b>Numbers are exact.</b> It parses the stored JSON text itself ({@link StoredJson}): a decimal keeps every digit
 * and its scale, and is never a binary float. The parsed tree is its own, so each value is a node of it, never copied;
 * values never share nodes. Parsing is iterative and the walk stops at {@link #MAX_DEPTH}, so no document, however
 * deep, can exhaust the stack.</p>
 *
 * <p><b>The walk:</b></p>
 * <ul>
 *   <li>Each key of an object, from the root down, is resolved at its data path by the form's definitions
 *       ({@link FormDefinition}): the pinned version first, then the form's other versions, by the stored value's
 *       shape.</li>
 *   <li>An object at a plain section, or at a path no version defines, is a container: its keys are walked in turn.
 *       A non-empty array of objects at a repeating section is that section's rows; an empty one is zero rows.</li>
 *   <li>Anything resolving to a field is that field's value, kept whole. Anything else is unmapped, kept whole with
 *       its data path and the reason; nothing is dropped. A stored JSON null is no value.</li>
 *   <li>A row's {@code _id}, {@code _index}, {@code _parentId} and {@code _submissionUid}, and the root's
 *       {@code _status}, are structure, not values: they repeat what the structure and the {@code status} column
 *       already say.</li>
 *   <li>A row is keyed by its stored {@code _id} exactly as stored; a row without one gets a derived id, hashed from
 *       the submission's id, its section path, its parent row's id and its index ({@link DerivedIds}). Its parent is
 *       the row it is nested in, through any plain sections between them.</li>
 * </ul>
 *
 * <p><b>Ids and fields used twice</b> resolve by fixed rules, so keys stay unique:</p>
 * <ul>
 *   <li>A row whose id an earlier row of the submission already has, in walk order (arrays in stored order,
 *       sections in key order), keeps its stored id in {@link NormalizedRow#storedId()} but is keyed by its
 *       position-derived id ({@link RowIdOrigin#DERIVED}, {@link DerivedIds#duplicateRowId}), flagged
 *       {@link NormalizationFlag#DUPLICATE_ID}. The first row keeps the id: it is the one a phone or a reader most
 *       likely already knows by it.</li>
 *   <li>Two stored paths of one row, or of the submission outside every row, that resolve to the same field, in the
 *       same section or not (a field placed in two sections, a section renamed with both names stored): the value
 *       resolved by the pinned version's own definition stays, otherwise the first in walk order; the other is
 *       unmapped whole at its path ({@link UnmappedReason#FIELD_ALREADY_HAS_VALUE}). Consumers key a value by its
 *       row and field id, so one row holds one value per field; the section stays an attribute of the value.</li>
 * </ul>
 */
public final class SubmissionNormalizer {

    /**
     * How many objects and rows deep the walk goes. Below that, a container is kept whole as unmapped
     * ({@link UnmappedReason#NESTED_TOO_DEEP}), so no document can exhaust the stack. Stored documents nest a few
     * levels.
     */
    public static final int MAX_DEPTH = 32;

    private static final Set<String> ROW_STRUCTURE_KEYS = Set.of("_id", "_index", "_parentId", "_submissionUid");
    private static final String ROOT_STRUCTURE_KEY = "_status";

    private SubmissionNormalizer() {
    }

    /**
     * Normalizes one stored submission.
     *
     * @param submission the stored submission
     * @param form       the definition of the submission's form, or {@code null} when none is stored; then every value
     *                   is unmapped and the result is flagged {@link NormalizationFlag#FORM_MISSING}
     */
    public static NormalizedSubmission normalize(StoredSubmission submission, FormDefinition form) {
        Objects.requireNonNull(submission, "submission");
        Map<NormalizationFlag, Integer> flags = new EnumMap<>(NormalizationFlag.class);
        FormDefinition definition = form == null ? FormDefinition.NO_VERSIONS : form;
        if (form == null) {
            flags.put(NormalizationFlag.FORM_MISSING, 1);
        }
        FormVersionDefinition pinned = definition.versionByUid(submission.formVersionUid()).orElse(null);
        if (pinned == null) {
            flags.put(NormalizationFlag.PINNED_VERSION_MISSING, 1);
        }
        Walk walk = new Walk(definition, pinned, submission.id(), flags);
        Container top = new Container();
        JsonNode data = parse(submission.formData(), walk, top);
        if (data != null && !data.isNull()) {
            if (data.isObject()) {
                walk.members(data, null, top, null, false, 0);
            } else {
                walk.unmapped(top, "", data, UnmappedReason.DOCUMENT_NOT_AN_OBJECT);
            }
        }
        int issues = countIssues(top.values);
        for (NormalizedRow row : walk.rows) {
            issues += countIssues(row.values());
        }
        if (issues > 0) {
            flags.put(NormalizationFlag.VALUE_ISSUE, issues);
        }
        return new NormalizedSubmission(
            submission.id(),
            form == null ? null : form.id(),
            pinned == null ? null : pinned.id(),
            top.values,
            top.unmapped,
            walk.rows,
            flags);
    }

    /** The parsed document, or {@code null} when there is none or it is not JSON (then kept whole as text). */
    private static JsonNode parse(String text, Walk walk, Container top) {
        if (text == null) {
            return null;
        }
        try {
            return StoredJson.parse(text);
        } catch (JsonProcessingException notJson) {
            walk.unmapped(top, "", TextNode.valueOf(text), UnmappedReason.DOCUMENT_NOT_PARSED);
            return null;
        }
    }

    private static int countIssues(List<FieldValue> values) {
        int issues = 0;
        for (FieldValue value : values) {
            if (value.issue() != null) {
                issues++;
            }
        }
        return issues;
    }

    /** One submission's walk. */
    private static final class Walk {

        private final FormDefinition form;
        private final FormVersionDefinition pinned;
        private final String submissionId;
        private final Map<NormalizationFlag, Integer> flags;
        private final List<NormalizedRow> rows = new ArrayList<>();
        private final Set<String> rowIds = new HashSet<>();

        Walk(FormDefinition form, FormVersionDefinition pinned, String submissionId,
             Map<NormalizationFlag, Integer> flags) {
            this.form = form;
            this.pinned = pinned;
            this.submissionId = submissionId;
            this.flags = flags;
        }

        /**
         * Walks an object's keys in a fixed order.
         *
         * @param object    the object
         * @param path      its data path, or {@code null} for the document's root
         * @param container where its values go: the enclosing row's, or the submission's
         * @param row       the enclosing row, or {@code null}
         * @param isRow     whether the object is a row (its structure keys are skipped)
         * @param depth     how many objects and rows enclose its keys' values, itself included
         */
        void members(JsonNode object, String path, Container container, RowKey row, boolean isRow, int depth) {
            List<String> keys = new ArrayList<>(object.size());
            for (Iterator<String> names = object.fieldNames(); names.hasNext(); ) {
                keys.add(names.next());
            }
            keys.sort(null);
            for (String key : keys) {
                if (isRow ? ROW_STRUCTURE_KEYS.contains(key) : path == null && ROOT_STRUCTURE_KEY.equals(key)) {
                    continue;
                }
                node(object.get(key), path == null ? key : path + "." + key, container, row, depth);
            }
        }

        private void node(JsonNode value, String path, Container container, RowKey row, int depth) {
            if (value == null || value.isNull()) {
                return;
            }
            StoredShape shape = StoredShape.of(value);
            FormDefinition.Resolution resolution = form.resolve(pinned, path, shape);
            if (resolution != null && form.isAmbiguous(path)) {
                flags.merge(NormalizationFlag.AMBIGUOUS_PATH, 1, Integer::sum);
            }
            boolean walksObject = shape == StoredShape.OBJECT && (resolution == null || resolution.isPlainSection());
            boolean walksRows = resolution != null && resolution.isRepeatingSection()
                && (shape == StoredShape.ROWS || shape == StoredShape.ANY);
            if ((walksObject || walksRows) && depth >= MAX_DEPTH) {
                unmapped(container, path, value, UnmappedReason.NESTED_TOO_DEEP);
            } else if (walksObject) {
                members(value, path, container, row, false, depth + 1);
            } else if (walksRows) {
                rows(value, path, resolution.section(), row, depth + 1);
            } else if (resolution != null && resolution.isField()) {
                FieldDefinition field = resolution.field();
                if (field.id() == null) {
                    unmapped(container, path, value, UnmappedReason.FIELD_WITHOUT_ID);
                } else {
                    value(container, new FieldValue(
                        field.id(),
                        field.sectionId(),
                        path,
                        value,
                        field.valueType(),
                        ValueConformance.check(field.valueType(), value),
                        resolution.version().id(),
                        resolution.fromPinnedVersion()));
                }
            } else {
                unmapped(container, path, value, resolution == null ? UnmappedReason.PATH_NOT_IN_FORM
                    : resolution.isRepeatingSection() ? UnmappedReason.REPEATING_SECTION_HOLDS_NON_ROWS
                    : UnmappedReason.SECTION_HOLDS_VALUE);
            }
        }

        /** Adds a field's value, unless the container already has one for that field, in any section. */
        private void value(Container container, FieldValue value) {
            Integer taken = container.positions.putIfAbsent(value.fieldId(), container.values.size());
            if (taken == null) {
                container.values.add(value);
                return;
            }
            FieldValue kept = container.values.get(taken);
            FieldValue other = value;
            if (value.fromPinnedVersion() && !kept.fromPinnedVersion()) {
                container.values.set(taken, value);
                other = kept;
            }
            unmapped(container, other.path(), other.value(), UnmappedReason.FIELD_ALREADY_HAS_VALUE);
        }

        /** Keeps a value no field keys, whole. */
        void unmapped(Container container, String path, JsonNode value, UnmappedReason reason) {
            flags.merge(NormalizationFlag.of(reason), 1, Integer::sum);
            container.unmapped.add(new UnmappedValue(path, value, reason));
        }

        private void rows(JsonNode array, String sectionPath, SectionDefinition section, RowKey parent, int depth) {
            int index = 0;
            for (JsonNode element : array) {
                index++;
                RowKey key = rowKey(element.get("_id"), sectionPath, parent, index);
                if (!rowIds.add(key.id())) {
                    key = duplicateKey(key.storedId(), sectionPath, parent, index);
                    rowIds.add(key.id());
                    flags.merge(NormalizationFlag.DUPLICATE_ID, 1, Integer::sum);
                }
                // the row goes before its nested rows: reserve its place, walk it, then fill the place
                int place = rows.size();
                rows.add(null);
                Container container = new Container();
                members(element, sectionPath, container, key, true, depth);
                rows.set(place, new NormalizedRow(
                    key.id(),
                    key.origin(),
                    key.storedId(),
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
                return new RowKey(DerivedIds.derivedRowId(submissionId, sectionPath, parent == null ? null : parent.id(),
                    index), RowIdOrigin.DERIVED, null);
            }
            return new RowKey(storedId, RowIdOrigin.STORED, storedId);
        }

        /** The first free id of {@link DerivedIds#duplicateRowId}'s attempts. */
        private RowKey duplicateKey(String storedId, String sectionPath, RowKey parent, int index) {
            String parentId = parent == null ? null : parent.id();
            for (int attempt = 1; ; attempt++) {
                String id = DerivedIds.duplicateRowId(submissionId, sectionPath, parentId, index, attempt);
                if (!rowIds.contains(id)) {
                    return new RowKey(id, RowIdOrigin.DERIVED, storedId);
                }
            }
        }
    }

    /** A row's id, where it comes from, and its stored id. */
    private record RowKey(String id, RowIdOrigin origin, String storedId) {
    }

    /** The values of one row, or of the submission outside every row. */
    private static final class Container {

        private final List<FieldValue> values = new ArrayList<>();
        private final List<UnmappedValue> unmapped = new ArrayList<>();
        /** Field id to the position of its value: one value per field in a row, or outside every row. */
        private final Map<String, Integer> positions = new HashMap<>();
    }
}
