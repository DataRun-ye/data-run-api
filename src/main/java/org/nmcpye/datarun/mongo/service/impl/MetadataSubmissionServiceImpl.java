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

import java.util.List;
import java.util.Set;
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
//        final MetadataSubmission metadataSubmission = createSubmission(newSubmission);

        switch (newSubmission.getResourceType()) {
            case Team -> teamRepository.findByUid(newSubmission.getResourceId())
                .ifPresentOrElse((a) -> newSubmission.setResourceId(a.getUid()),
                    () -> {
                        throw new PropertyNotFoundException("Team not found: " + newSubmission.getResourceId());
                    });
            case Activity -> activityRepository.findByUid(newSubmission.getResourceId())
                .ifPresentOrElse((a) -> newSubmission.setResourceId(a.getUid()),
                    () -> {
                        throw new PropertyNotFoundException("Activity not found: " + newSubmission.getResourceId());
                    });
            case OrgUnit -> orgUnitRelationalRepositoryCustom.findByUid(newSubmission.getResourceId())
                .ifPresentOrElse((a) -> newSubmission.setResourceId(a.getUid()),
                    () -> {
                        throw new PropertyNotFoundException("OrgUnit not found: " + newSubmission.getResourceId());
                    });

            case Assignment -> assignmentRepository.findByUid(newSubmission.getResourceId())
                .ifPresentOrElse((a) -> newSubmission.setResourceId(a.getUid()),
                    () -> {
                        throw new PropertyNotFoundException("Assignment not found: " + newSubmission.getResourceId());
                    });
            default ->
                throw new NotImplementedException(newSubmission.getResourceType().toString() + "not Implemented");
        }

        if (newSubmission.getSerialNumber() == null) {
            // Generate a unique serial number for new submissions
            long serialNumber = sequenceGeneratorService.getNextSequence("metadataSubmissionId");
            newSubmission.setSerialNumber(serialNumber);
        }

        final MetadataSubmission dataFormSubmission = newSubmission
            .createSubmission()
            .populateFormDataAttributes();

        return repository.save(dataFormSubmission);
    }

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
            .findAllWithRelation();

        final Set<String> uids = userOrgUnits
            .stream()
            .map(OrgUnit::getUid)
            .collect(Collectors.toSet());

        if (SecurityUtils.getCurrentUserLogin().isPresent()) {
//            String login = SecurityUtils.getCurrentUserLogin().get();
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
}
