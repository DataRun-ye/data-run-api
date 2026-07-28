package org.nmcpye.datarun.captureshadow;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.nmcpye.datarun.jpa.datasubmission.DataSubmission;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class CaptureSubmissionCanonicalizerTest {

    private final CaptureSubmissionCanonicalizer canonicalizer =
        new CaptureSubmissionCanonicalizer(new ObjectMapper());

    @Test
    void timestampsMatchPostgresMicrosecondRounding() {
        DataSubmission submission = submissionAt(
            "2026-07-28T21:31:13.522731500Z"
        );

        assertThat(
            canonicalizer.canonicalize(submission)
                .path("lastModifiedDate")
                .textValue()
        ).isEqualTo("2026-07-28T21:31:13.522732Z");
    }

    @Test
    void timestampRoundingCarriesIntoTheNextSecond() {
        DataSubmission submission = submissionAt(
            "2026-07-28T21:31:13.999999500Z"
        );

        assertThat(
            canonicalizer.canonicalize(submission)
                .path("lastModifiedDate")
                .textValue()
        ).isEqualTo("2026-07-28T21:31:14Z");
    }

    private static DataSubmission submissionAt(String timestamp) {
        DataSubmission submission = new DataSubmission();
        submission.setUid("Capture0001");
        submission.setLastModifiedDate(Instant.parse(timestamp));
        return submission;
    }
}
