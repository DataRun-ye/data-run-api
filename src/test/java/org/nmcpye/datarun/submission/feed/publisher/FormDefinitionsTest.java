package org.nmcpye.datarun.submission.feed.publisher;

import org.junit.jupiter.api.Test;
import org.nmcpye.datarun.submission.normalized.FormDefinition;
import org.nmcpye.datarun.submission.normalized.FormVersionDefinition;
import org.nmcpye.datarun.submission.normalized.jdbc.FormDefinitionJdbcReader;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** When the publisher reads a form's definition again, and when it uses the one it keeps. */
class FormDefinitionsTest {

    private static final String FORM = "fmUid000001";

    private final FormDefinitionJdbcReader reader = mock(FormDefinitionJdbcReader.class);
    private final AtomicLong clock = new AtomicLong();
    private final FormDefinitions definitions = new FormDefinitions(clock::get);

    private static FormDefinitionJdbcReader.Snapshot snapshot(String token, String... versionUids) {
        List<FormVersionDefinition> versions = new java.util.ArrayList<>();
        for (int i = 0; i < versionUids.length; i++) {
            versions.add(new FormVersionDefinition("v-" + versionUids[i], versionUids[i], i + 1, List.of(), List.of()));
        }
        return new FormDefinitionJdbcReader.Snapshot(FormDefinition.of("form-id", versions), token);
    }

    private void later(long nanos) {
        clock.addAndGet(nanos);
    }

    @Test
    void aKeptDefinitionIsUsedWithoutAReadUntilItsNextCheck() {
        FormDefinitionJdbcReader.Snapshot stored = snapshot("t1", "ver1");
        when(reader.read(FORM)).thenReturn(Optional.of(stored));

        FormDefinition first = definitions.forHead(reader, FORM, "ver1");
        later(FormDefinitions.CHECK_EVERY.toNanos() - 1);
        FormDefinition second = definitions.forHead(reader, FORM, "ver1");

        assertThat(second).isSameAs(first).isSameAs(stored.definition());
        verify(reader, times(1)).read(FORM);
        verify(reader, never()).changeTokens(any());
    }

    @Test
    void atItsCheckAnUnchangedTokenKeepsTheDefinitionAndAChangedOneReadsItAgain() {
        when(reader.read(FORM)).thenReturn(Optional.of(snapshot("t1", "ver1")));
        FormDefinition kept = definitions.forHead(reader, FORM, "ver1");

        later(FormDefinitions.CHECK_EVERY.toNanos());
        when(reader.changeTokens(Set.of(FORM))).thenReturn(Map.of(FORM, "t1"));
        assertThat(definitions.forHead(reader, FORM, "ver1")).isSameAs(kept);
        verify(reader, times(1)).read(FORM);

        later(FormDefinitions.CHECK_EVERY.toNanos());
        FormDefinitionJdbcReader.Snapshot changed = snapshot("t2", "ver1");
        when(reader.changeTokens(Set.of(FORM))).thenReturn(Map.of(FORM, "t2"));
        when(reader.read(FORM)).thenReturn(Optional.of(changed));
        assertThat(definitions.forHead(reader, FORM, "ver1")).isSameAs(changed.definition());
        verify(reader, times(2)).read(FORM);
    }

    @Test
    void aHeadPinnedToAVersionTheKeptDefinitionLacksReadsItAgainAtOnce() {
        when(reader.read(FORM)).thenReturn(Optional.of(snapshot("t1", "ver1")));
        definitions.forHead(reader, FORM, "ver1");
        FormDefinitionJdbcReader.Snapshot withNewVersion = snapshot("t2", "ver1", "ver2");
        when(reader.read(FORM)).thenReturn(Optional.of(withNewVersion));

        assertThat(definitions.forHead(reader, FORM, "ver2")).isSameAs(withNewVersion.definition());
        verify(reader, times(2)).read(FORM);
        verify(reader, never()).changeTokens(any());
    }

    @Test
    void aFormFoundMissingIsReadAgainForAHeadPinnedToAVersion() {
        when(reader.read(FORM)).thenReturn(Optional.empty());
        assertThat(definitions.forHead(reader, FORM, null)).isNull();
        assertThat(definitions.forHead(reader, FORM, null)).as("kept as missing until the next check").isNull();
        verify(reader, times(1)).read(FORM);

        FormDefinitionJdbcReader.Snapshot published = snapshot("t1", "ver1");
        when(reader.read(FORM)).thenReturn(Optional.of(published));
        assertThat(definitions.forHead(reader, FORM, "ver1")).isSameAs(published.definition());
    }

    @Test
    void aSubmissionWithoutAFormUidHasNoDefinitionAndReadsNothing() {
        assertThat(definitions.forHead(reader, null, "ver1")).isNull();
        verify(reader, never()).read(any(String.class));
    }
}
