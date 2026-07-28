package org.nmcpye.datarun.eventjournal;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EventJournalPort {

    JournalEvent append(AppendJournalEvent event);

    Optional<JournalEvent> findByEventId(UUID eventId);

    List<JournalEvent> findBySubject(String subjectType, UUID subjectId);
}
