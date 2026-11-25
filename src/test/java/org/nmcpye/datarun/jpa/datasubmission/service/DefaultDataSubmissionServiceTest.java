package org.nmcpye.datarun.jpa.datasubmission.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.nmcpye.datarun.common.EntitySaveSummaryVM;
import org.nmcpye.datarun.common.enumeration.FlowStatus;
import org.nmcpye.datarun.jpa.accessfilter.UserAccessService;
import org.nmcpye.datarun.jpa.datasubmission.DataSubmission;
import org.nmcpye.datarun.jpa.datasubmission.repository.DataSubmissionRepository;
import org.nmcpye.datarun.jpa.datasubmission.revision.RevisionContext;
import org.nmcpye.datarun.jpa.datasubmission.revision.RevisionKind;
import org.nmcpye.datarun.jpa.datasubmission.revision.SubmissionRevisionWriter;
import org.nmcpye.datarun.outbox.repository.OutboxWritePort;
import org.springframework.cache.CacheManager;

import java.time.Instant;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * The persistence layer with its repository mocked. The mocks do what Hibernate does to the version: a persist stores
 * version 0, and a flush raises the version of each submission it updates by one.
 */
class DefaultDataSubmissionServiceTest {

    private static final RevisionContext UPLOAD = RevisionContext.upload("01JUSER0000000000000000001");
    /** The version every stored submission of these tests is at. */
    private static final long STORED = 4L;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private DataSubmissionRepository repository;
    private OutboxWritePort outbox;
    private SubmissionRevisionWriter revisions;
    private DefaultDataSubmissionService service;

    @BeforeEach
    void setUp() {
        repository = mock(DataSubmissionRepository.class);
        outbox = mock(OutboxWritePort.class);
        revisions = mock(SubmissionRevisionWriter.class);
        service = new DefaultDataSubmissionService(
            repository,
            mock(CacheManager.class),
            mock(UserAccessService.class),
            objectMapper,
            outbox,
            revisions);
    }

    @Test
    void unchangedNormalRetryReturnsSuccessWithoutWriting() {
        DataSubmission existing = persistedSubmission(
            "unchang0001",
            "01UNCHANGED000000000000001");
        DataSubmission incoming = incomingSubmission("unchang0001");

        EntitySaveSummaryVM summary = new EntitySaveSummaryVM();
        service.upsert(incoming, existing, summary, UPLOAD);

        assertEquals(List.of("unchang0001"), summary.getUpdated());
        verify(repository, never()).persistAllAndFlush(anyList());
        verify(repository, never()).updateAllAndFlush(anyList());
        verifyNoInteractions(outbox, revisions);
    }

    @ParameterizedTest(name = "{0} change is persisted")
    @MethodSource("mutableFieldChanges")
    void eachMutableFieldIndependentlyTriggersUpdate(
        String field,
        Consumer<DataSubmission> change) {
        DataSubmission existing = persistedSubmission(
            "mutable0001",
            "01MUTABLE0000000000000001");
        DataSubmission incoming = incomingSubmission("mutable0001");
        change.accept(incoming);
        updateReturnsInput();

        EntitySaveSummaryVM summary = new EntitySaveSummaryVM();
        service.upsert(incoming, existing, summary, UPLOAD);

        assertEquals(List.of("mutable0001"), summary.getUpdated(), field);
        verifySingleUpdate(existing, RevisionKind.UPDATE, "UPDATE");
    }

    @Test
    void ignoredIncomingFieldsDoNotTriggerUpdate() {
        DataSubmission existing = persistedSubmission(
            "ignored0001",
            "01IGNORED0000000000000001");
        DataSubmission incoming = incomingSubmission("ignored0001");
        incoming.setId("01DIFFERENT00000000000001");
        incoming.setSerialNumber(999L);
        incoming.setForm("otherform01");
        incoming.setFormVersion("othervers001");
        incoming.setVersion(99);
        incoming.setStartEntryTime(Instant.parse("2030-01-01T00:00:00Z"));
        incoming.setFinishedEntryTime(Instant.parse("2030-01-02T00:00:00Z"));
        incoming.setLockVersion(99L);
        incoming.setDeletedAt(Instant.parse("2030-01-03T00:00:00Z"));
        incoming.setCreatedBy("incoming-creator");
        incoming.setLastModifiedBy("incoming-modifier");
        incoming.setCreatedDate(Instant.parse("2030-01-04T00:00:00Z"));
        incoming.setLastModifiedDate(Instant.parse("2030-01-05T00:00:00Z"));

        EntitySaveSummaryVM summary = new EntitySaveSummaryVM();
        service.upsert(incoming, existing, summary, UPLOAD);

        assertEquals(List.of("ignored0001"), summary.getUpdated());
        verify(repository, never()).updateAllAndFlush(anyList());
        verifyNoInteractions(outbox, revisions);
    }

