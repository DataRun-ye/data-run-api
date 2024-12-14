package org.nmcpye.datarun.mongo.service.impl;

import jakarta.el.PropertyNotFoundException;
import org.apache.commons.lang3.NotImplementedException;
import org.nmcpye.datarun.drun.postgres.domain.OrgUnit;
import org.nmcpye.datarun.drun.postgres.repository.ActivityRelationalRepositoryCustom;
import org.nmcpye.datarun.drun.postgres.repository.AssignmentRelationalRepositoryCustom;
import org.nmcpye.datarun.drun.postgres.repository.OrgUnitRelationalRepositoryCustom;
import org.nmcpye.datarun.drun.postgres.repository.TeamRelationalRepositoryCustom;
import org.nmcpye.datarun.mongo.domain.MetadataSubmission;
import org.nmcpye.datarun.mongo.repository.MetadataSubmissionRepositoryCustom;
import org.nmcpye.datarun.mongo.service.MetadataSubmissionService;
import org.nmcpye.datarun.security.AuthoritiesConstants;
import org.nmcpye.datarun.security.SecurityUtils;
import org.nmcpye.datarun.utils.CodeGenerator;
import org.nmcpye.datarun.web.rest.mongo.submission.QueryRequest;
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

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service Implementation for managing {@link MetadataSubmission}.
 */
