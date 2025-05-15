package org.nmcpye.datarun.mongo.service.impl;

import org.nmcpye.datarun.common.mongo.impl.DefaultMongoAuditableObjectService;
import org.nmcpye.datarun.drun.postgres.domain.Assignment;
import org.nmcpye.datarun.drun.postgres.repository.AssignmentRepository;
import org.nmcpye.datarun.mongo.domain.AssignmentSubmissionHistory;
import org.nmcpye.datarun.mongo.domain.DataFormSubmission;
import org.nmcpye.datarun.mongo.repository.AssignmentSubmissionHistoryRepository;
import org.nmcpye.datarun.mongo.repository.DataFormTemplateRepository;
import org.nmcpye.datarun.mongo.service.AssignmentSubmissionHistoryService;
import org.springframework.cache.CacheManager;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class AssignmentSubmissionHistoryServiceImpl
    extends DefaultMongoAuditableObjectService<AssignmentSubmissionHistory>
    implements AssignmentSubmissionHistoryService {

    private final AssignmentSubmissionHistoryRepository repository;
    private final AssignmentRepository assignmentRepository;
    private final DataFormTemplateRepository dataFormRepository;

    public AssignmentSubmissionHistoryServiceImpl(AssignmentSubmissionHistoryRepository repository,
                                                  CacheManager cacheManager,
                                                  AssignmentRepository assignmentRepository,
                                                  DataFormTemplateRepository dataFormRepository) {
        super(repository, cacheManager);
        this.repository = repository;
        this.assignmentRepository = assignmentRepository;
        this.dataFormRepository = dataFormRepository;
    }

    @Override
    public List<AssignmentSubmissionHistory> findByUidAndTeamId(String uid, String teamId) {
        return repository.findByUidAndTeam(uid, teamId);
    }

    @Override
    public Optional<AssignmentSubmissionHistory.HistoryEntry> findLastEntryByUidAndTeamId(String uid, String teamId) {
        return repository.findLastEntryByUidAndTeam(uid, teamId)
            .flatMap(history -> history.getEntries().stream().findFirst());
    }

    @Override
    public Optional<AssignmentSubmissionHistory.HistoryEntry> findLastEntryByUid(String uid) {
        return repository.findLastEntryByUid(uid)
            .flatMap(history -> history.getEntries().stream().findFirst());
    }

    @Override
    public Optional<AssignmentSubmissionHistory.HistoryEntry> findLastEntryByUidAndUser(String uid, String user) {
        return repository.findLastEntryByUidAndUser(uid, user)
            .flatMap(history -> history.getEntries().stream().findFirst());
    }

    public void updateSubmissionHistory(DataFormSubmission submission) {
        String assignmentUid = submission.getAssignment();
        Assignment assignment = assignmentRepository.findByUid(assignmentUid).orElseThrow();

        AssignmentSubmissionHistory history = repository.findByUid(assignmentUid)
            .orElseGet(() -> {
                AssignmentSubmissionHistory newHistory = new AssignmentSubmissionHistory();
                newHistory.setUid(assignmentUid);
                return newHistory;
            });

        AssignmentSubmissionHistory.HistoryEntry entry = new AssignmentSubmissionHistory.HistoryEntry();
        entry.setSubmission(submission.getUid());
        entry.setDeleted(submission.getDeleted());

        entry.setAssignedTeam(assignment.getTeam().getUid());
        entry.setEntryDate(Instant.now());
        entry.setSubmissionDate(submission.getCreatedDate());
        entry.setSubmissionTeam(submission.getTeam());
        var username = submission.getFormData().get("_username");
        if (username instanceof String) {
            entry.setSubmissionUser((String) username);
        }
        entry.setForm(submission.getForm());
        entry.setFormVersion(submission.getForm());

//        final DataFormTemplate dataForm = dataFormRepository.findByUid(submission.getForm()).orElseThrow();

//        var totalResources = ResourceSummarizer.sumNumericResources(submission.getFormData());
//        var totalResources2 = ResourceSummarizer.sumNumericResources2(submission.getFormData());


//        var extractedValues = FormProcessor.extractValues(submission.getFormData(), dataForm);

//        var status = extractedValues.get("status");
//        var status = submission.getStatus();

//        if (status instanceof AssignmentStatus status1) {
        entry.setSubmissionStatus(submission.getStatus());
//        }

        entry.setAllocatedResources(assignment.getAllocatedResources());
//        entry.setSubmittedResources(totalResources);

        history.getEntries().add(entry);
        repository.save(history);
    }

//    private Map<String, Object> extractSubmittedResources(Map<String, Object> formData, Map<String, Object> allocatedResources) {
//        Map<String, Object> submittedResources = new HashMap<>();
//        for (String key : allocatedResources.keySet()) {
//            Object value = extractNestedValue(formData, key.toLowerCase());
//            if (value != null) {
//                submittedResources.put(key.toLowerCase(), value);
//            }
//        }
//        return submittedResources;
//    }
//
//    private Object extractNestedValue(Map<String, Object> data, String key) {
//        if (data.containsKey(key)) {
//            return data.get(key);
//        }
//        for (Object value : data.values()) {
//            if (value instanceof Map) {
//                Object nestedValue = extractNestedValue((Map<String, Object>) value, key);
//                if (nestedValue != null) {
//                    return nestedValue;
//                }
//            } else if (value instanceof List) {
//                for (Object item : (List) value) {
//                    if (item instanceof Map) {
//                        Object nestedValue = extractNestedValue((Map<String, Object>) item, key);
//                        if (nestedValue != null) {
//                            return nestedValue;
//                        }
//                    }
//                }
//            }
//        }
//        return null;
//    }
}
