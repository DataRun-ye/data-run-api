package org.nmcpye.datarun.drun.mongo.service.impl;

import jakarta.el.PropertyNotFoundException;
import org.nmcpye.datarun.domain.Activity;
import org.nmcpye.datarun.drun.mongo.domain.DataField;
import org.nmcpye.datarun.drun.mongo.domain.DataForm;
import org.nmcpye.datarun.drun.mongo.repository.DataFormRepositoryCustom;
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

    private final DataFormRepositoryCustom repositoryCustom;

    private final ActivityRelationalRepositoryCustom activityRepository;

    private final AssignmentRelationalRepositoryCustom assignmentRepository;

    private final OrgUnitRelationalRepositoryCustom orgUnitRepository;

    public DataFormServiceImpl(DataFormRepositoryCustom repositoryCustom, ActivityRelationalRepositoryCustom activityRepository, AssignmentRelationalRepositoryCustom assignmentRepository, OrgUnitRelationalRepositoryCustom orgUnitRepository) {
        super(repositoryCustom);
        this.repositoryCustom = repositoryCustom;
        this.activityRepository = activityRepository;
        this.assignmentRepository = assignmentRepository;
        this.orgUnitRepository = orgUnitRepository;
    }

    public void processFields(Set<DataField> fields, String parentPath) {
        for (DataField field : fields) {
            String currentPath = parentPath.isEmpty() ? field.getName() : parentPath + DataField.PATH_SEP + field.getName();
            field.setPath(currentPath);
            field.setSection(parentPath.isEmpty() ? null : parentPath);

            // Recursively process nested sections
            if (field.getType().isSectionType() && field.getFields() != null) {
                processFields(field.getFields(), currentPath);
            }
        }
    }

    @Override
    public DataForm saveWithRelations(DataForm dataForm) {
        processFields(dataForm.getFields(), "");
        Activity activity = activityRepository.findByUid(dataForm.getActivity())
            .orElseThrow(() -> new PropertyNotFoundException("Activity not found: " + dataForm.getActivity()));
        dataForm.setActivity(activity.getUid());

        Set<String> orgUnitUids = dataForm.getOrgUnits(); // Extract from JSON

        List<OrgUnit> validOrgUnits = orgUnitRepository.findAllByUidIn(orgUnitUids);

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
            Objects.requireNonNullElse(object.getVersion(), 0) + 1;
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
            .findAllByStatusAndUser(false, pageable)
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
                Set<String> filteredOrgUnits = dataForm.getOrgUnits()
                    .stream()
                    .filter((orgUnit) ->
                        orgUnitsUidsByActivity.get(dataForm.getActivity()).contains(orgUnit))
                    .collect(Collectors.toSet());
                dataForm.setOrgUnits(filteredOrgUnits);
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
