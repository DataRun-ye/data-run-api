package org.nmcpye.datarun.drun.mongo.service.impl;

import jakarta.el.PropertyNotFoundException;
import org.nmcpye.datarun.drun.mongo.domain.DataFormSubmission;
import org.nmcpye.datarun.drun.mongo.domain.DataFormSubmissionHistory;
import org.nmcpye.datarun.drun.mongo.repository.DataFormSubmissionHistoryRepository;
import org.nmcpye.datarun.drun.mongo.repository.DataFormSubmissionRepositoryCustom;
import org.nmcpye.datarun.drun.mongo.service.DataFormSubmissionService;
import org.nmcpye.datarun.drun.postgres.repository.ActivityRelationalRepositoryCustom;
import org.nmcpye.datarun.drun.postgres.repository.TeamRelationalRepositoryCustom;
import org.nmcpye.datarun.security.AuthoritiesConstants;
import org.nmcpye.datarun.security.SecurityUtils;
import org.nmcpye.datarun.utils.CodeGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

/**
 * Service Implementation for managing {@link DataFormSubmission}.
 */
@Service
@Primary
public class DataFormSubmissionServiceImpl
    extends IdentifiableMongoServiceImpl<DataFormSubmission>
    implements DataFormSubmissionService {

    private final Logger log = LoggerFactory.getLogger(DataFormSubmissionServiceImpl.class);

    private final DataFormSubmissionRepositoryCustom repository;
    final DataFormSubmissionHistoryRepository historyRepository;
    private final ActivityRelationalRepositoryCustom activityRepository;
    private final TeamRelationalRepositoryCustom teamRepository;
    final private MongoTemplate mongoTemplate;
    private final SequenceGeneratorService sequenceGeneratorService;

    public DataFormSubmissionServiceImpl(
        DataFormSubmissionRepositoryCustom repository, DataFormSubmissionHistoryRepository historyRepository,
        ActivityRelationalRepositoryCustom activityRepository,
        TeamRelationalRepositoryCustom teamRepository, MongoTemplate mongoTemplate,
        SequenceGeneratorService sequenceGeneratorService) {
        super(repository);
        this.repository = repository;
        this.historyRepository = historyRepository;
        this.activityRepository = activityRepository;
        this.teamRepository = teamRepository;
        this.mongoTemplate = mongoTemplate;
        this.sequenceGeneratorService = sequenceGeneratorService;
    }

    @Override
    public DataFormSubmission saveVersioning(DataFormSubmission submission) {
        DataFormSubmission existingSubmission = repository.findById(submission.getId()).orElse(null);
        if (existingSubmission != null) {
            // Create a new version of the current data
            DataFormSubmissionHistory history = new DataFormSubmissionHistory(
                existingSubmission,
                Instant.now()
            );

            // Save the old version in the history collection
            historyRepository.save(history);

            // Increment version number and update the current document
            submission.setVersion(existingSubmission.getVersion() + 1);
        }

        return repository.save(submission);
    }

    @Override
    public DataFormSubmission saveWithRelations(DataFormSubmission newSubmission) {
        final DataFormSubmission dataFormSubmission = createSubmission(newSubmission);

        activityRepository.findByUid(dataFormSubmission.getActivity())
            .ifPresentOrElse((a) -> dataFormSubmission.setActivity(a.getUid()),
                () -> {
                    throw new PropertyNotFoundException("Activity not found: " + dataFormSubmission.getOrgUnit());
                });
        teamRepository.findByUid(dataFormSubmission.getTeam())
            .ifPresentOrElse((a) -> dataFormSubmission.setTeam(a.getUid()),
                () -> {
                    throw new PropertyNotFoundException("Team not found: " + dataFormSubmission.getOrgUnit());
                });
//        orgUnitRelationalRepositoryCustom.findByUid(dataFormSubmission.getOrgUnit())
//            .ifPresentOrElse((a) -> dataFormSubmission.setOrgUnit(a.getUid()),
//                () -> {
//                    throw new PropertyNotFoundException("OrgUnit not found: " + dataFormSubmission.getOrgUnit());
//                });

//        return saveVersioning(dataFormSubmission);
        return repository.save(dataFormSubmission);
    }

    @Override
    public Page<DataFormSubmission> findSubmissionsBySerialNumber(Long serialNumber, String form, Pageable pageable) {
        if (form != null) {
            return repository.findBySerialNumberGreaterThanAndForm(serialNumber, form, pageable);
        }
        return repository.findBySerialNumberGreaterThan(serialNumber, pageable);
    }

    @Override
    public Page<DataFormSubmission> findAllByForm(List<String> forms, Pageable pageable) {
        Query query = new Query(Criteria.where("form").in(forms));
        List<DataFormSubmission> submissions = mongoTemplate.find(query, DataFormSubmission.class);

        return getDataFormSubmissions(pageable, submissions);
    }

    @Override
    public Page<DataFormSubmission> findAllByUser(Pageable pageable) {
        if (SecurityUtils.hasCurrentUserAnyOfAuthorities(AuthoritiesConstants.ADMIN)) {
            return repository.findAll(pageable);
        }

        if (SecurityUtils.getCurrentUserLogin().isPresent()) {
            String login = SecurityUtils.getCurrentUserLogin().get();
            Query query = new Query(Criteria.where("createdBy").is(login));
            List<DataFormSubmission> submissions = mongoTemplate.find(query, DataFormSubmission.class);
            return getDataFormSubmissions(pageable, submissions);
        }
        return Page.empty(pageable);
    }

    private static Page<DataFormSubmission> getDataFormSubmissions(Pageable pageable, List<DataFormSubmission> submissions) {
        if (!pageable.isPaged()) {
            return new PageImpl<>(submissions);
        }

        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), submissions.size());
        if (start > end) {
            return Page.empty(pageable);
        }

        List<DataFormSubmission> sublist = submissions.subList(start, end);
        return new PageImpl<>(sublist, pageable, submissions.size());
    }

    @Override
    public List<String> getTeamsAfterDate(Date createdDate) {
        Query query = new Query();
        query.addCriteria(Criteria.where("created_date").gte(createdDate));

        return mongoTemplate.findDistinct(query, "team", "data_form_submission", String.class);
    }

    /// temp solution
    public DataFormSubmission createSubmission(DataFormSubmission submission) {

        Map<String, Object> formData = submission.getFormData();

        // Automatically add group indices to any arrays of objects inside formData
        Map<String, Object> updatedFormData = addGroupIndicesToFormData(formData);
        submission.setFormData(updatedFormData);

        if (submission.getSerialNumber() == null) {
            // Generate a unique serial number for new submissions
            long serialNumber = sequenceGeneratorService.getNextSequence("dataFormSubmissionId");
            submission.setSerialNumber(serialNumber);
        }

        return submission;
    }


    private Map<String, Object> addGroupIndicesToFormData(Map<String, Object> formData) {
        Map<String, Object> updatedFormData = new HashMap<>();
        final Object parentId = formData.getOrDefault("uid",
            CodeGenerator.generateUid() + "_" + CodeGenerator.generateCode(11));
        formData.putIfAbsent("uid", parentId);

        for (Map.Entry<String, Object> entry : formData.entrySet()) {
            Object value = entry.getValue();

            // If it's an array of objects, add group indices
            if (value instanceof List) {
                List<?> list = (List<?>) value;
                if (!list.isEmpty() && list.get(0) instanceof Map) {
                    List<Map<String, Object>> updatedList = new ArrayList<>();
                    for (int i = 0; i < list.size(); i++) {
                        Map<String, Object> objectInArray = (Map<String, Object>) list.get(i);
                        objectInArray.put("repeatIndex", i + 1);  // Add groupIndex (starting from 1)
                        objectInArray.putIfAbsent("repeatUid", CodeGenerator.generateUid() + "_" + CodeGenerator.generateCode(3));  // Add groupIndex (starting from 1)
                        objectInArray.putIfAbsent("parentUid", parentId);  // Add groupIndex (starting from 1)
                        updatedList.add(objectInArray);
                    }
                    updatedFormData.put(entry.getKey(), updatedList);
                } else {
                    // If it's not an array of objects, just copy as is
                    updatedFormData.put(entry.getKey(), list);
                }
            } else if (value instanceof Map) {
                // If it's a nested map, recursively process it
                updatedFormData.put(entry.getKey(), addGroupIndicesToFormData((Map<String, Object>) value));
            } else {
                // If it's a simple value, just copy as is
                updatedFormData.put(entry.getKey(), value);
            }
        }
        return updatedFormData;
    }
}
