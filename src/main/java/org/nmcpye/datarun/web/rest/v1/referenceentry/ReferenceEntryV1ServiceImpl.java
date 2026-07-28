package org.nmcpye.datarun.web.rest.v1.referenceentry;

import lombok.RequiredArgsConstructor;
import org.nmcpye.datarun.assignmentshadow.AssignmentCaptureShadowComparator;
import org.nmcpye.datarun.common.exceptions.IllegalQueryException;
import org.nmcpye.datarun.common.feedback.ErrorCode;
import org.nmcpye.datarun.datatemplateprocessor.ReferenceTemplateCapabilityService;
import org.nmcpye.datarun.jpa.accessfilter.AssignmentFormAccessService;
import org.nmcpye.datarun.jpa.assignment.Assignment;
import org.nmcpye.datarun.jpa.assignment.service.AssignmentService;
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
    private final AssignmentCaptureShadowComparator captureShadow;

    @Override
    public PagedResponse<ReferenceEntryV1Dto> getForAssignment(
        String assignmentUid,
        QueryRequest queryRequest) {
        Assignment assignment = assignmentService.findAccessibleByIdOrUid(assignmentUid)
            .orElseThrow(() -> new AccessDeniedException("Assignment is not accessible"));
        Set<String> permittedReferenceForms =
            assertReferenceSubmissionAccess(assignment);
        captureShadow.compareReferenceCatalog(
            SecurityUtils.getCurrentUserDetailsOrThrow(),
            assignment,
            permittedReferenceForms
        );

        if (assignment.getOrgUnit() == null) {
            throw new IllegalQueryException(
                ErrorCode.E1199,
                "Assignment has no organization unit");
        }

        int pageNumber = Math.max(0, queryRequest.getPage());
        int pageSize = Math.min(
            MAX_PAGE_SIZE,
            Math.max(1, queryRequest.getSize()));
        PageRequest pageable = PageRequest.of(
            pageNumber,
            pageSize,
            Sort.by(Sort.Direction.ASC, "uid"));

        Page<ReferenceEntryV1Dto> page = referenceEntryRepository
            .findAllByOrgUnitIdOrderByUidAsc(
                assignment.getOrgUnit().getId(),
                pageable)
            .map(this::toDto);
        String next = PagingConfigurator.createNextPageLink(page);
        return PagingConfigurator.initPageResponse(
            page,
            next,
            "referenceEntries");
    }

    private Set<String> assertReferenceSubmissionAccess(Assignment assignment) {
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
        return permittedForms;
    }

    private ReferenceEntryV1Dto toDto(ReferenceEntry entry) {
        return ReferenceEntryV1Dto.builder()
            .uid(entry.getUid())
            .name(entry.getDisplayName())
            .orgUnitUid(entry.getOrgUnit().getUid())
            .build();
    }
}
