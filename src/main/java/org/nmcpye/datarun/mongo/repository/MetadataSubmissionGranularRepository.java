package org.nmcpye.datarun.mongo.repository;

import org.nmcpye.datarun.drun.postgres.domain.Assignment;
import org.nmcpye.datarun.drun.postgres.domain.Team;
import org.nmcpye.datarun.drun.postgres.domain.TeamFormPermissions;
import org.nmcpye.datarun.drun.postgres.repository.AssignmentRepository;
import org.nmcpye.datarun.drun.postgres.service.TeamService;
import org.nmcpye.datarun.mongo.domain.MetadataSubmission;
import org.nmcpye.datarun.mongo.domain.datafield.AbstractField;
import org.nmcpye.datarun.mongo.domain.datafield.ReferenceField;
import org.nmcpye.datarun.mongo.domain.enumeration.ReferenceType;
import org.nmcpye.datarun.security.AuthoritiesConstants;
import org.nmcpye.datarun.security.SecurityUtils;
import org.nmcpye.datarun.web.rest.mongo.submission.QueryRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.stream.Collectors;

@Repository
public class MetadataSubmissionGranularRepository {

    final private MongoTemplate mongoTemplate;
    final private TeamService teamService;
    final private MetadataSubmissionRepository metadataSubmissionRepository;
    final private DataFormRepository dataFormRepository;
    private final AssignmentRepository assignmentRepository;

    public MetadataSubmissionGranularRepository(MongoTemplate mongoTemplate, TeamService teamService,
                                                MetadataSubmissionRepository metadataSubmissionRepository,
                                                DataFormRepository dataFormRepository,
                                                AssignmentRepository assignmentRepository) {
        this.mongoTemplate = mongoTemplate;
        this.teamService = teamService;
        this.metadataSubmissionRepository = metadataSubmissionRepository;
        this.dataFormRepository = dataFormRepository;
        this.assignmentRepository = assignmentRepository;
    }

    // uids of assigned assignment for resourceType == Assignment
    public List<Assignment> getAssignedAssignments() {
        var assignments = assignmentRepository.findAllByStatusUser(false);
        return assignments;

    }

    // uids of assigned orgUnits for resourceType == OrgUnit
    private List<String> getAssignedOrgUnits() {
        var assignedOrgs = getAssignedAssignments().stream().map((assignment) ->
                assignment.getOrgUnit().getUid())
            .toList();
        return assignedOrgs;
    }

    // uids of assigned activities for resourceType == Activity
    private List<String> getAssignedActivities() {
        return getAssignedAssignments().stream().map((assignment) ->
                assignment.getActivity().getUid())
            .distinct()
            .collect(Collectors.toList());
    }

    // uids of assigned teams for resourceType == Team
    private List<String> getAssignedTeams(QueryRequest queryRequest) {
        return teamService.findAllByUser(queryRequest).stream().map(Team::getUid).collect(Collectors.toList());
    }

    public List<ReferenceField> getUserFieldsOfResourceType() {
        if (SecurityUtils.hasCurrentUserAnyOfAuthorities(AuthoritiesConstants.ADMIN)) {
            return dataFormRepository.findAll().stream().flatMap(form -> form.getFlattenedFields()
                    .stream()).filter(AbstractField::isResourceTypeField)
                .map(ReferenceField.class::cast).toList();
        }

        var forms = getAssignedAssignments().stream().filter(assignment ->
                !assignment.getActivity().getDisabled()).map(Assignment::getTeam)
            .filter(team -> !team.getDisabled()).distinct()
            .flatMap(team -> Objects.requireNonNullElse(team.getFormPermissions(),
                new HashSet<TeamFormPermissions>()).stream()).map(TeamFormPermissions::getForm).distinct().toList();


        List<ReferenceField> typeReferenceFields = forms.stream().flatMap(uid ->
                dataFormRepository.findByUid(uid).stream())
            .flatMap(form -> form.flattenFields().stream())
            .filter(AbstractField::isResourceTypeField)
            .map(ReferenceField.class::cast)
            .toList();

        return typeReferenceFields;
    }

    public Page<MetadataSubmission> getReferencedMetadataSubmissions(QueryRequest queryRequest) {
        Pageable pageable = queryRequest.getPageable();
        Set<MetadataSubmission> metadataSubmissions = new HashSet<>();

        for (var field : getUserFieldsOfResourceType()) {

            ReferenceType resourceType = field.getResourceType();
            String metadataSchema = field.getResourceMetadataSchema();
            List<String> resourceIds = getResourceUids(resourceType, queryRequest);
            List<MetadataSubmission> resourceMetadataSubmissions = resourceIds.stream().flatMap(uid ->
                getMetadataSubmissionsForResource(resourceType.name(), metadataSchema, uid).stream()).toList();
            metadataSubmissions.addAll(resourceMetadataSubmissions);
        }

        return getMetadataSubmissions(pageable, metadataSubmissions.stream().toList());
    }

    private List<String> getResourceUids(ReferenceType resourceType, QueryRequest queryRequest) {
        List<String> uids = new ArrayList<>();
        switch (resourceType) {
            case Activity -> uids.addAll(getAssignedActivities());
            case Team -> uids.addAll(getAssignedTeams(queryRequest));
            case OrgUnit -> uids.addAll(getAssignedOrgUnits());
            case Assignment -> uids.addAll(getAssignedAssignments().stream().map(Assignment::getUid).toList());
        }
        return uids;
    }

    private List<MetadataSubmission> getMetadataSubmissionsForResource(String resourceType, String metadataSchema, String resourceId) {
        Query query = new Query(Criteria.where("resourceType").is(resourceType)
            .and("metadataSchema").is(metadataSchema).and("resourceId").is(resourceId));
        var subs = metadataSubmissionRepository.findAllByResourceTypeAndResourceIdAndAndMetadataSchema(resourceType,
            resourceId, metadataSchema, Pageable.unpaged());

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
