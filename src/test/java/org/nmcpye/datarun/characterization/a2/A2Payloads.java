package org.nmcpye.datarun.characterization.a2;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Map;

/**
 * Upload items in the shape each installed app generation sends, derived by hand from the mobile tags (A2
 * inventory, section 2.3). Phones never send status, version, team, teamCode, orgUnit, orgUnitCode, orgUnitName,
 * activity or deletedAt, and always send deleted=false.
 * <ul>
 *   <li>G0 (v5.2.0+17 to v5.3.0+20): the legacy route; repeat rows carry field values only
 *       (m@v5.3.0+20 packages/drun_sdk/lib/database/extensions/data_submission.extension.dart:4-23;
 *       lib/features/form_submission/application/element/repeat_item_instance.dart:44-54).</li>
 *   <li>G1 (v6.0.0, v6.0.1): the legacy route; every map in a list carries _id, _index, _parentId and
 *       _submissionUid (lib/core/data_instance/repeat_metadata_normalizer.dart:95-122).</li>
 *   <li>G2 (v6.0.2, v6.0.3): the versioned route, plus referenceDefinitions for the Reference values whose entry
 *       is local to the assignment's org unit (reference_upload_payload_builder.dart:72-100).</li>
 *   <li>G3 (v6.1.0 to v6.1.2): as G2, plus the X-App-Version header and the practice route.</li>
 * </ul>
 */
final class A2Payloads {

    static final JsonNodeFactory NODES = JsonNodeFactory.instance;

    static final String BULK_LEGACY = "/api/v1/dataSubmission/bulk";
    static final String BULK_VERSIONED = "/api/v1/dataSubmission/bulk?referenceVersion=1";
    static final String BULK_PRACTICE = "/api/v1/dataSubmission/bulk/practice?referenceVersion=1";

    static final String START = "2026-11-20T05:00:00.000Z";
    static final String FINISH = "2026-11-20T05:10:00.000Z";
    static final String FINISH_LATER = "2026-11-20T06:00:00.000Z";

    enum Group {
        G0, G1, G2, G3;

        boolean versioned() {
            return this == G2 || this == G3;
        }

        boolean normalizesRepeats() {
            return this != G0;
        }

        String route() {
            return versioned() ? BULK_VERSIONED : BULK_LEGACY;
        }

        Map<String, String> headers() {
            return this == G3 ? Map.of("X-App-Version", "6.1.2+57") : Map.of();
        }
    }

    private A2Payloads() {
    }

    /** A submission uid for a scenario: 11 characters, the a2 prefix every snapshot and clean-up scopes on. */
    static String uid(String tag) {
        return ("a2" + tag + "xxxxxxxxxxx").substring(0, 11);
    }

    /** A repeat-row id as a phone makes one: a 26-character ULID. */
    static String rowId(int n) {
        return String.format("01HA2%021d", n);
    }

    static Item item(Group group, String uid, String form, String formVersion, String assignment, ObjectNode formData) {
        ObjectNode node = NODES.objectNode();
        node.put("uid", uid);
        node.put("formVersion", formVersion);
        node.put("form", form);
        node.put("deleted", false);
        node.putNull("lastSyncMassage");
        node.putNull("lastSyncDate");
        node.put("startEntryTime", START);
        node.put("finishedEntryTime", FINISH);
        node.put("assignment", assignment);
        node.set("formData", formData);
        return new Item(group, node);
    }

    /**
     * FORM_A data: one row with one nested row. G1 and later carry the phone's repeat metadata, with row ids
     * {@code rowId(rowBase)} and {@code rowId(rowBase + 1)}.
     */
    static ObjectNode ordinary(Group group, String submissionUid, String name, String item, String sub, int rowBase) {
        ObjectNode subRow = NODES.objectNode().put("a2_sub", sub);
        ObjectNode row = NODES.objectNode().put("a2_item", item);
        row.set("subrows", NODES.arrayNode().add(subRow));
        if (group.normalizesRepeats()) {
            metadata(subRow, rowId(rowBase + 1), 1, rowId(rowBase), submissionUid);
            metadata(row, rowId(rowBase), 1, submissionUid, submissionUid);
        }
        ObjectNode main = NODES.objectNode().put("a2_name", name);
        main.set("rows", NODES.arrayNode().add(row));
        return NODES.objectNode().set("main", main);
    }

    /** FORM_R data: one household row per Reference value. */
    static ObjectNode households(Group group, String submissionUid, String... houses) {
        ArrayNode rows = NODES.arrayNode();
        for (int i = 0; i < houses.length; i++) {
            ObjectNode row = NODES.objectNode().put("a2_house", houses[i]);
            if (group.normalizesRepeats()) {
                metadata(row, rowId(100 + i), i + 1, submissionUid, submissionUid);
            }
            rows.add(row);
        }
        ObjectNode main = NODES.objectNode();
        main.set("households", rows);
        return NODES.objectNode().set("main", main);
    }

    /** FORM_X data. */
    static ObjectNode note(String note) {
        return NODES.objectNode().set("main", NODES.objectNode().put("a2_note", note));
    }

    static void metadata(ObjectNode row, String id, int index, String parentId, String submissionUid) {
        row.put("_id", id);
        row.put("_index", index);
        row.put("_parentId", parentId);
        row.put("_submissionUid", submissionUid);
    }

    static ArrayNode body(Item... items) {
        ArrayNode body = NODES.arrayNode();
        for (Item item : items) {
            body.add(item.node);
        }
        return body;
    }

    /** One upload item, adjustable before it is sent. */
    static final class Item {
        final Group group;
        final ObjectNode node;

        Item(Group group, ObjectNode node) {
            this.group = group;
            this.node = node;
        }

        Item set(String field, String value) {
            if (value == null) {
                node.putNull(field);
            } else {
                node.put(field, value);
            }
            return this;
        }

        Item set(String field, boolean value) {
            node.put(field, value);
            return this;
        }

        Item remove(String field) {
            node.remove(field);
            return this;
        }

        Item data(ObjectNode formData) {
            node.set("formData", formData);
            return this;
        }

        /** Adds the definitions a G2/G3 phone sends; G0 and G1 never send them. */
        Item definitions(String... uidNamePairs) {
            if (!group.versioned()) {
                return this;
            }
            ArrayNode definitions = NODES.arrayNode();
            for (int i = 0; i < uidNamePairs.length; i += 2) {
                definitions.add(NODES.objectNode().put("uid", uidNamePairs[i]).put("name", uidNamePairs[i + 1]));
            }
            node.set("referenceDefinitions", definitions);
            return this;
        }

        Item copy() {
            return new Item(group, node.deepCopy());
        }

        JsonNode uid() {
            return node.get("uid");
        }
    }
}