    @Test
    void alreadyDeletedRetryIgnoresOtherValuesAndDoesNotWrite() {
        Instant deletedAt = Instant.parse("2026-07-20T10:15:30Z");
        DataSubmission existing = persistedSubmission(
            "deleted0001",
            "01DELETED0000000000000001");
        existing.setDeleted(true);
        existing.setDeletedAt(deletedAt);
        JsonNode originalFormData = existing.getFormData().deepCopy();
        String originalActivity = existing.getActivity();

        DataSubmission incoming = incomingSubmission("deleted0001");
        incoming.setDeleted(true);
        incoming.setDeletedAt(Instant.parse("2030-01-01T00:00:00Z"));
        incoming.setFormData(objectMapper.createObjectNode().put("changed", true));
        incoming.setActivity("changed-act");

        EntitySaveSummaryVM summary = new EntitySaveSummaryVM();
        service.upsert(incoming, existing, summary, UPLOAD);

        assertEquals(List.of("deleted0001"), summary.getUpdated());
        assertEquals(deletedAt, existing.getDeletedAt());
        assertEquals(originalFormData, existing.getFormData());
        assertEquals(originalActivity, existing.getActivity());
        verify(repository, never()).updateAllAndFlush(anyList());
        verifyNoInteractions(outbox, revisions);
    }

    @ParameterizedTest(name = "already deleted with deleted={0} is unchanged")
    @ValueSource(strings = {"false", "omitted"})
    void alreadyDeletedFalseOrOmittedRemainsDeletedWithoutWriting(String deletedValue) {
        Instant deletedAt = Instant.parse("2026-07-20T10:15:30Z");
        DataSubmission existing = persistedSubmission(
            "delkeep0001",
            "01DELKEEP0000000000000001");
        existing.setDeleted(true);
        existing.setDeletedAt(deletedAt);
        DataSubmission incoming = incomingSubmission("delkeep0001");
        incoming.setDeleted("false".equals(deletedValue) ? false : null);

        EntitySaveSummaryVM summary = new EntitySaveSummaryVM();
        service.upsert(incoming, existing, summary, UPLOAD);

        assertEquals(List.of("delkeep0001"), summary.getUpdated());
        assertTrue(existing.getDeleted());
        assertEquals(deletedAt, existing.getDeletedAt());
        verify(repository, never()).updateAllAndFlush(anyList());
        verifyNoInteractions(outbox, revisions);
    }

    @ParameterizedTest(name = "already deleted with deleted={0} can update mutable state")
    @ValueSource(strings = {"false", "omitted"})
    void alreadyDeletedFalseOrOmittedUpdatesChangedMutableState(String deletedValue) {
        Instant deletedAt = Instant.parse("2026-07-20T10:15:30Z");
        DataSubmission existing = persistedSubmission(
            "delupdt0001",
            "01DELUPDATE000000000000001");
        existing.setDeleted(true);
        existing.setDeletedAt(deletedAt);
        DataSubmission incoming = incomingSubmission("delupdt0001");
        incoming.setDeleted("false".equals(deletedValue) ? false : null);
        incoming.setActivity("changed-act");
        updateReturnsInput();

        EntitySaveSummaryVM summary = new EntitySaveSummaryVM();
        service.upsert(incoming, existing, summary, UPLOAD);

        assertEquals(List.of("delupdt0001"), summary.getUpdated());
        assertTrue(existing.getDeleted());
        assertEquals(deletedAt, existing.getDeletedAt());
        assertEquals("changed-act", existing.getActivity());
        verifySingleUpdate(existing, RevisionKind.UPDATE, "DELETE");
    }

