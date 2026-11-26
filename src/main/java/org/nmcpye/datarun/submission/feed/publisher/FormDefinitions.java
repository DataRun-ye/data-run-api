package org.nmcpye.datarun.submission.feed.publisher;

import org.nmcpye.datarun.submission.normalized.FormDefinition;
import org.nmcpye.datarun.submission.normalized.jdbc.FormDefinitionJdbcReader;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.LongSupplier;

/**
 * The form definitions the publisher normalizes with, kept between changes and read again when they may have changed.
 *
 * <p>A definition kept is used as it is for {@link #CHECK_EVERY} after it was read or last found unchanged; then its
 * change token ({@link FormDefinitionJdbcReader#changeTokens}) is compared with the stored one, and the definition is
 * read again when they differ. A head pinned to a version the kept definition lacks (a version published since, or a
 * form whose first version came after it was found missing) reads it again at once, so a new version is never
 * normalized against definitions that do not know it. A version rewritten in place reaches records published after
 * the next check; records published before keep their values, as the contract has the dictionary change under them
 * (contract 6).</p>
 *
 * <p>Reads run in the caller's transaction, through the reader it passes (a feed part's, bounded by the part's time
 * limit). Safe for publishers on several threads.</p>
 */
final class FormDefinitions {

    static final Duration CHECK_EVERY = Duration.ofMinutes(1);

    private record Kept(FormDefinition definition, String token, long checkedAtNanos) {
    }

    private final ConcurrentMap<String, Kept> kept = new ConcurrentHashMap<>();
    private final LongSupplier nanoTime;

    FormDefinitions() {
        this(System::nanoTime);
    }

    FormDefinitions(LongSupplier nanoTime) {
        this.nanoTime = nanoTime;
    }

    /**
     * The definition of the form with this id, or {@code null} when none is stored (the normalizer then flags
     * {@code FORM_MISSING}).
     *
     * @param formId          the form's id, as the stored-submission reader resolved it; {@code null} gives
     *                        {@code null}
     * @param pinnedVersionId the id of the version the head is pinned to, or {@code null}
     */
    FormDefinition forHead(FormDefinitionJdbcReader reader, String formId, String pinnedVersionId) {
        if (formId == null) {
            return null;
        }
        long now = nanoTime.getAsLong();
        Kept entry = kept.get(formId);
        boolean pinnedUnknown = pinnedVersionId != null && (entry == null || entry.definition() == null
            || entry.definition().versionById(pinnedVersionId).isEmpty());
        if (entry != null && !pinnedUnknown) {
            if (now - entry.checkedAtNanos() < CHECK_EVERY.toNanos()) {
                return entry.definition();
            }
            String token = reader.changeTokens(Set.of(formId)).get(formId);
            if (Objects.equals(token, entry.token())) {
                kept.put(formId, new Kept(entry.definition(), entry.token(), now));
                return entry.definition();
            }
        }
        Optional<FormDefinitionJdbcReader.Snapshot> read = reader.read(formId);
        Kept fresh = new Kept(read.map(FormDefinitionJdbcReader.Snapshot::definition).orElse(null),
            read.map(FormDefinitionJdbcReader.Snapshot::changeToken).orElse(null), now);
        kept.put(formId, fresh);
        return fresh.definition();
    }
}
