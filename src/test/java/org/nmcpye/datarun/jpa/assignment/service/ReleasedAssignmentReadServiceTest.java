package org.nmcpye.datarun.jpa.assignment.service;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.nmcpye.datarun.apiquery.JpaQueryBuilder;
import org.nmcpye.datarun.apiquery.LegacyQueryConverter;
import org.nmcpye.datarun.apiquery.QueryRequest;
import org.nmcpye.datarun.apiquery.filter.FilterOperator;
import org.nmcpye.datarun.apiquery.filter.SimpleFilter;
import org.nmcpye.datarun.assignmentshadow.AssignmentAuthorityCommandService;
import org.nmcpye.datarun.assignmentshadow.ReleasedWorkReadAuthority;
import org.nmcpye.datarun.assignmentshadow.ReleasedWorkReadScope;
import org.nmcpye.datarun.datatemplateprocessor.ReferenceAssignmentFormGate;
import org.nmcpye.datarun.jpa.accessfilter.UserAccessService;
import org.nmcpye.datarun.jpa.assignment.Assignment;
import org.nmcpye.datarun.jpa.assignment.dto.AssignmentWithAccessDto;
import org.nmcpye.datarun.jpa.assignment.mapper.AssignmentWithAccessMapper;
import org.nmcpye.datarun.jpa.assignment.repository.AssignmentRepository;
import org.nmcpye.datarun.security.CurrentUserDetails;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReleasedAssignmentReadServiceTest {

    private static final String ACTIVE_UID = "Asg00000001";
    private static final String DISPLAY_UID = "Asg00000002";

    private AssignmentRepository repository;
    private ReleasedWorkReadAuthority authority;
    private AssignmentWithAccessMapper mapper;
    private ReferenceAssignmentFormGate referenceGate;
    private CurrentUserDetails user;
    private DefaultAssignmentService service;
    private JpaQueryBuilder<Assignment> queryBuilder;
    private LegacyQueryConverter legacyQueryConverter;

    @BeforeEach
    void setUp() {
        repository = mock(AssignmentRepository.class);
        authority = mock(ReleasedWorkReadAuthority.class);
        mapper = mock(AssignmentWithAccessMapper.class);
        referenceGate = mock(ReferenceAssignmentFormGate.class);
        service = new DefaultAssignmentService(
            repository,
            mock(UserAccessService.class),
            mock(CacheManager.class),
            mock(AssignmentMaintenanceService.class),
            mapper,
            referenceGate,
            mock(AssignmentAuthorityCommandService.class),
            authority
        );
        queryBuilder = mock(JpaQueryBuilder.class);
        legacyQueryConverter = mock(LegacyQueryConverter.class);
        ReflectionTestUtils.setField(service, "jpaQueryBuilder", queryBuilder);
        ReflectionTestUtils.setField(
            service,
            "legacyQueryConverter",
            legacyQueryConverter
        );
        user = mock(CurrentUserDetails.class);
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(user, null)
        );
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void authorizedUidsAndClientFiltersAreCombinedBeforePaging() {
        ReleasedWorkReadScope scope = mock(ReleasedWorkReadScope.class);
        when(scope.administrator()).thenReturn(false);
        when(scope.assignmentUids()).thenReturn(Set.of(
            ACTIVE_UID,
            DISPLAY_UID
        ));
        when(authority.readAll(user)).thenReturn(scope);
        QueryRequest request = new QueryRequest().setPage(3).setSize(7);
        request.getFilters().put("code__eq", "client-code");
        when(legacyQueryConverter.convert(request)).thenReturn(
            new SimpleFilter("code", FilterOperator.EQ, "client-code")
        );
        Specification<Assignment> clientFilters = mock(Specification.class);
        when(queryBuilder.buildQuery(anyList())).thenReturn(clientFilters);
        when(repository.findAll(
            any(Specification.class),
            eq(request.getPageable())
        )).thenReturn(Page.empty(request.getPageable()));

        service.findAllReleasedWork(request, null);

        ArgumentCaptor<Specification<Assignment>> specification =
            ArgumentCaptor.forClass(Specification.class);
        verify(repository).findAll(
            specification.capture(),
            eq(request.getPageable())
        );
        Root<Assignment> root = mock(Root.class);
        CriteriaQuery query = mock(CriteriaQuery.class);
        CriteriaBuilder criteriaBuilder = mock(CriteriaBuilder.class);
        Path<String> uidPath = mock(Path.class);
        Predicate clientPredicate = mock(Predicate.class);
        Predicate authorityPredicate = mock(Predicate.class);
        when(root.<String>get("uid")).thenReturn(uidPath);
        when(uidPath.in(Set.of(ACTIVE_UID, DISPLAY_UID)))
            .thenReturn(authorityPredicate);
        when(clientFilters.toPredicate(root, query, criteriaBuilder))
            .thenReturn(clientPredicate);
        when(criteriaBuilder.and(clientPredicate, authorityPredicate))
            .thenReturn(mock(Predicate.class));

        specification.getValue().toPredicate(
            root,
            query,
            criteriaBuilder
        );

        verify(legacyQueryConverter).convert(request);
        verify(clientFilters).toPredicate(root, query, criteriaBuilder);
        verify(uidPath).in(Set.of(ACTIVE_UID, DISPLAY_UID));
    }

    @Test
    void assignmentListAndFormsUseSameAuthorizedAssignmentSet() {
        ReleasedWorkReadScope scope = mock(ReleasedWorkReadScope.class);
        when(scope.administrator()).thenReturn(false);
        when(scope.assignmentUids()).thenReturn(Set.of(
            ACTIVE_UID,
            DISPLAY_UID
        ));
        when(scope.formUids(ACTIVE_UID)).thenReturn(List.of("Frm00000001"));
        when(scope.formUids(DISPLAY_UID)).thenReturn(List.of());
        when(authority.readAll(user)).thenReturn(scope);
        when(queryBuilder.buildQuery(anyList())).thenReturn(
            Specification.where(null)
        );
        Assignment active = assignment(ACTIVE_UID);
        Assignment display = assignment(DISPLAY_UID);
        Page<Assignment> assignments = new PageImpl<>(List.of(active, display));
        when(repository.findAll(
            any(Specification.class),
            any(Pageable.class)
        ))
            .thenReturn(assignments);
        when(mapper.toDto(active, user, List.of("Frm00000001")))
            .thenReturn(new AssignmentWithAccessDto());
        when(mapper.toDto(display, user, List.of()))
            .thenReturn(new AssignmentWithAccessDto());
        QueryRequest request = new QueryRequest().setPaged(false);

        Page<Assignment> list = service.findAllReleasedWork(request, null);
        Page<AssignmentWithAccessDto> forms =
            service.getAllReleasedWorkDto(request, null, 1);

        assertThat(list.getContent()).extracting(Assignment::getUid)
            .containsExactly(ACTIVE_UID, DISPLAY_UID);
        assertThat(forms).hasSize(2);
        verify(mapper).toDto(active, user, List.of("Frm00000001"));
        verify(mapper).toDto(display, user, List.of());
        verify(referenceGate).filterUnsupportedForms(
            any(),
            eq(1)
        );
    }

    @Test
    void authorityFailureOccursBeforeClientFilteringAndPaging() {
        QueryRequest request = new QueryRequest().setPage(4).setSize(9);
        when(authority.readAll(user)).thenThrow(
            new org.nmcpye.datarun.assignmentshadow
                .AssignmentCaptureAuthorityUnavailableException()
        );

        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
            service.findAllReleasedWork(request, null)
        ).isInstanceOf(
            org.nmcpye.datarun.assignmentshadow
                .AssignmentCaptureAuthorityUnavailableException.class
        );

        verify(repository, never()).findAll(
            any(Specification.class),
            any(Pageable.class)
        );
        verify(legacyQueryConverter, never()).convert(any());
    }

    @Test
    void teamlessAbsentActorReturnsEmptyListAndForms() {
        when(user.getUserTeamsUIDs()).thenReturn(Set.of());
        ReleasedWorkReadScope scope =
            ReleasedWorkReadScope.actorAliasAbsentScope();
        when(authority.readAll(user)).thenReturn(scope);
        when(repository.findAll(
            any(Specification.class),
            any(Pageable.class)
        )).thenReturn(Page.empty());
        QueryRequest request = new QueryRequest().setPaged(false);

        Page<Assignment> assignments =
            service.findAllReleasedWork(request, null);
        Page<AssignmentWithAccessDto> forms =
            service.getAllReleasedWorkDto(request, null, 1);

        assertThat(assignments).isEmpty();
        assertThat(forms).isEmpty();
        verify(user, never()).getUserTeamsUIDs();
    }

    private Assignment assignment(String uid) {
        Assignment assignment = new Assignment();
        assignment.setUid(uid);
        assignment.setDeleted(false);
        return assignment;
    }
}
