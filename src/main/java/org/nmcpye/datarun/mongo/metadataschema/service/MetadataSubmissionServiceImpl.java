package org.nmcpye.datarun.mongo.metadataschema.service;

import jakarta.el.PropertyNotFoundException;
import org.apache.commons.lang3.NotImplementedException;
import org.nmcpye.datarun.jpa.activity.repository.ActivityRepository;
import org.nmcpye.datarun.jpa.assignment.repository.AssignmentRepository;
import org.nmcpye.datarun.jpa.orgunit.OrgUnit;
import org.nmcpye.datarun.jpa.orgunit.repository.OrgUnitRepository;
import org.nmcpye.datarun.jpa.team.repository.TeamRepository;
import org.nmcpye.datarun.mongo.common.DefaultMongoIdentifiableObjectService;
import org.nmcpye.datarun.mongo.domain.MetadataSubmission;
import org.nmcpye.datarun.mongo.metadataschema.repository.MetadataSubmissionRepository;
import org.nmcpye.datarun.mongo.service.impl.SequenceGeneratorService;
import org.nmcpye.datarun.security.AuthoritiesConstants;
import org.nmcpye.datarun.security.SecurityUtils;
import org.nmcpye.datarun.web.rest.mongo.submission.QueryRequest;
import org.springframework.cache.CacheManager;
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
    extends DefaultMongoIdentifiableObjectService<MetadataSubmission>
    implements MetadataSubmissionService {

    private final MongoTemplate mongoTemplate;
    private final MetadataSubmissionRepository repository;
    private final TeamRepository teamRepository;
    private final ActivityRepository activityRepository;
    private final AssignmentRepository assignmentRepository;

    private final OrgUnitRepository orgUnitRepository;
    private final SequenceGeneratorService sequenceGeneratorService;

    public MetadataSubmissionServiceImpl(
        MetadataSubmissionRepository repository,
        CacheManager cacheManager, MongoTemplate mongoTemplate,
        ActivityRepository activityRepository, TeamRepository teamRepository, AssignmentRepository assignmentRepository,
        OrgUnitRepository orgUnitRepository,
        SequenceGeneratorService sequenceGeneratorService) {
        super(repository, cacheManager);
        this.repository = repository;
        this.mongoTemplate = mongoTemplate;
        this.activityRepository = activityRepository;
        this.teamRepository = teamRepository;
        this.assignmentRepository = assignmentRepository;
        this.orgUnitRepository = orgUnitRepository;
        this.sequenceGeneratorService = sequenceGeneratorService;
    }


    @Override
    public MetadataSubmission saveWithRelations(MetadataSubmission newSubmission) {
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
            case OrgUnit -> orgUnitRepository.findByUid(newSubmission.getResourceId())
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

        return save(dataFormSubmission);
    }

    @Override
    public Page<MetadataSubmission> findAllByUser(QueryRequest queryRequest, String jsonQueryBody) {
        Pageable pageable = queryRequest.getPageable();
        if (SecurityUtils.hasCurrentUserAnyOfAuthorities(AuthoritiesConstants.ADMIN)) {
            return repository.findAll(pageable);
        }

        final List<OrgUnit> userOrgUnits = orgUnitRepository
            .findAllWithRelation();

        final Set<String> uids = userOrgUnits
            .stream()
            .map(OrgUnit::getUid)
            .collect(Collectors.toSet());

        if (SecurityUtils.getCurrentUserLogin().isPresent()) {
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
