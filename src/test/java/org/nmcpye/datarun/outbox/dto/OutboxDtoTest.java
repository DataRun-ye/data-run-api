package org.nmcpye.datarun.outbox.dto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OutboxDtoTest {

    @Test
    void itsTextNamesTheRowButLeavesOutTheSubmittedPayload() {
        OutboxDto outbox = OutboxDto.builder()
            .outboxId(42L)
            .submissionUid("subUid00001")
            .payload("{\"main\":{\"hh_name\":\"a person's name\"}}")
            .build();

        assertThat(outbox.toString()).contains("outboxId=42", "submissionUid=subUid00001").doesNotContain("person", "payload");
    }
}
