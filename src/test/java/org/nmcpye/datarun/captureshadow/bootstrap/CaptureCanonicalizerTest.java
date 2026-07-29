package org.nmcpye.datarun.captureshadow.bootstrap;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.nmcpye.datarun.captureshadow.CaptureShadowProtocol;

import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CaptureCanonicalizerTest {

    private final CaptureCanonicalizer canonicalizer = new CaptureCanonicalizer(
        new ObjectMapper()
    );

    @Test
    void namespacesAndGoldenIdsAreFrozen() {
        assertThat(CaptureShadowProtocol.CAPTURE_ID_NAMESPACE)
            .isEqualTo("datarun-baseline/capture/");
        assertThat(CaptureShadowProtocol.CAPTURE_EVENT_ID_NAMESPACE)
            .isEqualTo("datarun-baseline/event/submission-captured/");
        assertThat(CaptureShadowProtocol.CHECKPOINT_KEY)
            .isEqualTo("capture_shadow_bootstrap/v1");
        assertThat(CaptureShadowProtocol.captureId("S0000000001"))
            .isEqualTo(UUID.fromString("5fc5dc13-f2b7-3cbe-9156-c0158c0103d7"));
        assertThat(CaptureShadowProtocol.captureEventId("S0000000001"))
            .isEqualTo(UUID.fromString("9723a288-efcc-3519-8b1f-b97322f42ee8"));
    }

    @Test
    void canonicalFingerprintEncodingAndFieldOrderAreFrozen() throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        canonicalizer.updateFingerprint(digest, completeRow("{\"z\":2,\"a\":1}"));

        assertThat(CaptureShadowProtocol.FINGERPRINT_ENCODING)
            .isEqualTo("length-prefixed-utf8-fields/v1");
        assertThat(CaptureShadowProtocol.SOURCE_FIELD_ORDER).containsExactly(
            "baselineSubmissionId",
            "uid",
            "serialNumber",
            "deleted",
            "deletedAt",
            "formData",
            "status",
            "formUid",
            "formVersionUid",
            "formVersionNumber",
            "assignmentUid",
            "teamUid",
            "teamCode",
            "orgUnitUid",
            "orgUnitCode",
            "orgUnitName",
            "activityUid",
            "startEntryTime",
            "finishedEntryTime",
            "createdBy",
            "createdDate",
            "lastModifiedBy",
            "lastModifiedDate"
        );
        assertThat(HexFormat.of().formatHex(digest.digest()))
            .isEqualTo("7eed107edccdf93d19bd12a9c0efed0915d0ab3ccda446edf6e8c13b9cf84af1");
    }

    @Test
    void payloadPreservesNullsAndMicrosecondUtcPrecision() {
        CaptureSourceRow source = completeRow(null);

        CanonicalCapture capture = canonicalizer.canonicalize(source);

        assertThat(capture.recordedAt())
            .isEqualTo(Instant.parse("2026-07-25T08:09:10.654321Z"));
        assertThat(capture.payload().path("submission").fieldNames()).toIterable()
            .containsExactly(
                "uid",
                "deleted",
                "deletedAt",
                "formData",
                "status",
                "formUid",
                "formVersionUid",
                "formVersionNumber",
                "assignmentUid",
                "teamUid",
                "teamCode",
                "orgUnitUid",
                "orgUnitCode",
                "orgUnitName",
                "activityUid",
                "startEntryTime",
                "finishedEntryTime",
                "createdBy",
                "createdDate",
                "lastModifiedBy",
                "lastModifiedDate"
            );
        assertThat(capture.payload().at("/submission/formData").isNull()).isTrue();
        assertThat(capture.payload().at("/submission/status").isNull()).isTrue();
        assertThat(capture.payload().at("/submission/deletedAt").textValue())
            .isEqualTo("2026-07-25T01:02:03.123456Z");
        assertThat(capture.payload().at("/submission/createdDate").textValue())
            .isEqualTo("2026-07-25T04:05:06.000001Z");
        assertThat(capture.payload().at("/submission/lastModifiedDate").textValue())
            .isEqualTo("2026-07-25T08:09:10.654321Z");
    }

    @Test
    void jsonLiteralNullAndNonObjectBodiesAreRejected() {
        for (String unsupported : List.of("null", "[]", "\"value\"", "1")) {
            assertThatThrownBy(() -> canonicalizer.canonicalize(completeRow(unsupported)))
                .isInstanceOf(CaptureShadowBootstrapConflictException.class)
                .hasMessageContaining("form_data");
        }
    }

    private CaptureSourceRow completeRow(String formData) {
        return new CaptureSourceRow(
            "01J00000000000000000000001",
            42,
            "S0000000001",
            true,
            Instant.parse("2026-07-25T01:02:03.123456Z"),
            formData,
            null,
            "F0000000001",
            "V0000000001",
            7,
            "A0000000001",
            "T0000000001",
            "TEAM",
            "O0000000001",
            "OU",
            "\u0648\u062d\u062f\u0629",
            "C0000000001",
            Instant.parse("2026-07-25T02:03:04.000001Z"),
            Instant.parse("2026-07-25T03:04:05.999999Z"),
            "system",
            Instant.parse("2026-07-25T04:05:06.000001Z"),
            null,
            Instant.parse("2026-07-25T08:09:10.654321Z")
        );
    }
}
