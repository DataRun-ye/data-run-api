package org.nmcpye.datarun.jpa.user.service;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class LastSeenServiceTest {

    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final LastSeenService service = new LastSeenService(jdbc);

    @Test
    void flushesTheLatestTimeAndTheLatestKnownClientValues() {
        Instant first = Instant.parse("2026-11-18T08:00:00Z");
        Instant later = first.plusSeconds(60);
        service.touch("user000001", first, "6.1.0+55", null);
        service.touch("user000001", later, null, 1);

        Object[] row = flushedRows().get(0);

        assertEquals("user000001", row[0]);
        assertEquals(Timestamp.from(later), row[1]);
        assertEquals("6.1.0+55", row[2]);
        assertEquals(1, row[3]);
    }

    @Test
    void leavesUnknownClientValuesNullSoStoredOnesAreKept() {
        service.touch("user000001", Instant.now());

        Object[] row = flushedRows().get(0);

        assertNull(row[2]);
        assertNull(row[3]);
    }

    @Test
    void doesNothingWhenNobodyWasSeen() {
        service.flushBuffer();

        verifyNoInteractions(jdbc);
    }

    @SuppressWarnings("unchecked")
    private List<Object[]> flushedRows() {
        when(jdbc.batchUpdate(anyString(), anyList(), any(int[].class))).thenReturn(new int[]{1});
        service.flushBuffer();
        ArgumentCaptor<List<Object[]>> rows = ArgumentCaptor.forClass(List.class);
        verify(jdbc).batchUpdate(contains("reference_version"), rows.capture(), any(int[].class));
        return rows.getValue();
    }
}
