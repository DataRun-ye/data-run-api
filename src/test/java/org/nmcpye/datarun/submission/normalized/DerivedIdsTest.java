package org.nmcpye.datarun.submission.normalized;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The derived ids follow the D feed spike's published recipes. Every expected value was computed twice, outside this
 * code: with Python's {@code uuid} module, and with the spike's own SQL functions ({@code tr.ns}, {@code tr.ulid_to_uuid},
 * {@code tr.entity_uuid}, {@code tr.section_uuid}, {@code tr.row_uuid}) on its scratch database. The ids are invented.
 */
class DerivedIdsTest {

    private static final String SUBMISSION = "01J2Q7Y0V6M3N4P5R6S7T8V9WX";

    @Test
    void theNamespaceIsTheVersion5UuidOfTheFeedUrl() {
        assertThat(DerivedIds.NAMESPACE).hasToString("4b1aa021-0abd-532b-88b8-f71a9078093d");
    }

    @Test
    void aUlidReadsAsTheUuidWithTheSame128Bits() {
        // the spike's own check values (10_translate_setup.sql)
        assertThat(DerivedIds.ulidToUuid("01ARZ3NDEKTSV4RRFFQ69G5FAV")).hasToString("01563e3a-b5d3-d676-4c61-efb99302bd5b");
        assertThat(DerivedIds.ulidToUuid("7zzzzzzzzzzzzzzzzzzzzzzzzz")).hasToString("ffffffff-ffff-ffff-ffff-ffffffffffff");
        assertThat(DerivedIds.ulidToUuid("01J9ZK4Q8A0000000000000001")).hasToString("01927f32-5d0a-0000-0000-000000000001");
    }

    @Test
    void textThatIsNotAUlidHasNoUuid() {
        assertThat(DerivedIds.ulidToUuid("0123456789abcdef")).isNull();           // a 16-character legacy id
        assertThat(DerivedIds.ulidToUuid("8ZZZZZZZZZZZZZZZZZZZZZZZZZ")).isNull(); // more than 128 bits
        assertThat(DerivedIds.ulidToUuid("01ARZ3NDEKTSV4RRFFQ69G5FAI")).isNull(); // I is not a Crockford digit
        assertThat(DerivedIds.ulidToUuid("5f2a9c0b1d3e4f5061728394")).isNull();   // a 24-hex form id
        assertThat(DerivedIds.ulidToUuid(null)).isNull();
        assertThat(DerivedIds.isUlid("01arz3ndektsv4rrffq69g5fav")).isTrue();
    }

    @Test
    void aSectionIdDerivesFromItsFormAndItsPath() {
        assertThat(DerivedIds.sectionId("01HZY8X3K2ABCDEFGHJKMNPQRS", "visit.households"))
            .isEqualTo("6babd61f-da70-5ee9-bdd9-bb94b1adf370");
        // a 24-hex form id is keyed by the version 5 UUID over data_template|<id>
        assertThat(DerivedIds.formKey("5f2a9c0b1d3e4f5061728394")).hasToString("2631da49-8fb7-54e7-af6d-b0c287d468f7");
        assertThat(DerivedIds.sectionId("5f2a9c0b1d3e4f5061728394", "visit"))
            .isEqualTo("6f0d676e-b554-59f8-816d-46953712659b");
    }

    @Test
    void aRowWithoutAStoredIdDerivesItsIdFromSubmissionPathParentAndIndex() {
        UUID submission = DerivedIds.submissionKey(SUBMISSION);
        assertThat(submission).hasToString("0190ae7f-0366-a0ea-4b17-06c9f48da79d");
        assertThat(DerivedIds.derivedRowKey(submission, "visit.households", null, 1))
            .hasToString("0aac2c77-7aa8-5403-a4ee-1230d1d35147");
        UUID parent = DerivedIds.ulidToUuid("01J2Q7Y0V6M3N4P5R6S7T8V9WY");
        assertThat(DerivedIds.derivedRowKey(submission, "visit.households.members", parent, 2))
            .hasToString("c79f5631-38cd-5ae1-9790-34e02d09b7aa");
    }

    @Test
    void aRowWhoseIdIsTakenTriesItsPositionThenNumberedAttempts() {
        UUID submission = DerivedIds.submissionKey(SUBMISSION);
        assertThat(DerivedIds.duplicateRowKey(submission, "visit.households", null, 1, 1))
            .isEqualTo(DerivedIds.derivedRowKey(submission, "visit.households", null, 1));
        assertThat(DerivedIds.duplicateRowKey(submission, "visit.households", null, 1, 2))
            .hasToString("6f9e4d6b-3026-5852-acfb-7a782f5d8162");
        assertThat(DerivedIds.duplicateRowKey(submission, "visit.households.members",
            DerivedIds.ulidToUuid("01J2Q7Y0V6M3N4P5R6S7T8V9WY"), 1, 3))
            .hasToString("2969d948-5f27-5894-a148-7defc6c11893");
    }

    @Test
    void aLegacyRowKeyIsTheVersion5UuidOverSubmissionAndStoredId() {
        assertThat(DerivedIds.legacyRowKey(DerivedIds.submissionKey(SUBMISSION), "aB3dE5fG7hJ9kL1m"))
            .hasToString("05e08d91-85e5-5ffe-a6f4-951e6507ec14");
    }

    @Test
    void aSubmissionIdThatIsNotAUlidStillHasAKey() {
        assertThat(DerivedIds.submissionKey("abc")).hasToString("b2ef7169-2b3e-5743-a467-d0d4ce597bc6");
        assertThat(DerivedIds.submissionKey("0190ae7f-0366-7aea-8b17-06c9f48da79d"))
            .hasToString("0190ae7f-0366-7aea-8b17-06c9f48da79d");
    }
}
