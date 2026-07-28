package org.nmcpye.datarun.web.rest.v1.referenceentry;

import lombok.RequiredArgsConstructor;
import org.nmcpye.datarun.assignmentshadow.AssignmentCaptureAuthorityUnavailableException;
import org.nmcpye.datarun.assignmentshadow.AssignmentCaptureEventGrant;
import org.nmcpye.datarun.assignmentshadow.ReleasedWorkReadAuthority;
import org.nmcpye.datarun.assignmentshadow.ReleasedWorkReadScope;
import org.nmcpye.datarun.common.exceptions.IllegalQueryException;
import org.nmcpye.datarun.common.feedback.ErrorCode;
import org.nmcpye.datarun.datatemplateprocessor.ReferenceTemplateCapabilityService;
import org.nmcpye.datarun.jpa.accessfilter.AssignmentFormAccessService;
import org.nmcpye.datarun.jpa.assignment.Assignment;
import org.nmcpye.datarun.jpa.assignment.service.AssignmentService;
import org.nmcpye.datarun.jpa.orgunit.OrgUnit;
import org.nmcpye.datarun.jpa.orgunit.repository.OrgUnitRepository;
import org.nmcpye.datarun.jpa.reference.ReferenceEntry;
import org.nmcpye.datarun.jpa.reference.ReferenceEntryRepository;
import org.nmcpye.datarun.security.SecurityUtils;
import org.nmcpye.datarun.web.rest.common.PagedResponse;
import org.nmcpye.datarun.apiquery.QueryRequest;
import org.nmcpye.datarun.web.rest.v1.paging.PagingConfigurator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Set;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ReferenceEntryV1ServiceImpl implements ReferenceEntryV1Service {

    private static final int MAX_PAGE_SIZE = 500;

    private final AssignmentService assignmentService;
    private final ReferenceTemplateCapabilityService capabilityService;
    private final AssignmentFormAccessService formAccessService;
    private final ReferenceEntryRepository referenceEntryRepository;
    private final OrgUnitRepository orgUnitRepository;
    private final ReleasedWorkReadAuthority releasedWorkAuthority;

    @Override
    public PagedResponse<ReferenceEntryV1Dto> getForAssignment(
        String assignmentUid,
        QueryRequest queryRequest) {
        var user = SecurityUtils.getCurrentUserDetailsOrThrow();
        if (user.isSuper()) {
            return getBaselineForAssignment(assignmentUid, queryRequest);
        }

        ReleasedWorkReadScope scope = releasedWorkAuthority.readAssignments(
            user,
            Set.of(assignmentUid)
        );
        if (scope.actorAliasAbsent()) {
            return catalogResponse(Page.empty(pageable(queryRequest)));
        }
        AssignmentCaptureEventGrant grant = scope.activeGrant(assignmentUid)
            .orElseThrow(() ->
                new AccessDeniedException("Assignment is not accessible")
            );
        assignmentService.findByIdOrUid(grant.baselineAssignmentUid())
            .orElseThrow(AssignmentCaptureAuthorityUnavailableException::new);
        OrgUnit catalogOrgUnit = releasedCatalogOrgUnit(grant);
        return catalogResponse(catalogOrgUnit, queryRequest);
    }

    @Override
    public PagedResponse<ReferenceEntryV1Dto> getBaselineForAssignment(
        String assignmentUid,
        QueryRequest queryRequest
    ) {
        Assignment assignment = assignmentService
            .findAccessibleByIdOrUid(assignmentUid)
            .orElseThrow(() ->
                new AccessDeniedException("Assignment is not accessible")
            );
        OrgUnit catalogOrgUnit = baselineCatalogOrgUnit(assignment);
        if (catalogOrgUnit == null) {
            throw new IllegalQueryException(
                ErrorCode.E1199,
                "Assignment has no organization unit");
        }
        return catalogResponse(catalogOrgUnit, queryRequest);
    }

    private PagedResponse<ReferenceEntryV1Dto> catalogResponse(
        OrgUnit catalogOrgUnit,
        QueryRequest queryRequest
    ) {
        Page<ReferenceEntryV1Dto> page = referenceEntryRepository
            .findAllByOrgUnitIdOrderByUidAsc(
                catalogOrgUnit.getId(),
                pageable(queryRequest))
            .map(this::toDto);
        return catalogResponse(page);
    }

    private PagedResponse<ReferenceEntryV1Dto> catalogResponse(
        Page<ReferenceEntryV1Dto> page
    ) {
        String next = PagingConfigurator.createNextPageLink(page);
        return PagingConfigurator.initPageResponse(
            page,
            next,
            "referenceEntries");
    }

    private PageRequest pageable(QueryRequest queryRequest) {
        int pageNumber = Math.max(0, queryRequest.getPage());
        int pageSize = Math.min(
            MAX_PAGE_SIZE,
            Math.max(1, queryRequest.getSize())
        );
        return PageRequest.of(
            pageNumber,
            pageSize,
            Sort.by(Sort.Direction.ASC, "uid")
        );
    }

    private OrgUnit baselineCatalogOrgUnit(Assignment assignment) {
        Set<String> referenceForms = capabilityService.findReferenceTemplateUids(
            Optional.ofNullable(assignment.getForms()).orElse(Set.of()));
        var user = SecurityUtils.getCurrentUserDetailsOrThrow();
        Set<String> permittedForms = referenceForms.stream()
            .filter(form -> formAccessService.canAddSubmissions(
                user,
                assignment,
                form))
            .collect(java.util.stream.Collectors.toSet());
        if (permittedForms.isEmpty()) {
            throw new AccessDeniedException(
                "No Reference form is available for submission");
        }
        return assignment.getOrgUnit();
    }

    private OrgUnit releasedCatalogOrgUnit(
        AssignmentCaptureEventGrant grant
    ) {
        Set<String> referenceForms = capabilityService
            .findReferenceTemplateUids(grant.formUids());
        if (referenceForms.isEmpty()) {
            throw new AccessDeniedException(
                "No Reference form is available for submission");
        }
        return orgUnitRepository.findByUid(grant.baselineOrgUnitUid())
            .orElseThrow(AssignmentCaptureAuthorityUnavailableException::new);
    }

    private ReferenceEntryV1Dto toDto(ReferenceEntry entry) {
        return ReferenceEntryV1Dto.builder()
            .uid(entry.getUid())
            .name(entry.getDisplayName())
            .orgUnitUid(entry.getOrgUnit().getUid())
            .build();
    }
}
