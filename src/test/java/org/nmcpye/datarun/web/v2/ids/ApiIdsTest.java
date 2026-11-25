package org.nmcpye.datarun.web.v2.ids;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** The server's guard accepts every id shape stored today and refuses what no stored id can be. */
class ApiIdsTest {

    @Test
    void everyStoredShapeIsWellFormed() {
        assertThat(ApiIds.isWellFormed("01JDQ3ZK5V8Y7M2N4P6R8T0W2X")).as("ULID").isTrue();
        assertThat(ApiIds.isWellFormed("5f2b7c9d1e3a4b6c8d0e2f4a")).as("24-hex legacy id").isTrue();
        assertThat(ApiIds.isWellFormed("0193a7c2-5d4e-7f81-9a2b-3c4d5e6f7a8b")).as("UUID text").isTrue();
        assertThat(ApiIds.isWellFormed("1")).as("org-unit level").isTrue();
        assertThat(ApiIds.isWellFormed("9223372036854775807")).as("bigint text").isTrue();
    }

    @Test
    void valuesNoStoredIdCanTakeAreRefused() {
        assertThat(ApiIds.isWellFormed(null)).isFalse();
        assertThat(ApiIds.isWellFormed("")).isFalse();
        assertThat(ApiIds.isWellFormed("a".repeat(ApiIds.MAX_LENGTH + 1))).isFalse();
        assertThat(ApiIds.isWellFormed("a".repeat(ApiIds.MAX_LENGTH))).isTrue();
        assertThat(ApiIds.isWellFormed("abc def")).isFalse();
        assertThat(ApiIds.isWellFormed("abc\ndef")).isFalse();
        assertThat(ApiIds.isWellFormed("x' OR '1'='1")).isFalse();
        assertThat(ApiIds.isWellFormed("café")).isFalse();
    }
}