    @Test
    void firstDeleteWritesOnceAndIgnoresAccompanyingValues() {
        DataSubmission existing = persistedSubmission(
            "delete00001",
            "01DELETE000000000000000001");
        JsonNode originalFormData = existing.getFormData().deepCopy();
        String originalActivity = existing.getActivity();
        DataSubmission incoming = incomingSubmission("delete00001");
        Instant suppliedDeletedAt = Instant.parse("2030-01-01T00:00:00Z");
        incoming.setDeleted(true);
        incoming.setDeletedAt(suppliedDeletedAt);
        incoming.setFormData(objectMapper.createObjectNode().put("changed", true));
        incoming.setActivity("changed-act");
        updateReturnsInput();
        Instant beforeDelete = Instant.now();

        EntitySaveSummaryVM summary = new EntitySaveSummaryVM();
        service.upsert(incoming, existing, summary, UPLOAD);

        assertEquals(List.of("delete00001"), summary.getUpdated());
        assertTrue(existing.getDeleted());
        assertNotNull(existing.getDeletedAt());
        assertFalse(existing.getDeletedAt().isBefore(beforeDelete));
        assertNotEquals(suppliedDeletedAt, existing.getDeletedAt());
        assertEquals(originalFormData, existing.getFormData());
        assertEquals(originalActivity, existing.getActivity());
        verifySingleUpdate(existing, RevisionKind.DELETE, "DELETE");
    }

    /** A new submission gets an id, is persisted at version 0, and is recorded before its outbox row announces it. */
    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void aNewSubmissionIsPersistedAtVersionZeroAndRecordedBeforeItIsAnnounced() {
        DataSubmission incoming = incomingSubmission("new00000001");
        persistStoresVersionZero();

        EntitySaveSummaryVM summary = new EntitySaveSummaryVM();
        service.upsert(incoming, null, summary, UPLOAD);

        ArgumentCaptor<List<DataSubmission>> persistCaptor = ArgumentCaptor.forClass((Class) List.class);
        verify(repository).persistAllAndFlush(persistCaptor.capture());
        assertEquals(List.of(incoming), persistCaptor.getValue());
        assertNotNull(incoming.getId());
        verify(repository, never()).updateAllAndFlush(anyList());

        var recorded = org.mockito.Mockito.inOrder(revisions, outbox);
        recorded.verify(revisions).append(incoming.getId(), 0L, null, RevisionKind.CREATE, UPLOAD);
        ArgumentCaptor<List<OutboxWritePort.OutboxInsert>> outboxCaptor =
            ArgumentCaptor.forClass((Class) List.class);
        recorded.verify(outbox).insert(outboxCaptor.capture());
        assertEquals(List.of("new00000001 SAVE 0"), outboxCaptor.getValue().stream()
            .map(row -> row.submissionUid() + " " + row.eventType() + " " + row.submissionVersion()).toList());
        org.mockito.Mockito.verifyNoMoreInteractions(revisions, outbox);

        assertEquals(List.of("new00000001"), summary.getCreated());
        assertTrue(summary.getUpdated().isEmpty());
    }

