package org.nmcpye.datarun.jpa.datasubmission.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.nmcpye.datarun.common.EntitySaveSummaryVM;
import org.nmcpye.datarun.jpa.accessfilter.UserAccessService;
import org.nmcpye.datarun.jpa.datasubmission.DataSubmission;
import org.nmcpye.datarun.jpa.datasubmission.repository.DataSubmissionRepository;
import org.nmcpye.datarun.outbox.repository.OutboxWritePort;
import org.nmcpye.datarun.security.CurrentUserDetails;
import org.springframework.cache.CacheManager;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
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
    @SuppressWarnings({"rawtypes", "unchecked"})
    void persistsUpdateAndDeleteCollectionsSeparately() {
        DataSubmission existingUpdate = persistedSubmission(
            "update00001",
            "01UPDATE000000000000000001");
        DataSubmission existingDelete = persistedSubmission(
            "delete00001",
            "01DELETE000000000000000001");
        DataSubmission incomingUpdate = incomingSubmission("update00001");
        DataSubmission incomingDelete = incomingSubmission("delete00001");
        incomingDelete.setDeleted(true);

        when(repository.findAllByUidIn(any()))
            .thenReturn(List.of(existingUpdate, existingDelete));
        when(repository.updateAllAndFlush(anyList()))
            .thenAnswer(invocation -> invocation.getArgument(0));

        EntitySaveSummaryVM summary = new EntitySaveSummaryVM();
        List<DataSubmission> results = service.upsertAll(
            List.of(incomingUpdate, incomingDelete),
            mock(CurrentUserDetails.class),
            summary);

        ArgumentCaptor<List<DataSubmission>> persistedBatches =
            ArgumentCaptor.forClass((Class) List.class);
        verify(repository, times(2))
            .updateAllAndFlush(persistedBatches.capture());
        assertEquals(2, persistedBatches.getAllValues().size());
        assertEquals(List.of(existingUpdate),
            persistedBatches.getAllValues().get(0));
        assertEquals(List.of(existingDelete),
            persistedBatches.getAllValues().get(1));
        assertNotNull(existingDelete.getDeletedAt());

        ArgumentCaptor<List<OutboxWritePort.OutboxInsert>> outboxBatches =
            ArgumentCaptor.forClass((Class) List.class);
        ArgumentCaptor<String> eventTypes =
            ArgumentCaptor.forClass(String.class);
        verify(outbox, times(2)).insertByEventType(
            outboxBatches.capture(),
            eventTypes.capture());
        assertEquals(List.of("UPDATE", "DELETE"),
            eventTypes.getAllValues());
        assertEquals("update00001",
            outboxBatches.getAllValues().get(0).get(0).submissionUid);
        assertEquals("delete00001",
            outboxBatches.getAllValues().get(1).get(0).submissionUid);

        assertEquals(List.of("update00001", "delete00001"),
            summary.getUpdated());
        assertEquals(2, results.size());
        assertSame(existingUpdate, results.get(0));
        assertSame(existingDelete, results.get(1));
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
        submission.setFormData(
            objectMapper.createObjectNode().put("value", uid));
        submission.setDeleted(false);
        return submission;
    }
}
