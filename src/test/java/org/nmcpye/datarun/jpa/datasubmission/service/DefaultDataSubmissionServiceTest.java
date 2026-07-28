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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class DefaultDataSubmissionServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private DataSubmissionRepository repository;
    private OutboxWritePort outbox;
    private DefaultDataSubmissionService service;

    @BeforeEach
    void setUp() {
        repository = mock(DataSubmissionRepository.class);
        outbox = mock(OutboxWritePort.class);
        service = new DefaultDataSubmissionService(
            repository,
            mock(CacheManager.class),
            mock(UserAccessService.class),
            objectMapper,
            outbox);
    }

    @Test
    void unchangedNormalRetryReturnsSuccessWithoutWriting() {
        DataSubmission existing = persistedSubmission(
            "unchang0001",
            "01UNCHANGED000000000000001");
        DataSubmission incoming = incomingSubmission("unchang0001");
        findExisting(existing);

        EntitySaveSummaryVM summary = new EntitySaveSummaryVM();
        List<DataSubmission> results = service.upsertAll(List.of(incoming), summary);

        assertEquals(List.of("unchang0001"), summary.getUpdated());
        assertEquals(1, results.size());
        assertSame(existing, results.get(0));
        verify(repository, never()).persistAllAndFlush(anyList());
        verify(repository, never()).updateAllAndFlush(anyList());
        verifyNoInteractions(outbox);
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
        findExisting(existing);
        updateReturnsInput();

        EntitySaveSummaryVM summary = new EntitySaveSummaryVM();
        List<DataSubmission> results = service.upsertAll(List.of(incoming), summary);

        assertEquals(List.of("mutable0001"), summary.getUpdated(), field);
        assertSame(existing, results.get(0));
        verifySingleUpdate(existing, "UPDATE");
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
        findExisting(existing);

        EntitySaveSummaryVM summary = new EntitySaveSummaryVM();
        List<DataSubmission> results = service.upsertAll(List.of(incoming), summary);

        assertEquals(List.of("ignored0001"), summary.getUpdated());
        assertSame(existing, results.get(0));
        verify(repository, never()).updateAllAndFlush(anyList());
        verifyNoInteractions(outbox);
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
        findExisting(existing);

        EntitySaveSummaryVM summary = new EntitySaveSummaryVM();
        List<DataSubmission> results = service.upsertAll(List.of(incoming), summary);

        assertEquals(List.of("deleted0001"), summary.getUpdated());
        assertSame(existing, results.get(0));
        assertEquals(deletedAt, existing.getDeletedAt());
        assertEquals(originalFormData, existing.getFormData());
        assertEquals(originalActivity, existing.getActivity());
        verify(repository, never()).updateAllAndFlush(anyList());
        verifyNoInteractions(outbox);
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
        findExisting(existing);

        EntitySaveSummaryVM summary = new EntitySaveSummaryVM();
        List<DataSubmission> results = service.upsertAll(List.of(incoming), summary);

        assertEquals(List.of("delkeep0001"), summary.getUpdated());
        assertSame(existing, results.get(0));
        assertTrue(existing.getDeleted());
        assertEquals(deletedAt, existing.getDeletedAt());
        verify(repository, never()).updateAllAndFlush(anyList());
        verifyNoInteractions(outbox);
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
        findExisting(existing);
        updateReturnsInput();

        EntitySaveSummaryVM summary = new EntitySaveSummaryVM();
        List<DataSubmission> results = service.upsertAll(List.of(incoming), summary);

        assertEquals(List.of("delupdt0001"), summary.getUpdated());
        assertSame(existing, results.get(0));
        assertTrue(existing.getDeleted());
        assertEquals(deletedAt, existing.getDeletedAt());
        assertEquals("changed-act", existing.getActivity());
        verifySingleUpdate(existing, "UPDATE");
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
        findExisting(existing);
        updateReturnsInput();
        Instant beforeDelete = Instant.now();

        EntitySaveSummaryVM summary = new EntitySaveSummaryVM();
        List<DataSubmission> results = service.upsertAll(List.of(incoming), summary);

        assertEquals(List.of("delete00001"), summary.getUpdated());
        assertSame(existing, results.get(0));
        assertTrue(existing.getDeleted());
        assertNotNull(existing.getDeletedAt());
        assertFalse(existing.getDeletedAt().isBefore(beforeDelete));
        assertNotEquals(suppliedDeletedAt, existing.getDeletedAt());
        assertEquals(originalFormData, existing.getFormData());
        assertEquals(originalActivity, existing.getActivity());
        verifySingleUpdate(existing, "DELETE");
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void mixedBatchWritesOnlyNewChangedAndFirstDelete() {
        DataSubmission newSubmission = incomingSubmission("new00000001");

        DataSubmission existingChanged = persistedSubmission(
            "changed0001",
            "01CHANGED0000000000000001");
        DataSubmission incomingChanged = incomingSubmission("changed0001");
        incomingChanged.setOrgUnitName("Changed Org Unit");

        DataSubmission existingUnchanged = persistedSubmission(
            "unchang0001",
            "01UNCHANGED000000000000001");
        DataSubmission incomingUnchanged = incomingSubmission("unchang0001");

        DataSubmission existingFirstDelete = persistedSubmission(
            "delete00001",
            "01DELETE000000000000000001");
        DataSubmission incomingFirstDelete = incomingSubmission("delete00001");
        incomingFirstDelete.setDeleted(true);

        Instant deletedAt = Instant.parse("2026-07-20T10:15:30Z");
        DataSubmission existingDeletedRetry = persistedSubmission(
            "deleted0001",
            "01DELETED0000000000000001");
        existingDeletedRetry.setDeleted(true);
        existingDeletedRetry.setDeletedAt(deletedAt);
        DataSubmission incomingDeletedRetry = incomingSubmission("deleted0001");
        incomingDeletedRetry.setDeleted(true);
        incomingDeletedRetry.setTeam("ignored-team");

        when(repository.findAllByUidIn(any())).thenReturn(List.of(
            existingChanged,
            existingUnchanged,
            existingFirstDelete,
            existingDeletedRetry));
        when(repository.persistAllAndFlush(anyList()))
            .thenAnswer(invocation -> invocation.getArgument(0));
        updateReturnsInput();

        EntitySaveSummaryVM summary = new EntitySaveSummaryVM();
        List<DataSubmission> results = service.upsertAll(List.of(
            newSubmission,
            incomingChanged,
            incomingUnchanged,
            incomingFirstDelete,
            incomingDeletedRetry), summary);

        ArgumentCaptor<List<DataSubmission>> persistCaptor =
            ArgumentCaptor.forClass((Class) List.class);
        verify(repository).persistAllAndFlush(persistCaptor.capture());
        assertEquals(List.of(newSubmission), persistCaptor.getValue());

        ArgumentCaptor<List<DataSubmission>> updateCaptor =
            ArgumentCaptor.forClass((Class) List.class);
        verify(repository, times(2)).updateAllAndFlush(updateCaptor.capture());
        assertEquals(List.of(existingChanged), updateCaptor.getAllValues().get(0));
        assertEquals(List.of(existingFirstDelete), updateCaptor.getAllValues().get(1));

        ArgumentCaptor<List<OutboxWritePort.OutboxInsert>> outboxCaptor =
            ArgumentCaptor.forClass((Class) List.class);
        ArgumentCaptor<String> eventTypeCaptor = ArgumentCaptor.forClass(String.class);
        verify(outbox, times(3)).insertByEventType(
            outboxCaptor.capture(),
            eventTypeCaptor.capture());
        assertEquals(List.of("SAVE", "UPDATE", "DELETE"), eventTypeCaptor.getAllValues());
        assertEquals("new00000001", outboxCaptor.getAllValues().get(0).get(0).submissionUid);
        assertEquals("changed0001", outboxCaptor.getAllValues().get(1).get(0).submissionUid);
        assertEquals("delete00001", outboxCaptor.getAllValues().get(2).get(0).submissionUid);

        assertEquals(List.of("new00000001"), summary.getCreated());
        assertEquals(List.of(
            "changed0001",
            "unchang0001",
            "delete00001",
            "deleted0001"), summary.getUpdated());
        assertEquals(5, results.size());
        assertSame(newSubmission, results.get(0));
        assertSame(existingChanged, results.get(1));
        assertSame(existingUnchanged, results.get(2));
        assertSame(existingFirstDelete, results.get(3));
        assertSame(existingDeletedRetry, results.get(4));
        assertEquals(deletedAt, existingDeletedRetry.getDeletedAt());
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
        findExisting(existing);
        updateReturnsInput();

        EntitySaveSummaryVM summary = new EntitySaveSummaryVM();
        List<DataSubmission> results = service.upsertAll(List.of(incoming), summary);

        assertEquals(List.of("jsonnull001"), summary.getUpdated(), transition);
        assertSame(existing, results.get(0));
        if (expectsUpdate) {
            verifySingleUpdate(existing, "UPDATE");
            assertEquals(incomingFormData, existing.getFormData());
            if (incomingFormData != null) {
                assertNotSame(incomingFormData, existing.getFormData());
            } else {
                assertNull(existing.getFormData());
            }
        } else {
            verify(repository, never()).updateAllAndFlush(anyList());
            verifyNoInteractions(outbox);
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

    private void findExisting(DataSubmission existing) {
        when(repository.findAllByUidIn(any())).thenReturn(List.of(existing));
    }

    private void updateReturnsInput() {
        when(repository.updateAllAndFlush(anyList()))
            .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void verifySingleUpdate(DataSubmission expected, String eventType) {
        ArgumentCaptor<List<DataSubmission>> updateCaptor =
            ArgumentCaptor.forClass((Class) List.class);
        verify(repository).updateAllAndFlush(updateCaptor.capture());
        assertEquals(List.of(expected), updateCaptor.getValue());

        ArgumentCaptor<List<OutboxWritePort.OutboxInsert>> outboxCaptor =
            ArgumentCaptor.forClass((Class) List.class);
        verify(outbox).insertByEventType(outboxCaptor.capture(),
            org.mockito.ArgumentMatchers.eq(eventType));
        assertEquals(1, outboxCaptor.getValue().size());
        assertEquals(expected.getUid(), outboxCaptor.getValue().get(0).submissionUid);
    }

    private DataSubmission persistedSubmission(String uid, String id) {
        DataSubmission submission = incomingSubmission(uid);
        submission.setId(id);
        submission.setSerialNumber((long) uid.hashCode());
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
