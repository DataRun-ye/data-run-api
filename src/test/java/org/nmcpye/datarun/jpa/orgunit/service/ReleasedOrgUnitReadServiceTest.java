package org.nmcpye.datarun.jpa.orgunit.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.nmcpye.datarun.apiquery.QueryRequest;
import org.nmcpye.datarun.assignmentshadow.AssignmentCaptureAuthorityUnavailableException;
import org.nmcpye.datarun.assignmentshadow.ReleasedWorkReadAuthority;
import org.nmcpye.datarun.assignmentshadow.ReleasedWorkReadScope;
import org.nmcpye.datarun.jpa.accessfilter.UserAccessService;
import org.nmcpye.datarun.jpa.orgunit.OrgUnit;
import org.nmcpye.datarun.jpa.orgunit.repository.OrgUnitRepository;
import org.nmcpye.datarun.security.CurrentUserDetails;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReleasedOrgUnitReadServiceTest {

    private OrgUnitRepository repository;
    private ReleasedWorkReadAuthority authority;
    private CurrentUserDetails user;
    private DefaultOrgUnitService service;

    @BeforeEach
    void setUp() {
        repository = mock(OrgUnitRepository.class);
        authority = mock(ReleasedWorkReadAuthority.class);
        user = mock(CurrentUserDetails.class);
        service = new DefaultOrgUnitService(
            repository,
            mock(UserAccessService.class),
            mock(CacheManager.class),
            mock(OrgUnitMaintenanceService.class),
            authority
        );
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(user, null)
        );
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void teamlessAbsentActorReturnsEmptyWithoutLegacyTeamScope() {
        when(user.getUserTeamsUIDs()).thenReturn(Set.of());
        when(authority.readAll(user)).thenReturn(
            ReleasedWorkReadScope.actorAliasAbsentScope()
        );
        when(repository.findAll(
            any(Specification.class),
            any(Pageable.class)
        )).thenReturn(Page.empty());

        Page<OrgUnit> result = service.findAllReleasedWork(
            new QueryRequest().setPaged(false),
            null
        );

        assertThat(result).isEmpty();
        verify(user, never()).getUserTeamsUIDs();
        verify(repository, never()).findAllByUidIn(any());
    }

    @Test
    void unavailableAuthorityFailsBeforeOrgUnitQuery() {
        when(authority.readAll(user)).thenThrow(
            new AssignmentCaptureAuthorityUnavailableException()
        );

        assertThatThrownBy(() -> service.findAllReleasedWork(
            new QueryRequest(),
            null
        )).isInstanceOf(
            AssignmentCaptureAuthorityUnavailableException.class
        );

        verify(repository, never()).findAll(
            any(Specification.class),
            any(Pageable.class)
        );
    }

    @Test
    void missingDirectOrgUnitProjectionFailsBeforePaging() {
        ReleasedWorkReadScope scope = mock(ReleasedWorkReadScope.class);
        when(scope.administrator()).thenReturn(false);
        when(scope.directOrgUnitUids()).thenReturn(Set.of("Org00000001"));
        when(authority.readAll(user)).thenReturn(scope);
        when(repository.findAllByUidIn(Set.of("Org00000001")))
            .thenReturn(List.of());

        assertThatThrownBy(() -> service.findAllReleasedWork(
            new QueryRequest().setPage(3).setSize(7),
            null
        )).isInstanceOf(
            AssignmentCaptureAuthorityUnavailableException.class
        );

        verify(repository, never()).findAll(
            any(Specification.class),
            any(Pageable.class)
        );
    }
}