    /** The save writes the stored submission its caller locked, so that submission must be the incoming one. */
    @Test
    void theStoredSubmissionMustBeTheIncomingOne() {
        DataSubmission other = persistedSubmission("other000001", "01OTHER00000000000000000001");
        DataSubmission noUid = incomingSubmission(null);

        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
            () -> service.upsert(incomingSubmission("mine0000001"), other, new EntitySaveSummaryVM(), UPLOAD));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
            () -> service.upsert(noUid, null, new EntitySaveSummaryVM(), UPLOAD));

        verifyNoInteractions(repository, outbox, revisions);
    }

    @Test
    void anAdminDeleteIsFlushedThenRecordedAtItsNewVersion() {
        DataSubmission stored = persistedSubmission("delete00002", "01DELETE000000000000000002");
        org.mockito.Mockito.doAnswer(invocation -> {
            stored.setLockVersion(stored.getLockVersion() + 1);
            return null;
        }).when(repository).flush();
        RevisionContext delete = RevisionContext.adminDelete("01JADMIN000000000000000001");

        service.softDelete(stored, delete);

        assertTrue(stored.getDeleted());
        assertNotNull(stored.getDeletedAt());
        var recorded = org.mockito.Mockito.inOrder(repository, revisions, outbox);
        recorded.verify(repository).flush();
        recorded.verify(revisions).append("01DELETE000000000000000002", STORED + 1, STORED, RevisionKind.DELETE, delete);
        recorded.verify(outbox).insert(List.of(new OutboxWritePort.OutboxInsert("01DELETE000000000000000002",
            "delete00002", "version0001", "{\"nested\":{\"value\":\"delete00002\"}}", (long) "delete00002".hashCode(),
            STORED + 1, "DELETE")));
    }

    /** A2 W1-15: a new uid sent already deleted is created deleted, and announced as DELETE, not SAVE. */
    @Test
    void aNewSubmissionSentDeletedIsCreatedAndAnnouncedAsDeleted() {
        DataSubmission incoming = incomingSubmission("newdel00001");
        incoming.setDeleted(true);
        persistStoresVersionZero();

        EntitySaveSummaryVM summary = new EntitySaveSummaryVM();
        service.upsert(incoming, null, summary, UPLOAD);

        assertEquals(List.of("newdel00001"), summary.getCreated());
        verify(revisions).append(incoming.getId(), 0L, null, RevisionKind.CREATE, UPLOAD);
        verify(outbox).insert(org.mockito.ArgumentMatchers.argThat(rows -> rows.size() == 1
            && rows.get(0).eventType().equals("DELETE") && rows.get(0).submissionVersion() == 0L));
    }

    /**
     * The version decides, not the comparison: when the flush finds nothing to write (the stored and the incoming
     * values only differed in how they were held in memory), the version stays and nothing is recorded or announced.
     */
    @Test
    void aWriteThatLeavesTheVersionRecordsNothing() {
        DataSubmission existing = persistedSubmission("noflush0001", "01NOFLUSH0000000000000001");
        DataSubmission incoming = incomingSubmission("noflush0001");
        incoming.setActivity("changed-act");
        when(repository.updateAllAndFlush(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        EntitySaveSummaryVM summary = new EntitySaveSummaryVM();
        service.upsert(incoming, existing, summary, UPLOAD);

        assertEquals(List.of("noflush0001"), summary.getUpdated());
        verify(repository).updateAllAndFlush(List.of(existing));
        verifyNoInteractions(outbox, revisions);
    }

    /** The generic writes would store a submission without a revision or an outbox row: they are refused. */
    @Test
    void theGenericWritesAreRefused() {
        DataSubmission stored = persistedSubmission("generic0001", "01GENERIC0000000000000001");

        org.junit.jupiter.api.Assertions.assertThrows(UnsupportedOperationException.class, () -> service.save(stored));
        org.junit.jupiter.api.Assertions.assertThrows(UnsupportedOperationException.class, () -> service.update(stored));
        org.junit.jupiter.api.Assertions.assertThrows(UnsupportedOperationException.class,
            () -> service.saveWithRelations(stored));

        verifyNoInteractions(outbox, revisions);
        verify(repository, never()).save(any());
        verify(repository, never()).persist(any());
        verify(repository, never()).merge(any());
        verify(repository, never()).update(any());
    }

    /** Every delete names who made it: the generic delete without a revision context is refused. */
    @Test
    void aDeleteThatNamesNoOneIsRefused() {
        DataSubmission stored = persistedSubmission("delete00003", "01DELETE000000000000000003");

        org.junit.jupiter.api.Assertions.assertThrows(UnsupportedOperationException.class,
            () -> service.softDelete(stored));

        assertFalse(stored.getDeleted());
        verifyNoInteractions(outbox, revisions);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("formDataTransitions")
    void formDataComparisonIsNullSafe(
        String transition,
        JsonNode existingFormData,
        JsonNode incomingFormData,
        boolean expectsUpdate) {
        DataSubmission existing = persistedSubmission(
            "jsonnull001",
            "01JSONNULL0000000000000001");
        existing.setFormData(existingFormData);
        DataSubmission incoming = incomingSubmission("jsonnull001");
        incoming.setFormData(incomingFormData);
        updateReturnsInput();

        EntitySaveSummaryVM summary = new EntitySaveSummaryVM();
        service.upsert(incoming, existing, summary, UPLOAD);

        assertEquals(List.of("jsonnull001"), summary.getUpdated(), transition);
        if (expectsUpdate) {
            verifySingleUpdate(existing, RevisionKind.UPDATE, "UPDATE");
            assertEquals(incomingFormData, existing.getFormData());
            if (incomingFormData != null) {
                assertNotSame(incomingFormData, existing.getFormData());
            } else {
                assertNull(existing.getFormData());
            }
        } else {
            verify(repository, never()).updateAllAndFlush(anyList());
            verifyNoInteractions(outbox, revisions);
        }
    }

    private static Stream<Arguments> mutableFieldChanges() {
        return Stream.of(
            Arguments.of("formData", (Consumer<DataSubmission>) submission ->
                submission.setFormData(JsonNodeFactory.instance.objectNode().put("changed", true))),
            Arguments.of("activity", (Consumer<DataSubmission>) submission ->
                submission.setActivity("changed-act")),
            Arguments.of("assignment", (Consumer<DataSubmission>) submission ->
                submission.setAssignment("changed-asg")),
            Arguments.of("team", (Consumer<DataSubmission>) submission ->
                submission.setTeam("changedteam")),
            Arguments.of("orgUnit", (Consumer<DataSubmission>) submission ->
                submission.setOrgUnit("changed-org")),
            Arguments.of("status", (Consumer<DataSubmission>) submission ->
                submission.setStatus(FlowStatus.DONE)),
            Arguments.of("orgUnitCode", (Consumer<DataSubmission>) submission ->
                submission.setOrgUnitCode("CHANGED-OU")),
            Arguments.of("orgUnitName", (Consumer<DataSubmission>) submission ->
                submission.setOrgUnitName("Changed Org Unit")),
            Arguments.of("teamCode", (Consumer<DataSubmission>) submission ->
                submission.setTeamCode("CHANGED-TM"))
        );
    }

    private static Stream<Arguments> formDataTransitions() {
        return Stream.of(
            Arguments.of(
                "null to JSON updates",
                null,
                JsonNodeFactory.instance.objectNode().put("value", "incoming"),
                true),
            Arguments.of(
                "JSON to null updates",
                JsonNodeFactory.instance.objectNode().put("value", "existing"),
                null,
                true),
            Arguments.of("null to null is unchanged", null, null, false),
            Arguments.of(
                "structurally equal JSON is unchanged",
                JsonNodeFactory.instance.objectNode()
                    .set("nested", JsonNodeFactory.instance.objectNode().put("value", 1)),
                JsonNodeFactory.instance.objectNode()
                    .set("nested", JsonNodeFactory.instance.objectNode().put("value", 1)),
                false)
        );
    }

    /** A flush raises the version of each submission it updates, as Hibernate does. */
    private void updateReturnsInput() {
        when(repository.updateAllAndFlush(anyList())).thenAnswer(invocation -> {
            List<DataSubmission> updated = invocation.getArgument(0);
            updated.forEach(submission -> submission.setLockVersion(submission.getLockVersion() + 1));
            return updated;
        });
    }

    /** A persist stores version 0, as Hibernate does. */
    private void persistStoresVersionZero() {
        when(repository.persistAllAndFlush(anyList())).thenAnswer(invocation -> {
            List<DataSubmission> persisted = invocation.getArgument(0);
            persisted.forEach(submission -> submission.setLockVersion(0L));
            return persisted;
        });
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void verifySingleUpdate(DataSubmission expected, RevisionKind kind, String eventType) {
        ArgumentCaptor<List<DataSubmission>> updateCaptor =
            ArgumentCaptor.forClass((Class) List.class);
        verify(repository).updateAllAndFlush(updateCaptor.capture());
        assertEquals(List.of(expected), updateCaptor.getValue());

        var recorded = org.mockito.Mockito.inOrder(revisions, outbox);
        recorded.verify(revisions).append(expected.getId(), STORED + 1, STORED, kind, UPLOAD);
        ArgumentCaptor<List<OutboxWritePort.OutboxInsert>> outboxCaptor =
            ArgumentCaptor.forClass((Class) List.class);
        recorded.verify(outbox).insert(outboxCaptor.capture());
        assertEquals(1, outboxCaptor.getValue().size());
        OutboxWritePort.OutboxInsert row = outboxCaptor.getValue().get(0);
        assertEquals(expected.getUid(), row.submissionUid());
        assertEquals(eventType, row.eventType());
        assertEquals(STORED + 1, row.submissionVersion());
        org.mockito.Mockito.verifyNoMoreInteractions(revisions, outbox);
    }

    private DataSubmission persistedSubmission(String uid, String id) {
        DataSubmission submission = incomingSubmission(uid);
        submission.setId(id);
        submission.setSerialNumber((long) uid.hashCode());
        submission.setLockVersion(STORED);
        return submission;
    }

    private DataSubmission incomingSubmission(String uid) {
        DataSubmission submission = new DataSubmission();
        submission.setUid(uid);
        submission.setForm("form0000001");
        submission.setFormVersion("version0001");
        submission.setVersion(3);
        submission.setFormData(objectMapper.createObjectNode()
            .set("nested", objectMapper.createObjectNode().put("value", uid)));
        submission.setActivity("activity001");
        submission.setAssignment("assign00001");
        submission.setTeam("team0000001");
        submission.setTeamCode("TEAM-001");
        submission.setOrgUnit("orgunit0001");
        submission.setOrgUnitCode("OU-001");
        submission.setOrgUnitName("Org Unit One");
        submission.setStatus(FlowStatus.IN_PROGRESS);
        submission.setStartEntryTime(Instant.parse("2026-07-01T10:00:00Z"));
        submission.setFinishedEntryTime(Instant.parse("2026-07-01T11:00:00Z"));
        submission.setDeleted(false);
        return submission;
    }
}
