package org.nmcpye.datarun.submission.normalized;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The derived ids follow the published recipe: UUIDv5 over stored id texts and paths, never a UUID read from an id.
 * Every expected value was computed twice, outside this code: with Python's {@code uuid} module, and with util-linux
 * {@code uuidgen --sha1}. The ids are invented.
 */
class DerivedIdsTest {

    private static final String SUBMISSION = "01J2Q7Y0V6M3N4P5R6S7T8V9WX";

    @Test
    void theNamespaceIsTheVersion5UuidOfTheFeedUrl() {
        assertThat(DerivedIds.NAMESPACE).hasToString("4b1aa021-0abd-532b-88b8-f71a9078093d");
    }

    @Test
    void aSectionIdHashesItsFormsIdTextAndItsPath() {
        assertThat(DerivedIds.sectionId("01HZY8X3K2ABCDEFGHJKMNPQRS", "visit.households"))
            .isEqualTo("35b9e2f1-b362-596c-8043-f87a4417adc7");
        // any stored id text: a 24-hex form id, or a 1-character one
        assertThat(DerivedIds.sectionId("5f2a9c0b1d3e4f5061728394", "visit"))
            .isEqualTo("1505ff0d-00c3-5f35-90ad-a730cc4c40bb");
        assertThat(DerivedIds.sectionId("A", "s")).isEqualTo("f18419f0-2ce9-57a8-8b6b-5686ca864e6c");
    }

    @Test
    void aRowWithoutAStoredIdHashesSubmissionPathParentAndIndex() {
        assertThat(DerivedIds.derivedRowId(SUBMISSION, "visit.households", null, 1))
            .isEqualTo("ea8b77f1-ebb9-5f13-aa3f-3aeec966b4ca");
        assertThat(DerivedIds.derivedRowId(SUBMISSION, "visit.households.members", "01J2Q7Y0V6M3N4P5R6S7T8V9WY", 2))
            .isEqualTo("dd2bccab-dd79-57f4-a8de-07ab2df663f7");
    }

    @Test
    void idTextsAreNeverRead_soTwoSpellingsOfOneUlidAreTwoParents() {
        assertThat(DerivedIds.derivedRowId(SUBMISSION, "a", "01J2Q7Y0V6M3N4P5R6S7T8V9WY", 1))
            .isNotEqualTo(DerivedIds.derivedRowId(SUBMISSION, "a", "01j2q7y0v6m3n4p5r6s7t8v9wy", 1));
    }

    @Test
    void aRowWhoseIdIsTakenTriesItsDerivedIdThenNumberedAttempts() {
        assertThat(DerivedIds.duplicateRowId(SUBMISSION, "visit.households", null, 1, 1))
            .isEqualTo(DerivedIds.derivedRowId(SUBMISSION, "visit.households", null, 1));
        assertThat(DerivedIds.duplicateRowId(SUBMISSION, "visit.households", null, 1, 2))
            .isEqualTo("3c0744c6-26a1-5c36-8e2b-e6cc939d7f51");
    }
}
