package org.nmcpye.datarun.submission.normalized;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.nmcpye.datarun.submission.normalized.StoredForms.version;

/**
 * No stored document makes the normalizer fail: seeded random documents, built from the form's own keys, structure
 * keys and stray keys, with every JSON type in every place, against a form of two versions, a pinned version that
 * exists, one that is gone and none, and no stored definition at all. Each result is checked for determinism, an
 * unchanged input, unique keys, and flags that agree with what the lists hold.
 */
class SubmissionNormalizerRobustnessTest {

    private static final String FORM = "5f2a9c0b1d3e4f5061728394";
    private static final String SUBMISSION = "01J2Q7Y0V6M3N4P5R6S7T8V9WX";

    private static final FormVersionDefinition V1 = version("01JV0000000000000000000001", "fvUid000001", 1)
        .section("details", false)
        .section("items", false)
        .field("deUidOldNote", "details", "note", "Text")
        .field("deUidDay0001", "details", "old_day", "Date")
        .field("deUidCount01", "items", "count", "Integer")
        .build(FORM);

    private static final FormVersionDefinition V2 = version("01JV0000000000000000000002", "fvUid000002", 2)
        .section("details", false)
        .section("items", true)
        .section("items.parts", true)
        .section("items.extra", false)
        .field("deUidTitle01", null, "title", "Text")
        .field("deUidDay0001", "details", "day", "Date")
        .field("deUidMulti01", "details", "choices", "SelectMulti")
        .field("deUidCount01", "items", "count", "Integer")
        .field("deUidPartQty", "items.parts", "qty", "IntegerPositive")
        .field("deUidKind001", "items.extra", "kind", "SelectOne")
        .fieldWithoutDataElement("deUidGone001", "details", "note", "Text")
        .build(FORM);

    private static final FormDefinition FORM_DEFINITION = FormDefinition.of(FORM, List.of(V1, V2));

    private static final String[] KEYS = {"title", "details", "day", "old_day", "choices", "note", "items", "count", "parts",
        "qty", "extra", "kind", "_id", "_index", "_parentId", "_submissionUid", "_status", "stray", "", "x.y"};
    private static final String[] IDS = {"01J2Q7Y0V6M3N4P5R6S7T8V9W1", "01j2q7y0v6m3n4p5r6s7t8v9w1",
        "01J2Q7Y0V6M3N4P5R6S7T8V9W2", "aB3dE5fG7hJ9kL1m", ""};
    private static final String[] PINS = {"fvUid000002", "fvUid000001", "fvUidGone00", null};

    private static final JsonNodeFactory NODES = JsonNodeFactory.instance;

    @Test
    void noStoredDocumentMakesItFail() {
        Random random = new Random(20261125);
        Set<NormalizationFlag> seen = EnumSet.noneOf(NormalizationFlag.class);
        for (int i = 0; i < 4_000; i++) {
            JsonNode document = i % 50 == 0 ? scalar(random) : object(random, 0);
            if (document.isObject() && random.nextBoolean()) {
                // half the documents hold rows where the form has a repeating section
                ((ObjectNode) document).set("items", rows(random, 1));
            }
            String pin = PINS[random.nextInt(PINS.length)];
            FormDefinition form = random.nextInt(12) == 0 ? null : FORM_DEFINITION;
            JsonNode before = document.deepCopy();

            NormalizedSubmission normalized =
                SubmissionNormalizer.normalize(new StoredSubmission(SUBMISSION, pin, document), form);

            assertThat(SubmissionNormalizer.normalize(new StoredSubmission(SUBMISSION, pin, document), form))
                .as("deterministic").isEqualTo(normalized);
            assertThat(document).as("unchanged").isEqualTo(before);
            flagsAgreeWithTheLists(normalized, form, pin);
            seen.addAll(normalized.flags().keySet());
        }
        // the documents reached every condition but depth, which nestingDeeperThanTheWalkGoesIsKeptWholeAndFlagged
        // covers
        assertThat(seen).containsAll(EnumSet.complementOf(EnumSet.of(NormalizationFlag.NESTED_TOO_DEEP)));
    }

