package org.nmcpye.datarun.drun.mongo.repository;

import org.nmcpye.datarun.domain.Activity;
import org.nmcpye.datarun.drun.mongo.domain.DataField;
import org.nmcpye.datarun.drun.mongo.domain.MetadataSubmission;
import org.nmcpye.datarun.drun.mongo.domain.enumeration.MetadataResourceType;
import org.nmcpye.datarun.drun.postgres.domain.Assignment;
import org.nmcpye.datarun.drun.postgres.domain.Team;
import org.nmcpye.datarun.drun.postgres.repository.AssignmentRelationalRepositoryCustom;
import org.nmcpye.datarun.drun.postgres.repository.TeamRelationalRepositoryCustom;
import org.nmcpye.datarun.security.AuthoritiesConstants;
import org.nmcpye.datarun.security.SecurityUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Repository
@Transactional
public class MetadataSubmissionGranularRepository {

    final private MongoTemplate mongoTemplate;
    final private TeamRelationalRepositoryCustom teamRepository;
    final private MetadataSubmissionRepositoryCustom metadataSubmissionRepository;
    final private DataFormRepository dataFormRepository;
    private final AssignmentRelationalRepositoryCustom assignmentRepository;

    public MetadataSubmissionGranularRepository(MongoTemplate mongoTemplate, TeamRelationalRepositoryCustom teamRepository, MetadataSubmissionRepositoryCustom metadataSubmissionRepository,
                                                DataFormRepository dataFormRepository,
                                                AssignmentRelationalRepositoryCustom assignmentRepository) {
        this.mongoTemplate = mongoTemplate;
        this.teamRepository = teamRepository;
        this.metadataSubmissionRepository = metadataSubmissionRepository;
        this.dataFormRepository = dataFormRepository;
        this.assignmentRepository = assignmentRepository;
    }

    // uids of assigned assignment for resourceType == Assignment
    public List<Assignment> getAssignedAssignments() {
        var assignments = assignmentRepository
            .findAllByStatusUser(false);
        return assignments;

    }

    // uids of assigned orgUnits for resourceType == OrgUnit
    private List<String> getAssignedOrgUnits() {
        var assignedOrgs = getAssignedAssignments().stream()
            .map((assignment) -> assignment.getOrgUnit().getUid()).toList();
        return assignedOrgs;
    }

    // uids of assigned activities for resourceType == Activity
    private List<String> getAssignedActivities() {
        return getAssignedAssignments().stream()
            .map((assignment) -> assignment.getActivity().getUid())
            .distinct()
            .collect(Collectors.toList());
    }

    // uids of assigned teams for resourceType == Team
    private List<String> getAssignedTeams() {
        return teamRepository.findAllByUser().stream()
            .map(Team::getUid)
            .collect(Collectors.toList());
    }

    public List<DataField> getUserFieldsOfReferenceType() {
        if (SecurityUtils.hasCurrentUserAnyOfAuthorities(AuthoritiesConstants.ADMIN)) {
            return dataFormRepository.findAll()
                .stream()
                .flatMap(form -> form.getFlattenedFields().stream())
                .filter(DataField::ofReferenceType).toList();
        }

        var assignments = getAssignedAssignments();

        // Get a list of distinct activity UIDs
        List<String> activityUids = assignments.stream()
            .map(Assignment::getActivity)
            .map(Activity::getUid)
            .distinct()
            .toList();

        List<DataField> typeReferenceFields = activityUids.stream()
            .flatMap(uid -> dataFormRepository.findAllByActivity(uid).stream())
            .flatMap(form -> form.getFlattenedFields().stream())
            .filter(DataField::ofReferenceType).toList();

        return typeReferenceFields;
    }

    public Page<MetadataSubmission> getReferencedMetadataSubmissions(Pageable pageable) {
        Set<MetadataSubmission> metadataSubmissions = new HashSet<>();

        for (DataField field : getUserFieldsOfReferenceType()) {

            MetadataResourceType resourceType = field.getResourceType();
            String metadataSchema = field.getResourceMetadataSchema();
            List<String> resourceIds = getResourceUids(resourceType);
            List<MetadataSubmission> resourceMetadataSubmissions = resourceIds.stream()
                .flatMap(uid -> getMetadataSubmissionsForResource(resourceType.name(), metadataSchema, uid).stream())
                .toList();
            metadataSubmissions.addAll(resourceMetadataSubmissions);
        }

        return getMetadataSubmissions(pageable, metadataSubmissions.stream().toList());
    }

    private List<String> getResourceUids(MetadataResourceType resourceType) {
        List<String> uids = new ArrayList<>();
        switch (resourceType) {
            case Activity -> uids.addAll(getAssignedActivities());
            case Team -> uids.addAll(getAssignedTeams());
            case OrgUnit -> uids.addAll(getAssignedOrgUnits());
            case Assignment -> uids.addAll(getAssignedAssignments().stream().map(Assignment::getUid).toList());
        }
        return uids;
    }

    private List<MetadataSubmission> getMetadataSubmissionsForResource(String resourceType,
                                                                       String metadataSchema, String resourceId) {
        Query query = new Query(Criteria.where("resourceType")
            .is(resourceType)
            .and("metadataSchema").is(metadataSchema)
            .and("resourceId").is(resourceId));
        var subs = metadataSubmissionRepository.findAllByResourceTypeAndResourceIdAndAndMetadataSchema(resourceType, resourceId, metadataSchema, Pageable.unpaged());

        var submissions = mongoTemplate.find(query, MetadataSubmission.class);
        return mongoTemplate.find(query, MetadataSubmission.class);
    }

    private static <T> Page<T> getMetadataSubmissions(Pageable pageable, List<T> submissions) {
        if (!pageable.isPaged()) {
            return new PageImpl<>(submissions);
        }

        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), submissions.size());
        if (start > end) {
            return Page.empty(pageable);
        }

        List<T> sublist = submissions.subList(start, end);
        return new PageImpl<>(sublist, pageable, submissions.size());
    }
}
