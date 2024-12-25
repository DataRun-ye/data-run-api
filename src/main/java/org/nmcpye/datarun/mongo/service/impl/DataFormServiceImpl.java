package org.nmcpye.datarun.mongo.service.impl;

import org.nmcpye.datarun.drun.postgres.domain.OrgUnit;
import org.nmcpye.datarun.drun.postgres.domain.Team;
import org.nmcpye.datarun.drun.postgres.domain.enumeration.FormPermission;
import org.nmcpye.datarun.drun.postgres.repository.ActivityRelationalRepositoryCustom;
import org.nmcpye.datarun.drun.postgres.repository.AssignmentRelationalRepositoryCustom;
import org.nmcpye.datarun.drun.postgres.repository.OrgUnitRelationalRepositoryCustom;
import org.nmcpye.datarun.drun.postgres.repository.TeamRelationalRepositoryCustom;
import org.nmcpye.datarun.mongo.domain.DataForm;
import org.nmcpye.datarun.mongo.domain.datafield.AbstractField;
import org.nmcpye.datarun.mongo.domain.datafield.ResourceField;
import org.nmcpye.datarun.mongo.domain.datafield.Section;
import org.nmcpye.datarun.mongo.repository.DataFormRepository;
import org.nmcpye.datarun.mongo.repository.MetadataSchemaRepository;
import org.nmcpye.datarun.mongo.service.DataFormService;
import org.nmcpye.datarun.repository.UserRepository;
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
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service Implementation for managing {@link DataForm}.
 */
