package org.nmcpye.datarun.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class LatinDigitsTest {

    @Test
    void convertsArabicIndicDigits() {
        assertEquals("0123456789", LatinDigits.normalize("٠١٢٣٤٥٦٧٨٩"));
    }

    @Test
    void convertsExtendedArabicIndicDigits() {
        assertEquals("0123456789", LatinDigits.normalize("۰۱۲۳۴۵۶۷۸۹"));
    }

    @Test
    void keepsEverythingElse() {
        assertEquals("team-12 علي", LatinDigits.normalize("team-١٢ علي"));
    }

    @Test
    void returnsTheSameValueWhenThereIsNothingToConvert() {
        String value = "user123";
        assertSame(value, LatinDigits.normalize(value));
    }

    @Test
    void acceptsNull() {
        assertNull(LatinDigits.normalize(null));
    }
}
