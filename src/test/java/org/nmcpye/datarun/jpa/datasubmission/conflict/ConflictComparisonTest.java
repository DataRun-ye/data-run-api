package org.nmcpye.datarun.jpa.datasubmission.conflict;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.api.Test;
import org.nmcpye.datarun.submission.normalized.StoredJson;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.nmcpye.datarun.jpa.datasubmission.conflict.ConflictComparison.sameDocument;
import static org.nmcpye.datarun.jpa.datasubmission.conflict.ConflictComparison.sameState;

/** The conflict comparison (spec, Conflicts), on invented documents. */
class ConflictComparisonTest {

    /** As the upload routes read a request: decimals as binary doubles. */
    private static final ObjectMapper REQUEST = new ObjectMapper();

    @Test
    void rowsMetadataIsLeftOutOfRowsAtAnyDepth() throws Exception {
        JsonNode withIds = REQUEST.readTree("""
            {"main": {"name": "a"},
             "rooms": [{"_id": "r1", "_index": 1, "_parentId": "s", "_submissionUid": "s", "size": 2,
                        "beds": [{"_id": "b1", "_index": 1, "_parentId": "r1", "kind": "x"}]}]}""");
        JsonNode withoutIds = REQUEST.readTree("""
            {"rooms": [{"size": 2, "beds": [{"kind": "x"}]}], "main": {"name": "a"}}""");

        assertThat(sameDocument(withIds, withoutIds)).isTrue();
        assertThat(sameDocument(withIds, REQUEST.readTree("""
            {"main": {"name": "a"}, "rooms": [{"size": 2, "beds": [{"kind": "y"}]}]}"""))).isFalse();
    }

    @Test
    void theSameKeysOutsideARowCompare() throws Exception {
        assertThat(sameDocument(
            REQUEST.readTree("{\"main\": {\"_id\": \"a\"}}"),
            REQUEST.readTree("{\"main\": {}}"))).isFalse();
        assertThat(sameDocument(
            REQUEST.readTree("{\"_id\": \"a\"}"),
            REQUEST.readTree("{}"))).isFalse();
    }

    @Test
    void theTopLevelStatusCompares() throws Exception {
        assertThat(sameDocument(
            REQUEST.readTree("{\"_status\": \"COMPLETED\", \"main\": {}}"),
            REQUEST.readTree("{\"main\": {}}"))).isFalse();
        assertThat(sameDocument(
            REQUEST.readTree("{\"_status\": \"COMPLETED\"}"),
            REQUEST.readTree("{\"_status\": \"ACTIVE\"}"))).isFalse();
    }

    @Test
    void numbersCompareByValueWhateverTheirSpellingOrReader() throws Exception {
        assertThat(sameDocument(StoredJson.parse("{\"n\": 2.50}"), REQUEST.readTree("{\"n\": 2.5}"))).isTrue();
        assertThat(sameDocument(StoredJson.parse("{\"n\": 1.0}"), REQUEST.readTree("{\"n\": 1}"))).isTrue();
        assertThat(sameDocument(StoredJson.parse("{\"n\": 12345678901234567890}"),
            StoredJson.parse("{\"n\": 12345678901234567891}"))).isFalse();
        assertThat(sameDocument(REQUEST.readTree("{\"n\": 1}"), REQUEST.readTree("{\"n\": \"1\"}"))).isFalse();
    }

    @Test
    void aNullIsAValueUnlikeAMissingKey() throws Exception {
        assertThat(sameDocument(REQUEST.readTree("{\"a\": null}"), REQUEST.readTree("{}"))).isFalse();
        assertThat(sameDocument(REQUEST.readTree("{\"a\": null}"), REQUEST.readTree("{\"a\": null}"))).isTrue();
        assertThat(sameDocument(null, JsonNodeFactory.instance.nullNode())).isTrue();
        assertThat(sameDocument(null, REQUEST.readTree("{}"))).isFalse();
    }

    @Test
    void rowsCompareInOrder() throws Exception {
        assertThat(sameDocument(
            REQUEST.readTree("{\"rows\": [{\"v\": 1}, {\"v\": 2}]}"),
            REQUEST.readTree("{\"rows\": [{\"v\": 2}, {\"v\": 1}]}"))).isFalse();
        assertThat(sameDocument(
            REQUEST.readTree("{\"rows\": [{\"v\": 1}]}"),
            REQUEST.readTree("{\"rows\": [{\"v\": 1}, {\"v\": 1}]}"))).isFalse();
    }

    @Test
    void theDeletedFlagTheAssignmentAndTheFormVersionCount() throws Exception {
        JsonNode document = REQUEST.readTree("{\"main\": {\"name\": \"a\"}}");
        ComparedState state = new ComparedState(document, false, "assignment1", "version1", null);

        assertThat(sameState(state, new ComparedState(document.deepCopy(), false, "assignment1", "version1", null)))
            .isTrue();
        assertThat(sameState(state, new ComparedState(document, true, "assignment1", "version1", null))).isFalse();
        assertThat(sameState(state, new ComparedState(document, false, "assignment2", "version1", null))).isFalse();
        assertThat(sameState(state, new ComparedState(document, false, "assignment1", "version2", null))).isFalse();
        assertThat(sameState(state, new ComparedState(document, false, null, "version1", null))).isFalse();
    }

    /** API callers can send a status, and an upload writes it: a copy that changes only the status is changed. */
    @Test
    void theStatusCounts() throws Exception {
        JsonNode document = REQUEST.readTree("{\"main\": {\"name\": \"a\"}}");
        ComparedState none = new ComparedState(document, false, "assignment1", "version1", null);

        assertThat(sameState(none, new ComparedState(document, false, "assignment1", "version1", "DONE"))).isFalse();
        assertThat(sameState(new ComparedState(document, false, "assignment1", "version1", "DONE"),
            new ComparedState(document, false, "assignment1", "version1", "DONE"))).isTrue();
        assertThat(ComparedState.ofUploadItem(REQUEST.readTree("{\"status\": \"DONE\"}")).status()).isEqualTo("DONE");
        assertThat(ComparedState.ofUploadItem(REQUEST.readTree("{}")).status()).isNull();
    }

    @Test
    void anUploadItemIsReadInEitherRoutesShape() throws Exception {
        ComparedState item = ComparedState.ofUploadItem(REQUEST.readTree("""
            {"uid": "s", "formData": {"main": {}}, "assignment": "a1", "formVersion": "v1"}"""));

        assertThat(item.deleted()).as("absent is false").isFalse();
        assertThat(item.assignment()).isEqualTo("a1");
        assertThat(item.formVersion()).isEqualTo("v1");
        assertThat(ComparedState.ofUploadItem(REQUEST.readTree("{\"deleted\": null}")).deleted()).isFalse();
        assertThat(ComparedState.ofUploadItem(REQUEST.readTree("{\"deleted\": true}")).deleted()).isTrue();
    }

    @Test
    void documentsNestedPastTheGuardAreNotCompared() {
        ArrayNode deep = JsonNodeFactory.instance.arrayNode();
        ArrayNode innermost = deep;
        for (int i = 0; i < ConflictComparison.MAX_DEPTH + 5; i++) {
            innermost = innermost.addArray();
        }

        assertThatThrownBy(() -> sameDocument(deep, deep.deepCopy()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageNotContaining("[");
    }
}