@Service
@Primary
public class MetadataSubmissionServiceImpl
    extends IdentifiableMongoServiceImpl<MetadataSubmission>
    implements MetadataSubmissionService {

    private final Logger log = LoggerFactory.getLogger(MetadataSubmissionServiceImpl.class);

    private final MetadataSubmissionRepositoryCustom repository;
    private final TeamRelationalRepositoryCustom teamRepository;
    private final ActivityRelationalRepositoryCustom activityRepository;
    private final AssignmentRelationalRepositoryCustom assignmentRepository;

    private final OrgUnitRelationalRepositoryCustom orgUnitRelationalRepositoryCustom;
    final private MongoTemplate mongoTemplate;
    private final SequenceGeneratorService sequenceGeneratorService;

    public MetadataSubmissionServiceImpl(
        MetadataSubmissionRepositoryCustom repository, ActivityRelationalRepositoryCustom activityRepository, TeamRelationalRepositoryCustom teamRepository, AssignmentRelationalRepositoryCustom assignmentRepository,
        OrgUnitRelationalRepositoryCustom orgUnitRelationalRepositoryCustom,
        MongoTemplate mongoTemplate, SequenceGeneratorService sequenceGeneratorService) {
        super(repository, mongoTemplate);
        this.repository = repository;
        this.activityRepository = activityRepository;
        this.teamRepository = teamRepository;
        this.assignmentRepository = assignmentRepository;
        this.orgUnitRelationalRepositoryCustom = orgUnitRelationalRepositoryCustom;
        this.mongoTemplate = mongoTemplate;
        this.sequenceGeneratorService = sequenceGeneratorService;
    }


    @Override
    public MetadataSubmission saveWithRelations(MetadataSubmission newSubmission) {
        final MetadataSubmission metadataSubmission = createSubmission(newSubmission);

        switch (newSubmission.getResourceType()) {
            case Team -> teamRepository.findByUid(metadataSubmission.getResourceId())
                .ifPresentOrElse((a) -> metadataSubmission.setResourceId(a.getUid()),
                    () -> {
                        throw new PropertyNotFoundException("Team not found: " + metadataSubmission.getResourceId());
                    });
            case Activity -> activityRepository.findByUid(metadataSubmission.getResourceId())
                .ifPresentOrElse((a) -> metadataSubmission.setResourceId(a.getUid()),
                    () -> {
                        throw new PropertyNotFoundException("Activity not found: " + metadataSubmission.getResourceId());
                    });
            case OrgUnit -> orgUnitRelationalRepositoryCustom.findByUid(metadataSubmission.getResourceId())
                .ifPresentOrElse((a) -> metadataSubmission.setResourceId(a.getUid()),
                    () -> {
                        throw new PropertyNotFoundException("OrgUnit not found: " + metadataSubmission.getResourceId());
                    });
            case Assignment -> assignmentRepository.findByUid(newSubmission.getResourceId())
                .ifPresentOrElse((a) -> newSubmission.setResourceId(a.getUid()),
                    () -> {
                        throw new PropertyNotFoundException("Assignment not found: " + newSubmission.getResourceId());
                    });
            default ->
                throw new NotImplementedException(newSubmission.getResourceType().toString() + "not Implemented");
        }
        return repository.save(metadataSubmission);
    }

//    @Override
//    public Page<MetadataSubmission> findSubmissionsBySerialNumber(Long serialNumber, String form, Pageable pageable) {
//        if (form != null) {
//            return repository.findBySerialNumberGreaterThanAndMetadataSchema(serialNumber, form, pageable);
//        }
//        return repository.findBySerialNumberGreaterThan(serialNumber, pageable);
//    }

//    @Override
//    public Page<MetadataSubmission> findAllByForm(List<String> forms, Pageable pageable) {
//        Query query = new Query(Criteria.where("form").in(forms));
//        List<MetadataSubmission> submissions = mongoTemplate.find(query, MetadataSubmission.class);
//
//        return getMetadataSubmissions(pageable, submissions);
//    }

//    @Override
//    public Page<MetadataSubmission> findAllByEntity(List<String> entityUids, Pageable pageable) {
//        Query query = new Query(Criteria.where("entityUid").in(entityUids));
//        List<MetadataSubmission> submissions = mongoTemplate.find(query, MetadataSubmission.class);
//
//        return getMetadataSubmissions(pageable, submissions);
//    }

//    @Override
//    public Page<MetadataSubmission> findAllByResourceType(String resourceType, Pageable pageable) {
//        Query query = new Query(Criteria.where("resourceType").in(resourceType));
//        List<MetadataSubmission> submissions = mongoTemplate.find(query, MetadataSubmission.class);
//
//        return getMetadataSubmissions(pageable, submissions);
//    }

    public Page<MetadataSubmission> findAllByEntity(String uid, Pageable pageable) {
        if (SecurityUtils.hasCurrentUserAnyOfAuthorities(AuthoritiesConstants.ADMIN)) {
            Query query = new Query(Criteria.where("entityUid").is(uid));
            List<MetadataSubmission> submissions = mongoTemplate.find(query, MetadataSubmission.class);

            return getMetadataSubmissions(pageable, submissions);
        }

        return Page.empty(pageable);
    }

    @Override
    public Page<MetadataSubmission> findAllByUser(Pageable pageable, QueryRequest queryRequest) {
        if (SecurityUtils.hasCurrentUserAnyOfAuthorities(AuthoritiesConstants.ADMIN)) {
            return repository.findAll(pageable);
        }

        final List<OrgUnit> userOrgUnits = orgUnitRelationalRepositoryCustom
            .findAllWithEagerRelation();

        final Set<String> uids = userOrgUnits
            .stream()
            .map(OrgUnit::getUid)
            .collect(Collectors.toSet());

        if (SecurityUtils.getCurrentUserLogin().isPresent()) {
            String login = SecurityUtils.getCurrentUserLogin().get();
            Query query = new Query(Criteria.where("entityUid").in(uids));
            List<MetadataSubmission> submissions = mongoTemplate.find(query, MetadataSubmission.class);

            return getMetadataSubmissions(pageable, submissions);
        }
        return Page.empty(pageable);

    }

    private static Page<MetadataSubmission> getMetadataSubmissions(Pageable pageable, List<MetadataSubmission> submissions) {
        if (!pageable.isPaged()) {
            return new PageImpl<>(submissions);
        }

        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), submissions.size());
        if (start > end) {
            return Page.empty(pageable);
        }

        List<MetadataSubmission> sublist = submissions.subList(start, end);
        return new PageImpl<>(sublist, pageable, submissions.size());
    }

    /// temp solution
    public MetadataSubmission createSubmission(MetadataSubmission submission) {

        Map<String, Object> formData = submission.getFormData();

        // Automatically add group indices to any arrays of objects inside formData
        Map<String, Object> updatedFormData = addGroupIndicesToFormData(formData);
        submission.setFormData(updatedFormData);

        if (submission.getSerialNumber() == null) {
            // Generate a unique serial number for new submissions
            long serialNumber = sequenceGeneratorService.getNextSequence("metadataSubmissionId");
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
