package org.nmcpye.datarun.mongo.metadataschema.repository;

import lombok.RequiredArgsConstructor;
import org.nmcpye.datarun.datatemplateelement.FormDataElementConf;
import org.nmcpye.datarun.datatemplateelement.enumeration.ReferenceType;
import org.nmcpye.datarun.jpa.assignment.Assignment;
import org.nmcpye.datarun.jpa.assignment.repository.AssignmentRepository;
import org.nmcpye.datarun.jpa.datatemplate.DataTemplate;
import org.nmcpye.datarun.jpa.datatemplate.repository.DataTemplateRepository;
import org.nmcpye.datarun.jpa.datatemplate.service.DataTemplateInstanceService;
import org.nmcpye.datarun.jpa.team.Team;
import org.nmcpye.datarun.jpa.team.TeamFormPermissions;
import org.nmcpye.datarun.jpa.team.service.TeamService;
import org.nmcpye.datarun.mongo.domain.MetadataSubmission;
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

@SuppressWarnings("ALL")
@Repository
@RequiredArgsConstructor
public class MetadataSubmissionGranularRepository {

    private final MongoTemplate mongoTemplate;
    private final TeamService teamService;
    private final DataTemplateInstanceService templateInstanceService;
    private final AssignmentRepository assignmentRepository;
    private final DataTemplateRepository templateRepository;

    // uids of assigned assignment for resourceType == Assignment
    public List<Assignment> getAssignedAssignments() {
        return assignmentRepository.findAllByStatusUser(false);
    }

    // uids of assigned orgUnits for resourceType == OrgUnit
    private List<String> getAssignedOrgUnits() {
        return getAssignedAssignments().stream().map((assignment) ->
                assignment.getOrgUnit().getUid())
            .toList();
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
        return teamService.findAllByUser(queryRequest, null).stream().map(Team::getUid).collect(Collectors.toList());
    }

    public List<FormDataElementConf> getUserFieldsOfResourceType() {
        if (SecurityUtils.hasCurrentUserAnyOfAuthorities(AuthoritiesConstants.ADMIN)) {

            final var allForms = templateRepository.findAll().stream().map(DataTemplate::getUid).toList();
            return templateInstanceService.findAllByUidIn(allForms).stream().flatMap(form -> form.getFields()
                    .stream()).filter((field) -> field.getType().isReference())
//                .map((field) -> {
//                    final var refField = new ReferenceField();
//                    refField.setResourceType(field.getResourceType());
//                    refField.setResourceMetadataSchema(field.getResourceMetadataSchema());
//                    refField.setType(field.getType());
//                    refField.setName(field.getName());
//                    refField.setMandatory(field.getMandatory());
//                    refField.setConstraint(field.getConstraint());
//                    refField.setConstraintMessage(field.getConstraintMessage());
//                    refField.setLabel(field.getLabel());
//                    refField.setMainField(field.getMainField());
//                    refField.setRules(field.getRules());
//                    refField.setDescription(field.getDescription());
//                    refField.setDefaultValue(field.getDefaultValue());
//                    return refField;
//                })
                .toList();
        }

        var forms = getAssignedAssignments().stream().filter(assignment ->
                !assignment.getActivity().getDisabled()).map(Assignment::getTeam)
            .filter(team -> !team.getDisabled()).distinct()
            .flatMap(team -> Objects.requireNonNullElse(team.getFormPermissions(),
                new HashSet<TeamFormPermissions>()).stream()).map(TeamFormPermissions::getForm).distinct().toList();


        return forms.stream().flatMap(uid ->
                templateInstanceService.findByUid(uid).stream())
            .flatMap(form -> form.getFields().stream())
            .filter((field) -> field.getType().isReference())
            .toList();
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
//        var subs = metadataSubmissionRepository.findAllByResourceTypeAndResourceIdAndAndMetadataSchema(resourceType,
//            resourceId, metadataSchema, Pageable.unpaged());

//        var submissions = mongoTemplate.find(query, MetadataSubmission.class);
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
