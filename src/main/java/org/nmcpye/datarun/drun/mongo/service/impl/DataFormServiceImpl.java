package org.nmcpye.datarun.drun.mongo.service.impl;

import jakarta.el.PropertyNotFoundException;
import org.nmcpye.datarun.domain.Activity;
import org.nmcpye.datarun.drun.mongo.domain.DataForm;
import org.nmcpye.datarun.drun.mongo.domain.datafield.AbstractField;
import org.nmcpye.datarun.drun.mongo.domain.datafield.ResourceField;
import org.nmcpye.datarun.drun.mongo.domain.datafield.Section;
import org.nmcpye.datarun.drun.mongo.repository.DataFormRepository;
import org.nmcpye.datarun.drun.mongo.repository.MetadataSchemaRepository;
import org.nmcpye.datarun.drun.mongo.service.DataFormService;
import org.nmcpye.datarun.drun.postgres.domain.Assignment;
import org.nmcpye.datarun.drun.postgres.domain.OrgUnit;
import org.nmcpye.datarun.drun.postgres.repository.ActivityRelationalRepositoryCustom;
import org.nmcpye.datarun.drun.postgres.repository.AssignmentRelationalRepositoryCustom;
import org.nmcpye.datarun.drun.postgres.repository.OrgUnitRelationalRepositoryCustom;
import org.nmcpye.datarun.security.AuthoritiesConstants;
import org.nmcpye.datarun.security.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
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

    public DataFormServiceImpl(DataFormRepository repositoryCustom, MetadataSchemaRepository metadataSchemaRepository, ActivityRelationalRepositoryCustom activityRepository, AssignmentRelationalRepositoryCustom assignmentRepository, OrgUnitRelationalRepositoryCustom orgUnitRepository) {
        super(repositoryCustom);
        this.repositoryCustom = repositoryCustom;
        this.metadataSchemaRepository = metadataSchemaRepository;
        this.activityRepository = activityRepository;
        this.assignmentRepository = assignmentRepository;
        this.orgUnitRepository = orgUnitRepository;
    }

    public <T extends AbstractField> void processFields(List<T> fields, String parentPath) {
        for (AbstractField field : fields) {
            String currentPath = parentPath.isEmpty() ? field.getName() : parentPath + AbstractField.PATH_SEP + field.getName();
            field.setPath(currentPath);
//            field.setSection(parentPath.isEmpty() ? null : parentPath);
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

        Activity activity = activityRepository.findByUid(dataForm.getActivity())
            .orElseThrow(() -> new PropertyNotFoundException("Activity not found: " + dataForm.getActivity()));
        dataForm.setActivity(activity.getUid());

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

    @Override
    public List<DataForm> findAllByActivity(String activity) {
        return repositoryCustom.findAllByActivity(activity);
    }


    @Override
    public Page<DataForm> findAllByUser(Pageable pageable) {
        if (SecurityUtils.hasCurrentUserAnyOfAuthorities(AuthoritiesConstants.ADMIN)) {
            return repositoryCustom.findAll(pageable);
        }

        List<Assignment> assignments = assignmentRepository
            .findAllByStatusUser(false)
            .stream()
            .toList();

        var orgUnitsUidsByActivity = assignments.stream()
            .filter((assignment) -> Objects.nonNull(assignment.getOrgUnit()))
            .collect(Collectors
                .groupingBy((assignment) -> assignment.getActivity().getUid(),
                    Collectors.mapping((assignment) -> assignment.getOrgUnit().getUid(),
                        Collectors.toSet())));

        List<String> activities = assignments
            .stream()
            .map(Assignment::getActivity)
            .map(Activity::getUid).distinct()
            .toList();

        List<DataForm> dataForms = activities.stream()
            .flatMap(uid -> repositoryCustom.findAllByActivity(uid).stream())
            .filter(Objects::nonNull)
            .peek(dataForm -> {
//                Set<String> filteredOrgUnits =
//                    dataForm.getOrgUnits()
//                    .stream()
//                    .filter((orgUnit) ->
//                        orgUnitsUidsByActivity.get(dataForm.getActivity()).contains(orgUnit))
//                    .collect(Collectors.toSet());
                dataForm.setOrgUnits(orgUnitsUidsByActivity.get(dataForm.getActivity()));
            })
            .collect(Collectors.toList());

        if (!pageable.isPaged()) {
            return new PageImpl<>(dataForms);
        }

        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), dataForms.size());
        if (start > end) {
            return Page.empty(pageable);
        }

        List<DataForm> sublist = dataForms.subList(start, end);
        var pp = new PageImpl<>(sublist, pageable, dataForms.size());
        return new PageImpl<>(sublist, pageable, dataForms.size());
    }


//    private List<String> getAssignedOrgUnitsByFormActivity(String activity) {
//        return assignmentRepository
//            .findAllByActivityAndUser(activity, Pageable.unpaged())
//            .stream()
//            .map(Assignment::getOrgUnit)
//            .map(OrgUnit::getUid)
//            .toList();
//    }
}
