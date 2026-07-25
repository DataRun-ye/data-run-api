package org.nmcpye.datarun.jpa.reference;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ReferenceDisplayNamePolicyTest {

    private final ReferenceDisplayNamePolicy policy =
        new ReferenceDisplayNamePolicy();

    @Test
    void normalizesWhitespaceAndAcceptsFourValidParts() {
        assertEquals(
            "Ahmed Ali Saleh Hassan",
            policy.normalizeNewName("  Ahmed   Ali Saleh Hassan  "));
    }

    @Test
    void treatsConfiguredConnectorsLikeTheMobileValidator() {
        assertEquals(
            "عبد الله محمد علي صالح",
            policy.normalizeNewName("عبد الله محمد علي صالح"));
    }

    @Test
    void rejectsTooFewPartsAndUnsupportedCharacters() {
        assertNull(policy.normalizeNewName("Ahmed Ali Saleh"));
        assertNull(policy.normalizeNewName("Ahmed Ali Saleh Hassan 1"));
    }
}