    private static void flagsAgreeWithTheLists(NormalizedSubmission normalized, FormDefinition form, String pin) {
        List<UnmappedValue> unmapped = new ArrayList<>(normalized.unmapped());
        List<FieldValue> values = new ArrayList<>(normalized.values());
        for (NormalizedRow row : normalized.rows()) {
            unmapped.addAll(row.unmapped());
            values.addAll(row.values());
        }
        for (UnmappedReason reason : UnmappedReason.values()) {
            assertThat(normalized.flagged(NormalizationFlag.of(reason))).as(reason.name())
                .isEqualTo((int) unmapped.stream().filter(u -> u.reason() == reason).count());
        }
        assertThat(normalized.flagged(NormalizationFlag.VALUE_ISSUE))
            .isEqualTo((int) values.stream().filter(v -> v.issue() != null).count());
        assertThat(normalized.flagged(NormalizationFlag.FORM_MISSING)).isEqualTo(form == null ? 1 : 0);
        boolean pinned = form != null && form.versionByUid(pin).isPresent();
        assertThat(normalized.flagged(NormalizationFlag.PINNED_VERSION_MISSING)).isEqualTo(pinned ? 0 : 1);
        assertThat(normalized.flags().values()).allSatisfy(count -> assertThat(count).isPositive());
        assertThat(values).allSatisfy(v -> assertThat(v.value().isNull()).isFalse());
        assertThat(unmapped).allSatisfy(u -> assertThat(u.value().isNull()).isFalse());
        // keys are unique: row ids within the submission, (section, field) within each row and outside every row
        assertThat(normalized.rows()).extracting(NormalizedRow::id).doesNotHaveDuplicates();
        assertThat(normalized.values()).extracting(v -> v.sectionId() + "|" + v.fieldId()).doesNotHaveDuplicates();
        for (NormalizedRow row : normalized.rows()) {
            assertThat(row.values()).extracting(v -> v.sectionId() + "|" + v.fieldId()).doesNotHaveDuplicates();
        }
        assertThat(normalized.flagged(NormalizationFlag.DUPLICATE_ID)).isEqualTo((int) normalized.rows().stream()
            .filter(r -> r.keyKind() == RowKeyKind.DUPLICATE_ID).count());
    }

    private static JsonNode any(Random random, int depth) {
        int pick = random.nextInt(depth > 5 ? 6 : 10);
        return switch (pick) {
            case 6, 7 -> object(random, depth + 1);
            case 8, 9 -> array(random, depth + 1);
            default -> scalar(random);
        };
    }

    private static ObjectNode object(Random random, int depth) {
        ObjectNode object = NODES.objectNode();
        int size = random.nextInt(6);
        for (int i = 0; i < size; i++) {
            String key = KEYS[random.nextInt(KEYS.length)];
            object.set(key, "_id".equals(key) && random.nextBoolean()
                ? NODES.textNode(IDS[random.nextInt(IDS.length)]) : any(random, depth));
        }
        return object;
    }

    private static ArrayNode rows(Random random, int depth) {
        ArrayNode rows = NODES.arrayNode();
        int size = 1 + random.nextInt(4);
        for (int i = 0; i < size; i++) {
            ObjectNode row = object(random, depth);
            if (random.nextInt(5) < 3) {
                row.put("_id", IDS[random.nextInt(IDS.length)]);
            }
            if (random.nextInt(4) == 0) {
                row.putObject("extra").set("parts", depth > 3 ? NODES.arrayNode() : rows(random, depth + 2));
            }
            rows.add(row);
        }
        return rows;
    }

    private static ArrayNode array(Random random, int depth) {
        ArrayNode array = NODES.arrayNode();
        int size = random.nextInt(4);
        boolean rows = random.nextInt(3) > 0;
        for (int i = 0; i < size; i++) {
            if (rows) {
                ObjectNode row = object(random, depth);
                if (random.nextInt(5) < 3) {
                    // rows mostly carry an id, from a small pool, so ids repeat
                    row.put("_id", IDS[random.nextInt(IDS.length)]);
                }
                array.add(row);
            } else {
                array.add(any(random, depth));
            }
        }
        return array;
    }

    private static JsonNode scalar(Random random) {
        return switch (random.nextInt(8)) {
            case 0 -> NODES.nullNode();
            case 1 -> NODES.booleanNode(random.nextBoolean());
            case 2 -> NODES.numberNode(random.nextInt(21) - 10);
            case 3 -> NODES.numberNode(new BigDecimal("2.50"));
            case 4 -> NODES.textNode("2026-12-0" + random.nextInt(10));
            case 5 -> NODES.textNode("");
            case 6 -> NODES.numberNode(Double.NaN);
            default -> NODES.textNode("t" + random.nextInt(5));
        };
    }
}