@Service
@Primary
@Transactional
public class DataFormServiceImpl
    extends IdentifiableMongoServiceImpl<DataForm>
    implements DataFormService {

    private final Logger log = LoggerFactory.getLogger(DataFormServiceImpl.class);

    private final DataFormRepository repositoryCustom;

    private final MetadataSchemaRepository metadataSchemaRepository;

    private final ActivityRelationalRepositoryCustom activityRepository;

    private final AssignmentRelationalRepositoryCustom assignmentRepository;

    private final OrgUnitRelationalRepositoryCustom orgUnitRepository;

    private final TeamRelationalRepositoryCustom teamRepository;

    private final UserRepository userRepository;

    public DataFormServiceImpl(DataFormRepository repositoryCustom,
                               MetadataSchemaRepository metadataSchemaRepository,
                               ActivityRelationalRepositoryCustom activityRepository,
                               AssignmentRelationalRepositoryCustom assignmentRepository,
                               OrgUnitRelationalRepositoryCustom orgUnitRepository,
                               MongoTemplate mongoTemplate,
                               UserRepository userRepository,
                               TeamRelationalRepositoryCustom teamRepository) {
        super(repositoryCustom, mongoTemplate);
        this.repositoryCustom = repositoryCustom;
        this.metadataSchemaRepository = metadataSchemaRepository;
        this.activityRepository = activityRepository;
        this.assignmentRepository = assignmentRepository;
        this.orgUnitRepository = orgUnitRepository;
        this.teamRepository = teamRepository;
        this.userRepository = userRepository;
    }

    public <T extends AbstractField> void processFields(List<T> fields, String parentPath) {
        for (AbstractField field : fields) {
            String currentPath = parentPath.isEmpty() ? field.getName() : parentPath + AbstractField.PATH_SEP + field.getName();
            field.setPath(currentPath);
            if (field instanceof ResourceField resourceField) {
                if (resourceField.getResourceType() == null || resourceField.getResourceMetadataSchema() == null) {
                    throw new IllegalArgumentException(field.getName() + ": is of Reference type but does not specify Resource Type [OrgUnit, Team, Activity...etc] or ResourceMetadataSchema (The form used to submit the metadata of the reference type)");
                }

                if (metadataSchemaRepository.findByUid(resourceField.getResourceMetadataSchema()).isEmpty()) {
                    throw new IllegalArgumentException("Field: " + field.getName() + ": Specified ResourceMetadataSchema " + resourceField.getResourceMetadataSchema() + " does not exist");
                }

            }
            // Recursively process nested sections
            if (field instanceof Section section && section.getFields() != null) {
                processFields(section.getFields(), currentPath);
            }
        }
    }

    @Override
    public DataForm saveWithRelations(DataForm dataForm) {
        processFields(dataForm.getFields(), "");
        dataForm.updateFlattenedFields();

//        Activity activity = activityRepository.findByUid(dataForm.getActivity())
//            .orElseThrow(() -> new PropertyNotFoundException("Activity not found: " + dataForm.getActivity()));
//        dataForm.setActivity(activity.getUid());

        Set<String> orgUnitUids = dataForm.getOrgUnits(); // Extract from JSON

        Set<OrgUnit> validOrgUnits = orgUnitRepository.findAllByUidIn(orgUnitUids);

        Set<String> foundOrgUnitUids = validOrgUnits.stream()
            .map(OrgUnit::getUid)
            .collect(Collectors.toSet());

        Set<String> missingOrgUnitUids = new HashSet<>(orgUnitUids);
        missingOrgUnitUids.removeAll(foundOrgUnitUids);

        if (!missingOrgUnitUids.isEmpty()) {
//            throw new MissingFormOrgUnitUidsException(
//                dataForm.getUid() + ',' + dataForm.getName(),
//                missingOrgUnitUids);
            dataForm.setOrgUnits(foundOrgUnitUids);
        }

        return repositoryCustom.save(dataForm);
    }

    @Override
    public DataForm save(DataForm object) {
        final Integer version =
            Objects.requireNonNullElse(object.getVersion(), 0) + 1;
        object.setVersion(version);
        return super.save(object);
    }

    @Override
    public DataForm update(DataForm object) {
        final Integer version =
            Objects.requireNonNullElse(repositoryCustom
                .findByUid(object.getUid())
                .get().getVersion(), 0) + 1;
        object.setVersion(version);

        return super.update(object);
    }

//    @Override
//    public List<DataForm> getAccessibleForms(String teamId, Permission permission) {
//        // Fetch form template IDs for which the team has the specified permission
//        List<String> formTemplateIds = permissionRepository.findFormsByTeamAndPermission(teamId, permission.getName());
//
//        // Fetch and return the full form templates
//        return repositoryCustom.findAllById(formTemplateIds);
//    }


    @Override
    public Page<DataForm> getAccessibleForms(Pageable pageable) {
        // Fetch form template IDs for which the team has the specified permission
        List<Long> userTeams = teamRepository
            .findAllWithEagerRelation()
            .stream().map(Team::getId)
            .toList();

        Set<String> formTemplateIds = teamRepository
            .findAllWithEagerRelation().stream().flatMap((team) -> team.getFormsWithPermission(
                FormPermission.ADD_SUBMISSIONS).stream()).collect(Collectors.toSet());

        var dataForms = repositoryCustom.findAllByUidIn(formTemplateIds.stream().toList());

        if (!pageable.isPaged()) {
            return new PageImpl<>(dataForms);
        }

        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), dataForms.size());
        if (start > end) {
            return Page.empty(pageable);
        }

        List<DataForm> sublist = dataForms.subList(start, end);
        return new PageImpl<>(sublist, pageable, dataForms.size());
    }


    @Override
    public Page<DataForm> findAllByUser(Pageable pageable, QueryRequest queryRequest) {

        if (!SecurityUtils.isAuthenticated()) {
            return Page.empty(pageable);
        }

        if (SecurityUtils.hasCurrentUserAnyOfAuthorities(AuthoritiesConstants.ADMIN)) {
            return repositoryCustom.findAll(pageable);
        }

        Set<String> userForms = teamRepository
            .findAllWithEagerRelation().stream().flatMap((team) -> team.getFormsWithPermission(
                FormPermission.ADD_SUBMISSIONS).stream()).collect(Collectors.toSet());

        Query query = new Query();
        query = query.addCriteria(Criteria.where("uid").in(userForms));

        if (!queryRequest.isIncludeDeleted()) {
            query.addCriteria(Criteria.where("deleted").is(false));
        }

        query.with(pageable);

        final Query totalQuery = Query.of(query).limit(-1).skip(-1);

        List<DataForm> results = mongoTemplate.find(query, DataForm.class);

        Page<DataForm> resultsPage = PageableExecutionUtils.getPage(
            results,
            pageable,
            () -> mongoTemplate.count(totalQuery, DataForm.class));

        return resultsPage;
    }
}
